package jp.igapyon.mikuscore.musicxml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jp.igapyon.mikuscore.core.StaffClefPolicy;

public final class MusicXmlState {
    private MusicXmlState() {
    }

    public static MusicXmlStateSummary summarizeMusicXmlState(String xmlText) {
        Document doc = parseMusicXmlDocument(xmlText);
        Element root = doc.getDocumentElement();
        if (root == null || !"score-partwise".equals(root.getTagName())) {
            throw new IllegalArgumentException("input is not a score-partwise MusicXML document.");
        }

        List<Element> parts = directChildren(root, "part");
        List<Element> measures = new ArrayList<Element>();
        Set<String> measureNumbers = new LinkedHashSet<String>();
        Set<String> voices = new LinkedHashSet<String>();

        for (Element part : parts) {
            List<Element> partMeasures = directChildren(part, "measure");
            measures.addAll(partMeasures);
            for (Element measure : partMeasures) {
                String measureNumber = trimToNull(measure.getAttribute("number"));
                if (measureNumber != null) {
                    measureNumbers.add(measureNumber);
                }
                for (Element note : directChildren(measure, "note")) {
                    String voice = directChildText(note, "voice");
                    if (voice != null) {
                        voices.add(voice);
                    }
                }
            }
        }

        String title = directChildText(directChild(root, "work"), "work-title");
        if (title == null) {
            title = directChildText(root, "movement-title");
        }

        return new MusicXmlStateSummary(title, parts.size(), measures.size(), new ArrayList<String>(measureNumbers),
                new ArrayList<String>(voices));
    }

    public static MusicXmlMeasureInspection inspectMusicXmlMeasure(String xmlText, String measureNumber) {
        Document doc = parseMusicXmlDocument(xmlText);
        Element root = doc.getDocumentElement();
        if (root == null || !"score-partwise".equals(root.getTagName())) {
            throw new IllegalArgumentException("input is not a score-partwise MusicXML document.");
        }

        List<IndexedNote> indexedNotes = buildIndexedMeasureNotes(root);
        List<MusicXmlMeasureInspection.Measure> measures = new ArrayList<MusicXmlMeasureInspection.Measure>();
        for (Element part : directChildren(root, "part")) {
            String partId = trimToNull(part.getAttribute("id"));
            for (Element measure : directChildren(part, "measure")) {
                String currentMeasureNumber = trimToNull(measure.getAttribute("number"));
                if (!equalsNullable(currentMeasureNumber, measureNumber)) {
                    continue;
                }
                List<MusicXmlMeasureInspection.Note> notes = new ArrayList<MusicXmlMeasureInspection.Note>();
                List<Element> noteElements = directChildren(measure, "note");
                for (int index = 0; index < noteElements.size(); index++) {
                    Element note = noteElements.get(index);
                    IndexedNote indexed = findIndexedNote(indexedNotes, partId, measureNumber, index + 1);
                    String voice = directChildText(note, "voice");
                    Integer duration = parseIntegerOrNull(directChildText(note, "duration"));
                    boolean isRest = directChild(note, "rest") != null;
                    MusicXmlMeasureInspection.Pitch pitch = buildPitch(note);
                    MusicXmlMeasureInspection.Selector selector = indexed == null
                            ? new MusicXmlMeasureInspection.Selector(partId, measureNumber, index + 1, voice, null)
                            : indexed.selector;
                    notes.add(new MusicXmlMeasureInspection.Note(indexed == null ? null : indexed.nodeId, selector, voice,
                            duration, isRest, pitch));
                }
                measures.add(new MusicXmlMeasureInspection.Measure(partId, notes));
            }
        }
        return new MusicXmlMeasureInspection(measureNumber, measures);
    }

    public static MusicXmlStateDiff diffMusicXmlState(String beforeXml, String afterXml) {
        Document beforeDoc = parseMusicXmlDocument(beforeXml);
        Document afterDoc = parseMusicXmlDocument(afterXml);
        Element beforeRoot = requireScorePartwiseRoot(beforeDoc);
        Element afterRoot = requireScorePartwiseRoot(afterDoc);

        MusicXmlStateDiff.Summary beforeSummary = buildDiffSummary(beforeRoot);
        MusicXmlStateDiff.Summary afterSummary = buildDiffSummary(afterRoot);
        List<String> changedFields = new ArrayList<String>();
        String[] fieldNames = new String[] { "title", "part_count", "measure_count", "note_count", "measure_numbers" };
        for (String fieldName : fieldNames) {
            if (!beforeSummary.fieldEquals(afterSummary, fieldName)) {
                changedFields.add(fieldName);
            }
        }

        Map<String, MeasureSignature> beforeMeasures = buildMeasureDiffSignatureMap(beforeRoot);
        Map<String, MeasureSignature> afterMeasures = buildMeasureDiffSignatureMap(afterRoot);
        Set<String> allKeys = new LinkedHashSet<String>();
        allKeys.addAll(beforeMeasures.keySet());
        allKeys.addAll(afterMeasures.keySet());

        List<String> changedMeasureNumbers = new ArrayList<String>();
        List<MusicXmlStateDiff.ChangedMeasure> changedMeasures = new ArrayList<MusicXmlStateDiff.ChangedMeasure>();
        Set<String> seenMeasureNumbers = new HashSet<String>();
        for (String key : allKeys) {
            MeasureSignature before = beforeMeasures.get(key);
            MeasureSignature after = afterMeasures.get(key);
            if (before != null && after != null && before.signature.equals(after.signature)) {
                continue;
            }
            MeasureSignature preferred = after == null ? before : after;
            String measureNumber = preferred.measureNumber;
            if (!seenMeasureNumbers.contains(measureNumber)) {
                changedMeasureNumbers.add(measureNumber);
                seenMeasureNumbers.add(measureNumber);
            }
            changedMeasures.add(new MusicXmlStateDiff.ChangedMeasure(preferred.partId, measureNumber,
                    before == null ? 0 : before.noteCount, after == null ? 0 : after.noteCount));
        }

        return new MusicXmlStateDiff(!changedFields.isEmpty() || !changedMeasures.isEmpty(), changedFields,
                changedMeasureNumbers, changedMeasures, beforeSummary, afterSummary);
    }

    public static MusicXmlCommandValidation validateMusicXmlCommand(String xmlText, String commandJson) {
        Document doc = parseMusicXmlDocument(xmlText);
        Element root = requireScorePartwiseRoot(doc);
        Map<String, Object> command = MusicXmlCommandJson.parseObject(commandJson);
        return validateMusicXmlCommand(root, command);
    }

