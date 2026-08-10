package jp.igapyon.mikuscore.musicxml;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
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
    /**
     * ScoreCore may attach a private, temporary attribute to retain DOM-like
     * node identity across its string-based Java command backend. It is never
     * emitted by the public ScoreCore API.
     */
    private static final String INTERNAL_NODE_ID_VALUE_PREFIX = "mksj:";
    /*
     * Node keeps its reindex map outside XML.  ScoreCore's Java facade carries
     * a temporary attribute only while it crosses this string-based backend.
     * Keep the exact attribute name in a call-scoped context: accepting every
     * vendor attribute with a private-looking prefix would make user markup
     * addressable as a ScoreCore node.
     */
    private static final ThreadLocal<String> ACTIVE_INTERNAL_NODE_ID_ATTRIBUTE = new ThreadLocal<String>();

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

        String title = directChildTrimmedTextOrNull(directChild(root, "work"), "work-title");
        if (title == null) {
            title = directChildTrimmedTextOrNull(root, "movement-title");
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
            String partId = trimmedAttributeOrNull(part, "id");
            for (Element measure : directChildren(part, "measure")) {
                String currentMeasureNumber = trimmedAttributeOrEmpty(measure, "number");
                if (!equalsNullable(currentMeasureNumber, measureNumber)) {
                    continue;
                }
                List<MusicXmlMeasureInspection.Note> notes = new ArrayList<MusicXmlMeasureInspection.Note>();
                List<Element> noteElements = directChildren(measure, "note");
                for (int index = 0; index < noteElements.size(); index++) {
                    Element note = noteElements.get(index);
                    IndexedNote indexed = findIndexedNote(indexedNotes, partId, measureNumber, index + 1);
                    String voice = getVoiceText(note);
                    Double duration = readCliStateDuration(note);
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

    /**
     * Validates a ScoreCore command while retaining that facade's exact
     * temporary node-id attribute.  Generic MusicXML entry points deliberately
     * do not infer identity from source attributes.
     */
    public static MusicXmlCommandValidation validateMusicXmlCommand(String xmlText, String commandJson,
            String internalNodeIdAttribute) {
        String previousAttribute = pushInternalNodeIdAttribute(internalNodeIdAttribute);
        try {
            return validateMusicXmlCommand(xmlText, commandJson);
        } finally {
            restoreInternalNodeIdAttribute(previousAttribute);
        }
    }

    /**
     * Applies the upstream ScoreCore editable-voice boundary before normal
     * command validation. A blank restriction means every voice is editable.
     */
    public static MusicXmlCommandValidation.Diagnostic validateMusicXmlCommandEditableVoice(String commandJson,
            String editableVoice) {
        String normalizedEditableVoice = trimToNull(editableVoice);
        if (normalizedEditableVoice == null) {
            return null;
        }
        Map<String, Object> command = MusicXmlCommandJson.parseObject(commandJson);
        if ("ui_noop".equals(MusicXmlCommandJson.stringValue(command, "type"))) {
            return null;
        }
        String commandVoice = MusicXmlCommandJson.stringValue(command, "voice");
        if (normalizedEditableVoice.equals(commandVoice)) {
            return null;
        }
        return new MusicXmlCommandValidation.Diagnostic("MVP_UNSUPPORTED_NON_EDITABLE_VOICE",
                "Voice " + commandVoiceForDiagnostic(command) + " is not editable in MVP.");
    }

    /**
     * Verifies that a command payload is a JSON object before the CLI consumes
     * the MusicXML input stream.
     */
    public static void requireMusicXmlCommandJsonObject(String commandJson) {
        MusicXmlCommandJson.parseObject(commandJson);
    }

    /**
     * Mirrors cli-api.ts selector normalization before a command enters
     * ScoreCore.  The generic state API retains its legacy selector support;
     * this explicit result carries the CLI's useful invalid/ambiguous selector
     * diagnostics and produces a node-id command for the stateful facade.
     */
    public static CliCommandNormalization normalizeCliCommandSelectors(String xmlText, String commandJson) {
        Document doc = parseMusicXmlDocument(xmlText);
        Element root = requireScorePartwiseRoot(doc);
        Map<String, Object> command = MusicXmlCommandJson.parseObject(commandJson);
        Map<String, Object> normalized = new java.util.LinkedHashMap<String, Object>(command);
        List<IndexedNote> indexedNotes = buildIndexedMeasureNotes(root);

        if (normalized.containsKey("selector") && !normalized.containsKey("targetNodeId")) {
            SelectorResolution resolution = resolveCliSelector(indexedNotes, normalized.get("selector"), "selector");
            if (!resolution.isOk()) {
                return CliCommandNormalization.failure("Failed to resolve CLI command selector: "
                        + resolution.getMessage());
            }
            normalized.put("targetNodeId", resolution.getNodeId());
            if (!normalized.containsKey("voice") && resolution.getVoice() != null) {
                normalized.put("voice", resolution.getVoice());
            }
        }
        if (normalized.containsKey("anchor_selector") && !normalized.containsKey("anchorNodeId")) {
            SelectorResolution resolution = resolveCliSelector(indexedNotes, normalized.get("anchor_selector"),
                    "anchor_selector");
            if (!resolution.isOk()) {
                return CliCommandNormalization.failure("Failed to resolve CLI command selector: "
                        + resolution.getMessage());
            }
            normalized.put("anchorNodeId", resolution.getNodeId());
            if (!normalized.containsKey("voice") && resolution.getVoice() != null) {
                normalized.put("voice", resolution.getVoice());
            }
        }
        normalized.remove("selector");
        normalized.remove("anchor_selector");
        return CliCommandNormalization.success(MusicXmlCommandJson.toJson(normalized));
    }

    public static MusicXmlCommandValidation validateMusicXmlForSave(String xmlText, boolean dirty) {
        return validateMusicXmlForSave(xmlText, dirty, null);
    }

    /**
     * Validates a save result using the same editable-voice timing boundary as
     * upstream ScoreCore. Note integrity remains global, while overfull timing
     * is checked only for the configured editable voice when present.
     */
    public static MusicXmlCommandValidation validateMusicXmlForSave(String xmlText, boolean dirty,
            String editableVoice) {
        Document doc = parseMusicXmlDocument(xmlText);
        Element root = requireScorePartwiseRoot(doc);
        MusicXmlCommandValidation.Diagnostic invalidNote = findInvalidNoteDiagnostic(root, !dirty);
        if (invalidNote != null) {
            return validationFailure(invalidNote.getCode(), invalidNote.getMessage());
        }
        MusicXmlCommandValidation.Diagnostic overfull = findOverfullDiagnostic(root, trimToNull(editableVoice));
        if (overfull != null) {
            return validationFailure(overfull.getCode(), overfull.getMessage());
        }
        return new MusicXmlCommandValidation(true, false, new ArrayList<String>(), new ArrayList<String>(),
                new ArrayList<MusicXmlCommandValidation.Diagnostic>());
    }

    /** Validates save integrity using ScoreCore's exact temporary node-id attribute. */
    public static MusicXmlCommandValidation validateMusicXmlForSave(String xmlText, boolean dirty,
            String editableVoice, String internalNodeIdAttribute) {
        String previousAttribute = pushInternalNodeIdAttribute(internalNodeIdAttribute);
        try {
            return validateMusicXmlForSave(xmlText, dirty, editableVoice);
        } finally {
            restoreInternalNodeIdAttribute(previousAttribute);
        }
    }

    public static String applyMusicXmlCommand(String xmlText, String commandJson) {
        return applyMusicXmlCommandWithWarnings(xmlText, commandJson).getOutput();
    }

    /**
     * Identifies validation failures that upstream ScoreCore reaches only after
     * taking a serialized snapshot and then restores through parse/reindex.
     */
    public static boolean scoreCoreRestoresSnapshotAfterValidationFailure(String commandJson,
            MusicXmlCommandValidation validation) {
        if (validation == null || validation.isOk() || validation.getDiagnostics().isEmpty()) {
            return false;
        }
        try {
            Map<String, Object> command = MusicXmlCommandJson.parseObject(commandJson);
            String type = MusicXmlCommandJson.stringValue(command, "type");
            if (!("change_duration".equals(type) || "split_note".equals(type))) {
                return false;
            }
            return "MEASURE_OVERFULL".equals(validation.getDiagnostics().get(0).getCode());
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Applies one command and retains successful timing warnings for the CLI
     * diagnostics channel.
     */
    public static MusicXmlCommandApplyResult applyMusicXmlCommandWithWarnings(String xmlText, String commandJson) {
        Document doc = parseMusicXmlDocument(xmlText);
        Element root = requireScorePartwiseRoot(doc);
        Map<String, Object> command = MusicXmlCommandJson.parseObject(commandJson);
        MusicXmlCommandValidation validation = validateMusicXmlCommand(root, command);
        if (!validation.isOk()) {
            return new MusicXmlCommandApplyResult(validation.toApplyJson(), new ArrayList<MusicXmlCommandValidation.Warning>());
        }

        String type = MusicXmlCommandJson.stringValue(command, "type");
        if ("ui_noop".equals(type)) {
            return new MusicXmlCommandApplyResult(xmlText, new ArrayList<MusicXmlCommandValidation.Warning>());
        }

        List<IndexedNote> indexedNotes = buildIndexedMeasureNotes(root);
        IndexedNote target = resolveCommandTarget(command, indexedNotes);
        if (target == null) {
            return new MusicXmlCommandApplyResult(
                    validationFailure("MVP_TARGET_NOT_FOUND", "Command target was not resolved.").toApplyJson(),
                    new ArrayList<MusicXmlCommandValidation.Warning>());
        }

        Element note = findNoteElement(root, target);
        if (note == null) {
            return new MusicXmlCommandApplyResult(
                    validationFailure("MVP_TARGET_NOT_FOUND", "Unknown nodeId: " + target.nodeId).toApplyJson(),
                    new ArrayList<MusicXmlCommandValidation.Warning>());
        }

        List<MusicXmlCommandValidation.Warning> warnings = new ArrayList<MusicXmlCommandValidation.Warning>();
        // ScoreCore normalizes a missing/empty target-note voice before every edit
        // that mutates an existing note, not only pitch edits. This also ensures a
        // dirty save can pass note-integrity validation after duration/delete/split.
        if (!"insert_note_after".equals(type)) {
            ensureCommandVoice(note, command);
        }
        if ("change_to_pitch".equals(type)) {
            setPitch(note, MusicXmlCommandJson.castMap(command.get("pitch")));
            autoAssignGrandStaffByPitch(note);
        } else if ("change_duration".equals(type)) {
            Object rawVoice = command.get("voice");
            String generatedRestVoice = command.containsKey("voice")
                    ? javascriptDomTextContentValue(rawVoice)
                    : "undefined";
            warnings.addAll(changeDuration(note, MusicXmlCommandJson.stringValue(command, "voice"), generatedRestVoice,
                    MusicXmlCommandJson.finiteIntegerValue(command, "duration").doubleValue()));
        } else if ("delete_note".equals(type)) {
            deleteNote(note, MusicXmlCommandJson.stringValue(command, "voice"));
        } else if ("split_note".equals(type)) {
            MusicXmlCommandValidation.Diagnostic splitFailure = splitNote(note,
                    MusicXmlCommandJson.stringValue(command, "voice"));
            if (splitFailure != null) {
                return new MusicXmlCommandApplyResult(validationFailure(splitFailure.getCode(), splitFailure.getMessage())
                        .toApplyJson(), new ArrayList<MusicXmlCommandValidation.Warning>());
            }
        } else if ("insert_note_after".equals(type)) {
            insertNoteAfter(note, command);
            warnings.addAll(validation.getWarnings());
        }
        return new MusicXmlCommandApplyResult(serializeMusicXmlDocument(doc), warnings);
    }

    /**
     * Applies a ScoreCore command while retaining that facade's exact
     * temporary node-id attribute across parse/serialize.
     */
    public static MusicXmlCommandApplyResult applyMusicXmlCommandWithWarnings(String xmlText, String commandJson,
            String internalNodeIdAttribute) {
        String previousAttribute = pushInternalNodeIdAttribute(internalNodeIdAttribute);
        try {
            return applyMusicXmlCommandWithWarnings(xmlText, commandJson);
        } finally {
            restoreInternalNodeIdAttribute(previousAttribute);
        }
    }

    /**
     * Retains the voice-only mutation that upstream {@code ScoreCore.dispatch}
     * performs just before certain command-local failures.  It intentionally
     * does not mark the caller dirty and is only used by the stateful facade;
     * stateless validation/apply APIs keep their ordinary atomic contract.
     */
    public static String applyScoreCoreFailureVoiceNormalization(String xmlText, String commandJson,
            List<MusicXmlCommandValidation.Diagnostic> diagnostics, String internalNodeIdAttribute) {
        if (!hasScoreCorePostEnsureFailure(diagnostics)) {
            return xmlText;
        }
        String previousAttribute = pushInternalNodeIdAttribute(internalNodeIdAttribute);
        try {
            Document doc = parseMusicXmlDocument(xmlText);
            Element root = requireScorePartwiseRoot(doc);
            Map<String, Object> command = MusicXmlCommandJson.parseObject(commandJson);
            String type = MusicXmlCommandJson.stringValue(command, "type");
            if (!("change_duration".equals(type) || "delete_note".equals(type) || "split_note".equals(type))) {
                return xmlText;
            }
            IndexedNote target = resolveCommandTarget(command, buildIndexedMeasureNotes(root));
            if (target == null) {
                return xmlText;
            }
            Element note = findNoteElement(root, target);
            if (note == null) {
                return xmlText;
            }
            ensureCommandVoice(note, command);
            return serializeMusicXmlDocument(doc);
        } finally {
            restoreInternalNodeIdAttribute(previousAttribute);
        }
    }

    private static boolean hasScoreCorePostEnsureFailure(List<MusicXmlCommandValidation.Diagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.size() != 1) {
            return false;
        }
        MusicXmlCommandValidation.Diagnostic diagnostic = diagnostics.get(0);
        if (diagnostic == null) {
            return false;
        }
        String code = diagnostic.getCode();
        String message = diagnostic.getMessage();
        if ("MVP_INVALID_NOTE_DURATION".equals(code)
                && "Target note has invalid duration.".equals(message)) {
            return true;
        }
        if (!"MVP_INVALID_COMMAND_PAYLOAD".equals(code)) {
            return false;
        }
        return "Tuplet durations are not allowed because this measure/voice has no tuplet context.".equals(message)
                || "split_note requires duration >= 2.".equals(message)
                || "split_note requires an even duration value.".equals(message);
    }

    private static MusicXmlCommandValidation validateMusicXmlCommand(Element root, Map<String, Object> command) {
        String type = MusicXmlCommandJson.stringValue(command, "type");
        if (!"change_to_pitch".equals(type) && !"change_duration".equals(type) && !"delete_note".equals(type)
                && !"split_note".equals(type) && !"insert_note_after".equals(type) && !"ui_noop".equals(type)) {
            // commands.ts has no default discriminator branch. At the public Core
            // boundary an unrecognized runtime value therefore has no target id and
            // receives the ordinary missing-target diagnostic.
            return validationFailure("MVP_COMMAND_TARGET_MISSING", "Command target is missing.");
        }
        if ("ui_noop".equals(type)) {
            return new MusicXmlCommandValidation(true, false, new ArrayList<String>(), new ArrayList<String>(),
                    new ArrayList<MusicXmlCommandValidation.Diagnostic>());
        }

        if ("change_to_pitch".equals(type) && !isValidPitch(MusicXmlCommandJson.castMap(command.get("pitch")))) {
            return validationFailure("MVP_INVALID_COMMAND_PAYLOAD", "change_to_pitch.pitch is invalid.");
        }
        if ("change_duration".equals(type)
                && !isPositiveInteger(MusicXmlCommandJson.finiteIntegerValue(command, "duration"))) {
            return validationFailure("MVP_INVALID_COMMAND_PAYLOAD",
                    "change_duration.duration must be a positive integer.");
        }
        if ("insert_note_after".equals(type)) {
            Map<String, Object> notePayload = MusicXmlCommandJson.castMap(command.get("note"));
            if (notePayload == null
                    || !isPositiveInteger(MusicXmlCommandJson.finiteIntegerValue(notePayload, "duration"))) {
                return validationFailure("MVP_INVALID_COMMAND_PAYLOAD",
                        "insert_note_after.note.duration must be a positive integer.");
            }
            if (!isValidPitch(MusicXmlCommandJson.castMap(notePayload.get("pitch")))) {
                return validationFailure("MVP_INVALID_COMMAND_PAYLOAD", "insert_note_after.note.pitch is invalid.");
            }
        }

        CommandNodeId resolvedCommandNodeId = commandNodeId(command);
        String nodeId = resolvedCommandNodeId == null ? null : resolvedCommandNodeId.getDisplayValue();
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

        Element note = findNoteElement(root, target);
        MusicXmlCommandValidation noteKindFailure = validateSupportedNoteKind(type, note);
        if (noteKindFailure != null) {
            return noteKindFailure;
        }
        if ("split_note".equals(type)) {
            Double duration = parseFiniteNumberOrNull(directChildRawText(note, "duration"));
            if (duration == null || !isJavaScriptInteger(duration.doubleValue()) || duration.doubleValue() <= 1) {
                return validationFailure("MVP_INVALID_COMMAND_PAYLOAD", "split_note requires duration >= 2.");
            }
            if (duration.doubleValue() % 2 != 0) {
                return validationFailure("MVP_INVALID_COMMAND_PAYLOAD", "split_note requires an even duration value.");
            }
        }

        String commandVoice = MusicXmlCommandJson.stringValue(command, "voice");
        // Selector addressing is a Java API extension. Keep its legacy convenience
        // default, but do not apply it to Node-compatible node-id commands: upstream
        // ScoreCore compares a missing command voice with the target voice directly.
        if (commandVoice == null && resolvedCommandNodeId == null
                && MusicXmlCommandJson.castMap(command.get("selector")) != null) {
            commandVoice = target.selector.getVoice();
        }
        // xmlUtils.getVoiceText() preserves the distinction between a missing
        // <voice> and a trimmed-but-empty one.  Validators use JavaScript
        // truthiness here, while lane comparisons below retain the raw empty
        // string distinction.
        String targetVoice = getVoiceText(note);
        if (targetVoice != null && targetVoice.length() > 0 && !targetVoice.equals(commandVoice)) {
            return validationFailure("MVP_UNSUPPORTED_NON_EDITABLE_VOICE",
                    "Target note voice (" + targetVoice + ") does not match command voice ("
                            + commandVoiceForDiagnostic(command) + ").");
        }
        if ("change_duration".equals(type)
                && isTripletDuration(note, MusicXmlCommandJson.finiteIntegerValue(command, "duration"))
                && !measureVoiceHasTupletContext(note, commandVoice)) {
            return validationFailure("MVP_INVALID_COMMAND_PAYLOAD",
                    "Tuplet durations are not allowed because this measure/voice has no tuplet context.");
        }
        MusicXmlCommandValidation structuralFailure = validateStructuralEditBoundary(type, note, command,
                commandVoice);
        if (structuralFailure != null) {
            return structuralFailure;
        }
        // ScoreCore checks the existing duration immediately before replacing a
        // deleted note with a rest.  Do the same validation here so both this
        // stateless API and the stateful ScoreCore facade report its original
        // diagnostic instead of serializing a fabricated one-tick rest.
        if ("delete_note".equals(type)) {
            Double duration = parseFiniteNumberOrNull(directChildRawText(note, "duration"));
            if (duration == null || duration.doubleValue() <= 0) {
                return validationFailure("MVP_INVALID_NOTE_DURATION", "Target note has invalid duration.");
            }
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
        // ScoreCore.collectAffectedMeasureNumbers reads the raw measure attribute:
        // it returns [] for a missing/empty number and preserves non-empty
        // whitespace exactly instead of deriving this public result from a
        // normalized selector.
        Element targetMeasure = findAncestor(note, "measure");
        String measureNumber = targetMeasure == null ? "" : targetMeasure.getAttribute("number");
        if (measureNumber != null && measureNumber.length() > 0) {
            affectedMeasureNumbers.add(measureNumber);
        }
        return new MusicXmlCommandValidation(true, true, changedNodeIds, affectedMeasureNumbers, warnings,
                new ArrayList<MusicXmlCommandValidation.Diagnostic>());
    }

    private static MusicXmlCommandValidation.Diagnostic findInvalidNoteDiagnostic(Element root,
            boolean ignoreMissingVoice) {
        // ScoreCore uses document.querySelectorAll("note"), not a
        // score-partwise-only traversal. Retain that DOM selector scope for
        // malformed/vendor-wrapped input as well as normal MusicXML.
        for (Element note : descendantElements(root, "note")) {
            String voice = getVoiceText(note);
            if ((voice == null || voice.length() == 0) && !ignoreMissingVoice) {
                return new MusicXmlCommandValidation.Diagnostic("MVP_INVALID_NOTE_VOICE",
                        "Note is missing a valid <voice> value.");
            }
            Double duration = parseFiniteNumberOrNull(directChildRawText(note, "duration"));
            if (directChild(note, "grace") == null && (duration == null || duration.doubleValue() <= 0)) {
                return new MusicXmlCommandValidation.Diagnostic("MVP_INVALID_NOTE_DURATION",
                        "Note is missing a valid positive <duration> value. " + describeNoteContext(note));
            }
            MusicXmlCommandValidation.Diagnostic pitchDiagnostic = validateNotePitch(note);
            if (pitchDiagnostic != null) {
                return pitchDiagnostic;
            }
        }
        return null;
    }

    /** Mirrors the diagnostic context emitted by upstream ScoreCore on save. */
    private static String describeNoteContext(Element note) {
        Element measure = findAncestor(note, "measure");
        Element part = findAncestor(note, "part");
        String partId = part == null ? null : trimToNull(part.getAttribute("id"));
        String measureNumber = measure == null ? null : trimToNull(measure.getAttribute("number"));
        String voice = getVoiceText(note);
        String nodeId = internalNodeId(note);
        return "part=" + (partId == null ? "(unknown-part)" : partId) + " measure="
                + (measureNumber == null ? "(unknown-measure)" : measureNumber) + " voice="
                + (voice == null || voice.length() == 0 ? "(missing-voice)" : voice) + " nodeId="
                + (nodeId == null ? "(no-node-id)" : nodeId) + " grace="
                + (directChild(note, "grace") != null) + " cue=" + (directChild(note, "cue") != null) + " rest="
                + (directChild(note, "rest") != null) + " chord=" + (directChild(note, "chord") != null);
    }

    private static MusicXmlCommandValidation.Diagnostic findOverfullDiagnostic(Element root, String editableVoice) {
        // Matches document.querySelectorAll("measure") and the nested note
        // selectors used by ScoreCore.findOverfullDiagnostic().
        for (Element measure : descendantElements(root, "measure")) {
            Set<String> voices = new LinkedHashSet<String>();
            if (editableVoice != null) {
                voices.add(editableVoice);
            } else {
                for (Element note : descendantElements(measure, "note")) {
                    String voice = getVoiceText(note);
                    if (voice != null && voice.length() > 0) {
                        voices.add(voice);
                    }
                }
            }
            if (voices.isEmpty()) {
                continue;
            }
            Element firstNote = firstDescendant(measure, "note");
            if (firstNote == null) {
                continue;
            }
            for (String voice : voices) {
                FloatingMeasureTiming timing = getFloatingMeasureTimingForVoice(firstNote, voice);
                if (timing == null) {
                    continue;
                }
                int tolerance = computeTupletRoundingTolerance(measure, voice);
                if (timing.occupied > timing.capacity + tolerance) {
                    return new MusicXmlCommandValidation.Diagnostic("MEASURE_OVERFULL",
                            "Occupied time " + javascriptNumberText(timing.occupied)
                                    + " exceeds capacity " + javascriptNumberText(timing.capacity) + ".");
                }
            }
        }
        return null;
    }

    private static int computeTupletRoundingTolerance(Element measure, String voice) {
        int tupletOnsetCount = 0;
        for (Element note : directChildren(measure, "note")) {
            if (!equalsNullable(getVoiceText(note), voice)) {
                continue;
            }
            if (directChild(note, "chord") != null || directChild(note, "time-modification") == null) {
                continue;
            }
            Double duration = parseFiniteNumberOrNull(directChildRawText(note, "duration"));
            if (duration != null && duration.doubleValue() > 0) {
                tupletOnsetCount++;
            }
        }
        return tupletOnsetCount <= 0 ? 0 : tupletOnsetCount / 2;
    }

    private static MusicXmlCommandValidation.Diagnostic validateNotePitch(Element note) {
        boolean hasRest = directChild(note, "rest") != null;
        boolean hasChord = directChild(note, "chord") != null;
        Element pitch = directChild(note, "pitch");
        if (hasRest && hasChord) {
            return new MusicXmlCommandValidation.Diagnostic("MVP_INVALID_NOTE_PITCH",
                    "Note must not contain both <rest> and <chord>.");
        }
        if (hasRest && pitch != null) {
            return new MusicXmlCommandValidation.Diagnostic("MVP_INVALID_NOTE_PITCH",
                    "Rest note must not contain <pitch>.");
        }
        if (hasChord && pitch == null) {
            return new MusicXmlCommandValidation.Diagnostic("MVP_INVALID_NOTE_PITCH",
                    "Chord note must contain a valid <pitch>.");
        }
        if (pitch == null) {
            if (hasRest) {
                return null;
            }
            return new MusicXmlCommandValidation.Diagnostic("MVP_INVALID_NOTE_PITCH",
                    "Non-rest note is missing a valid <pitch>.");
        }
        String step = descendantText(pitch, "step");
        if (!("A".equals(step) || "B".equals(step) || "C".equals(step) || "D".equals(step) || "E".equals(step)
                || "F".equals(step) || "G".equals(step))) {
            return new MusicXmlCommandValidation.Diagnostic("MVP_INVALID_NOTE_PITCH", "Pitch step is invalid.");
        }
        Element octaveNode = firstDescendant(pitch, "octave");
        Double octave = parseFiniteNumberOrNull(octaveNode == null ? "" : octaveNode.getTextContent());
        if (octave == null || octave.doubleValue() != Math.rint(octave.doubleValue())) {
            return new MusicXmlCommandValidation.Diagnostic("MVP_INVALID_NOTE_PITCH", "Pitch octave is invalid.");
        }
        Element alterNode = firstDescendant(pitch, "alter");
        if (alterNode != null) {
            Double alter = parseFiniteNumberOrNull(alterNode.getTextContent());
            if (alter == null || alter.doubleValue() != Math.rint(alter.doubleValue()) || alter.doubleValue() < -2
                    || alter.doubleValue() > 2) {
                return new MusicXmlCommandValidation.Diagnostic("MVP_INVALID_NOTE_PITCH", "Pitch alter is invalid.");
            }
        }
        return null;
    }

    private static IndexedNote resolveCommandTarget(Map<String, Object> command, List<IndexedNote> indexedNotes) {
        CommandNodeId nodeId = commandNodeId(command);
        if (nodeId != null) {
            return nodeId.isStringValue() ? findIndexedNoteByNodeId(indexedNotes, nodeId.getDisplayValue()) : null;
        }
        if ("insert_note_after".equals(MusicXmlCommandJson.stringValue(command, "type"))) {
            return resolveSelector(indexedNotes, MusicXmlCommandJson.castMap(command.get("anchor_selector")));
        }
        return resolveSelector(indexedNotes, MusicXmlCommandJson.castMap(command.get("selector")));
    }

    private static CommandNodeId commandNodeId(Map<String, Object> command) {
        Object rawNodeId;
        if ("insert_note_after".equals(MusicXmlCommandJson.stringValue(command, "type"))) {
            rawNodeId = command.get("anchorNodeId");
        } else {
            rawNodeId = command.get("targetNodeId");
        }
        // ScoreCore uses `if (!targetId)`. A supplied truthy JSON value is
        // therefore a target lookup even when it is not a string; Map lookup
        // then fails without coercing that value to a string key.
        if (!javascriptTruthy(rawNodeId)) {
            return null;
        }
        return new CommandNodeId(rawNodeId instanceof String,
                MusicXmlCommandJson.javascriptStringValue(rawNodeId));
    }

    private static boolean javascriptTruthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            return !Double.isNaN(number) && number != 0d;
        }
        if (value instanceof String) {
            return ((String) value).length() > 0;
        }
        return true;
    }

    private static String commandVoiceForDiagnostic(Map<String, Object> command) {
        if (!command.containsKey("voice")) {
            return "undefined";
        }
        return MusicXmlCommandJson.javascriptStringValue(command.get("voice"));
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

    private static MusicXmlCommandValidation validateStructuralEditBoundary(String type, Element note,
            Map<String, Object> command, String commandVoice) {
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
            String nextVoice = nextNote == null ? null : getVoiceText(nextNote);
            if (nextNote != null && !commandVoiceStrictlyMatches(nextVoice, command)) {
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

    /** Mirrors {@code nextVoice !== command.voice} in validateInsertLaneBoundary. */
    private static boolean commandVoiceStrictlyMatches(String targetVoice, Map<String, Object> command) {
        // A missing JSON field is JavaScript undefined. It never strictly equals
        // a MusicXML string or the null used for a missing <voice> child.
        if (!command.containsKey("voice")) {
            return false;
        }
        Object commandVoice = command.get("voice");
        if (commandVoice == null) {
            return targetVoice == null;
        }
        return commandVoice instanceof String && commandVoice.equals(targetVoice);
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
        Double projected = null;
        if ("change_duration".equals(type)) {
            Double oldDuration = parseFiniteNumberOrNull(directChildRawText(note, "duration"));
            Double newDuration = MusicXmlCommandJson.finiteIntegerValue(command, "duration");
            if (oldDuration != null && newDuration != null) {
                projected = Double.valueOf(timing.occupied - oldDuration.doubleValue() + newDuration.doubleValue());
            }
        } else if ("insert_note_after".equals(type)) {
            Map<String, Object> notePayload = MusicXmlCommandJson.castMap(command.get("note"));
            Double insertedDuration = MusicXmlCommandJson.finiteIntegerValue(notePayload, "duration");
            if (insertedDuration != null) {
                projected = Double.valueOf(timing.occupied + insertedDuration.doubleValue());
            }
        } else if ("split_note".equals(type)) {
            projected = Double.valueOf(timing.occupied);
        }
        if (projected != null && projected.doubleValue() > timing.capacity) {
            if ("change_duration".equals(type)) {
                double overflow = projected.doubleValue() - timing.capacity;
                if (isJavaScriptInteger(overflow)
                        && overflow <= availableRestDurationForExpansion(note, commandVoice, overflow)) {
                    return null;
                }
            }
            return validationFailure("MEASURE_OVERFULL",
                    "Projected occupied time " + javascriptNumberText(projected.doubleValue())
                            + " exceeds capacity " + javascriptNumberText(timing.capacity) + ".");
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
        Double insertedDuration = MusicXmlCommandJson.finiteIntegerValue(notePayload, "duration");
        if (insertedDuration == null) {
            return warnings;
        }
        double projected = timing.occupied + insertedDuration.doubleValue();
        if (projected < timing.capacity) {
            warnings.add(underfullTimingWarning(projected, timing.capacity));
        }
        return warnings;
    }

    private static MusicXmlCommandValidation.Warning underfullTimingWarning(double projected, double capacity) {
        return new MusicXmlCommandValidation.Warning("MEASURE_UNDERFULL",
                "Projected occupied time " + javascriptNumberText(projected) + " is below capacity "
                        + javascriptNumberText(capacity) + ".");
    }

    private static boolean isTripletDuration(Element note, Double duration) {
        if (duration == null) {
            return false;
        }
        Double divisions = resolveEffectiveDivisions(note);
        if (divisions == null || divisions.doubleValue() <= 0) {
            return false;
        }
        DurationNotation notation = durationToNotation(duration.doubleValue(), divisions.doubleValue());
        return notation != null && notation.triplet;
    }

    private static boolean measureVoiceHasTupletContext(Element target, String voice) {
        Element measure = findAncestor(target, "measure");
        if (measure == null) {
            return false;
        }
        for (Element note : directChildren(measure, "note")) {
            if (!equalsNullable(getVoiceText(note), voice)) {
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
        upsertSimpleChild(pitchElement, "octave",
                javascriptNumberText(MusicXmlCommandJson.finiteIntegerValue(pitch, "octave").doubleValue()));
    }

    private static void ensureCommandVoice(Element note, Map<String, Object> command) {
        String existingVoice = getVoiceText(note);
        if (existingVoice != null && existingVoice.length() > 0) {
            return;
        }
        // Matches ensureVoiceValue(): String(fallbackVoice).trim() || "1".
        // The command type is statically constrained upstream, but retaining the
        // JavaScript conversion here keeps JSON boundary behavior deterministic.
        String fallbackVoice = command.containsKey("voice")
                ? MusicXmlCommandJson.javascriptStringValue(command.get("voice"))
                : "undefined";
        String normalizedFallback = fallbackVoice.trim();
        upsertSimpleChild(note, "voice", normalizedFallback.length() == 0 ? "1" : normalizedFallback);
    }

    private static void setDurationValue(Element note, int duration) {
        upsertSimpleChild(note, "duration", Integer.toString(duration));
        syncSimpleTypeFromDuration(note, duration);
    }

    /** Mirrors xmlUtils.setDurationValue for source durations that are finite but non-integral. */
    private static void setDurationValue(Element note, double duration) {
        upsertSimpleChild(note, "duration", javascriptNumberText(duration));
        if (isJavaScriptInteger(duration) && duration > 0) {
            syncSimpleTypeFromDuration(note, duration);
        }
    }

    private static void autoAssignGrandStaffByPitch(Element note) {
        if (!hasGrandStaffContext(note)) {
            return;
        }
        Double midi = notePitchToMidi(note);
        if (midi == null) {
            return;
        }
        Element staff = directChild(note, "staff");
        String existingStaffText = directChildText(note, "staff");
        Integer previousStaff = "1".equals(existingStaffText) ? Integer.valueOf(1)
                : ("2".equals(existingStaffText) ? Integer.valueOf(2) : null);
        int desiredStaff = StaffClefPolicy.pickStaffByPitchWithHysteresis(midi.doubleValue(), previousStaff);
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
            Double parsedStaves = parseFiniteNumberOrNull(directChildText(attributes, "staves"));
            if (parsedStaves != null && parsedStaves.doubleValue() == Math.rint(parsedStaves.doubleValue())
                    && parsedStaves.doubleValue() > 0 && parsedStaves.doubleValue() <= Integer.MAX_VALUE) {
                staves = (int) parsedStaves.doubleValue();
            }
            for (Element clef : directChildren(attributes, "clef")) {
                // The source uses a CSS attribute selector, which is an exact
                // match: number=" 1 " is not clef[number="1"].
                String number = clef.getAttribute("number");
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

    private static Double notePitchToMidi(Element note) {
        Element pitch = directChild(note, "pitch");
        if (pitch == null) {
            return null;
        }
        String step = directChildText(pitch, "step");
        Double octave = parseFiniteNumberOrNull(directChildText(pitch, "octave"));
        Double alter = parseFiniteNumberOrNull(directChildText(pitch, "alter"));
        Integer base = semitoneByStep(step);
        if (base == null || octave == null) {
            return null;
        }
        return Double.valueOf((octave.doubleValue() + 1) * 12 + base.intValue()
                + (alter == null ? 0 : alter.doubleValue()));
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

    private static List<MusicXmlCommandValidation.Warning> changeDuration(Element note, String voice,
            String generatedRestVoice, double duration) {
        List<MusicXmlCommandValidation.Warning> warnings = new ArrayList<MusicXmlCommandValidation.Warning>();
        Double oldDuration = parseFiniteNumberOrNull(directChildRawText(note, "duration"));
        // Do not replace a missing/non-string command voice with the value
        // normalized on the target. ScoreCore keeps the raw command value for
        // timing comparisons, so undefined does not equal the new "undefined"
        // DOM text written by ensureVoiceValue().
        String effectiveVoice = voice;
        MeasureTiming timing = getMeasureTimingForVoice(note, effectiveVoice);
        double underfullDelta = 0;
        double adjustedProjected = -1;
        if (oldDuration != null && timing != null) {
            double projected = timing.occupied - oldDuration.doubleValue() + duration;
            double overflow = projected - timing.capacity;
            if (overflow > 0 && isJavaScriptInteger(overflow)) {
                double integerOverflow = overflow;
                double consumedAfter = consumeFollowingRestsForDurationExpansion(note, effectiveVoice, integerOverflow);
                double remainingAfter = integerOverflow - consumedAfter;
                if (remainingAfter > 0) {
                    consumePrecedingRestsForDurationExpansion(note, effectiveVoice, remainingAfter);
                }
            }
            MeasureTiming adjusted = getMeasureTimingForVoice(note, effectiveVoice);
            if (adjusted != null) {
                adjustedProjected = adjusted.occupied - oldDuration.doubleValue() + duration;
                if (adjustedProjected < timing.capacity) {
                    underfullDelta = timing.capacity - adjustedProjected;
                }
            }
        }
        setDurationValue(note, duration);
        if (underfullDelta > 0) {
            boolean filled = isJavaScriptInteger(underfullDelta)
                    && fillUnderfullGapAfterTarget(note, effectiveVoice, generatedRestVoice, underfullDelta);
            if (!filled && adjustedProjected >= 0) {
                warnings.add(underfullTimingWarning(adjustedProjected, timing.capacity));
            }
        }
        return warnings;
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
        // xmlUtils.getDurationValue uses Number(), and ScoreCore passes that
        // number back as forcedDuration.  This intentionally retains a valid
        // fractional source duration (for example "1.5") rather than coercing
        // it to one tick as the older integer-only implementation did.
        Double duration = parseFiniteNumberOrNull(directChildRawText(note, "duration"));
        if (duration == null || duration.doubleValue() <= 0) {
            throw new IllegalArgumentException("Target note has invalid duration.");
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
        upsertSimpleChild(note, "duration", javascriptNumberText(duration.doubleValue()));
        upsertSimpleChild(note, "voice", voice);
    }

    private static MusicXmlCommandValidation.Diagnostic splitNote(Element note, String voice) {
        String effectiveVoice = voice;
        MeasureTiming timingBefore = getMeasureTimingForVoice(note, effectiveVoice);
        Double duration = parseFiniteNumberOrNull(directChildRawText(note, "duration"));
        if (duration == null || !isJavaScriptInteger(duration.doubleValue()) || duration.doubleValue() <= 1) {
            return new MusicXmlCommandValidation.Diagnostic("MVP_INVALID_COMMAND_PAYLOAD",
                    "split_note requires duration >= 2.");
        }
        if (duration.doubleValue() % 2 != 0) {
            return new MusicXmlCommandValidation.Diagnostic("MVP_INVALID_COMMAND_PAYLOAD",
                    "split_note requires an even duration value.");
        }
        double half = duration.doubleValue() / 2;
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
        if (timingBefore == null) {
            return null;
        }
        MeasureTiming timingAfter = getMeasureTimingForVoice(note, effectiveVoice);
        if (timingAfter == null) {
            return new MusicXmlCommandValidation.Diagnostic("MVP_COMMAND_EXECUTION_FAILED",
                    "Failed to validate split timing.");
        }
        if (timingAfter.occupied != timingBefore.occupied) {
            return new MusicXmlCommandValidation.Diagnostic("MVP_COMMAND_EXECUTION_FAILED",
                    "Split changed lane timing unexpectedly near backup/forward boundary.");
        }
        if (timingAfter.occupied > timingAfter.capacity) {
            return new MusicXmlCommandValidation.Diagnostic("MEASURE_OVERFULL",
                    "Projected occupied time " + javascriptNumberText(timingAfter.occupied)
                            + " exceeds capacity " + javascriptNumberText(timingAfter.capacity) + ".");
        }
        return null;
    }

    private static void insertNoteAfter(Element anchor, Map<String, Object> command) {
        String voice = command.containsKey("voice")
                ? javascriptDomTextContentValue(command.get("voice"))
                : "undefined";
        Element note = createNoteElement(anchor.getOwnerDocument(), voice,
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
        upsertSimpleChild(pitchElement, "octave",
                javascriptNumberText(MusicXmlCommandJson.finiteIntegerValue(pitch, "octave").doubleValue()));
        note.appendChild(pitchElement);
        if (alter != null) {
            upsertSimpleChild(note, "accidental", accidentalFromAlter(alter.intValue()));
        }
        Double duration = MusicXmlCommandJson.finiteIntegerValue(notePayload, "duration");
        upsertSimpleChild(note, "duration", javascriptNumberText(duration.doubleValue()));
        upsertSimpleChild(note, "voice", voice);
        return note;
    }

    private static Element createRestElement(Document doc, String voice, double duration) {
        Element note = doc.createElement("note");
        Element rest = doc.createElement("rest");
        note.appendChild(rest);
        upsertSimpleChild(note, "duration", javascriptNumberText(duration));
        upsertSimpleChild(note, "voice", voice == null ? "1" : voice);
        return note;
    }

    private static void syncSimpleTypeFromDuration(Element note, double duration) {
        Double divisions = resolveEffectiveDivisions(note);
        if (divisions == null || divisions.doubleValue() <= 0) {
            return;
        }
        DurationNotation notation = durationToNotation(duration, divisions.doubleValue());
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

    private static Double resolveEffectiveDivisions(Element note) {
        Element measure = findAncestor(note, "measure");
        Element part = findAncestor(measure, "part");
        if (measure == null || part == null) {
            return null;
        }
        List<Element> measures = directChildren(part, "measure");
        int measureIndex = measures.indexOf(measure);
        for (int index = measureIndex; index >= 0; index--) {
            Element attributes = directChild(measures.get(index), "attributes");
            Double divisions = parseFiniteNumberOrNull(directChildText(attributes, "divisions"));
            if (divisions != null && isJavaScriptInteger(divisions.doubleValue()) && divisions.doubleValue() > 0) {
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
        Double capacity = getMeasureCapacity(measure);
        if (capacity == null) {
            return null;
        }
        return new MeasureTiming(capacity.doubleValue(), getOccupiedTime(measure, voice));
    }

    private static double availableRestDurationForExpansion(Element target, String voice, double maximum) {
        return availableFollowingRestDurationForExpansion(target, voice, maximum)
                + availablePrecedingRestDurationForExpansion(target, voice, maximum);
    }

    private static double availableFollowingRestDurationForExpansion(Element target, String voice, double overflow) {
        if (overflow <= 0) {
            return 0;
        }
        double remaining = overflow;
        Element cursor = nextElementSibling(target);
        while (cursor != null && remaining > 0) {
            if (isBackupOrForward(cursor)) {
                break;
            }
            Element next = nextElementSibling(cursor);
            if ("note".equals(cursor.getTagName()) && isConsumableRest(cursor, voice)) {
                Double duration = parseFiniteNumberOrNull(directChildRawText(cursor, "duration"));
                remaining -= Math.min(remaining, duration == null ? 0 : duration.doubleValue());
            }
            cursor = next;
        }
        return overflow - remaining;
    }

    private static double availablePrecedingRestDurationForExpansion(Element target, String voice, double overflow) {
        if (overflow <= 0) {
            return 0;
        }
        double remaining = overflow;
        Element cursor = previousElementSibling(target);
        while (cursor != null && remaining > 0) {
            if (isBackupOrForward(cursor)) {
                break;
            }
            Element previous = previousElementSibling(cursor);
            if ("note".equals(cursor.getTagName()) && isConsumableRest(cursor, voice)) {
                Double duration = parseFiniteNumberOrNull(directChildRawText(cursor, "duration"));
                remaining -= Math.min(remaining, duration == null ? 0 : duration.doubleValue());
            }
            cursor = previous;
        }
        return overflow - remaining;
    }

    private static double consumeFollowingRestsForDurationExpansion(Element target, String voice, double overflow) {
        if (overflow <= 0) {
            return 0;
        }
        double remaining = overflow;
        Element cursor = nextElementSibling(target);
        while (cursor != null && remaining > 0) {
            if (isBackupOrForward(cursor)) {
                break;
            }
            Element next = nextElementSibling(cursor);
            if ("note".equals(cursor.getTagName()) && isConsumableRest(cursor, voice)) {
                Double duration = parseFiniteNumberOrNull(directChildRawText(cursor, "duration"));
                double restDuration = duration == null ? 0 : duration.doubleValue();
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

    private static double consumePrecedingRestsForDurationExpansion(Element target, String voice, double overflow) {
        if (overflow <= 0) {
            return 0;
        }
        double remaining = overflow;
        Element cursor = previousElementSibling(target);
        while (cursor != null && remaining > 0) {
            if (isBackupOrForward(cursor)) {
                break;
            }
            Element previous = previousElementSibling(cursor);
            if ("note".equals(cursor.getTagName()) && isConsumableRest(cursor, voice)) {
                Double duration = parseFiniteNumberOrNull(directChildRawText(cursor, "duration"));
                double restDuration = duration == null ? 0 : duration.doubleValue();
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

    private static boolean fillUnderfullGapAfterTarget(Element target, String matchingVoice, String generatedRestVoice,
            double deficit) {
        if (!isJavaScriptInteger(deficit) || deficit <= 0) {
            return true;
        }
        Element measure = findAncestor(target, "measure");
        if (measure == null || measureHasBackupOrForward(measure)) {
            return false;
        }
        Element next = nextElementSibling(target);
        if (matchingVoice != null && next != null && "note".equals(next.getTagName())
                && isConsumableRest(next, matchingVoice)) {
            Double current = parseFiniteNumberOrNull(directChildRawText(next, "duration"));
            setDurationValue(next, (current == null ? 0 : current.doubleValue()) + deficit);
            return true;
        }
        Element rest = createRestElement(target.getOwnerDocument(), generatedRestVoice, deficit);
        Node parent = target.getParentNode();
        Node nextNode = target.getNextSibling();
        if (nextNode == null) {
            parent.appendChild(rest);
        } else {
            parent.insertBefore(rest, nextNode);
        }
        // createRestElement only creates the structural note. Like upstream
        // ScoreCore, attach it first so inherited divisions can be resolved
        // before synchronizing notation metadata.
        setDurationValue(rest, deficit);
        return true;
    }

    private static boolean isConsumableRest(Element note, String voice) {
        if (note == null || !"note".equals(note.getTagName())) {
            return false;
        }
        if (directChild(note, "rest") == null || directChild(note, "chord") != null) {
            return false;
        }
        if (!equalsNullable(getVoiceText(note), voice)) {
            return false;
        }
        Double duration = parseFiniteNumberOrNull(directChildRawText(note, "duration"));
        return duration != null && duration.doubleValue() > 0;
    }

    private static boolean measureHasBackupOrForward(Element measure) {
        for (Element child : directElementChildren(measure)) {
            if (isBackupOrForward(child)) {
                return true;
            }
        }
        return false;
    }

    private static Double getMeasureCapacity(Element measure) {
        TimingContext context = resolveTimingContext(measure);
        if (context == null || !Double.isFinite(context.beats) || !Double.isFinite(context.beatType)
                || !Double.isFinite(context.divisions) || context.beatType <= 0) {
            return null;
        }
        double beatUnit = (4.0d / context.beatType) * context.divisions;
        return Double.valueOf(javascriptMathRound(context.beats * beatUnit));
    }

    /**
     * The upstream XML reader keeps finite numeric duration values as numbers,
     * not only MusicXML's usual integers. Save-time validation must therefore
     * retain fractional onsets even though edit-command payloads stay integral.
     */
    private static FloatingMeasureTiming getFloatingMeasureTimingForVoice(Element noteInMeasure, String voice) {
        Element measure = findAncestor(noteInMeasure, "measure");
        if (measure == null) {
            return null;
        }
        Double capacity = getFloatingMeasureCapacity(measure);
        if (capacity == null) {
            return null;
        }
        return new FloatingMeasureTiming(capacity.doubleValue(), getFloatingOccupiedTime(measure, voice));
    }

    private static Double getFloatingMeasureCapacity(Element measure) {
        FloatingTimingContext context = resolveFloatingTimingContext(measure);
        if (context == null || context.beatType <= 0) {
            return null;
        }
        double beatUnit = (4.0d / context.beatType) * context.divisions;
        return Double.valueOf(javascriptMathRound(context.beats * beatUnit));
    }

    private static double getFloatingOccupiedTime(Element measure, String voice) {
        double cursor = 0;
        double occupied = 0;
        for (Element child : directElementChildren(measure)) {
            if ("backup".equals(child.getTagName()) || "forward".equals(child.getTagName())) {
                Double shift = parseFiniteNumberOrNull(directChildRawText(child, "duration"));
                if (shift == null) {
                    continue;
                }
                cursor = "backup".equals(child.getTagName()) ? Math.max(0, cursor - shift.doubleValue())
                        : cursor + shift.doubleValue();
                continue;
            }
            if (!"note".equals(child.getTagName()) || directChild(child, "chord") != null
                    || !equalsNullable(getVoiceText(child), voice)) {
                continue;
            }
            Double duration = parseFiniteNumberOrNull(directChildRawText(child, "duration"));
            if (duration == null) {
                continue;
            }
            double end = cursor + duration.doubleValue();
            occupied = Math.max(occupied, end);
            cursor = end;
        }
        return occupied;
    }

    private static double getOccupiedTime(Element measure, String voice) {
        double cursor = 0;
        double occupied = 0;
        for (Element child : directElementChildren(measure)) {
            if ("backup".equals(child.getTagName()) || "forward".equals(child.getTagName())) {
                Double shift = parseFiniteNumberOrNull(directChildRawText(child, "duration"));
                if (shift == null) {
                    continue;
                }
                cursor = "backup".equals(child.getTagName()) ? Math.max(0, cursor - shift.doubleValue())
                        : cursor + shift.doubleValue();
                continue;
            }
            if (!"note".equals(child.getTagName())) {
                continue;
            }
            if (directChild(child, "chord") != null) {
                continue;
            }
            String noteVoice = getVoiceText(child);
            if (!equalsNullable(noteVoice, voice)) {
                continue;
            }
            Double duration = parseFiniteNumberOrNull(directChildRawText(child, "duration"));
            if (duration == null) {
                continue;
            }
            double end = cursor + duration.doubleValue();
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
        Double beats = null;
        Double beatType = null;
        Double divisions = null;
        List<Element> measures = directChildren(part, "measure");
        int measureIndex = measures.indexOf(measure);
        for (int index = measureIndex; index >= 0; index--) {
            Element attributes = firstDescendant(measures.get(index), "attributes");
            if (attributes == null) {
                continue;
            }
            if (divisions == null) {
                divisions = parseNonEmptyFiniteNumberOrNull(descendantText(attributes, "divisions"));
            }
            Element time = firstDescendant(attributes, "time");
            if (beats == null) {
                beats = parseNonEmptyFiniteNumberOrNull(directChildText(time, "beats"));
            }
            if (beatType == null) {
                beatType = parseNonEmptyFiniteNumberOrNull(directChildText(time, "beat-type"));
            }
            if (beats != null && beatType != null && divisions != null) {
                return new TimingContext(beats.doubleValue(), beatType.doubleValue(), divisions.doubleValue());
            }
        }
        return null;
    }

    /** Mirrors timeIndex.resolveTimingContext's finite JavaScript Number parsing for save validation. */
    private static FloatingTimingContext resolveFloatingTimingContext(Element measure) {
        Element part = findAncestor(measure, "part");
        if (part == null) {
            return null;
        }
        Double beats = null;
        Double beatType = null;
        Double divisions = null;
        List<Element> measures = directChildren(part, "measure");
        int measureIndex = measures.indexOf(measure);
        for (int index = measureIndex; index >= 0; index--) {
            Element attributes = firstDescendant(measures.get(index), "attributes");
            if (attributes == null) {
                continue;
            }
            if (divisions == null) {
                divisions = parseNonEmptyFiniteNumberOrNull(descendantText(attributes, "divisions"));
            }
            Element time = firstDescendant(attributes, "time");
            if (beats == null) {
                beats = parseNonEmptyFiniteNumberOrNull(directChildText(time, "beats"));
            }
            if (beatType == null) {
                beatType = parseNonEmptyFiniteNumberOrNull(directChildText(time, "beat-type"));
            }
            if (beats != null && beatType != null && divisions != null) {
                return new FloatingTimingContext(beats.doubleValue(), beatType.doubleValue(), divisions.doubleValue());
            }
        }
        return null;
    }

    private static DurationNotation durationToNotation(double duration, double divisions) {
        DurationNotation[] definitions = new DurationNotation[] { new DurationNotation(4, 1, "whole", 0, false),
                new DurationNotation(3, 1, "half", 1, false), new DurationNotation(2, 1, "half", 0, false),
                new DurationNotation(4, 3, "half", 0, true), new DurationNotation(3, 2, "quarter", 1, false),
                new DurationNotation(1, 1, "quarter", 0, false), new DurationNotation(2, 3, "quarter", 0, true),
                new DurationNotation(3, 4, "eighth", 1, false), new DurationNotation(1, 2, "eighth", 0, false),
                new DurationNotation(1, 3, "eighth", 0, true), new DurationNotation(3, 8, "16th", 1, false),
                new DurationNotation(1, 4, "16th", 0, false), new DurationNotation(1, 6, "16th", 0, true),
                new DurationNotation(1, 8, "32nd", 0, false), new DurationNotation(1, 16, "64th", 0, false) };
        for (DurationNotation definition : definitions) {
            double value = (divisions * definition.num) / definition.den;
            if (!isJavaScriptInteger(value) || value <= 0) {
                continue;
            }
            if (duration == value) {
                return definition;
            }
        }
        return null;
    }

    private static String accidentalFromAlter(int alter) {
        String accidental = AccidentalSpelling.accidentalTextFromAlter(alter);
        return accidental == null ? "natural" : accidental;
    }

    /** Mirrors core/xmlUtils.getVoiceText: trim an existing direct value. */
    private static String getVoiceText(Element note) {
        Element voice = directChild(note, "voice");
        return voice == null || voice.getTextContent() == null ? null : voice.getTextContent().trim();
    }

    /** DOM {@code textContent = null} clears text, whereas {@code String(null)} is "null". */
    private static String javascriptDomTextContentValue(Object value) {
        return value == null ? "" : MusicXmlCommandJson.javascriptStringValue(value);
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

    private static Element findNoteElement(Element root, IndexedNote indexed) {
        if (indexed == null) {
            return null;
        }
        if (indexed.documentNoteIndex > 0) {
            List<Element> notes = descendantElements(root, "note");
            int index = indexed.documentNoteIndex - 1;
            return index >= 0 && index < notes.size() ? notes.get(index) : null;
        }
        return findNoteElementBySelector(root, indexed.selector);
    }

    private static Element findNoteElementBySelector(Element root, MusicXmlMeasureInspection.Selector selector) {
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
        String title = directChildTrimmedTextOrNull(directChild(root, "work"), "work-title");
        if (title == null) {
            title = directChildTrimmedTextOrNull(root, "movement-title");
        }
        return new MusicXmlStateDiff.Summary(title, parts.size(), measures.size(), notes.size(),
                new ArrayList<String>(measureNumbers));
    }

    private static Map<String, MeasureSignature> buildMeasureDiffSignatureMap(Element root) {
        Map<String, MeasureSignature> result = new java.util.LinkedHashMap<String, MeasureSignature>();
        for (Element part : directChildren(root, "part")) {
            String partId = trimmedAttributeOrNull(part, "id");
            for (Element measure : directChildren(part, "measure")) {
                String measureNumber = trimmedAttributeOrEmpty(measure, "number");
                List<Element> notes = directChildren(measure, "note");
                String signature = buildNoteSummarySignature(notes);
                String key = (partId == null ? "" : partId) + ":" + measureNumber;
                result.put(key, new MeasureSignature(partId, measureNumber, notes.size(), signature));
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
            String voice = getVoiceText(note);
            Double duration = readCliStateDuration(note);
            boolean isRest = directChild(note, "rest") != null;
            MusicXmlMeasureInspection.Pitch pitch = buildPitch(note);
            builder.append("{voice=");
            builder.append(voice == null ? "null" : voice);
            builder.append(",duration=");
            builder.append(duration == null ? "null" : javascriptNumberText(duration.doubleValue()));
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
        // ScoreCore keeps node IDs in a WeakMap populated by
        // document.querySelectorAll("note").  Its temporary Java attribute is
        // active only on that stateful path, so retain the narrower public CLI
        // selector model while resolving Core node IDs in the same document
        // order as upstream.
        if (ACTIVE_INTERNAL_NODE_ID_ATTRIBUTE.get() != null) {
            return buildIndexedDocumentNotes(root);
        }
        return buildIndexedDirectMeasureNotes(root);
    }

    private static List<IndexedNote> buildIndexedDirectMeasureNotes(Element root) {
        List<IndexedNote> indexedNotes = new ArrayList<IndexedNote>();
        int sequence = 0;
        for (Element part : directChildren(root, "part")) {
            String partId = trimmedAttributeOrNull(part, "id");
            for (Element measure : directChildren(part, "measure")) {
                String measureNumber = trimmedAttributeOrEmpty(measure, "number");
                Map<String, Integer> voiceNoteCounts = new HashMap<String, Integer>();
                List<Element> notes = directChildren(measure, "note");
                for (int index = 0; index < notes.size(); index++) {
                    sequence++;
                    Element note = notes.get(index);
                    String voice = getVoiceText(note);
                    String voiceKey = voice == null ? "__none__" : voice;
                    Integer previous = voiceNoteCounts.get(voiceKey);
                    int voiceNoteIndex = previous == null ? 1 : previous.intValue() + 1;
                    voiceNoteCounts.put(voiceKey, Integer.valueOf(voiceNoteIndex));
                    MusicXmlMeasureInspection.Selector selector = new MusicXmlMeasureInspection.Selector(partId,
                            measureNumber, index + 1, voice, Integer.valueOf(voiceNoteIndex));
                    String retainedNodeId = internalNodeId(note);
                    indexedNotes.add(new IndexedNote(retainedNodeId == null ? "n" + sequence : retainedNodeId,
                            selector, 0));
                }
            }
        }
        return indexedNotes;
    }

    private static List<IndexedNote> buildIndexedDocumentNotes(Element root) {
        List<IndexedNote> indexedNotes = new ArrayList<IndexedNote>();
        List<Element> notes = descendantElements(root, "note");
        for (int index = 0; index < notes.size(); index++) {
            Element note = notes.get(index);
            Element measure = findAncestor(note, "measure");
            Element part = findAncestor(measure, "part");
            String partId = part == null ? null : trimmedAttributeOrNull(part, "id");
            String measureNumber = measure == null ? "" : trimmedAttributeOrEmpty(measure, "number");
            String voice = getVoiceText(note);
            int measureNoteIndex = 0;
            Integer voiceNoteIndex = null;
            if (measure != null) {
                List<Element> measureNotes = directChildren(measure, "note");
                int directIndex = measureNotes.indexOf(note);
                if (directIndex >= 0) {
                    measureNoteIndex = directIndex + 1;
                    int count = 0;
                    for (int prior = 0; prior <= directIndex; prior++) {
                        if (equalsNullable(getVoiceText(measureNotes.get(prior)), voice)) {
                            count++;
                        }
                    }
                    voiceNoteIndex = Integer.valueOf(count);
                }
            }
            MusicXmlMeasureInspection.Selector selector = new MusicXmlMeasureInspection.Selector(partId,
                    measureNumber, measureNoteIndex, voice, voiceNoteIndex);
            String retainedNodeId = internalNodeId(note);
            indexedNotes.add(new IndexedNote(retainedNodeId == null ? "n" + (index + 1) : retainedNodeId,
                    selector, index + 1));
        }
        return indexedNotes;
    }

    private static String internalNodeId(Element note) {
        String internalNodeIdAttribute = ACTIVE_INTERNAL_NODE_ID_ATTRIBUTE.get();
        if (note == null || internalNodeIdAttribute == null || !note.hasAttribute(internalNodeIdAttribute)) {
            return null;
        }
        String storedValue = trimToNull(note.getAttribute(internalNodeIdAttribute));
        if (storedValue != null && storedValue.startsWith(INTERNAL_NODE_ID_VALUE_PREFIX)) {
            String nodeId = trimToNull(storedValue.substring(INTERNAL_NODE_ID_VALUE_PREFIX.length()));
            if (nodeId != null) {
                return nodeId;
            }
        }
        return null;
    }

    private static String pushInternalNodeIdAttribute(String internalNodeIdAttribute) {
        String previousAttribute = ACTIVE_INTERNAL_NODE_ID_ATTRIBUTE.get();
        String normalizedAttribute = trimToNull(internalNodeIdAttribute);
        if (normalizedAttribute == null) {
            ACTIVE_INTERNAL_NODE_ID_ATTRIBUTE.remove();
        } else {
            ACTIVE_INTERNAL_NODE_ID_ATTRIBUTE.set(normalizedAttribute);
        }
        return previousAttribute;
    }

    private static void restoreInternalNodeIdAttribute(String previousAttribute) {
        if (previousAttribute == null) {
            ACTIVE_INTERNAL_NODE_ID_ATTRIBUTE.remove();
        } else {
            ACTIVE_INTERNAL_NODE_ID_ATTRIBUTE.set(previousAttribute);
        }
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

    private static SelectorResolution resolveCliSelector(List<IndexedNote> indexedNotes, Object rawSelector,
            String selectorName) {
        Map<String, Object> selector = MusicXmlCommandJson.castMap(rawSelector);
        if (selector == null) {
            return SelectorResolution.failure(selectorName + " must be an object when provided.");
        }
        String partId = selector.get("part_id") == null ? null
                : MusicXmlCommandJson.javascriptStringValue(selector.get("part_id"));
        String measureNumber = selector.get("measure_number") == null ? null
                : MusicXmlCommandJson.javascriptStringValue(selector.get("measure_number"));
        Double measureNoteIndex = jsonInteger(selector.get("measure_note_index"));
        String voice = selector.get("voice") == null ? null
                : MusicXmlCommandJson.javascriptStringValue(selector.get("voice"));
        Double voiceNoteIndex = jsonInteger(selector.get("voice_note_index"));
        if (partId == null && measureNumber == null && measureNoteIndex == null && voice == null
                && voiceNoteIndex == null) {
            return SelectorResolution.failure(selectorName + " must include at least one selector field.");
        }
        List<IndexedNote> matches = new ArrayList<IndexedNote>();
        for (IndexedNote indexed : indexedNotes) {
            MusicXmlMeasureInspection.Selector current = indexed.selector;
            if (partId != null && !equalsNullable(partId, current.getPartId())) {
                continue;
            }
            if (measureNumber != null && !equalsNullable(measureNumber, current.getMeasureNumber())) {
                continue;
            }
            if (measureNoteIndex != null && measureNoteIndex.doubleValue() != current.getMeasureNoteIndex()) {
                continue;
            }
            if (voice != null && !equalsNullable(voice, current.getVoice())) {
                continue;
            }
            if (voiceNoteIndex != null && voiceNoteIndex.doubleValue() != current.getVoiceNoteIndex()) {
                continue;
            }
            matches.add(indexed);
        }
        if (matches.isEmpty()) {
            return SelectorResolution.failure(selectorName + " did not match any note in the current MusicXML state.");
        }
        if (matches.size() > 1) {
            return SelectorResolution.failure(
                    selectorName + " matched multiple notes; add more selector fields to disambiguate.");
        }
        IndexedNote match = matches.get(0);
        return SelectorResolution.success(match.nodeId, match.selector.getVoice());
    }

    private static Double jsonInteger(Object value) {
        if (!(value instanceof Number)) {
            return null;
        }
        double number = ((Number) value).doubleValue();
        if (!Double.isFinite(number) || number != Math.rint(number)) {
            return null;
        }
        return Double.valueOf(number);
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
        Double octave = MusicXmlCommandJson.finiteIntegerValue(pitch, "octave");
        if (!("A".equals(step) || "B".equals(step) || "C".equals(step) || "D".equals(step) || "E".equals(step)
                || "F".equals(step) || "G".equals(step))) {
            return false;
        }
        if (octave == null || !isJavaScriptInteger(octave.doubleValue())) {
            return false;
        }
        // Upstream validates alter only when it is a JavaScript number. A JSON number
        // must then be a finite integer in range; non-number values retain the
        // JavaScript runtime behavior and are ignored by the validator.
        Object rawAlter = pitch.get("alter");
        if (!(rawAlter instanceof Number)) {
            return true;
        }
        Integer alter = MusicXmlCommandJson.intValue(pitch, "alter");
        return alter != null && alter.intValue() >= -2 && alter.intValue() <= 2;
    }

    private static boolean isPositiveInteger(Double value) {
        return value != null && isJavaScriptInteger(value.doubleValue()) && value.doubleValue() > 0;
    }

    private static MusicXmlMeasureInspection.Pitch buildPitch(Element note) {
        Element pitch = directChild(note, "pitch");
        if (pitch == null) {
            return null;
        }
        String step = directChildTrimmedTextOrNull(pitch, "step");
        String octaveText = directChildTrimmedTextOrNull(pitch, "octave");
        if (step == null || step.length() == 0 || octaveText == null || octaveText.length() == 0) {
            return null;
        }
        String alterText = directChildTrimmedTextOrNull(pitch, "alter");
        Double octave = parseFiniteNumberOrNull(octaveText);
        Double alter = alterText == null ? null : parseFiniteNumberOrNull(alterText);
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

    /** Mirrors Number.isInteger(Number(text)) for XML numeric text. */
    private static Integer parsePositiveIntegerNumberOrNull(String value) {
        Double parsed = parseFiniteNumberOrNull(value);
        if (parsed == null || parsed.doubleValue() != Math.rint(parsed.doubleValue()) || parsed.doubleValue() <= 0
                || parsed.doubleValue() > Integer.MAX_VALUE) {
            return null;
        }
        return Integer.valueOf((int) parsed.doubleValue());
    }

    private static Double parseFiniteNumberOrNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() == 0) {
            return Double.valueOf(0);
        }
        try {
            if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
                return parseFiniteRadixNumberOrNull(normalized.substring(2), 16);
            }
            if (normalized.startsWith("0b") || normalized.startsWith("0B")) {
                return parseFiniteRadixNumberOrNull(normalized.substring(2), 2);
            }
            if (normalized.startsWith("0o") || normalized.startsWith("0O")) {
                return parseFiniteRadixNumberOrNull(normalized.substring(2), 8);
            }
            double parsed = Double.parseDouble(normalized);
            return Double.isNaN(parsed) || Double.isInfinite(parsed) ? null : Double.valueOf(parsed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Mirrors a source timing field guarded by a truthy trimmed text check before {@code Number()}. */
    private static Double parseNonEmptyFiniteNumberOrNull(String value) {
        return value == null || value.trim().length() == 0 ? null : parseFiniteNumberOrNull(value);
    }

    /**
     * Mirrors {@code Number()} for the unsigned hexadecimal, binary, and octal
     * forms accepted in XML text.  The JavaScript value need not fit in a
     * signed 64-bit integer: it is rounded to the nearest finite IEEE-754
     * double, or rejected only once that conversion overflows to infinity.
     */
    private static Double parseFiniteRadixNumberOrNull(String digits, int radix) {
        try {
            double parsed = new BigInteger(digits, radix).doubleValue();
            return Double.isNaN(parsed) || Double.isInfinite(parsed) ? null : Double.valueOf(parsed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String javascriptNumberText(double value) {
        // Keep XML values converted through JavaScript's String(Number) rules.
        // This matters for a valid source duration such as 1e20 when delete_note
        // replaces the pitch with a rest: JavaScript writes the expanded decimal
        // form below 1e21 and lower-case exponential form outside that range.
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (Double.isInfinite(value)) {
            return value < 0 ? "-Infinity" : "Infinity";
        }
        if (value == 0d) {
            return "0";
        }
        double magnitude = Math.abs(value);
        if (magnitude >= 0.000001d && magnitude < 1.0e21d) {
            return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        }
        String text = Double.toString(value);
        int exponentIndex = Math.max(text.indexOf('E'), text.indexOf('e'));
        if (exponentIndex < 0) {
            return text;
        }
        String mantissa = text.substring(0, exponentIndex);
        if (mantissa.endsWith(".0")) {
            mantissa = mantissa.substring(0, mantissa.length() - 2);
        }
        int exponent = Integer.parseInt(text.substring(exponentIndex + 1));
        return mantissa + "e" + (exponent >= 0 ? "+" : "") + exponent;
    }

    /** Mirrors {@code Math.round()} without narrowing a finite Number to a Java {@code long}. */
    private static double javascriptMathRound(double value) {
        return Math.floor(value + 0.5d);
    }

    private static boolean equalsNullable(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static boolean isJavaScriptInteger(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value == Math.rint(value);
    }

    private static final class FloatingMeasureTiming {
        private final double capacity;
        private final double occupied;

        private FloatingMeasureTiming(double capacity, double occupied) {
            this.capacity = capacity;
            this.occupied = occupied;
        }
    }

    private static final class FloatingTimingContext {
        private final double beats;
        private final double beatType;
        private final double divisions;

        private FloatingTimingContext(double beats, double beatType, double divisions) {
            this.beats = beats;
            this.beatType = beatType;
            this.divisions = divisions;
        }
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

    private static String directChildRawText(Element parent, String tagName) {
        Element child = directChild(parent, tagName);
        return child == null ? null : child.getTextContent();
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

    /** Implements document/querySelector descendant order for the small DOM subset used by ScoreCore. */
    private static List<Element> descendantElements(Element parent, String tagName) {
        List<Element> result = new ArrayList<Element>();
        collectDescendantElements(parent, tagName, result);
        return result;
    }

    private static void collectDescendantElements(Node parent, String tagName, List<Element> result) {
        if (parent == null) {
            return;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element) {
                Element element = (Element) child;
                if (tagName.equals(element.getTagName())) {
                    result.add(element);
                }
                collectDescendantElements(element, tagName, result);
            }
            child = child.getNextSibling();
        }
    }

    private static Element firstDescendant(Element parent, String tagName) {
        List<Element> matches = descendantElements(parent, tagName);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private static String descendantText(Element parent, String tagName) {
        Element child = firstDescendant(parent, tagName);
        return child == null ? null : trimToNull(child.getTextContent());
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

    /** Mirrors direct-child {@code textContent?.trim() ?? null}, retaining an empty string. */
    private static String directChildTrimmedTextOrNull(Element parent, String tagName) {
        Element child = directChild(parent, tagName);
        return child == null || child.getTextContent() == null ? null : child.getTextContent().trim();
    }

    /** Mirrors cli-api.ts getDurationValue, including an empty text node being absent. */
    private static Double readCliStateDuration(Element note) {
        String raw = directChildRawText(note, "duration");
        return raw == null || raw.length() == 0 ? null : parseFiniteNumberOrNull(raw);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    /** Mirrors {@code element.getAttribute(name)?.trim() ?? null}. */
    private static String trimmedAttributeOrNull(Element element, String name) {
        if (element == null || !element.hasAttribute(name)) {
            return null;
        }
        return element.getAttribute(name).trim();
    }

    /** Mirrors {@code element.getAttribute(name)?.trim() ?? ""}. */
    private static String trimmedAttributeOrEmpty(Element element, String name) {
        String value = trimmedAttributeOrNull(element, name);
        return value == null ? "" : value;
    }

    /** Result of the cli-api selector-to-node-id normalization step. */
    public static final class CliCommandNormalization {
        private final boolean ok;
        private final String commandJson;
        private final String message;

        private CliCommandNormalization(boolean ok, String commandJson, String message) {
            this.ok = ok;
            this.commandJson = commandJson;
            this.message = message;
        }

        private static CliCommandNormalization success(String commandJson) {
            return new CliCommandNormalization(true, commandJson, null);
        }

        private static CliCommandNormalization failure(String message) {
            return new CliCommandNormalization(false, null, message);
        }

        public boolean isOk() {
            return ok;
        }

        public String getCommandJson() {
            return commandJson;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final class SelectorResolution {
        private final boolean ok;
        private final String nodeId;
        private final String voice;
        private final String message;

        private SelectorResolution(boolean ok, String nodeId, String voice, String message) {
            this.ok = ok;
            this.nodeId = nodeId;
            this.voice = voice;
            this.message = message;
        }

        private static SelectorResolution success(String nodeId, String voice) {
            return new SelectorResolution(true, nodeId, voice, null);
        }

        private static SelectorResolution failure(String message) {
            return new SelectorResolution(false, null, null, message);
        }

        private boolean isOk() {
            return ok;
        }

        private String getNodeId() {
            return nodeId;
        }

        private String getVoice() {
            return voice;
        }

        private String getMessage() {
            return message;
        }
    }

    public static final class MusicXmlCommandApplyResult {
        private final String output;
        private final List<MusicXmlCommandValidation.Warning> warnings;

        private MusicXmlCommandApplyResult(String output, List<MusicXmlCommandValidation.Warning> warnings) {
            this.output = output == null ? "" : output;
            this.warnings = new ArrayList<MusicXmlCommandValidation.Warning>(warnings);
        }

        public String getOutput() {
            return output;
        }

        public List<MusicXmlCommandValidation.Warning> getWarnings() {
            return new ArrayList<MusicXmlCommandValidation.Warning>(warnings);
        }
    }

    private static final class CommandNodeId {
        private final boolean stringValue;
        private final String displayValue;

        private CommandNodeId(boolean stringValue, String displayValue) {
            this.stringValue = stringValue;
            this.displayValue = displayValue;
        }

        private boolean isStringValue() {
            return stringValue;
        }

        private String getDisplayValue() {
            return displayValue;
        }
    }

    private static final class IndexedNote {
        private final String nodeId;
        private final MusicXmlMeasureInspection.Selector selector;
        private final int documentNoteIndex;

        private IndexedNote(String nodeId, MusicXmlMeasureInspection.Selector selector, int documentNoteIndex) {
            this.nodeId = nodeId;
            this.selector = selector;
            this.documentNoteIndex = documentNoteIndex;
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
        private final double capacity;
        private final double occupied;

        private MeasureTiming(double capacity, double occupied) {
            this.capacity = capacity;
            this.occupied = occupied;
        }
    }

    private static final class TimingContext {
        private final double beats;
        private final double beatType;
        private final double divisions;

        private TimingContext(double beats, double beatType, double divisions) {
            this.beats = beats;
            this.beatType = beatType;
            this.divisions = divisions;
        }
    }
}