    public static String applyMusicXmlCommand(String xmlText, String commandJson) {
        Document doc = parseMusicXmlDocument(xmlText);
        Element root = requireScorePartwiseRoot(doc);
        Map<String, Object> command = MusicXmlCommandJson.parseObject(commandJson);
        MusicXmlCommandValidation validation = validateMusicXmlCommand(root, command);
        if (!validation.isOk()) {
            return validation.toApplyJson();
        }

        String type = MusicXmlCommandJson.stringValue(command, "type");
        if ("ui_noop".equals(type)) {
            return xmlText;
        }

        List<IndexedNote> indexedNotes = buildIndexedMeasureNotes(root);
        IndexedNote target = resolveCommandTarget(command, indexedNotes);
        if (target == null) {
            return validationFailure("MVP_TARGET_NOT_FOUND", "Command target was not resolved.").toApplyJson();
        }

        Element note = findNoteElement(root, target.selector);
        if (note == null) {
            return validationFailure("MVP_TARGET_NOT_FOUND", "Unknown nodeId: " + target.nodeId).toApplyJson();
        }

        if ("change_to_pitch".equals(type)) {
            setPitch(note, MusicXmlCommandJson.castMap(command.get("pitch")));
            autoAssignGrandStaffByPitch(note);
        } else if ("change_duration".equals(type)) {
            changeDuration(note, MusicXmlCommandJson.stringValue(command, "voice"),
                    MusicXmlCommandJson.intValue(command, "duration").intValue());
        } else if ("delete_note".equals(type)) {
            deleteNote(note, MusicXmlCommandJson.stringValue(command, "voice"));
        } else if ("split_note".equals(type)) {
            splitNote(note);
        } else if ("insert_note_after".equals(type)) {
            insertNoteAfter(note, command);
        }
        return serializeMusicXmlDocument(doc);
    }

    private static MusicXmlCommandValidation validateMusicXmlCommand(Element root, Map<String, Object> command) {
        String type = MusicXmlCommandJson.stringValue(command, "type");
        if (!"change_to_pitch".equals(type) && !"change_duration".equals(type) && !"delete_note".equals(type)
                && !"split_note".equals(type) && !"insert_note_after".equals(type) && !"ui_noop".equals(type)) {
            return validationFailure("MVP_INVALID_COMMAND_PAYLOAD",
                    "Only change_to_pitch, change_duration, delete_note, split_note, insert_note_after, and ui_noop are migrated in the current Java validation slice.");
        }
        if ("ui_noop".equals(type)) {
            return new MusicXmlCommandValidation(true, false, new ArrayList<String>(), new ArrayList<String>(),
                    new ArrayList<MusicXmlCommandValidation.Diagnostic>());
        }

        if ("change_to_pitch".equals(type) && !isValidPitch(MusicXmlCommandJson.castMap(command.get("pitch")))) {
            return validationFailure("MVP_INVALID_COMMAND_PAYLOAD", "change_to_pitch.pitch is invalid.");
        }
        if ("change_duration".equals(type) && !isPositiveInteger(MusicXmlCommandJson.intValue(command, "duration"))) {
            return validationFailure("MVP_INVALID_COMMAND_PAYLOAD",
                    "change_duration.duration must be a positive integer.");
        }
        if ("insert_note_after".equals(type) && !isValidInsertNotePayload(command)) {
            return validationFailure("MVP_INVALID_COMMAND_PAYLOAD", "insert_note_after.note is invalid.");
        }

        String nodeId = commandNodeId(command);
        List<IndexedNote> indexedNotes = buildIndexedMeasureNotes(root);
        IndexedNote target = resolveCommandTarget(command, indexedNotes);
        if (nodeId == null && target != null) {
            nodeId = target.nodeId;
        }
        if (nodeId == null) {
            return validationFailure("MVP_COMMAND_TARGET_MISSING", "Command target is missing.");
        }
        if (target == null) {
            return validationFailure("MVP_TARGET_NOT_FOUND", "Unknown nodeId: " + nodeId);
        }

        Element note = findNoteElement(root, target.selector);
        MusicXmlCommandValidation noteKindFailure = validateSupportedNoteKind(type, note);
        if (noteKindFailure != null) {
            return noteKindFailure;
        }
        if ("split_note".equals(type)) {
            Integer duration = parseIntegerOrNull(directChildText(note, "duration"));
            if (duration == null || duration.intValue() <= 1) {
                return validationFailure("MVP_INVALID_COMMAND_PAYLOAD", "split_note requires duration >= 2.");
            }
            if (duration.intValue() % 2 != 0) {
                return validationFailure("MVP_INVALID_COMMAND_PAYLOAD", "split_note requires an even duration value.");
            }
        }

        String commandVoice = MusicXmlCommandJson.stringValue(command, "voice");
        if (commandVoice == null) {
            commandVoice = target.selector.getVoice();
        }
        if ("change_duration".equals(type) && isTripletDuration(note, MusicXmlCommandJson.intValue(command, "duration"))
                && !measureVoiceHasTupletContext(note, commandVoice)) {
            return validationFailure("MVP_INVALID_COMMAND_PAYLOAD",
                    "Tuplet durations are not allowed because this measure/voice has no tuplet context.");
        }
        if (target.selector.getVoice() != null && commandVoice != null && !target.selector.getVoice().equals(commandVoice)) {
            return validationFailure("MVP_UNSUPPORTED_NON_EDITABLE_VOICE",
                    "Target note voice (" + target.selector.getVoice() + ") does not match command voice (" + commandVoice
                            + ").");
        }
        MusicXmlCommandValidation structuralFailure = validateStructuralEditBoundary(type, note, commandVoice);
        if (structuralFailure != null) {
            return structuralFailure;
        }
        MusicXmlCommandValidation timingFailure = validateProjectedMeasureTiming(type, note, command, commandVoice);
        if (timingFailure != null) {
            return timingFailure;
        }
        List<MusicXmlCommandValidation.Warning> warnings = buildProjectedMeasureTimingWarnings(type, note, command,
                commandVoice);

        List<String> changedNodeIds = new ArrayList<String>();
        changedNodeIds.add(nodeId);
        if ("split_note".equals(type)) {
            changedNodeIds.add("n" + (indexedNotes.size() + 1));
        }
        if ("insert_note_after".equals(type)) {
            changedNodeIds.add("n" + (indexedNotes.size() + 1));
        }
        List<String> affectedMeasureNumbers = new ArrayList<String>();
        affectedMeasureNumbers.add(target.selector.getMeasureNumber());
        return new MusicXmlCommandValidation(true, true, changedNodeIds, affectedMeasureNumbers, warnings,
                new ArrayList<MusicXmlCommandValidation.Diagnostic>());
    }

    private static IndexedNote resolveCommandTarget(Map<String, Object> command, List<IndexedNote> indexedNotes) {
        String nodeId = commandNodeId(command);
        if (nodeId != null) {
            return findIndexedNoteByNodeId(indexedNotes, nodeId);
        }
        if ("insert_note_after".equals(MusicXmlCommandJson.stringValue(command, "type"))) {
            return resolveSelector(indexedNotes, MusicXmlCommandJson.castMap(command.get("anchor_selector")));
        }
        return resolveSelector(indexedNotes, MusicXmlCommandJson.castMap(command.get("selector")));
    }

    private static String commandNodeId(Map<String, Object> command) {
        if ("insert_note_after".equals(MusicXmlCommandJson.stringValue(command, "type"))) {
            return MusicXmlCommandJson.stringValue(command, "anchorNodeId");
        }
        return MusicXmlCommandJson.stringValue(command, "targetNodeId");
    }

    private static MusicXmlCommandValidation validateSupportedNoteKind(String commandType, Element note) {
        if (note == null) {
            return null;
        }
        boolean hasGraceCueOrChord = directChild(note, "grace") != null || directChild(note, "cue") != null
                || directChild(note, "chord") != null;
        boolean hasRest = directChild(note, "rest") != null;
        if ("change_to_pitch".equals(commandType) && !hasGraceCueOrChord) {
            return null;
        }
        if ("change_duration".equals(commandType) && !hasGraceCueOrChord && !hasRest) {
            return null;
        }
        if ("delete_note".equals(commandType) && !hasGraceCueOrChord && !hasRest) {
            return null;
        }
        if ("split_note".equals(commandType) && !hasGraceCueOrChord && !hasRest) {
            return null;
        }
        if ("insert_note_after".equals(commandType) && !hasGraceCueOrChord && !hasRest) {
            return null;
        }
        return validationFailure("MVP_UNSUPPORTED_NOTE_KIND",
                "Editing grace/cue/chord/rest notes is not supported in MVP.");
    }

    private static MusicXmlCommandValidation validateStructuralEditBoundary(String type, Element note, String commandVoice) {
        if (!"insert_note_after".equals(type) && !"delete_note".equals(type) && !"split_note".equals(type)) {
            return null;
        }

        Element previous = previousElementSibling(note);
        Element next = nextElementSibling(note);
        if ("insert_note_after".equals(type)) {
            if (isBackupOrForward(next)) {
                return validationFailure("MVP_UNSUPPORTED_NON_EDITABLE_VOICE",
                        "Insert point crosses a backup/forward boundary in MVP.");
            }
            Element nextNote = nextFollowingNoteSibling(note);
            String nextVoice = nextNote == null ? null : directChildText(nextNote, "voice");
            if (nextNote != null && commandVoice != null && !commandVoice.equals(nextVoice)) {
                return validationFailure("MVP_UNSUPPORTED_NON_EDITABLE_VOICE",
                        "Insert is restricted to a continuous local voice lane in MVP.");
            }
            return null;
        }
        if ("split_note".equals(type)) {
            if (next != null && "forward".equals(next.getTagName())) {
                return validationFailure("MVP_UNSUPPORTED_NON_EDITABLE_VOICE",
                        "Split point crosses a forward boundary in MVP.");
            }
            return null;
        }
        if (isBackupOrForward(previous) || isBackupOrForward(next)) {
            return validationFailure("MVP_UNSUPPORTED_NON_EDITABLE_VOICE",
                    "Delete point crosses a backup/forward boundary in MVP.");
        }
        return null;
    }

    private static MusicXmlCommandValidation validateProjectedMeasureTiming(String type, Element note,
            Map<String, Object> command, String commandVoice) {
        if (commandVoice == null) {
            return null;
        }
        MeasureTiming timing = getMeasureTimingForVoice(note, commandVoice);
        if (timing == null) {
            return null;
        }
        Integer projected = null;
        if ("change_duration".equals(type)) {
            Integer oldDuration = parseIntegerOrNull(directChildText(note, "duration"));
            Integer newDuration = MusicXmlCommandJson.intValue(command, "duration");
            if (oldDuration != null && newDuration != null) {
                projected = Integer.valueOf(timing.occupied - oldDuration.intValue() + newDuration.intValue());
            }
        } else if ("insert_note_after".equals(type)) {
            Map<String, Object> notePayload = MusicXmlCommandJson.castMap(command.get("note"));
            Integer insertedDuration = MusicXmlCommandJson.intValue(notePayload, "duration");
            if (insertedDuration != null) {
                projected = Integer.valueOf(timing.occupied + insertedDuration.intValue());
            }
        } else if ("split_note".equals(type)) {
            projected = Integer.valueOf(timing.occupied);
        }
        if (projected != null && projected.intValue() > timing.capacity) {
            if ("change_duration".equals(type)) {
                int overflow = projected.intValue() - timing.capacity;
                if (overflow <= availableRestDurationForExpansion(note, commandVoice)) {
                    return null;
                }
            }
            return validationFailure("MEASURE_OVERFULL",
                    "Projected occupied time " + projected + " exceeds capacity " + timing.capacity + ".");
        }
        return null;
    }

    private static List<MusicXmlCommandValidation.Warning> buildProjectedMeasureTimingWarnings(String type, Element note,
            Map<String, Object> command, String commandVoice) {
        List<MusicXmlCommandValidation.Warning> warnings = new ArrayList<MusicXmlCommandValidation.Warning>();
        if (!"insert_note_after".equals(type) || commandVoice == null) {
            return warnings;
        }
        MeasureTiming timing = getMeasureTimingForVoice(note, commandVoice);
        if (timing == null) {
            return warnings;
        }
        Map<String, Object> notePayload = MusicXmlCommandJson.castMap(command.get("note"));
        Integer insertedDuration = MusicXmlCommandJson.intValue(notePayload, "duration");
        if (insertedDuration == null) {
            return warnings;
        }
        int projected = timing.occupied + insertedDuration.intValue();
        if (projected < timing.capacity) {
            warnings.add(new MusicXmlCommandValidation.Warning("MEASURE_UNDERFULL",
                    "Projected occupied time " + projected + " is below capacity " + timing.capacity + "."));
        }
        return warnings;
    }

    private static boolean isTripletDuration(Element note, Integer duration) {
        if (duration == null) {
            return false;
        }
        Integer divisions = resolveEffectiveDivisions(note);
        if (divisions == null || divisions.intValue() <= 0) {
            return false;
        }
        DurationNotation notation = durationToNotation(duration.intValue(), divisions.intValue());
        return notation != null && notation.triplet;
    }

    private static boolean measureVoiceHasTupletContext(Element target, String voice) {
        Element measure = findAncestor(target, "measure");
        if (measure == null) {
            return false;
        }
        for (Element note : directChildren(measure, "note")) {
            if (!equalsNullable(directChildText(note, "voice"), voice)) {
                continue;
            }
            if (directChild(note, "time-modification") != null) {
                return true;
            }
            Element notations = directChild(note, "notations");
            if (notations != null && directChild(notations, "tuplet") != null) {
                return true;
            }
        }
        return false;
    }

    private static void setPitch(Element note, Map<String, Object> pitch) {
        Element rest = directChild(note, "rest");
        if (rest != null) {
            note.removeChild(rest);
        }

        Element pitchElement = directChild(note, "pitch");
        if (pitchElement == null) {
            pitchElement = note.getOwnerDocument().createElement("pitch");
            note.insertBefore(pitchElement, note.getFirstChild());
        }

        upsertSimpleChild(pitchElement, "step", MusicXmlCommandJson.stringValue(pitch, "step"));
        Integer alter = MusicXmlCommandJson.intValue(pitch, "alter");
        if (alter == null) {
            removeDirectChild(pitchElement, "alter");
            removeDirectChild(note, "accidental");
        } else {
            upsertSimpleChild(pitchElement, "alter", alter.toString());
            upsertSimpleChild(note, "accidental", accidentalFromAlter(alter.intValue()));
        }
        upsertSimpleChild(pitchElement, "octave", MusicXmlCommandJson.intValue(pitch, "octave").toString());
    }

    private static void setDurationValue(Element note, int duration) {
        upsertSimpleChild(note, "duration", Integer.toString(duration));
        syncSimpleTypeFromDuration(note, duration);
    }

    private static void autoAssignGrandStaffByPitch(Element note) {
        if (!hasGrandStaffContext(note)) {
            return;
        }
        Integer midi = notePitchToMidi(note);
        if (midi == null) {
            return;
        }
        Element staff = directChild(note, "staff");
        String existingStaffText = directChildText(note, "staff");
        Integer previousStaff = "1".equals(existingStaffText) ? Integer.valueOf(1)
                : ("2".equals(existingStaffText) ? Integer.valueOf(2) : null);
        int desiredStaff = StaffClefPolicy.pickStaffByPitchWithHysteresis(midi.intValue(), previousStaff);
        if (staff == null) {
            staff = note.getOwnerDocument().createElement("staff");
            note.appendChild(staff);
        }
        staff.setTextContent(Integer.toString(desiredStaff));
    }

    private static boolean hasGrandStaffContext(Element note) {
        Element measure = findAncestor(note, "measure");
        Element part = findAncestor(measure, "part");
        if (measure == null || part == null) {
            return false;
        }
        List<Element> measures = directChildren(part, "measure");
        int targetIndex = measures.indexOf(measure);
        if (targetIndex < 0) {
            return false;
        }
        int staves = 1;
        String clef1 = "";
        String clef2 = "";
        for (int index = 0; index <= targetIndex; index++) {
            Element attributes = directChild(measures.get(index), "attributes");
            if (attributes == null) {
                continue;
            }
            Integer parsedStaves = parseIntegerOrNull(directChildText(attributes, "staves"));
            if (parsedStaves != null && parsedStaves.intValue() > 0) {
                staves = parsedStaves.intValue();
            }
            for (Element clef : directChildren(attributes, "clef")) {
                String number = trimToNull(clef.getAttribute("number"));
                String sign = directChildText(clef, "sign");
                if ("1".equals(number) && sign != null) {
                    clef1 = sign;
                }
                if ("2".equals(number) && sign != null) {
                    clef2 = sign;
                }
            }
        }
        return staves >= 2 && "G".equals(clef1) && "F".equals(clef2);
    }

    private static Integer notePitchToMidi(Element note) {
        Element pitch = directChild(note, "pitch");
        if (pitch == null) {
            return null;
        }
        String step = directChildText(pitch, "step");
        Integer octave = parseIntegerOrNull(directChildText(pitch, "octave"));
        Integer alter = parseIntegerOrNull(directChildText(pitch, "alter"));
        Integer base = semitoneByStep(step);
        if (base == null || octave == null) {
            return null;
        }
        return Integer.valueOf((octave.intValue() + 1) * 12 + base.intValue() + (alter == null ? 0 : alter.intValue()));
    }

    private static Integer semitoneByStep(String step) {
        if ("C".equals(step)) {
            return Integer.valueOf(0);
        }
        if ("D".equals(step)) {
            return Integer.valueOf(2);
        }
        if ("E".equals(step)) {
            return Integer.valueOf(4);
        }
        if ("F".equals(step)) {
            return Integer.valueOf(5);
        }
        if ("G".equals(step)) {
            return Integer.valueOf(7);
        }
        if ("A".equals(step)) {
            return Integer.valueOf(9);
        }
        if ("B".equals(step)) {
            return Integer.valueOf(11);
        }
        return null;
    }

    private static void changeDuration(Element note, String voice, int duration) {
        Integer oldDuration = parseIntegerOrNull(directChildText(note, "duration"));
        String effectiveVoice = voice == null ? directChildText(note, "voice") : voice;
        MeasureTiming timing = effectiveVoice == null ? null : getMeasureTimingForVoice(note, effectiveVoice);
        int underfullDelta = 0;
        if (oldDuration != null && timing != null) {
            int projected = timing.occupied - oldDuration.intValue() + duration;
            int overflow = projected - timing.capacity;
            if (overflow > 0) {
                int consumedAfter = consumeFollowingRestsForDurationExpansion(note, effectiveVoice, overflow);
                int remainingAfter = overflow - consumedAfter;
                if (remainingAfter > 0) {
                    consumePrecedingRestsForDurationExpansion(note, effectiveVoice, remainingAfter);
                }
            }
            MeasureTiming adjusted = getMeasureTimingForVoice(note, effectiveVoice);
            if (adjusted != null) {
                int adjustedProjected = adjusted.occupied - oldDuration.intValue() + duration;
                if (adjustedProjected < timing.capacity) {
                    underfullDelta = timing.capacity - adjustedProjected;
                }
            }
        }
        setDurationValue(note, duration);
        if (underfullDelta > 0 && effectiveVoice != null) {
            fillUnderfullGapAfterTarget(note, effectiveVoice, underfullDelta);
        }
    }

    private static void deleteNote(Element note, String fallbackVoice) {
        Element next = nextElementSibling(note);
        if (next != null && "note".equals(next.getTagName()) && directChild(next, "chord") != null) {
            removeDirectChild(next, "chord");
            note.getParentNode().removeChild(note);
            return;
        }
        replaceWithRestNote(note, fallbackVoice);
    }

    private static void replaceWithRestNote(Element note, String fallbackVoice) {
        Integer duration = parseIntegerOrNull(directChildText(note, "duration"));
        if (duration == null || duration.intValue() <= 0) {
            duration = Integer.valueOf(1);
        }
        String voice = trimToNull(directChildText(note, "voice"));
        if (voice == null) {
            voice = trimToNull(fallbackVoice);
        }
        if (voice == null) {
            voice = "1";
        }

        removeDirectChild(note, "pitch");
        removeDirectChild(note, "accidental");
        removeDirectChild(note, "chord");
        removeAllDirectChildren(note, "tie");
        Element notations = directChild(note, "notations");
        if (notations != null) {
            removeAllDirectChildren(notations, "tied");
            if (!hasElementChildren(notations)) {
                note.removeChild(notations);
            }
        }

        Element rest = directChild(note, "rest");
        if (rest == null) {
            rest = note.getOwnerDocument().createElement("rest");
            Element durationElement = directChild(note, "duration");
            if (durationElement == null) {
                note.insertBefore(rest, note.getFirstChild());
            } else {
                note.insertBefore(rest, durationElement);
            }
        }
        upsertSimpleChild(note, "duration", duration.toString());
        upsertSimpleChild(note, "voice", voice);
    }

    private static void splitNote(Element note) {
        Integer duration = parseIntegerOrNull(directChildText(note, "duration"));
        int half = duration.intValue() / 2;
        Element duplicated = (Element) note.cloneNode(true);
        Node next = note.getNextSibling();
        Node parent = note.getParentNode();
        if (next == null) {
            parent.appendChild(duplicated);
        } else {
            parent.insertBefore(duplicated, next);
        }
        setDurationValue(note, half);
        setDurationValue(duplicated, half);
    }

    private static void insertNoteAfter(Element anchor, Map<String, Object> command) {
        Element note = createNoteElement(anchor.getOwnerDocument(), MusicXmlCommandJson.stringValue(command, "voice"),
                MusicXmlCommandJson.castMap(command.get("note")));
        Node next = anchor.getNextSibling();
        Node parent = anchor.getParentNode();
        if (next == null) {
            parent.appendChild(note);
        } else {
            parent.insertBefore(note, next);
        }
    }

    private static Element createNoteElement(Document doc, String voice, Map<String, Object> notePayload) {
        Element note = doc.createElement("note");
        Element pitchElement = doc.createElement("pitch");
        Map<String, Object> pitch = MusicXmlCommandJson.castMap(notePayload.get("pitch"));
        upsertSimpleChild(pitchElement, "step", MusicXmlCommandJson.stringValue(pitch, "step"));
        Integer alter = MusicXmlCommandJson.intValue(pitch, "alter");
        if (alter != null) {
            upsertSimpleChild(pitchElement, "alter", alter.toString());
        }
        upsertSimpleChild(pitchElement, "octave", MusicXmlCommandJson.intValue(pitch, "octave").toString());
        note.appendChild(pitchElement);
        if (alter != null) {
            upsertSimpleChild(note, "accidental", accidentalFromAlter(alter.intValue()));
        }
        Integer duration = MusicXmlCommandJson.intValue(notePayload, "duration");
        upsertSimpleChild(note, "duration", duration.toString());
        upsertSimpleChild(note, "voice", voice == null ? "1" : voice);
        syncSimpleTypeFromDuration(note, duration.intValue());
        return note;
    }

    private static Element createRestElement(Document doc, String voice, int duration) {
        Element note = doc.createElement("note");
        Element rest = doc.createElement("rest");
        note.appendChild(rest);
        upsertSimpleChild(note, "duration", Integer.toString(duration));
        upsertSimpleChild(note, "voice", voice == null ? "1" : voice);
        syncSimpleTypeFromDuration(note, duration);
        return note;
    }

    private static void syncSimpleTypeFromDuration(Element note, int duration) {
        Integer divisions = resolveEffectiveDivisions(note);
        if (divisions == null || divisions.intValue() <= 0) {
            return;
        }
        DurationNotation notation = durationToNotation(duration, divisions.intValue());
        if (notation == null) {
            return;
        }
        upsertSimpleChild(note, "type", notation.type);
        removeAllDirectChildren(note, "dot");
        removeAllDirectChildren(note, "time-modification");
        for (int index = 0; index < notation.dotCount; index++) {
            note.appendChild(note.getOwnerDocument().createElement("dot"));
        }
        if (notation.triplet) {
            Element timeModification = note.getOwnerDocument().createElement("time-modification");
            Element actual = note.getOwnerDocument().createElement("actual-notes");
            actual.setTextContent("3");
            Element normal = note.getOwnerDocument().createElement("normal-notes");
            normal.setTextContent("2");
            timeModification.appendChild(actual);
            timeModification.appendChild(normal);
            note.appendChild(timeModification);
        }
    }

    private static Integer resolveEffectiveDivisions(Element note) {
        Element measure = findAncestor(note, "measure");
        Element part = findAncestor(measure, "part");
        if (measure == null || part == null) {
            return null;
        }
        List<Element> measures = directChildren(part, "measure");
        int measureIndex = measures.indexOf(measure);
        for (int index = measureIndex; index >= 0; index--) {
            Element attributes = directChild(measures.get(index), "attributes");
            Integer divisions = parseIntegerOrNull(directChildText(attributes, "divisions"));
            if (divisions != null && divisions.intValue() > 0) {
                return divisions;
            }
        }
        return null;
    }

    private static MeasureTiming getMeasureTimingForVoice(Element noteInMeasure, String voice) {
        Element measure = findAncestor(noteInMeasure, "measure");
        if (measure == null) {
            return null;
        }
        Integer capacity = getMeasureCapacity(measure);
        if (capacity == null) {
            return null;
        }
        return new MeasureTiming(capacity.intValue(), getOccupiedTime(measure, voice));
    }

    private static int availableRestDurationForExpansion(Element target, String voice) {
        return availableFollowingRestDurationForExpansion(target, voice, Integer.MAX_VALUE)
                + availablePrecedingRestDurationForExpansion(target, voice, Integer.MAX_VALUE);
    }

    private static int availableFollowingRestDurationForExpansion(Element target, String voice, int overflow) {
        if (overflow <= 0) {
            return 0;
        }
        int remaining = overflow;
        Element cursor = nextElementSibling(target);
        while (cursor != null && remaining > 0) {
            if (isBackupOrForward(cursor)) {
                break;
            }
            Element next = nextElementSibling(cursor);
            if ("note".equals(cursor.getTagName()) && isConsumableRest(cursor, voice)) {
                Integer duration = parseIntegerOrNull(directChildText(cursor, "duration"));
                remaining -= Math.min(remaining, duration == null ? 0 : duration.intValue());
            }
            cursor = next;
        }
        return overflow - remaining;
    }

    private static int availablePrecedingRestDurationForExpansion(Element target, String voice, int overflow) {
        if (overflow <= 0) {
            return 0;
        }
        int remaining = overflow;
        Element cursor = previousElementSibling(target);
        while (cursor != null && remaining > 0) {
            if (isBackupOrForward(cursor)) {
                break;
            }
            Element previous = previousElementSibling(cursor);
            if ("note".equals(cursor.getTagName()) && isConsumableRest(cursor, voice)) {
                Integer duration = parseIntegerOrNull(directChildText(cursor, "duration"));
                remaining -= Math.min(remaining, duration == null ? 0 : duration.intValue());
            }
            cursor = previous;
        }
        return overflow - remaining;
    }

    private static int consumeFollowingRestsForDurationExpansion(Element target, String voice, int overflow) {
        if (overflow <= 0) {
            return 0;
        }
        int remaining = overflow;
        Element cursor = nextElementSibling(target);
        while (cursor != null && remaining > 0) {
            if (isBackupOrForward(cursor)) {
                break;
            }
            Element next = nextElementSibling(cursor);
            if ("note".equals(cursor.getTagName()) && isConsumableRest(cursor, voice)) {
                Integer duration = parseIntegerOrNull(directChildText(cursor, "duration"));
                int restDuration = duration == null ? 0 : duration.intValue();
                if (restDuration <= remaining) {
                    remaining -= restDuration;
                    cursor.getParentNode().removeChild(cursor);
                } else {
                    setDurationValue(cursor, restDuration - remaining);
                    remaining = 0;
                }
            }
            cursor = next;
        }
        return overflow - remaining;
    }

    private static int consumePrecedingRestsForDurationExpansion(Element target, String voice, int overflow) {
        if (overflow <= 0) {
            return 0;
        }
        int remaining = overflow;
        Element cursor = previousElementSibling(target);
        while (cursor != null && remaining > 0) {
            if (isBackupOrForward(cursor)) {
                break;
            }
            Element previous = previousElementSibling(cursor);
            if ("note".equals(cursor.getTagName()) && isConsumableRest(cursor, voice)) {
                Integer duration = parseIntegerOrNull(directChildText(cursor, "duration"));
                int restDuration = duration == null ? 0 : duration.intValue();
                if (restDuration <= remaining) {
                    remaining -= restDuration;
                    cursor.getParentNode().removeChild(cursor);
                } else {
                    setDurationValue(cursor, restDuration - remaining);
                    remaining = 0;
                }
            }
            cursor = previous;
        }
        return overflow - remaining;
    }

    private static boolean fillUnderfullGapAfterTarget(Element target, String voice, int deficit) {
        if (deficit <= 0) {
            return true;
        }
        Element measure = findAncestor(target, "measure");
        if (measure == null || measureHasBackupOrForward(measure)) {
            return false;
        }
        Element next = nextElementSibling(target);
        if (next != null && "note".equals(next.getTagName()) && isConsumableRest(next, voice)) {
            Integer current = parseIntegerOrNull(directChildText(next, "duration"));
            setDurationValue(next, (current == null ? 0 : current.intValue()) + deficit);
            return true;
        }
        Element rest = createRestElement(target.getOwnerDocument(), voice, deficit);
        Node parent = target.getParentNode();
        Node nextNode = target.getNextSibling();
        if (nextNode == null) {
            parent.appendChild(rest);
        } else {
            parent.insertBefore(rest, nextNode);
        }
        return true;
    }

    private static boolean isConsumableRest(Element note, String voice) {
        if (note == null || !"note".equals(note.getTagName())) {
            return false;
        }
        if (directChild(note, "rest") == null || directChild(note, "chord") != null) {
            return false;
        }
        if (!equalsNullable(directChildText(note, "voice"), voice)) {
            return false;
        }
        Integer duration = parseIntegerOrNull(directChildText(note, "duration"));
        return duration != null && duration.intValue() > 0;
    }

    private static boolean measureHasBackupOrForward(Element measure) {
        for (Element child : directElementChildren(measure)) {
            if (isBackupOrForward(child)) {
                return true;
            }
        }
        return false;
    }

    private static Integer getMeasureCapacity(Element measure) {
        TimingContext context = resolveTimingContext(measure);
        if (context == null || context.beatType <= 0 || context.divisions <= 0) {
            return null;
        }
        double beatUnit = (4.0d / (double) context.beatType) * (double) context.divisions;
        return Integer.valueOf((int) Math.round((double) context.beats * beatUnit));
    }

    private static int getOccupiedTime(Element measure, String voice) {
        int cursor = 0;
        int occupied = 0;
        for (Element child : directElementChildren(measure)) {
            if ("backup".equals(child.getTagName()) || "forward".equals(child.getTagName())) {
                Integer shift = parseIntegerOrNull(directChildText(child, "duration"));
                if (shift == null) {
                    continue;
                }
                cursor = "backup".equals(child.getTagName()) ? Math.max(0, cursor - shift.intValue())
                        : cursor + shift.intValue();
                continue;
            }
            if (!"note".equals(child.getTagName())) {
                continue;
            }
            if (directChild(child, "chord") != null) {
                continue;
            }
            String noteVoice = directChildText(child, "voice");
            if (!equalsNullable(noteVoice, voice)) {
                continue;
            }
            Integer duration = parseIntegerOrNull(directChildText(child, "duration"));
            if (duration == null) {
                continue;
            }
            int end = cursor + duration.intValue();
            occupied = Math.max(occupied, end);
            cursor = end;
        }
        return occupied;
    }

    private static TimingContext resolveTimingContext(Element measure) {
        Element part = findAncestor(measure, "part");
        if (part == null) {
            return null;
        }
        Integer beats = null;
        Integer beatType = null;
        Integer divisions = null;
        List<Element> measures = directChildren(part, "measure");
        int measureIndex = measures.indexOf(measure);
        for (int index = measureIndex; index >= 0; index--) {
            Element attributes = directChild(measures.get(index), "attributes");
            if (attributes == null) {
                continue;
            }
            if (divisions == null) {
                divisions = parseIntegerOrNull(directChildText(attributes, "divisions"));
            }
            Element time = directChild(attributes, "time");
            if (beats == null) {
                beats = parseIntegerOrNull(directChildText(time, "beats"));
            }
            if (beatType == null) {
                beatType = parseIntegerOrNull(directChildText(time, "beat-type"));
            }
            if (beats != null && beatType != null && divisions != null) {
                return new TimingContext(beats.intValue(), beatType.intValue(), divisions.intValue());
            }
        }
        return null;
    }

    private static DurationNotation durationToNotation(int duration, int divisions) {
        DurationNotation[] definitions = new DurationNotation[] { new DurationNotation(4, 1, "whole", 0, false),
                new DurationNotation(3, 1, "half", 1, false), new DurationNotation(2, 1, "half", 0, false),
                new DurationNotation(4, 3, "half", 0, true), new DurationNotation(3, 2, "quarter", 1, false),
                new DurationNotation(1, 1, "quarter", 0, false), new DurationNotation(2, 3, "quarter", 0, true),
                new DurationNotation(3, 4, "eighth", 1, false), new DurationNotation(1, 2, "eighth", 0, false),
                new DurationNotation(1, 3, "eighth", 0, true), new DurationNotation(3, 8, "16th", 1, false),
                new DurationNotation(1, 4, "16th", 0, false), new DurationNotation(1, 6, "16th", 0, true),
                new DurationNotation(1, 8, "32nd", 0, false), new DurationNotation(1, 16, "64th", 0, false) };
        for (DurationNotation definition : definitions) {
            int numerator = divisions * definition.num;
            if (numerator % definition.den != 0) {
                continue;
            }
            if (duration == numerator / definition.den) {
                return definition;
            }
        }
        return null;
    }

    private static String accidentalFromAlter(int alter) {
        String accidental = AccidentalSpelling.accidentalTextFromAlter(alter);
        return accidental == null ? "natural" : accidental;
    }

    private static void upsertSimpleChild(Element parent, String tagName, String text) {
        Element child = directChild(parent, tagName);
        if (child == null) {
            child = parent.getOwnerDocument().createElement(tagName);
            parent.appendChild(child);
        }
        child.setTextContent(text);
    }

    private static void removeDirectChild(Element parent, String tagName) {
        Element child = directChild(parent, tagName);
        if (child != null) {
            parent.removeChild(child);
        }
    }

    private static void removeAllDirectChildren(Element parent, String tagName) {
        List<Element> children = directChildren(parent, tagName);
        for (Element child : children) {
            parent.removeChild(child);
        }
    }

    private static Element findAncestor(Element node, String tagName) {
        Node cursor = node == null ? null : node.getParentNode();
        while (cursor != null) {
            if (cursor instanceof Element && tagName.equals(((Element) cursor).getTagName())) {
                return (Element) cursor;
            }
            cursor = cursor.getParentNode();
        }
        return null;
    }

    private static Element previousElementSibling(Element element) {
        Node cursor = element == null ? null : element.getPreviousSibling();
        while (cursor != null) {
            if (cursor instanceof Element) {
                return (Element) cursor;
            }
            cursor = cursor.getPreviousSibling();
        }
        return null;
    }

    private static Element nextElementSibling(Element element) {
        Node cursor = element == null ? null : element.getNextSibling();
        while (cursor != null) {
            if (cursor instanceof Element) {
                return (Element) cursor;
            }
            cursor = cursor.getNextSibling();
        }
        return null;
    }

    private static Element nextFollowingNoteSibling(Element element) {
        Element cursor = nextElementSibling(element);
        while (cursor != null) {
            if ("note".equals(cursor.getTagName())) {
                return cursor;
            }
            cursor = nextElementSibling(cursor);
        }
        return null;
    }

    private static boolean isBackupOrForward(Element element) {
        return element != null && ("backup".equals(element.getTagName()) || "forward".equals(element.getTagName()));
    }

    private static boolean hasElementChildren(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            if (children.item(index) instanceof Element) {
                return true;
            }
        }
        return false;
    }

    private static Element findNoteElement(Element root, MusicXmlMeasureInspection.Selector selector) {
        for (Element part : directChildren(root, "part")) {
            if (!equalsNullable(trimToNull(part.getAttribute("id")), selector.getPartId())) {
                continue;
            }
            for (Element measure : directChildren(part, "measure")) {
                if (!equalsNullable(trimToNull(measure.getAttribute("number")), selector.getMeasureNumber())) {
                    continue;
                }
                List<Element> notes = directChildren(measure, "note");
                int index = selector.getMeasureNoteIndex() - 1;
                return index >= 0 && index < notes.size() ? notes.get(index) : null;
            }
        }
        return null;
    }

    private static Document parseMusicXmlDocument(String xmlText) {
        if (xmlText == null) {
            throw new IllegalArgumentException("input is null.");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            disableFeatureIfAvailable(factory, "http://apache.org/xml/features/disallow-doctype-decl");
            disableFeatureIfAvailable(factory, "http://xml.org/sax/features/external-general-entities");
            disableFeatureIfAvailable(factory, "http://xml.org/sax/features/external-parameter-entities");
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xmlText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalArgumentException("input is not a valid MusicXML document.", ex);
        }
    }

    private static String serializeMusicXmlDocument(Document doc) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            java.io.StringWriter writer = new java.io.StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String output = writer.toString();
            return output.endsWith("\n") ? output : output + "\n";
        } catch (Exception ex) {
            throw new IllegalArgumentException("failed to serialize MusicXML document.", ex);
        }
    }

    private static Element requireScorePartwiseRoot(Document doc) {
        Element root = doc.getDocumentElement();
        if (root == null || !"score-partwise".equals(root.getTagName())) {
            throw new IllegalArgumentException("input is not a score-partwise MusicXML document.");
        }
        return root;
    }

    private static MusicXmlStateDiff.Summary buildDiffSummary(Element root) {
        List<Element> parts = directChildren(root, "part");
        List<Element> measures = new ArrayList<Element>();
        List<Element> notes = new ArrayList<Element>();
        Set<String> measureNumbers = new LinkedHashSet<String>();
        for (Element part : parts) {
            for (Element measure : directChildren(part, "measure")) {
                measures.add(measure);
                String measureNumber = trimToNull(measure.getAttribute("number"));
                if (measureNumber != null) {
                    measureNumbers.add(measureNumber);
                }
                notes.addAll(directChildren(measure, "note"));
            }
        }
        String title = directChildText(directChild(root, "work"), "work-title");
        if (title == null) {
            title = directChildText(root, "movement-title");
        }
        return new MusicXmlStateDiff.Summary(title, parts.size(), measures.size(), notes.size(),
                new ArrayList<String>(measureNumbers));
    }

    private static Map<String, MeasureSignature> buildMeasureDiffSignatureMap(Element root) {
        Map<String, MeasureSignature> result = new java.util.LinkedHashMap<String, MeasureSignature>();
        for (Element part : directChildren(root, "part")) {
            String partId = trimToNull(part.getAttribute("id"));
            for (Element measure : directChildren(part, "measure")) {
                String measureNumber = trimToNull(measure.getAttribute("number"));
                List<Element> notes = directChildren(measure, "note");
                String signature = buildNoteSummarySignature(notes);
                String key = (partId == null ? "" : partId) + ":" + (measureNumber == null ? "" : measureNumber);
                result.put(key, new MeasureSignature(partId, measureNumber == null ? "" : measureNumber, notes.size(),
                        signature));
            }
        }
        return result;
    }

    private static String buildNoteSummarySignature(List<Element> notes) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int index = 0; index < notes.size(); index++) {
            if (index > 0) {
                builder.append(",");
            }
            Element note = notes.get(index);
            String voice = directChildText(note, "voice");
            Integer duration = parseIntegerOrNull(directChildText(note, "duration"));
            boolean isRest = directChild(note, "rest") != null;
            MusicXmlMeasureInspection.Pitch pitch = buildPitch(note);
            builder.append("{voice=");
            builder.append(voice == null ? "null" : voice);
            builder.append(",duration=");
            builder.append(duration == null ? "null" : duration.toString());
            builder.append(",is_rest=");
            builder.append(isRest);
            builder.append(",pitch=");
            if (pitch == null || isRest) {
                builder.append("null");
            } else {
                builder.append(pitch.getStep()).append(":").append(pitch.getAlter()).append(":").append(pitch.getOctave());
            }
            builder.append("}");
        }
        builder.append("]");
        return builder.toString();
    }

    private static List<IndexedNote> buildIndexedMeasureNotes(Element root) {
        List<IndexedNote> indexedNotes = new ArrayList<IndexedNote>();
        int sequence = 0;
        for (Element part : directChildren(root, "part")) {
            String partId = trimToNull(part.getAttribute("id"));
            for (Element measure : directChildren(part, "measure")) {
                String measureNumber = trimToNull(measure.getAttribute("number"));
                Map<String, Integer> voiceNoteCounts = new HashMap<String, Integer>();
                List<Element> notes = directChildren(measure, "note");
                for (int index = 0; index < notes.size(); index++) {
                    sequence++;
                    Element note = notes.get(index);
                    String voice = directChildText(note, "voice");
                    String voiceKey = voice == null ? "__none__" : voice;
                    Integer previous = voiceNoteCounts.get(voiceKey);
                    int voiceNoteIndex = previous == null ? 1 : previous.intValue() + 1;
                    voiceNoteCounts.put(voiceKey, Integer.valueOf(voiceNoteIndex));
                    MusicXmlMeasureInspection.Selector selector = new MusicXmlMeasureInspection.Selector(partId,
                            measureNumber, index + 1, voice, Integer.valueOf(voiceNoteIndex));
                    indexedNotes.add(new IndexedNote("n" + sequence, selector));
                }
            }
        }
        return indexedNotes;
    }

    private static IndexedNote findIndexedNote(List<IndexedNote> indexedNotes, String partId, String measureNumber,
            int measureNoteIndex) {
        for (IndexedNote indexed : indexedNotes) {
            MusicXmlMeasureInspection.Selector selector = indexed.selector;
            if (equalsNullable(selector.getPartId(), partId)
                    && equalsNullable(selector.getMeasureNumber(), measureNumber)
                    && selector.getMeasureNoteIndex() == measureNoteIndex) {
                return indexed;
            }
        }
        return null;
    }

    private static IndexedNote findIndexedNoteByNodeId(List<IndexedNote> indexedNotes, String nodeId) {
        for (IndexedNote indexed : indexedNotes) {
            if (indexed.nodeId.equals(nodeId)) {
                return indexed;
            }
        }
        return null;
    }

    private static IndexedNote resolveSelector(List<IndexedNote> indexedNotes, Map<String, Object> selector) {
        if (selector == null) {
            return null;
        }
        String partId = MusicXmlCommandJson.stringValue(selector, "part_id");
        String measureNumber = MusicXmlCommandJson.stringValue(selector, "measure_number");
        Integer measureNoteIndex = MusicXmlCommandJson.intValue(selector, "measure_note_index");
        String voice = MusicXmlCommandJson.stringValue(selector, "voice");
        Integer voiceNoteIndex = MusicXmlCommandJson.intValue(selector, "voice_note_index");
        IndexedNote match = null;
        for (IndexedNote indexed : indexedNotes) {
            MusicXmlMeasureInspection.Selector current = indexed.selector;
            if (partId != null && !partId.equals(current.getPartId())) {
                continue;
            }
            if (measureNumber != null && !measureNumber.equals(current.getMeasureNumber())) {
                continue;
            }
            if (measureNoteIndex != null && measureNoteIndex.intValue() != current.getMeasureNoteIndex()) {
                continue;
            }
            if (voice != null && !voice.equals(current.getVoice())) {
                continue;
            }
            if (voiceNoteIndex != null && !voiceNoteIndex.equals(current.getVoiceNoteIndex())) {
                continue;
            }
            if (match != null) {
                return null;
            }
            match = indexed;
        }
        return match;
    }

    private static MusicXmlCommandValidation validationFailure(String code, String message) {
        List<MusicXmlCommandValidation.Diagnostic> diagnostics = new ArrayList<MusicXmlCommandValidation.Diagnostic>();
        diagnostics.add(new MusicXmlCommandValidation.Diagnostic(code, message));
        return new MusicXmlCommandValidation(false, false, new ArrayList<String>(), new ArrayList<String>(), diagnostics);
    }

    private static boolean isValidPitch(Map<String, Object> pitch) {
        if (pitch == null) {
            return false;
        }
        String step = MusicXmlCommandJson.stringValue(pitch, "step");
        Integer octave = MusicXmlCommandJson.intValue(pitch, "octave");
        Integer alter = MusicXmlCommandJson.intValue(pitch, "alter");
        if (!("A".equals(step) || "B".equals(step) || "C".equals(step) || "D".equals(step) || "E".equals(step)
                || "F".equals(step) || "G".equals(step))) {
            return false;
        }
        if (octave == null) {
            return false;
        }
        return alter == null || (alter.intValue() >= -2 && alter.intValue() <= 2);
    }

    private static boolean isValidInsertNotePayload(Map<String, Object> command) {
        Map<String, Object> note = MusicXmlCommandJson.castMap(command.get("note"));
        if (note == null) {
            return false;
        }
        if (!isPositiveInteger(MusicXmlCommandJson.intValue(note, "duration"))) {
            return false;
        }
        return isValidPitch(MusicXmlCommandJson.castMap(note.get("pitch")));
    }

    private static boolean isPositiveInteger(Integer value) {
        return value != null && value.intValue() > 0;
    }

    private static MusicXmlMeasureInspection.Pitch buildPitch(Element note) {
        Element pitch = directChild(note, "pitch");
        if (pitch == null) {
            return null;
        }
        String step = directChildText(pitch, "step");
        Integer octave = parseIntegerOrNull(directChildText(pitch, "octave"));
        if (step == null || octave == null) {
            return null;
        }
        Integer alter = parseIntegerOrNull(directChildText(pitch, "alter"));
        return new MusicXmlMeasureInspection.Pitch(step, alter, octave);
    }

    private static Integer parseIntegerOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static void disableFeatureIfAvailable(DocumentBuilderFactory factory, String feature) {
        try {
            factory.setFeature(feature, false);
        } catch (Exception ignored) {
            // Some XML parsers do not expose every hardening feature.
        }
    }

    private static Element directChild(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private static List<Element> directChildren(Element parent, String tagName) {
        List<Element> result = new ArrayList<Element>();
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                result.add((Element) child);
            }
        }
        return result;
    }

    private static List<Element> directElementChildren(Element parent) {
        List<Element> result = new ArrayList<Element>();
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element) {
                result.add((Element) child);
            }
        }
        return result;
    }

    private static String directChildText(Element parent, String tagName) {
        Element child = directChild(parent, tagName);
        if (child == null) {
            return null;
        }
        return trimToNull(child.getTextContent());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private static final class IndexedNote {
        private final String nodeId;
        private final MusicXmlMeasureInspection.Selector selector;

        private IndexedNote(String nodeId, MusicXmlMeasureInspection.Selector selector) {
            this.nodeId = nodeId;
            this.selector = selector;
        }
    }

    private static final class MeasureSignature {
        private final String partId;
        private final String measureNumber;
        private final int noteCount;
        private final String signature;

        private MeasureSignature(String partId, String measureNumber, int noteCount, String signature) {
            this.partId = partId;
            this.measureNumber = measureNumber;
            this.noteCount = noteCount;
            this.signature = signature;
        }
    }

    private static final class DurationNotation {
        private final int num;
        private final int den;
        private final String type;
        private final int dotCount;
        private final boolean triplet;

        private DurationNotation(int num, int den, String type, int dotCount, boolean triplet) {
            this.num = num;
            this.den = den;
            this.type = type;
            this.dotCount = dotCount;
            this.triplet = triplet;
        }
    }

    private static final class MeasureTiming {
        private final int capacity;
        private final int occupied;

        private MeasureTiming(int capacity, int occupied) {
            this.capacity = capacity;
            this.occupied = occupied;
        }
    }

    private static final class TimingContext {
        private final int beats;
        private final int beatType;
        private final int divisions;

        private TimingContext(int beats, int beatType, int divisions) {
            this.beats = beats;
            this.beatType = beatType;
            this.divisions = divisions;
        }
    }
}
