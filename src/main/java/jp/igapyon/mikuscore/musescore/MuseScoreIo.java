package jp.igapyon.mikuscore.musescore;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jp.igapyon.mikuscore.musicxml.AccidentalSpelling;

public final class MuseScoreIo {
    private MuseScoreIo() {
    }

    public static Integer durationTypeToDivisions(String durationType, int divisions) {
        int base = Math.max(1, Math.round(divisions));
        String normalized = durationType == null ? "" : durationType.trim().toLowerCase(Locale.ROOT);
        if ("whole".equals(normalized)) {
            return Integer.valueOf(base * 4);
        }
        if ("half".equals(normalized)) {
            return Integer.valueOf(base * 2);
        }
        if ("quarter".equals(normalized)) {
            return Integer.valueOf(base);
        }
        if ("eighth".equals(normalized)) {
            return Integer.valueOf(Math.round(base / 2.0f));
        }
        if ("16th".equals(normalized)) {
            return Integer.valueOf(Math.round(base / 4.0f));
        }
        if ("32nd".equals(normalized)) {
            return Integer.valueOf(Math.round(base / 8.0f));
        }
        if ("64th".equals(normalized)) {
            return Integer.valueOf(Math.round(base / 16.0f));
        }
        return null;
    }

    public static int durationWithDots(int baseDiv, int dots) {
        int out = Math.max(1, Math.round(baseDiv));
        int extra = out;
        int safeDots = Math.max(0, Math.round(dots));
        for (int index = 0; index < safeDots; index++) {
            extra = Math.max(1, Math.round(extra / 2.0f));
            out += extra;
        }
        return out;
    }

    public static TypeAndDots divisionToTypeAndDots(int divisions, int durationDiv) {
        int base = Math.max(1, Math.round(divisions));
        TypeCandidate[] candidates = new TypeCandidate[] {
                new TypeCandidate("whole", base * 4),
                new TypeCandidate("half", base * 2),
                new TypeCandidate("quarter", base),
                new TypeCandidate("eighth", Math.max(1, Math.round(base / 2.0f))),
                new TypeCandidate("16th", Math.max(1, Math.round(base / 4.0f))),
                new TypeCandidate("32nd", Math.max(1, Math.round(base / 8.0f))),
                new TypeCandidate("64th", Math.max(1, Math.round(base / 16.0f))) };
        for (TypeCandidate candidate : candidates) {
            if (durationWithDots(candidate.div, 0) == durationDiv) {
                return new TypeAndDots(candidate.type, 0);
            }
            if (durationWithDots(candidate.div, 1) == durationDiv) {
                return new TypeAndDots(candidate.type, 1);
            }
            if (durationWithDots(candidate.div, 2) == durationDiv) {
                return new TypeAndDots(candidate.type, 2);
            }
        }
        TypeCandidate nearest = candidates[0];
        int best = Math.abs(nearest.div - durationDiv);
        for (TypeCandidate candidate : candidates) {
            int distance = Math.abs(candidate.div - durationDiv);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return new TypeAndDots(nearest.type, 0);
    }

    public static List<String> chunkString(String value, int maxChunk) {
        List<String> out = new ArrayList<String>();
        String text = value == null ? "" : value;
        int size = Math.max(1, Math.round(maxChunk));
        for (int index = 0; index < text.length(); index += size) {
            out.add(text.substring(index, Math.min(text.length(), index + size)));
        }
        return out;
    }

    public static String buildWarningMiscXml(Collection<MuseScoreWarning> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return "";
        }
        List<MuseScoreWarning> warningList = new ArrayList<MuseScoreWarning>(warnings);
        int maxEntries = Math.min(256, warningList.size());
        StringBuilder xml = new StringBuilder();
        xml.append("<miscellaneous-field name=\"mks:diag:count\">").append(maxEntries)
                .append("</miscellaneous-field>");
        for (int index = 0; index < maxEntries; index++) {
            MuseScoreWarning warning = warningList.get(index);
            List<String> attrs = new ArrayList<String>();
            attrs.add("level=warn");
            attrs.add("code=" + warning.getCode());
            attrs.add("fmt=mscx");
            attrs.add("message=" + warning.getMessage());
            if (warning.getMeasure() != null) {
                attrs.add("measure=" + warning.getMeasure());
            }
            if (warning.getStaff() != null) {
                attrs.add("staff=" + warning.getStaff());
            }
            if (warning.getVoice() != null) {
                attrs.add("voice=" + warning.getVoice());
            }
            if (warning.getAtDiv() != null) {
                attrs.add("atDiv=" + warning.getAtDiv());
            }
            if (warning.getAction() != null && warning.getAction().length() > 0) {
                attrs.add("action=" + warning.getAction());
            }
            if (warning.getReason() != null && warning.getReason().length() > 0) {
                attrs.add("reason=" + warning.getReason());
            }
            if (warning.getTag() != null && warning.getTag().length() > 0) {
                attrs.add("tag=" + warning.getTag());
            }
            if (warning.getOccupiedDiv() != null) {
                attrs.add("occupiedDiv=" + warning.getOccupiedDiv());
            }
            if (warning.getCapacityDiv() != null) {
                attrs.add("capacityDiv=" + warning.getCapacityDiv());
            }
            String payload = joinWithSemicolon(attrs);
            xml.append("<miscellaneous-field name=\"mks:diag:")
                    .append(String.format(Locale.ROOT, "%04d", Integer.valueOf(index + 1))).append("\">")
                    .append(xmlEscape(payload)).append("</miscellaneous-field>");
        }
        return xml.toString();
    }

    public static String buildSourceMiscXml(String source) {
        String text = source == null ? "" : source;
        String encoded = encodeUriComponent(text);
        List<String> chunks = chunkString(encoded, 800);
        StringBuilder xml = new StringBuilder();
        xml.append("<miscellaneous-field name=\"mks:src:musescore:raw-encoding\">uri-v1</miscellaneous-field>");
        xml.append("<miscellaneous-field name=\"mks:src:musescore:raw-length\">").append(text.length())
                .append("</miscellaneous-field>");
        xml.append("<miscellaneous-field name=\"mks:src:musescore:raw-encoded-length\">").append(encoded.length())
                .append("</miscellaneous-field>");
        xml.append("<miscellaneous-field name=\"mks:src:musescore:raw-chunks\">").append(chunks.size())
                .append("</miscellaneous-field>");
        for (int index = 0; index < chunks.size(); index++) {
            xml.append("<miscellaneous-field name=\"mks:src:musescore:raw-")
                    .append(String.format(Locale.ROOT, "%04d", Integer.valueOf(index + 1))).append("\">")
                    .append(xmlEscape(chunks.get(index))).append("</miscellaneous-field>");
        }
        return xml.toString();
    }

    public static String buildMuseScoreImportMiscXml(String mscxSource, String sourceVersion,
            ResolvedMuseScoreImportOptions resolvedOptions, Collection<MuseScoreWarning> warnings) {
        StringBuilder sourceMiscXml = new StringBuilder();
        if (resolvedOptions.isSourceMetadata()) {
            sourceMiscXml.append(buildSourceMiscXml(mscxSource));
            if (sourceVersion != null && sourceVersion.length() > 0) {
                sourceMiscXml.append("<miscellaneous-field name=\"mks:src:musescore:version\">")
                        .append(xmlEscape(sourceVersion)).append("</miscellaneous-field>");
            }
        }
        return (resolvedOptions.isDebugMetadata() ? buildWarningMiscXml(warnings) : "") + sourceMiscXml.toString();
    }

    public static boolean isMuseDefaultWorkTitle(String title) {
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.length() == 0) {
            return true;
        }
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        return "untitled score".equals(normalized) || "untitled".equals(normalized) || "無題のスコア".equals(trimmed);
    }

    public static boolean isMuseDefaultComposer(String composer) {
        String trimmed = composer == null ? "" : composer.trim();
        if (trimmed.length() == 0) {
            return true;
        }
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        return "composer / arranger".equals(normalized) || "unknown".equals(normalized) || "作曲者 / 編曲者".equals(trimmed);
    }

    public static String museAccidentalSubtypeToMusicXml(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() == 0) {
            return null;
        }
        if ("accidentalsharp".equals(normalized)) {
            return "sharp";
        }
        if ("accidentalflat".equals(normalized)) {
            return "flat";
        }
        if ("accidentalnatural".equals(normalized)) {
            return "natural";
        }
        if ("accidentaldoublesharp".equals(normalized)) {
            return "double-sharp";
        }
        if ("accidentaldoubleflat".equals(normalized)) {
            return "flat-flat";
        }
        return null;
    }

    public static String parseMusicXmlAccidentalSubtype(String accidentalText) {
        String accidental = accidentalText == null ? "" : accidentalText.trim().toLowerCase(Locale.ROOT);
        if (accidental.length() == 0) {
            return null;
        }
        if ("natural".equals(accidental)) {
            return "accidentalNatural";
        }
        if ("sharp".equals(accidental)) {
            return "accidentalSharp";
        }
        if ("flat".equals(accidental)) {
            return "accidentalFlat";
        }
        if ("double-sharp".equals(accidental)) {
            return "accidentalDoubleSharp";
        }
        if ("flat-flat".equals(accidental)) {
            return "accidentalDoubleFlat";
        }
        return null;
    }

    public static int getNoteStaffNo(String staffText) {
        String staff = staffText == null ? "" : staffText.trim();
        if (staff.length() == 0) {
            return 1;
        }
        try {
            double parsed = Double.parseDouble(staff);
            if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
                return 1;
            }
            return Math.max(1, Math.round((float) parsed));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    public static Integer parseMusicXmlPitchToMidi(String stepRaw, String octaveRaw, String alterRaw) {
        String step = stepRaw == null ? "" : stepRaw.trim().toUpperCase(Locale.ROOT);
        String octaveText = octaveRaw == null ? "" : octaveRaw.trim();
        if (step.length() == 0 || octaveText.length() == 0) {
            return null;
        }
        double octave;
        try {
            octave = Double.parseDouble(octaveText);
        } catch (NumberFormatException ex) {
            return null;
        }
        if (Double.isNaN(octave) || Double.isInfinite(octave)) {
            return null;
        }
        double alter = 0.0d;
        String alterText = alterRaw == null ? "0" : alterRaw.trim();
        if (alterText.length() > 0) {
            try {
                alter = Double.parseDouble(alterText);
            } catch (NumberFormatException ex) {
                alter = 0.0d;
            }
        }
        int base;
        if ("C".equals(step)) {
            base = 0;
        } else if ("D".equals(step)) {
            base = 2;
        } else if ("E".equals(step)) {
            base = 4;
        } else if ("F".equals(step)) {
            base = 5;
        } else if ("G".equals(step)) {
            base = 7;
        } else if ("A".equals(step)) {
            base = 9;
        } else if ("B".equals(step)) {
            base = 11;
        } else {
            return null;
        }
        int midi = Math.round((float) ((octave + 1.0d) * 12.0d + base + alter));
        return Integer.valueOf(Math.max(0, Math.min(127, midi)));
    }

    public static Map<Integer, List<Integer>> collectMusicXmlPitchesByStaff(Collection<MusicXmlPitchStaff> notes) {
        Map<Integer, List<Integer>> byStaff = new java.util.LinkedHashMap<Integer, List<Integer>>();
        if (notes != null) {
            for (MusicXmlPitchStaff note : notes) {
                if (note == null || note.isRest()) {
                    continue;
                }
                Integer midi = parseMusicXmlPitchToMidi(note.getStep(), note.getOctave(), note.getAlter());
                if (midi == null) {
                    continue;
                }
                Integer staffNo = Integer.valueOf(getNoteStaffNo(note.getStaff()));
                List<Integer> list = byStaff.get(staffNo);
                if (list == null) {
                    list = new ArrayList<Integer>();
                    byStaff.put(staffNo, list);
                }
                list.add(midi);
            }
        }
        return byStaff;
    }

    public static boolean parseTruthyFlag(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return "1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized);
    }

    public static RepeatFlags parseMeasureRepeatFlagsFromBarlineSubtypes(Collection<String> subtypeValues) {
        boolean repeatForward = false;
        boolean repeatBackward = false;
        if (subtypeValues != null) {
            for (String subtypeValue : subtypeValues) {
                String raw = subtypeValue == null ? "" : subtypeValue.trim().toLowerCase(Locale.ROOT);
                if (raw.length() == 0) {
                    continue;
                }
                String normalized = raw.replaceAll("[\\s_]+", "-");
                boolean isEndStartRepeat = normalized.contains("end-start-repeat")
                        || normalized.contains("endstartrepeat");
                if (!isEndStartRepeat
                        && (normalized.contains("start-repeat") || normalized.contains("repeat-start"))) {
                    repeatForward = true;
                    continue;
                }
                if (normalized.contains("end-repeat") || normalized.contains("repeat-end")) {
                    repeatBackward = true;
                    continue;
                }
            }
        }
        return new RepeatFlags(repeatForward, repeatBackward);
    }

    public static MusePendingDirectionMarks parseMusicXmlMidBarlineRepeatMarks(String location,
            Collection<String> repeatDirections) {
        String normalizedLocation = location == null ? "" : location.trim().toLowerCase(Locale.ROOT);
        if (!"middle".equals(normalizedLocation)) {
            return null;
        }
        int repeatForwardCount = 0;
        int repeatBackwardCount = 0;
        if (repeatDirections != null) {
            for (String repeatDirection : repeatDirections) {
                String direction = repeatDirection == null ? "" : repeatDirection.trim().toLowerCase(Locale.ROOT);
                if ("forward".equals(direction)) {
                    repeatForwardCount++;
                }
                if ("backward".equals(direction)) {
                    repeatBackwardCount++;
                }
            }
        }
        if (repeatForwardCount <= 0 && repeatBackwardCount <= 0) {
            return null;
        }
        return new MusePendingDirectionMarks(null, 0, repeatForwardCount, repeatBackwardCount);
    }

    public static MusicXmlDirectionMarkPayload parseMusicXmlDirectionMarkPayloadValues(String staffText,
            String voiceText, Collection<MusicXmlOctaveShiftSource> octaveShifts, String forwardRepeatRaw,
            String backwardRepeatRaw) {
        int staffNo = getNoteStaffNo(staffText);
        int voiceNo = getNoteStaffNo(voiceText);
        List<String> startSubtypes = new ArrayList<String>();
        int stopCount = 0;
        int repeatForwardCount = 0;
        int repeatBackwardCount = 0;
        if (octaveShifts != null) {
            for (MusicXmlOctaveShiftSource octaveShift : octaveShifts) {
                if (octaveShift == null) {
                    continue;
                }
                String type = octaveShift.getType() == null ? ""
                        : octaveShift.getType().trim().toLowerCase(Locale.ROOT);
                if ("stop".equals(type)) {
                    stopCount++;
                    continue;
                }
                String subtype = parseMusicXmlOctaveShiftSubtype(octaveShift.getType(), octaveShift.getSize());
                if (subtype != null) {
                    startSubtypes.add(subtype);
                }
            }
        }
        if (parseTruthyFlag(forwardRepeatRaw)) {
            repeatForwardCount++;
        }
        if (parseTruthyFlag(backwardRepeatRaw)) {
            repeatBackwardCount++;
        }
        if (startSubtypes.isEmpty() && stopCount <= 0 && repeatForwardCount <= 0 && repeatBackwardCount <= 0) {
            return null;
        }
        return new MusicXmlDirectionMarkPayload(staffNo, voiceNo,
                new MusePendingDirectionMarks(startSubtypes, stopCount, repeatForwardCount, repeatBackwardCount));
    }

    public static TieFlags parseMusicXmlTieFlags(boolean hasTieStart, boolean hasTiedStart, boolean hasTieStop,
            boolean hasTiedStop) {
        return new TieFlags(hasTieStart || hasTiedStart, hasTieStop || hasTiedStop);
    }

    public static MusicXmlNumberSet parseMusicXmlSlurNumbers(Collection<MusicXmlTypedNumber> slurs) {
        return parseMusicXmlTypedNumbers(slurs);
    }

    public static MusicXmlNumberSet parseMusicXmlTrillNumbers(Collection<MusicXmlTypedNumber> wavyLines) {
        return parseMusicXmlTypedNumbers(wavyLines);
    }

    public static MusicXmlNumberSet parseMusicXmlTupletNumbers(Collection<MusicXmlTypedNumber> tuplets) {
        return parseMusicXmlTypedNumbers(tuplets);
    }

    public static TimeModification parseMusicXmlTupletTimeModification(String actualNotesRaw, String normalNotesRaw) {
        try {
            int actualNotes = Integer.parseInt(actualNotesRaw == null ? "" : actualNotesRaw.trim());
            int normalNotes = Integer.parseInt(normalNotesRaw == null ? "" : normalNotesRaw.trim());
            if (actualNotes <= 0 || normalNotes <= 0) {
                return null;
            }
            return new TimeModification(Math.max(1, Math.round(actualNotes)), Math.max(1, Math.round(normalNotes)));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static boolean hasMusicXmlTrillMarkOnly(boolean hasTrillMark, MusicXmlNumberSet trill) {
        if (trill != null && (!trill.getStarts().isEmpty() || !trill.getStops().isEmpty())) {
            return false;
        }
        return hasTrillMark;
    }

    public static List<Integer> mergeUniqueNumbers(Collection<Integer> base, Collection<Integer> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return base == null ? null : new ArrayList<Integer>(base);
        }
        java.util.TreeSet<Integer> merged = new java.util.TreeSet<Integer>();
        if (base != null) {
            for (Integer value : base) {
                if (value != null) {
                    merged.add(Integer.valueOf(Math.max(1, Math.round(value.intValue()))));
                }
            }
        }
        for (Integer value : incoming) {
            if (value != null) {
                merged.add(Integer.valueOf(Math.max(1, Math.round(value.intValue()))));
            }
        }
        return new ArrayList<Integer>(merged);
    }

    public static List<String> mergeUniqueStrings(Collection<String> base, Collection<String> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return base == null ? null : new ArrayList<String>(base);
        }
        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<String>();
        if (base != null) {
            merged.addAll(base);
        }
        merged.addAll(incoming);
        return new ArrayList<String>(merged);
    }

    public static List<String> parseMusicXmlArticulationSubtypes(Collection<String> articulationTags,
            Collection<String> technicalTags) {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<String>();
        if (articulationTags != null) {
            for (String articulationTag : articulationTags) {
                String tag = articulationTag == null ? "" : articulationTag.trim().toLowerCase(Locale.ROOT);
                if ("staccato".equals(tag)) {
                    out.add("articStaccatoAbove");
                }
                if ("accent".equals(tag)) {
                    out.add("articAccentAbove");
                }
                if ("tenuto".equals(tag)) {
                    out.add("articTenutoAbove");
                }
            }
        }
        if (technicalTags != null) {
            for (String technicalTag : technicalTags) {
                String tag = technicalTag == null ? "" : technicalTag.trim().toLowerCase(Locale.ROOT);
                if ("stopped".equals(tag)) {
                    out.add("articLhPizzicatoAbove");
                }
                if ("snap-pizzicato".equals(tag)) {
                    out.add("snapPizzicato");
                }
                if ("up-bow".equals(tag)) {
                    out.add("articUpBowAbove");
                }
                if ("down-bow".equals(tag)) {
                    out.add("articDownBowAbove");
                }
                if ("open-string".equals(tag)) {
                    out.add("articOpenStringAbove");
                }
                if ("harmonic".equals(tag)) {
                    out.add("articHarmonicAbove");
                }
            }
        }
        return new ArrayList<String>(out);
    }

    public static String parseMusicXmlTechnicalFingering(String fingeringText) {
        String text = fingeringText == null ? "" : fingeringText.trim();
        return text.length() == 0 ? null : text;
    }

    public static Integer parseMusicXmlTechnicalString(String stringText) {
        String raw = stringText == null ? "" : stringText.trim();
        if (raw.length() == 0) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(raw);
            return parsed > 0 ? Integer.valueOf(Math.round(parsed)) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String buildMidMeasureRepeatBarlineXml(String direction) {
        if ("end-start".equals(direction)) {
            return "<barline location=\"middle\"><bar-style>light-heavy</bar-style><repeat direction=\"backward\"/><repeat direction=\"forward\"/></barline>";
        }
        if ("forward".equals(direction)) {
            return "<barline location=\"middle\"><bar-style>heavy-light</bar-style><repeat direction=\"forward\"/></barline>";
        }
        return "<barline location=\"middle\"><bar-style>light-heavy</bar-style><repeat direction=\"backward\"/></barline>";
    }

    public static String parseMuseDynamicMark(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() == 0) {
            return null;
        }
        String[] allow = new String[] { "pppp", "ppp", "pp", "p", "mp", "mf", "f", "ff", "fff", "ffff", "sf",
                "sfz", "rfz" };
        for (String candidate : allow) {
            if (candidate.equals(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    public static String dynamicTagToMuseSubtype(String tag) {
        return parseMuseDynamicMark(tag);
    }

    public static Double parseMuseDynamicSoundValue(Integer velocity) {
        if (velocity == null || velocity.intValue() <= 0) {
            return null;
        }
        return Double.valueOf((velocity.doubleValue() / 90.0d) * 100.0d);
    }

    public static Integer musicXmlSoundDynamicsToMuseVelocity(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.length() == 0) {
            return null;
        }
        double value;
        try {
            value = Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return null;
        }
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0d) {
            return null;
        }
        int velocity = Math.round((float) ((value * 90.0d) / 100.0d));
        return Integer.valueOf(Math.max(1, Math.min(127, velocity)));
    }

    public static Double beatUnitToQuarterFactor(String beatUnit, int dots) {
        String unit = beatUnit == null ? "" : beatUnit.trim().toLowerCase(Locale.ROOT);
        double base;
        if ("whole".equals(unit)) {
            base = 4.0d;
        } else if ("half".equals(unit)) {
            base = 2.0d;
        } else if ("quarter".equals(unit)) {
            base = 1.0d;
        } else if ("eighth".equals(unit)) {
            base = 0.5d;
        } else if ("16th".equals(unit)) {
            base = 0.25d;
        } else if ("32nd".equals(unit)) {
            base = 0.125d;
        } else if ("64th".equals(unit)) {
            base = 0.0625d;
        } else {
            return null;
        }
        double factor = base;
        double add = base / 2.0d;
        for (int index = 0; index < Math.max(0, Math.round(dots)); index++) {
            factor += add;
            add /= 2.0d;
        }
        return Double.valueOf(factor);
    }

    public static double readDirectionTempoQps(String soundTempoRaw, String metronomePerMinuteRaw,
            String beatUnit, int beatUnitDots) {
        String soundTempoText = soundTempoRaw == null ? "" : soundTempoRaw.trim();
        if (soundTempoText.length() > 0) {
            try {
                double soundTempo = Double.parseDouble(soundTempoText);
                if (!Double.isNaN(soundTempo) && !Double.isInfinite(soundTempo) && soundTempo > 0.0d) {
                    return soundTempo / 60.0d;
                }
            } catch (NumberFormatException ex) {
                // Fall through to metronome.
            }
        }
        String perMinuteText = metronomePerMinuteRaw == null ? "" : metronomePerMinuteRaw.trim();
        if (perMinuteText.length() == 0) {
            return 0.0d;
        }
        double perMinute;
        try {
            perMinute = Double.parseDouble(perMinuteText);
        } catch (NumberFormatException ex) {
            return 0.0d;
        }
        if (Double.isNaN(perMinute) || Double.isInfinite(perMinute) || perMinute <= 0.0d) {
            return 0.0d;
        }
        Double quarterFactor = beatUnitToQuarterFactor(beatUnit, beatUnitDots);
        if (quarterFactor == null || quarterFactor.doubleValue() <= 0.0d) {
            return 0.0d;
        }
        return (perMinute * quarterFactor.doubleValue()) / 60.0d;
    }

    public static List<MuseDirectionSeed> collectDirectionSeedsFromMusicXmlMeasureValues(
            Collection<MusicXmlDirectionSeedSource> directions, Collection<String> measureSoundTempoTexts,
            int staffNo) {
        List<MuseDirectionSeed> out = new ArrayList<MuseDirectionSeed>();
        int targetStaffNo = Math.max(1, Math.round(staffNo));
        if (directions != null) {
            for (MusicXmlDirectionSeedSource direction : directions) {
                if (direction == null || getNoteStaffNo(direction.getStaff()) != targetStaffNo) {
                    continue;
                }
                double qps = readDirectionTempoQps(direction.getSoundTempo(), direction.getMetronomePerMinute(),
                        direction.getBeatUnit(), direction.getBeatUnitDots());
                boolean hasTempo = qps > 0.0d;
                Integer velocity = musicXmlSoundDynamicsToMuseVelocity(direction.getSoundDynamics());
                for (String dynamicTag : direction.getDynamicTags()) {
                    String subtype = dynamicTagToMuseSubtype(dynamicTag);
                    if (subtype != null) {
                        out.add(MuseDirectionSeed.dynamic(subtype, velocity));
                    }
                }
                if (direction.isHasSegno()) {
                    out.add(MuseDirectionSeed.marker("segno", "segno"));
                }
                if (direction.isHasCoda()) {
                    out.add(MuseDirectionSeed.marker("coda", "coda"));
                }
                boolean hasWords = false;
                for (MusicXmlDirectionWords words : direction.getWords()) {
                    String text = trimToEmpty(words.getText());
                    if (text.length() == 0) {
                        continue;
                    }
                    hasWords = true;
                    boolean italic = "italic".equals(trimToEmpty(words.getFontStyle()).toLowerCase(Locale.ROOT));
                    if (hasTempo) {
                        out.add(MuseDirectionSeed.tempo(qps, text, false, null));
                    } else if ("fine".equals(text.toLowerCase(Locale.ROOT))) {
                        out.add(MuseDirectionSeed.marker("fine", "Fine"));
                    } else {
                        out.add(MuseDirectionSeed.expression(text, italic));
                    }
                }
                if (hasTempo && !hasWords) {
                    out.add(MuseDirectionSeed.tempo(qps, null, false, null));
                }
                String soundDalsegno = trimToEmpty(direction.getSoundDalsegno());
                String soundDaCapo = trimToEmpty(direction.getSoundDaCapo()).toLowerCase(Locale.ROOT);
                String soundFine = trimToEmpty(direction.getSoundFine());
                String soundToCoda = trimToEmpty(direction.getSoundToCoda());
                if (soundDalsegno.length() > 0 || "yes".equals(soundDaCapo) || soundFine.length() > 0
                        || soundToCoda.length() > 0) {
                    String text = "yes".equals(soundDaCapo) ? "D.C."
                            : (soundDalsegno.length() > 0 ? "D.S." : "Jump");
                    String jumpTo = "yes".equals(soundDaCapo) ? "start"
                            : (soundDalsegno.length() > 0 ? soundDalsegno : null);
                    String playUntil = soundFine.length() > 0 ? soundFine
                            : (soundToCoda.length() > 0 ? "coda" : null);
                    String continueAt = soundToCoda.length() > 0 ? soundToCoda : null;
                    out.add(MuseDirectionSeed.jump(text, jumpTo, playUntil, continueAt));
                }
            }
        }
        if (targetStaffNo == 1 && measureSoundTempoTexts != null) {
            for (String tempoText : measureSoundTempoTexts) {
                String raw = trimToEmpty(tempoText);
                if (raw.length() == 0) {
                    continue;
                }
                try {
                    double bpm = Double.parseDouble(raw);
                    if (Double.isNaN(bpm) || Double.isInfinite(bpm) || bpm <= 0.0d) {
                        continue;
                    }
                    out.add(MuseDirectionSeed.tempo(bpm / 60.0d,
                            "<sym>metNoteQuarterUp</sym><font face=\"Edwin\"></font> = " + Math.round(bpm), true,
                            Boolean.FALSE));
                } catch (NumberFormatException ex) {
                    // Ignore invalid measure-level tempo values.
                }
            }
        }
        return out;
    }

    public static String buildMuseScoreDirectionSeedXml(MuseDirectionSeed seed) {
        if ("tempo".equals(seed.getKind())) {
            StringBuilder xml = new StringBuilder();
            xml.append("<Tempo><tempo>").append(String.format(Locale.ROOT, "%.6f", Double.valueOf(seed.getQps())))
                    .append("</tempo>");
            if (seed.isFollowText()) {
                xml.append("<followText>1</followText>");
            }
            if (Boolean.FALSE.equals(seed.getVisible())) {
                xml.append("<visible>0</visible>");
            }
            if (seed.getText() != null && seed.getText().length() > 0) {
                xml.append("<text>");
                xml.append(seed.getText().contains("<sym>") ? seed.getText() : xmlEscape(seed.getText()));
                xml.append("</text>");
            }
            xml.append("</Tempo>");
            return xml.toString();
        }
        if ("dynamic".equals(seed.getKind())) {
            return "<Dynamic><subtype>" + xmlEscape(seed.getSubtype()) + "</subtype>"
                    + (seed.getVelocity() != null ? "<velocity>" + seed.getVelocity().intValue() + "</velocity>" : "")
                    + "</Dynamic>";
        }
        if ("expression".equals(seed.getKind())) {
            String text = seed.isItalic() ? "<i></i>" + xmlEscape(seed.getText()) : xmlEscape(seed.getText());
            return "<Expression><text>" + text + "</text></Expression>";
        }
        if ("marker".equals(seed.getKind())) {
            return "<Marker><subtype>" + xmlEscape(seed.getSubtype()) + "</subtype><label>"
                    + xmlEscape(seed.getLabel()) + "</label></Marker>";
        }
        return "<Jump><text>" + xmlEscape(seed.getText()) + "</text>"
                + (seed.getJumpTo() != null ? "<jumpTo>" + xmlEscape(seed.getJumpTo()) + "</jumpTo>" : "")
                + (seed.getPlayUntil() != null ? "<playUntil>" + xmlEscape(seed.getPlayUntil()) + "</playUntil>"
                        : "")
                + (seed.getContinueAt() != null ? "<continueAt>" + xmlEscape(seed.getContinueAt()) + "</continueAt>"
                        : "")
                + "</Jump>";
    }

    public static String buildMuseScoreMeasureHeaderXml(MuseScoreExportMeasureContext measureContext,
            Transpose partTranspose) {
        StringBuilder xml = new StringBuilder();
        if (measureContext.isShouldWriteClef()) {
            String clefType = measureContext.getMeasureClefType() != null ? measureContext.getMeasureClefType()
                    : clefSignToMuseConcertClefType(measureContext.getTargetClef());
            xml.append("<Clef><concertClefType>").append(clefType).append("</concertClefType></Clef>");
        }
        if (measureContext.isShouldWriteTime()) {
            String cutSubtypeXml = "cut".equals(measureContext.getMeasureTimeSymbol()) ? "<subtype>2</subtype>" : "";
            xml.append("<TimeSig>").append(cutSubtypeXml).append("<sigN>")
                    .append(measureContext.getEffectiveMeasureBeats()).append("</sigN><sigD>")
                    .append(measureContext.getEffectiveMeasureBeatType()).append("</sigD></TimeSig>");
        }
        if (measureContext.isShouldWriteKey()) {
            xml.append(resolveMuseExportKeySigXml(measureContext.getMeasureFifths(), partTranspose));
        }
        if (measureContext.isNeedsDoubleBarlineAtMeasureStart()) {
            xml.append("<BarLine><subtype>double</subtype></BarLine>");
        }
        for (MuseDirectionSeed seed : measureContext.getDirectionSeeds()) {
            xml.append(buildMuseScoreDirectionSeedXml(seed));
        }
        if (measureContext.isHasStartRepeat()) {
            xml.append("<startRepeat/>");
        }
        return xml.toString();
    }

    public static List<Integer> resolveMuseExportSlurIds(MuseScoreExportSlurState state, int partNo, int staffNo,
            int voiceNo, Collection<Integer> sourceNumbers, boolean start) {
        List<Integer> out = new ArrayList<Integer>();
        if (sourceNumbers == null || sourceNumbers.isEmpty()) {
            return out;
        }
        for (Integer sourceNumber : sourceNumbers) {
            int normalizedSourceNo = Math.max(1, Math.round(sourceNumber == null ? 1 : sourceNumber.intValue()));
            String scopedKey = partNo + ":" + staffNo + ":" + voiceNo + ":" + normalizedSourceNo;
            List<Integer> active = state.getSlurActiveIdsBySource().get(scopedKey);
            if (active == null) {
                active = new ArrayList<Integer>();
            }
            if (start) {
                int resolved = state.getNextSlurId();
                state.setNextSlurId(resolved + 1);
                active.add(Integer.valueOf(resolved));
                state.getSlurActiveIdsBySource().put(scopedKey, active);
                out.add(Integer.valueOf(resolved));
                continue;
            }
            int resolved;
            if (!active.isEmpty()) {
                resolved = active.remove(active.size() - 1).intValue();
            } else {
                resolved = state.getNextSlurId();
            }
            if (active.isEmpty()) {
                state.getSlurActiveIdsBySource().remove(scopedKey);
            } else {
                state.getSlurActiveIdsBySource().put(scopedKey, active);
            }
            if (resolved == state.getNextSlurId()) {
                state.setNextSlurId(state.getNextSlurId() + 1);
            }
            out.add(Integer.valueOf(resolved));
        }
        return out;
    }

    public static MuseScoreExportSlurFractions resolveMuseExportSlurFractions(MuseScoreExportSlurState slurState,
            int partNo, int staffNo, int voiceNo, Collection<Integer> slurStarts, Collection<Integer> slurStops,
            String defaultSlurSpanFraction, Map<Integer, String> activeSlurSpanFractionById) {
        Map<Integer, String> safeActive = activeSlurSpanFractionById == null
                ? new java.util.LinkedHashMap<Integer, String>()
                : activeSlurSpanFractionById;
        List<Integer> resolvedSlurStops = resolveMuseExportSlurIds(slurState, partNo, staffNo, voiceNo, slurStops,
                false);
        List<Integer> resolvedSlurStarts = resolveMuseExportSlurIds(slurState, partNo, staffNo, voiceNo, slurStarts,
                true);
        List<String> slurStopFractions = new ArrayList<String>();
        for (Integer slurId : resolvedSlurStops) {
            String span = safeActive.containsKey(slurId) ? safeActive.get(slurId) : defaultSlurSpanFraction;
            safeActive.remove(slurId);
            slurStopFractions.add(span);
        }
        List<String> slurStartFractions = new ArrayList<String>();
        for (Integer slurId : resolvedSlurStarts) {
            safeActive.put(slurId, defaultSlurSpanFraction);
            slurStartFractions.add(defaultSlurSpanFraction);
        }
        return new MuseScoreExportSlurFractions(slurStartFractions, slurStopFractions);
    }

    public static MuseScoreExportTupletRef resolveMuseExportTupletRef(MuseScoreExportTupletRefState state,
            Collection<Integer> tupletStarts, Collection<Integer> tupletStops, TimeModification timeModification,
            int durationDiv) {
        Collection<Integer> safeStarts = tupletStarts == null ? Collections.<Integer>emptyList() : tupletStarts;
        Collection<Integer> safeStops = tupletStops == null ? Collections.<Integer>emptyList() : tupletStops;
        boolean hasTupletTiming = timeModification != null;
        StringBuilder xml = new StringBuilder();
        for (Integer number : safeStarts) {
            int normalized = Math.max(1, Math.round(number == null ? 1 : number.intValue()));
            String refId = "T" + state.getNextTupletRefNo();
            state.setNextTupletRefNo(state.getNextTupletRefNo() + 1);
            TimeModification tm = timeModification == null ? new TimeModification(3, 2) : timeModification;
            xml.append(buildMuseExportTupletRefDefinitionXml(refId, tm));
            state.getActiveTupletRefByNumber().put(Integer.valueOf(normalized), refId);
        }
        if (!hasTupletTiming && safeStarts.isEmpty() && safeStops.isEmpty()) {
            state.getActiveTupletRefByNumber().clear();
        }
        if (hasTupletTiming && state.getActiveTupletRefByNumber().isEmpty()) {
            int implicitNumber = 1000000 + state.getNextTupletRefNo();
            String refId = "T" + state.getNextTupletRefNo();
            state.setNextTupletRefNo(state.getNextTupletRefNo() + 1);
            xml.append(buildMuseExportTupletRefDefinitionXml(refId, timeModification));
            state.getActiveTupletRefByNumber().put(Integer.valueOf(implicitNumber), refId);
        }
        List<Integer> numbers = new ArrayList<Integer>(state.getActiveTupletRefByNumber().keySet());
        Collections.sort(numbers);
        String tupletRefId = null;
        if (hasTupletTiming && !numbers.isEmpty()) {
            tupletRefId = state.getActiveTupletRefByNumber().get(numbers.get(numbers.size() - 1));
        }
        int tupletDisplayDurationDiv = durationDiv;
        if (timeModification != null) {
            tupletDisplayDurationDiv = Math.max(1, Math.round(Math.max(1, durationDiv)
                    * timeModification.getActualNotes() / (float) timeModification.getNormalNotes()));
        }
        return new MuseScoreExportTupletRef(xml.toString(), tupletRefId, tupletDisplayDurationDiv);
    }

    public static void applyMuseExportTupletStops(MuseScoreExportTupletRefState state,
            Collection<Integer> tupletStops) {
        if (tupletStops == null) {
            return;
        }
        for (Integer number : tupletStops) {
            int normalized = Math.max(1, Math.round(number == null ? 1 : number.intValue()));
            state.getActiveTupletRefByNumber().remove(Integer.valueOf(normalized));
        }
    }

    private static String buildMuseExportTupletRefDefinitionXml(String refId, TimeModification timeModification) {
        TimeModification tm = timeModification == null ? new TimeModification(3, 2) : timeModification;
        return "<Tuplet id=\"" + xmlEscape(refId) + "\"><normalNotes>" + tm.getNormalNotes()
                + "</normalNotes><actualNotes>" + tm.getActualNotes() + "</actualNotes></Tuplet>";
    }

    public static String buildMuseScoreExportVoiceXml(Collection<MuseVoiceEvent> sourceEvents, int renderCapacityDiv,
            int voiceNo, int staffNo, int divisions, int partNo, MuseScoreExportSlurState slurState,
            Map<Integer, String> activeSlurSpanFractionById) {
        List<MuseVoiceEvent> events = sourceEvents == null ? new ArrayList<MuseVoiceEvent>()
                : new ArrayList<MuseVoiceEvent>(sourceEvents);
        Collections.sort(events, new java.util.Comparator<MuseVoiceEvent>() {
            @Override
            public int compare(MuseVoiceEvent left, MuseVoiceEvent right) {
                return Integer.valueOf(left.getAtDiv()).compareTo(Integer.valueOf(right.getAtDiv()));
            }
        });
        MuseScoreExportTupletRefState tupletRefState = new MuseScoreExportTupletRefState();
        Map<Integer, String> safeActiveSlurs = activeSlurSpanFractionById == null
                ? new java.util.LinkedHashMap<Integer, String>()
                : activeSlurSpanFractionById;
        MuseScoreExportSlurState safeSlurState = slurState == null ? new MuseScoreExportSlurState() : slurState;
        StringBuilder voiceXml = new StringBuilder();
        voiceXml.append("<voice>");
        int cursorDiv = 0;
        for (MuseVoiceEvent event : events) {
            if (event.getAtDiv() > cursorDiv) {
                int gap = Math.min(event.getAtDiv(), renderCapacityDiv) - cursorDiv;
                if (gap > 0) {
                    voiceXml.append(makeMuseRestXml(gap, gap, divisions, null));
                    cursorDiv += gap;
                }
            }
            voiceXml.append(buildMuseVoiceRepeatBarLineXml(event.isRepeatForwardAtStart(),
                    event.isRepeatBackwardAtStart()));
            MuseScoreExportTupletRef tuplet = resolveMuseExportTupletRef(tupletRefState, event.getTupletStarts(),
                    event.getTupletStops(), event.getTupletTimeModification(), event.getDurationDiv());
            voiceXml.append(tuplet.getDefinitionXml());
            if (event.getPitches() == null) {
                voiceXml.append(makeMuseRestXml(event.getDurationDiv(), tuplet.getDisplayDurationDiv(), divisions,
                        tuplet.getTupletRefId()));
            } else {
                String defaultSlurSpanFraction = fractionFromDivisions(
                        Math.max(1, Math.round(tuplet.getDisplayDurationDiv() > 0 ? tuplet.getDisplayDurationDiv()
                                : event.getDurationDiv())),
                        divisions);
                MuseScoreExportSlurFractions slurFractions = resolveMuseExportSlurFractions(safeSlurState, partNo,
                        staffNo, voiceNo, event.getSlurStarts(), event.getSlurStops(), defaultSlurSpanFraction,
                        safeActiveSlurs);
                voiceXml.append(makeMuseChordXml(new MuseScoreExportChord(event.getDurationDiv(),
                        tuplet.getDisplayDurationDiv(), divisions, event.getPitches(),
                        slurFractions.getSlurStartFractions(), slurFractions.getSlurStopFractions(),
                        event.getArticulationSubtypes(), event.isTrillMarkOnly(), event.getTrillStarts(),
                        event.getTrillStops(), tuplet.getTupletRefId(), event.getOttavaStartSubtypes(),
                        event.getOttavaStopCount(), event.isGrace(), event.isGraceSlash())));
            }
            applyMuseExportTupletStops(tupletRefState, event.getTupletStops());
            cursorDiv += event.getDurationDiv();
        }
        if (cursorDiv < renderCapacityDiv) {
            int rest = renderCapacityDiv - cursorDiv;
            voiceXml.append(makeMuseRestXml(rest, rest, divisions, null));
        }
        voiceXml.append("</voice>");
        return voiceXml.toString();
    }

    public static String buildMuseVoiceRepeatBarLineXml(boolean repeatForwardAtStart,
            boolean repeatBackwardAtStart) {
        String subtype = "";
        if (repeatForwardAtStart && repeatBackwardAtStart) {
            subtype = "end-start-repeat";
        } else if (repeatForwardAtStart) {
            subtype = "start-repeat";
        } else if (repeatBackwardAtStart) {
            subtype = "end-repeat";
        }
        return subtype.length() == 0 ? "" : "<BarLine><subtype>" + subtype + "</subtype></BarLine>";
    }

    public static MuseScoreExportMeasureVoiceXml buildMuseScoreExportMeasureVoiceXml(
            MuseScoreExportMeasureContext measureContext, Collection<MuseVoiceEvent> events, int renderCapacityDiv,
            int voiceNo, int voiceIndex, int staffNo, int divisions, int partNo, Transpose partTranspose,
            MuseScoreExportSlurState slurState, Map<Integer, Map<Integer, String>> activeSlurSpanFractionByVoice) {
        Map<Integer, Map<Integer, String>> safeByVoice = activeSlurSpanFractionByVoice == null
                ? new java.util.LinkedHashMap<Integer, Map<Integer, String>>()
                : activeSlurSpanFractionByVoice;
        Map<Integer, String> activeSlurSpanFractionById = safeByVoice.get(Integer.valueOf(voiceNo));
        if (activeSlurSpanFractionById == null) {
            activeSlurSpanFractionById = new java.util.LinkedHashMap<Integer, String>();
            safeByVoice.put(Integer.valueOf(voiceNo), activeSlurSpanFractionById);
        }
        String voiceXml = buildMuseScoreExportVoiceXml(events, renderCapacityDiv, voiceNo, staffNo, divisions, partNo,
                slurState, activeSlurSpanFractionById);
        if (voiceIndex == 0) {
            String header = buildMuseScoreMeasureHeaderXml(measureContext, partTranspose);
            StringBuilder xml = new StringBuilder();
            xml.append("<voice>").append(header).append(stripMuseVoiceWrapper(voiceXml));
            if (measureContext.isHasEndRepeat()) {
                xml.append("<endRepeat/>");
            }
            xml.append("</voice>");
            return new MuseScoreExportMeasureVoiceXml(xml.toString(), measureContext.getTargetClef());
        }
        return new MuseScoreExportMeasureVoiceXml(voiceXml, null);
    }

    public static String stripMuseVoiceWrapper(String voiceXml) {
        String xml = voiceXml == null ? "" : voiceXml;
        if (xml.startsWith("<voice>")) {
            xml = xml.substring("<voice>".length());
        }
        if (xml.endsWith("</voice>")) {
            xml = xml.substring(0, xml.length() - "</voice>".length());
        }
        return xml;
    }

    public static MuseScoreExportStaffState createInitialMuseScoreExportStaffState(int divisions, int currentBeats,
            int currentBeatType, int currentFifths, String initialClef) {
        return new MuseScoreExportStaffState(Math.max(1, divisions), Math.max(1, currentBeats),
                Math.max(1, currentBeatType), null, Math.max(-7, Math.min(7, currentFifths)), initialClef);
    }

    public static MuseScoreExportStaffState applyMuseScoreExportMeasureState(MuseScoreExportStaffState staffState,
            MuseScoreExportMeasureContext measureContext, String targetClef) {
        int sourceDivisions = measureContext.getMeasureSourceDivisions() > 0
                ? measureContext.getMeasureSourceDivisions()
                : staffState.getCurrentSourceDivisions();
        return new MuseScoreExportStaffState(sourceDivisions, measureContext.getEffectiveMeasureBeats(),
                measureContext.getEffectiveMeasureBeatType(), measureContext.getMeasureTimeSymbol(),
                measureContext.getMeasureFifths(), targetClef != null ? targetClef : staffState.getCurrentClef());
    }

    public static MuseScoreExportMeasureContextResult buildMuseScoreExportMeasureContextFromValues(
            int measureSourceDivisions, Map<Integer, List<MuseVoiceEvent>> byVoice, int measureIndex, int divisions,
            int currentBeats, int currentBeatType, String currentTimeSymbol, int currentFifths, String currentClef,
            int measureBeats, int measureBeatType, String measureTimeSymbolRaw, boolean hasExplicitTimeInMusicXml,
            int measureFifthsRaw, String measureClef, String measureClefType, boolean normalizeCutTimeToTwoTwo,
            boolean hasImplicitMeasureInMusicXml, boolean hasLeftDoubleBarlineInMusicXml,
            boolean hasPrevRightDoubleBarlineInMusicXml, Collection<MuseDirectionSeed> directionSeeds,
            boolean hasStartRepeat, boolean hasEndRepeat) {
        String timeSymbol = "cut".equals(normalizeToken(measureTimeSymbolRaw)) ? "cut" : currentTimeSymbol;
        boolean shouldNormalizeCut = normalizeCutTimeToTwoTwo && "cut".equals(timeSymbol) && measureBeats == 4
                && measureBeatType == 4;
        int effectiveMeasureBeats = shouldNormalizeCut ? 2 : Math.max(1, measureBeats);
        int effectiveMeasureBeatType = shouldNormalizeCut ? 2 : Math.max(1, measureBeatType);
        int measureFifths = Math.max(-7, Math.min(7, Math.round(measureFifthsRaw)));
        int capacityDiv = Math.max(1,
                Math.round((Math.max(1, divisions) * 4 * effectiveMeasureBeats)
                        / (float) Math.max(1, effectiveMeasureBeatType)));
        int usedDiv = computeMuseScoreExportUsedDiv(byVoice);
        Integer implicitLenDiv = hasImplicitMeasureInMusicXml
                ? Integer.valueOf(Math.max(1, Math.min(capacityDiv, usedDiv)))
                : null;
        int renderCapacityDiv = implicitLenDiv == null ? capacityDiv : implicitLenDiv.intValue();
        String lenAttr = implicitLenDiv != null && implicitLenDiv.intValue() < capacityDiv
                ? formatMeasureLenFromDivisions(implicitLenDiv.intValue(), divisions)
                : null;
        List<Integer> voiceNos = resolveMuseScoreExportVoiceNos(byVoice);
        String targetClef = measureClef != null ? measureClef : currentClef;
        boolean shouldWriteClef = measureIndex > 0 && measureClefType != null;
        boolean shouldWriteTime = measureIndex == 0 || effectiveMeasureBeats != currentBeats
                || effectiveMeasureBeatType != currentBeatType
                || !stringEquals(timeSymbol, currentTimeSymbol) || hasExplicitTimeInMusicXml;
        boolean shouldWriteKey = measureIndex == 0 || measureFifths != currentFifths;
        boolean needsDoubleBarlineAtMeasureStart = hasLeftDoubleBarlineInMusicXml
                || hasPrevRightDoubleBarlineInMusicXml;
        MuseScoreExportMeasureContext context = new MuseScoreExportMeasureContext(
                Math.max(1, measureSourceDivisions), effectiveMeasureBeats, effectiveMeasureBeatType, timeSymbol,
                measureFifths, targetClef, measureClefType, shouldWriteClef, shouldWriteTime, shouldWriteKey,
                needsDoubleBarlineAtMeasureStart, directionSeeds, hasStartRepeat, hasEndRepeat);
        return new MuseScoreExportMeasureContextResult(context, capacityDiv, renderCapacityDiv, lenAttr, voiceNos,
                usedDiv);
    }

    public static int computeMuseScoreExportUsedDiv(Map<Integer, List<MuseVoiceEvent>> byVoice) {
        int usedDiv = 0;
        if (byVoice != null) {
            for (List<MuseVoiceEvent> events : byVoice.values()) {
                if (events == null) {
                    continue;
                }
                for (MuseVoiceEvent event : events) {
                    if (event == null) {
                        continue;
                    }
                    int end = event.isGrace() ? event.getAtDiv()
                            : event.getAtDiv() + Math.max(0, Math.round(event.getDurationDiv()));
                    usedDiv = Math.max(usedDiv, end);
                }
            }
        }
        return usedDiv;
    }

    public static List<Integer> resolveMuseScoreExportVoiceNos(Map<Integer, List<MuseVoiceEvent>> byVoice) {
        List<Integer> voiceNos = new ArrayList<Integer>();
        if (byVoice != null) {
            voiceNos.addAll(byVoice.keySet());
        }
        Collections.sort(voiceNos);
        if (voiceNos.isEmpty()) {
            voiceNos.add(Integer.valueOf(1));
        }
        return voiceNos;
    }

    public static int lcmForMuseScoreExportDivisions(int a, int b) {
        int x = Math.max(1, Math.abs(Math.round(a)));
        int y = Math.max(1, Math.abs(Math.round(b)));
        return Math.max(1, Math.round((x / (float) gcdPositive(x, y)) * y));
    }

    public static int computeGlobalMusicXmlDivisionsFromValues(Collection<Integer> rawValues) {
        List<Integer> values = new ArrayList<Integer>();
        if (rawValues != null) {
            for (Integer raw : rawValues) {
                if (raw != null && raw.intValue() > 0) {
                    values.add(Integer.valueOf(Math.max(1, Math.round(raw.intValue()))));
                }
            }
        }
        if (values.isEmpty()) {
            return 480;
        }
        int lcm = values.get(0).intValue();
        for (int index = 1; index < values.size(); index++) {
            lcm = lcmForMuseScoreExportDivisions(lcm, values.get(index).intValue());
            if (lcm > 3840) {
                return 3840;
            }
        }
        return lcm;
    }

    public static int getMeasureStaffCountFromMusicXmlValues(Integer staves, Collection<String> noteStaffTexts) {
        int maxStaff = 1;
        if (staves != null && staves.intValue() > 0) {
            maxStaff = Math.max(maxStaff, Math.round(staves.intValue()));
        }
        if (noteStaffTexts != null) {
            for (String staffText : noteStaffTexts) {
                maxStaff = Math.max(maxStaff, getNoteStaffNo(staffText));
            }
        }
        return maxStaff;
    }

    public static int getPartStaffCountFromMusicXmlValues(Collection<Integer> measureStaffCounts) {
        int maxStaff = 1;
        if (measureStaffCounts != null) {
            for (Integer count : measureStaffCounts) {
                if (count != null && count.intValue() > 0) {
                    maxStaff = Math.max(maxStaff, Math.round(count.intValue()));
                }
            }
        }
        return maxStaff;
    }

    public static Map<String, MuseScoreExportPartName> readPartNameMapFromMusicXmlParts(
            Collection<MuseScoreExportPartNameEntry> entries) {
        Map<String, MuseScoreExportPartName> map = new java.util.LinkedHashMap<String, MuseScoreExportPartName>();
        if (entries != null) {
            for (MuseScoreExportPartNameEntry entry : entries) {
                if (entry == null) {
                    continue;
                }
                String id = entry.getId() == null ? "" : entry.getId().trim();
                if (id.length() == 0) {
                    continue;
                }
                String name = entry.getName() == null ? "" : entry.getName().trim();
                String abbreviation = entry.getAbbreviation() == null ? "" : entry.getAbbreviation().trim();
                map.put(id, new MuseScoreExportPartName(name.length() == 0 ? id : name, abbreviation));
            }
        }
        return map;
    }

    public static MuseScoreExportMetadata readMusicXmlExportMetadataFromValues(String workTitle,
            String movementTitle, String workNumber, String movementNumber, Collection<MuseScoreExportCredit> credits,
            Collection<MuseScoreExportCreator> creators, String rights, String creationDate) {
        String normalizedWorkTitle = trimToEmpty(workTitle);
        String normalizedMovementTitle = trimToEmpty(movementTitle);
        String title = normalizedWorkTitle.length() > 0 ? normalizedWorkTitle
                : (normalizedMovementTitle.length() > 0 ? normalizedMovementTitle : "mikuscore export");
        String composer = readMusicXmlCreatorByTypeFromValues(creators, "composer");
        if (composer.length() == 0) {
            composer = readFirstMusicXmlCreatorFromValues(creators);
        }
        return new MuseScoreExportMetadata(title, readMusicXmlSubtitleFromCredits(credits), composer,
                readMusicXmlCreatorByTypeFromValues(creators, "arranger"),
                readMusicXmlCreatorByTypeFromValues(creators, "lyricist"),
                readMusicXmlCreatorByTypeFromValues(creators, "translator"), trimToEmpty(rights),
                trimToEmpty(workNumber), normalizedMovementTitle, trimToEmpty(movementNumber),
                trimToEmpty(creationDate));
    }

    public static String readMusicXmlSubtitleFromCredits(Collection<MuseScoreExportCredit> credits) {
        if (credits != null) {
            for (MuseScoreExportCredit credit : credits) {
                if (credit == null) {
                    continue;
                }
                if (!"subtitle".equals(trimToEmpty(credit.getType()).toLowerCase(Locale.ROOT))) {
                    continue;
                }
                String words = trimToEmpty(credit.getWords());
                if (words.length() > 0) {
                    return words;
                }
            }
        }
        return "";
    }

    public static String readMusicXmlCreatorByTypeFromValues(Collection<MuseScoreExportCreator> creators,
            String type) {
        String normalizedType = trimToEmpty(type).toLowerCase(Locale.ROOT);
        if (creators != null) {
            for (MuseScoreExportCreator creator : creators) {
                if (creator == null) {
                    continue;
                }
                if (normalizedType.equals(trimToEmpty(creator.getType()).toLowerCase(Locale.ROOT))) {
                    return trimToEmpty(creator.getText());
                }
            }
        }
        return "";
    }

    public static String readFirstMusicXmlCreatorFromValues(Collection<MuseScoreExportCreator> creators) {
        if (creators != null) {
            for (MuseScoreExportCreator creator : creators) {
                if (creator == null) {
                    continue;
                }
                String text = trimToEmpty(creator.getText());
                if (text.length() > 0) {
                    return text;
                }
            }
        }
        return "";
    }

    public static String buildMuseScoreExportStaffXml(int staffId, Collection<MuseScoreExportStaffMeasure> measures,
            int staffNo, int divisions, int partNo, Transpose partTranspose, MuseScoreExportSlurState slurState,
            MuseScoreExportStaffState initialState) {
        MuseScoreExportStaffState staffState = initialState == null
                ? createInitialMuseScoreExportStaffState(divisions, 4, 4, 0, "G")
                : initialState;
        Map<Integer, Map<Integer, String>> activeSlurSpanFractionByVoice = new java.util.LinkedHashMap<Integer, Map<Integer, String>>();
        MuseScoreExportSlurState safeSlurState = slurState == null ? new MuseScoreExportSlurState() : slurState;
        StringBuilder staffXml = new StringBuilder();
        staffXml.append("<Staff id=\"").append(staffId).append("\">");
        if (measures != null) {
            for (MuseScoreExportStaffMeasure measure : measures) {
                if (measure == null) {
                    continue;
                }
                String lenAttr = measure.getLenAttr();
                staffXml.append(lenAttr != null && lenAttr.length() > 0 ? "<Measure len=\"" + xmlEscape(lenAttr) + "\">"
                        : "<Measure>");
                String targetClef = null;
                List<Integer> voiceNos = measure.getVoiceNos();
                for (int index = 0; index < voiceNos.size(); index++) {
                    int voiceNo = voiceNos.get(index).intValue();
                    MuseScoreExportMeasureVoiceXml voice = buildMuseScoreExportMeasureVoiceXml(
                            measure.getMeasureContext(), measure.getEventsByVoice().get(Integer.valueOf(voiceNo)),
                            measure.getRenderCapacityDiv(), voiceNo, index, staffNo, divisions, partNo, partTranspose,
                            safeSlurState, activeSlurSpanFractionByVoice);
                    if (voice.getTargetClef() != null) {
                        targetClef = voice.getTargetClef();
                    }
                    staffXml.append(voice.getXml());
                }
                staffXml.append("</Measure>");
                staffState = applyMuseScoreExportMeasureState(staffState, measure.getMeasureContext(), targetClef);
            }
        }
        staffXml.append("</Staff>");
        return staffXml.toString();
    }

    public static String buildMuseScoreExportInstrumentXml(String partName, String partAbbreviation,
            Map<Integer, String> initialClefByStaff, Transpose partTranspose) {
        StringBuilder instrumentClefXml = new StringBuilder();
        if (initialClefByStaff != null) {
            List<Integer> staffNos = new ArrayList<Integer>(initialClefByStaff.keySet());
            Collections.sort(staffNos);
            for (Integer staffNoValue : staffNos) {
                int staffNo = staffNoValue == null ? 1 : Math.max(1, staffNoValue.intValue());
                String museClef = clefSignToMuseDefaultClef(initialClefByStaff.get(staffNoValue));
                if (staffNo <= 1) {
                    instrumentClefXml.append("<clef>").append(museClef).append("</clef>");
                } else {
                    instrumentClefXml.append("<clef staff=\"").append(staffNo).append("\">").append(museClef)
                            .append("</clef>");
                }
            }
        }
        StringBuilder transposeXml = new StringBuilder();
        if (partTranspose != null && partTranspose.getDiatonic() != null) {
            transposeXml.append("<transposeDiatonic>").append(partTranspose.getDiatonic().intValue())
                    .append("</transposeDiatonic>");
        }
        if (partTranspose != null && partTranspose.getChromatic() != null) {
            transposeXml.append("<transposeChromatic>").append(partTranspose.getChromatic().intValue())
                    .append("</transposeChromatic>");
        }
        String name = partName == null ? "" : partName;
        String abbreviation = partAbbreviation == null ? "" : partAbbreviation;
        return "<Instrument><trackName>" + xmlEscape(name) + "</trackName><longName>" + xmlEscape(name)
                + "</longName>" + (abbreviation.length() > 0 ? "<shortName>" + xmlEscape(abbreviation)
                        + "</shortName>" : "")
                + instrumentClefXml.toString() + transposeXml.toString() + "</Instrument>";
    }

    public static String buildMuseScoreExportPartDefBodyXml(String partName, String partAbbreviation,
            Collection<Integer> staffIds, Map<Integer, String> initialClefByStaff, Transpose partTranspose) {
        StringBuilder staffDefs = new StringBuilder();
        int index = 1;
        if (staffIds != null) {
            for (Integer ignored : staffIds) {
                String clef = initialClefByStaff == null ? null : initialClefByStaff.get(Integer.valueOf(index));
                staffDefs.append("<Staff><defaultClef>").append(clefSignToMuseDefaultClef(clef))
                        .append("</defaultClef></Staff>");
                index++;
            }
        }
        String name = partName == null ? "" : partName;
        return staffDefs.toString() + "<trackName>" + xmlEscape(name) + "</trackName>"
                + buildMuseScoreExportInstrumentXml(name, partAbbreviation, initialClefByStaff, partTranspose);
    }

    public static MuseScoreExportPartIdentity resolveMuseScoreExportPartIdentity(String partId, int partNo,
            Map<String, MuseScoreExportPartName> partNameById) {
        String normalizedPartId = partId == null ? "" : partId.trim();
        MuseScoreExportPartName partInfo = partNameById == null ? null : partNameById.get(normalizedPartId);
        String partName = partInfo == null ? (normalizedPartId.length() > 0 ? normalizedPartId : "P" + partNo)
                : partInfo.getName();
        String partAbbreviation = partInfo == null ? "" : partInfo.getAbbreviation();
        return new MuseScoreExportPartIdentity(partName == null ? "" : partName.trim(),
                partAbbreviation == null ? "" : partAbbreviation.trim());
    }

    public static MuseScoreExportPartScaffold buildMuseScoreExportPartScaffold(String partName,
            String partAbbreviation, int nextStaffId, int laneCount, Transpose partTranspose,
            Map<Integer, String> explicitClefByStaff, Map<Integer, Collection<Integer>> pitchesByStaff) {
        int safeLaneCount = Math.max(1, laneCount);
        Map<Integer, String> initialClefByStaff = new java.util.LinkedHashMap<Integer, String>();
        for (int staffNo = 1; staffNo <= safeLaneCount; staffNo++) {
            String explicit = explicitClefByStaff == null ? null : explicitClefByStaff.get(Integer.valueOf(staffNo));
            String byName = staffNo == 1 ? inferClefSignFromPartName(partName) : null;
            Collection<Integer> pitches = pitchesByStaff == null ? null : pitchesByStaff.get(Integer.valueOf(staffNo));
            String fallback = inferClefSignFromPitches(pitches);
            initialClefByStaff.put(Integer.valueOf(staffNo),
                    explicit != null ? explicit : (byName != null ? byName : fallback));
        }
        List<Integer> staffIds = new ArrayList<Integer>();
        for (int index = 0; index < safeLaneCount; index++) {
            staffIds.add(Integer.valueOf(nextStaffId + index));
        }
        String partDefBodyXml = buildMuseScoreExportPartDefBodyXml(partName, partAbbreviation, staffIds,
                initialClefByStaff, partTranspose);
        return new MuseScoreExportPartScaffold(staffIds, partTranspose, initialClefByStaff, partDefBodyXml);
    }

    public static MuseScoreExportPartResult buildMuseScoreExportPartResult(int partNo, String partName,
            String partAbbreviation, int nextStaffId, Collection<MuseScoreExportStaffMeasure> measuresByStaffOne,
            Map<Integer, Collection<MuseScoreExportStaffMeasure>> measuresByStaff, int staffCount, int divisions,
            Map<Integer, String> initialClefByStaff, Transpose partTranspose, MuseScoreExportSlurState slurState) {
        int laneCount = Math.max(1, staffCount);
        List<Integer> staffIds = new ArrayList<Integer>();
        for (int index = 0; index < laneCount; index++) {
            staffIds.add(Integer.valueOf(nextStaffId + index));
        }
        String partDefBodyXml = buildMuseScoreExportPartDefBodyXml(partName, partAbbreviation, staffIds,
                initialClefByStaff, partTranspose);
        List<String> staffsXml = new ArrayList<String>();
        for (int staffNo = 1; staffNo <= laneCount; staffNo++) {
            Collection<MuseScoreExportStaffMeasure> measures = measuresByStaff == null ? null
                    : measuresByStaff.get(Integer.valueOf(staffNo));
            if (measures == null && staffNo == 1) {
                measures = measuresByStaffOne;
            }
            String initialClef = initialClefByStaff == null ? "G" : initialClefByStaff.get(Integer.valueOf(staffNo));
            staffsXml.add(buildMuseScoreExportStaffXml(nextStaffId + staffNo - 1, measures, staffNo, divisions, partNo,
                    partTranspose, slurState, createInitialMuseScoreExportStaffState(divisions, 4, 4, 0,
                            initialClef == null ? "G" : initialClef)));
        }
        return new MuseScoreExportPartResult(nextStaffId + staffIds.size(),
                "<Part id=\"" + partNo + "\">" + partDefBodyXml + "</Part>", staffsXml);
    }

    public static MuseScoreExportDocumentBody buildMuseScoreExportDocumentBody(
            Collection<MuseScoreExportPartResult> partResults) {
        List<String> partDefs = new ArrayList<String>();
        List<String> staffsXml = new ArrayList<String>();
        if (partResults != null) {
            for (MuseScoreExportPartResult partResult : partResults) {
                if (partResult == null) {
                    continue;
                }
                partDefs.add(partResult.getPartDefXml());
                staffsXml.addAll(partResult.getStaffsXml());
            }
        }
        return new MuseScoreExportDocumentBody(partDefs, staffsXml);
    }

    public static String buildMuseScoreExportDocumentXml(MuseScoreExportMetadata metadata, int divisions,
            MuseScoreExportDocumentBody body) {
        StringBuilder xml = new StringBuilder();
        xml.append(buildMuseScoreExportMetadataXml(metadata, divisions));
        if (body != null) {
            for (String partDef : body.getPartDefs()) {
                xml.append(partDef == null ? "" : partDef);
            }
            for (String staffXml : body.getStaffsXml()) {
                xml.append(staffXml == null ? "" : staffXml);
            }
        }
        xml.append("</Score></museScore>");
        return xml.toString();
    }

    public static String buildMuseScoreExportXmlFromParts(MuseScoreExportMetadata metadata, int divisions,
            Collection<MuseScoreExportPartResult> partResults) {
        if (partResults == null || partResults.isEmpty()) {
            String title = metadata == null ? "mikuscore export" : metadata.getTitle();
            return buildEmptyMuseScoreExportXml(divisions, title);
        }
        return buildMuseScoreExportDocumentXml(metadata, divisions, buildMuseScoreExportDocumentBody(partResults));
    }

    public static String buildEmptyMuseScoreExportXml(int divisions, String title) {
        int safeDivisions = Math.max(1, divisions);
        int capacity = Math.max(1, Math.round((safeDivisions * 4 * 4) / 4.0f));
        String workTitle = title == null || title.length() == 0 ? "Untitled" : title;
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><museScore version=\"4.0\"><Score><metaTag name=\"workTitle\">"
                + xmlEscape(workTitle) + "</metaTag><Division>" + safeDivisions
                + "</Division><Part><trackName>P1</trackName><Staff id=\"1\"/></Part><Staff id=\"1\"><Measure><voice>"
                + makeMuseRestXml(capacity, capacity, safeDivisions, null)
                + "</voice></Measure></Staff></Score></museScore>";
    }

    public static String buildMuseScoreExportMetadataXml(MuseScoreExportMetadata metadata, int divisions) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><museScore version=\"4.0\"><Score>");
        xml.append("<metaTag name=\"workTitle\">").append(xmlEscape(metadata.getTitle())).append("</metaTag>");
        if (metadata.getSubtitle() != null && metadata.getSubtitle().length() > 0) {
            xml.append("<metaTag name=\"subtitle\">").append(xmlEscape(metadata.getSubtitle())).append("</metaTag>");
        }
        if (metadata.getComposer() != null && metadata.getComposer().length() > 0) {
            xml.append("<metaTag name=\"composer\">").append(xmlEscape(metadata.getComposer())).append("</metaTag>");
        }
        if (metadata.getArranger() != null && metadata.getArranger().length() > 0) {
            xml.append("<metaTag name=\"arranger\">").append(xmlEscape(metadata.getArranger())).append("</metaTag>");
        }
        if (metadata.getLyricist() != null && metadata.getLyricist().length() > 0) {
            xml.append("<metaTag name=\"lyricist\">").append(xmlEscape(metadata.getLyricist())).append("</metaTag>");
        }
        if (metadata.getTranslator() != null && metadata.getTranslator().length() > 0) {
            xml.append("<metaTag name=\"translator\">").append(xmlEscape(metadata.getTranslator()))
                    .append("</metaTag>");
        }
        if (metadata.getRights() != null && metadata.getRights().length() > 0) {
            xml.append("<metaTag name=\"copyright\">").append(xmlEscape(metadata.getRights())).append("</metaTag>");
        }
        if (metadata.getWorkNumber() != null && metadata.getWorkNumber().length() > 0) {
            xml.append("<metaTag name=\"workNumber\">").append(xmlEscape(metadata.getWorkNumber()))
                    .append("</metaTag>");
        }
        if (metadata.getMovementTitle() != null && metadata.getMovementTitle().length() > 0) {
            xml.append("<metaTag name=\"movementTitle\">").append(xmlEscape(metadata.getMovementTitle()))
                    .append("</metaTag>");
        }
        if (metadata.getMovementNumber() != null && metadata.getMovementNumber().length() > 0) {
            xml.append("<metaTag name=\"movementNumber\">").append(xmlEscape(metadata.getMovementNumber()))
                    .append("</metaTag>");
        }
        if (metadata.getCreationDate() != null && metadata.getCreationDate().length() > 0) {
            xml.append("<metaTag name=\"creationDate\">").append(xmlEscape(metadata.getCreationDate()))
                    .append("</metaTag>");
        }
        xml.append("<Division>").append(divisions).append("</Division>");
        return xml.toString();
    }

    public static boolean isMuseElementVisible(Integer visible) {
        if (visible == null) {
            return true;
        }
        return visible.intValue() != 0;
    }

    public static NotationTag museArticulationSubtypeToMusicXmlTag(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() == 0) {
            return null;
        }
        if ((normalized.contains("left") || normalized.contains("lh")) && normalized.contains("pizz")) {
            return new NotationTag("technical", "stopped");
        }
        if (normalized.contains("stopped")) {
            return new NotationTag("technical", "stopped");
        }
        if (normalized.contains("snap") && normalized.contains("pizz")) {
            return new NotationTag("technical", "snap-pizzicato");
        }
        if (normalized.contains("upbow") || (normalized.contains("up") && normalized.contains("bow"))) {
            return new NotationTag("technical", "up-bow");
        }
        if (normalized.contains("downbow") || (normalized.contains("down") && normalized.contains("bow"))) {
            return new NotationTag("technical", "down-bow");
        }
        if (normalized.contains("open") && normalized.contains("string")) {
            return new NotationTag("technical", "open-string");
        }
        if (normalized.contains("harmonic")) {
            return new NotationTag("technical", "harmonic");
        }
        if (normalized.contains("staccatissimo")) {
            return new NotationTag("articulations", "staccatissimo");
        }
        if (normalized.contains("staccato")) {
            return new NotationTag("articulations", "staccato");
        }
        if (normalized.contains("tenuto")) {
            return new NotationTag("articulations", "tenuto");
        }
        if (normalized.contains("accent")) {
            return new NotationTag("articulations", "accent");
        }
        if (normalized.contains("marcato")) {
            return new NotationTag("articulations", "strong-accent");
        }
        return null;
    }

    public static NotationTag museOrnamentSubtypeToMusicXmlTag(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() == 0) {
            return null;
        }
        if ("brassmuteclosed".equals(normalized)
                || (normalized.contains("brass") && normalized.contains("mute") && normalized.contains("closed"))) {
            return new NotationTag("technical", "stopped");
        }
        return null;
    }

    public static String normalizeKeyMode(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() == 0) {
            return null;
        }
        if ("major".equals(normalized) || "maj".equals(normalized) || "0".equals(normalized)) {
            return "major";
        }
        if ("minor".equals(normalized) || "min".equals(normalized) || "1".equals(normalized)) {
            return "minor";
        }
        return null;
    }

    public static String inferKeyModeFromText(String raw) {
        String value = raw == null ? "" : raw;
        if (value.length() == 0) {
            return null;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.matches(".*\\bminor\\b.*") || value.contains("短調")) {
            return "minor";
        }
        if (lower.matches(".*\\bmajor\\b.*") || value.contains("長調")) {
            return "major";
        }
        return null;
    }

    public static String buildDynamicDirectionXml(String mark, Double soundDynamics) {
        String soundXml = soundDynamics != null && Double.isFinite(soundDynamics.doubleValue())
                ? "<sound dynamics=\"" + String.format(Locale.ROOT, "%.2f", soundDynamics.doubleValue()) + "\"/>"
                : "";
        return "<direction><direction-type><dynamics><" + mark + "/></dynamics></direction-type>" + soundXml
                + "</direction>";
    }

    public static String buildWordsDirectionXml(String text, String placement, Integer soundTempo, String fontStyle) {
        String placementAttr = placement == null || placement.length() == 0 ? "" : " placement=\"" + placement + "\"";
        String fontStyleAttr = fontStyle == null || fontStyle.length() == 0 ? "" : " font-style=\"" + fontStyle + "\"";
        String soundXml = soundTempo == null ? "" : "<sound tempo=\"" + soundTempo.intValue() + "\"/>";
        return "<direction" + placementAttr + "><direction-type><words" + fontStyleAttr + ">" + xmlEscape(text)
                + "</words></direction-type>" + soundXml + "</direction>";
    }

    public static String buildSegnoDirectionXml() {
        return "<direction><direction-type><segno/></direction-type></direction>";
    }

    public static String buildCodaDirectionXml() {
        return "<direction><direction-type><coda/></direction-type></direction>";
    }

    public static int normalizeKeyFifthsToMuseRange(int fifths) {
        int normalized = Math.round(fifths);
        while (normalized > 7) {
            normalized -= 12;
        }
        while (normalized < -7) {
            normalized += 12;
        }
        return normalized;
    }

    public static Transpose readPartTransposeFromValues(String diatonicRaw, String chromaticRaw) {
        Integer diatonic = parseRoundedIntegerOrNull(diatonicRaw);
        Integer chromatic = parseRoundedIntegerOrNull(chromaticRaw);
        if (diatonic == null && chromatic == null) {
            return null;
        }
        return new Transpose(diatonic, chromatic);
    }

    public static Integer readMuseKeyFifthsFromValues(String transposeKeyRaw, String accidentalRaw,
            String concertKeyRaw, boolean transposingPart) {
        Integer transposeKey = parseRoundedIntegerOrNull(transposeKeyRaw);
        Integer accidental = parseRoundedIntegerOrNull(accidentalRaw);
        Integer concertKey = parseRoundedIntegerOrNull(concertKeyRaw);
        Integer resolved = transposingPart ? firstNonNull(transposeKey, accidental, concertKey)
                : firstNonNull(accidental, concertKey, transposeKey);
        if (resolved == null) {
            return null;
        }
        return Integer.valueOf(Math.max(-7, Math.min(7, resolved.intValue())));
    }

    private static Integer firstNonNull(Integer first, Integer second, Integer third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private static Integer parseRoundedIntegerOrNull(String raw) {
        try {
            double parsed = Double.parseDouble(raw == null ? "" : raw.trim());
            if (!Double.isFinite(parsed)) {
                return null;
            }
            return Integer.valueOf((int) Math.round(parsed));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String buildTransposeXml(Transpose transpose) {
        if (transpose == null) {
            return "";
        }
        Integer diatonic = transpose.getDiatonic();
        Integer chromatic = transpose.getChromatic();
        if (diatonic == null && chromatic == null) {
            return "";
        }
        return "<transpose>" + (diatonic != null ? "<diatonic>" + diatonic.intValue() + "</diatonic>" : "")
                + (chromatic != null ? "<chromatic>" + chromatic.intValue() + "</chromatic>" : "") + "</transpose>";
    }

    public static String resolveMuseExportKeySigXml(int writtenFifths, Transpose transpose) {
        int normalizedWritten = normalizeKeyFifthsToMuseRange(writtenFifths);
        Integer chromatic = transpose == null ? null : transpose.getChromatic();
        if (chromatic == null) {
            return "<KeySig><accidental>" + normalizedWritten + "</accidental><concertKey>" + normalizedWritten
                    + "</concertKey></KeySig>";
        }
        int concertKey = normalizeKeyFifthsToMuseRange(normalizedWritten + (7 * Math.round(chromatic.intValue())));
        return "<KeySig><accidental>" + normalizedWritten + "</accidental><concertKey>" + concertKey
                + "</concertKey><transposeKey>" + normalizedWritten + "</transposeKey></KeySig>";
    }

    public static Clef parseMuseClefText(String raw) {
        String text = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (text.length() == 0) {
            return null;
        }
        if (text.contains("PERC")) {
            return new Clef("G", 2);
        }
        if (text.contains("TENOR")) {
            return new Clef("C", 4);
        }
        if (text.contains("ALTO")) {
            return new Clef("C", 3);
        }
        if (text.contains("BASS")) {
            return new Clef("F", 4);
        }
        if (text.contains("TREBLE")) {
            return new Clef("G", 2);
        }
        java.util.regex.Matcher explicit = java.util.regex.Pattern.compile("\\b([CFG])\\s*([1-5])\\b").matcher(text);
        if (explicit.find()) {
            return new Clef(explicit.group(1), Integer.parseInt(explicit.group(2)));
        }
        if (text.contains("F")) {
            return new Clef("F", 4);
        }
        if (text.contains("C")) {
            return new Clef("C", 3);
        }
        if (text.contains("G")) {
            return new Clef("G", 2);
        }
        return null;
    }

    public static String formatMeasureLenFromDivisions(int measureLenDiv, int divisions) {
        int numRaw = Math.max(1, Math.round(measureLenDiv));
        int denRaw = Math.max(1, Math.round(divisions)) * 4;
        int gcd = gcdPositive(numRaw, denRaw);
        return Math.max(1, Math.round(numRaw / gcd)) + "/" + Math.max(1, Math.round(denRaw / gcd));
    }

    public static String withDirectionStaff(String directionXml, int staffNo) {
        String xml = directionXml == null ? "" : directionXml;
        if (xml.matches("(?s).*<staff>\\d+</staff>.*")) {
            return xml;
        }
        if (!xml.contains("</direction>")) {
            return xml;
        }
        return xml.replaceFirst("</direction>\\s*$", "<staff>" + staffNo + "</staff></direction>");
    }

    public static String withDirectionPlacement(String directionXml, int staffNo, int voiceNo) {
        String out = withDirectionStaff(directionXml, staffNo);
        if (out.contains("<octave-shift")) {
            return out;
        }
        if (!out.matches("(?s).*<voice>\\d+</voice>.*") && out.contains("</direction>")) {
            out = out.replaceFirst("</direction>\\s*$", "<voice>" + voiceNo + "</voice></direction>");
        }
        return out;
    }

    public static TupletMusicXml buildTupletMusicXml(TimeModification timeModification,
            Collection<TupletStart> starts, Collection<Integer> stops) {
        String timeModificationXml = "";
        if (timeModification != null) {
            timeModificationXml = "<time-modification><actual-notes>" + timeModification.getActualNotes()
                    + "</actual-notes><normal-notes>" + timeModification.getNormalNotes()
                    + "</normal-notes></time-modification>";
        }

        List<String> notationItems = new ArrayList<String>();
        if (starts != null) {
            for (TupletStart start : starts) {
                if (start == null) {
                    continue;
                }
                String attrs = "type=\"start\" number=\"" + Math.max(1, Math.round(start.getNumber())) + "\"";
                if (start.getBracket() != null) {
                    attrs += " bracket=\"" + start.getBracket() + "\"";
                }
                if (start.getShowNumber() != null) {
                    attrs += " show-number=\"" + start.getShowNumber() + "\"";
                }
                notationItems.add("<tuplet " + attrs + "/>");
            }
        }
        if (stops != null) {
            for (Integer stop : stops) {
                if (stop == null) {
                    continue;
                }
                notationItems.add("<tuplet type=\"stop\" number=\"" + Math.max(1, Math.round(stop.intValue()))
                        + "\"/>");
            }
        }
        return new TupletMusicXml(timeModificationXml, notationItems);
    }

    public static int tupletRoundingToleranceByTimedEvents(Collection<TimedEvent> timedEvents) {
        int tupletCount = 0;
        if (timedEvents != null) {
            for (TimedEvent event : timedEvents) {
                if (event == null || event.getDurationDiv() <= 0) {
                    continue;
                }
                if (event.isGrace()) {
                    continue;
                }
                if (!event.hasTupletTimeModification()) {
                    continue;
                }
                tupletCount++;
            }
        }
        if (tupletCount <= 0) {
            return 0;
        }
        return (int) Math.floor(tupletCount / 2.0d);
    }

    public static int beamLevelFromType(String typeText) {
        String normalized = typeText == null ? "" : typeText.trim().toLowerCase(Locale.ROOT);
        if ("eighth".equals(normalized)) {
            return 1;
        }
        if ("16th".equals(normalized)) {
            return 2;
        }
        if ("32nd".equals(normalized)) {
            return 3;
        }
        if ("64th".equals(normalized)) {
            return 4;
        }
        return 0;
    }

    public static Map<Integer, String> buildMuseBeamXmlByEventInfo(Collection<MuseBeamEvent> events, int beatDiv,
            boolean allowImplicitInference) {
        List<MuseBeamEvent> infos = events == null ? Collections.<MuseBeamEvent>emptyList()
                : new ArrayList<MuseBeamEvent>(events);
        Map<Integer, BeamAssignment> assignmentByIndex = new LinkedHashMap<Integer, BeamAssignment>();
        boolean hasExplicitMuseBeamInfo = false;
        for (MuseBeamEvent info : infos) {
            if (info != null && info.isTimed()
                    && ("begin".equals(info.getExplicitMode()) || "mid".equals(info.getExplicitMode()))) {
                hasExplicitMuseBeamInfo = true;
                break;
            }
        }
        if (!hasExplicitMuseBeamInfo && !allowImplicitInference) {
            return Collections.emptyMap();
        }

        if (!hasExplicitMuseBeamInfo) {
            List<Integer> currentGroup = new ArrayList<Integer>();
            int cursorDiv = 0;
            int resolvedBeatDiv = Math.max(1, Math.round(beatDiv));
            for (int index = 0; index < infos.size(); index++) {
                MuseBeamEvent info = infos.get(index);
                if (info != null && info.isTimed() && cursorDiv > 0 && cursorDiv % resolvedBeatDiv == 0) {
                    flushMuseBeamGroup(currentGroup, infos, assignmentByIndex);
                    currentGroup = new ArrayList<Integer>();
                }
                if (info == null || !info.isChord() || !isBeamableMuseTimedEvent(info)) {
                    flushMuseBeamGroup(currentGroup, infos, assignmentByIndex);
                    currentGroup = new ArrayList<Integer>();
                    if (info != null && info.isTimed()) {
                        cursorDiv += Math.max(0, info.getDurationDiv());
                    }
                    continue;
                }
                currentGroup.add(Integer.valueOf(index));
                cursorDiv += Math.max(0, info.getDurationDiv());
            }
            flushMuseBeamGroup(currentGroup, infos, assignmentByIndex);
            return museBeamXmlByIndex(assignmentByIndex);
        }

        List<Integer> activeGroup = new ArrayList<Integer>();
        int cursorDiv = 0;
        int resolvedBeatDiv = Math.max(1, Math.round(beatDiv));
        for (int index = 0; index < infos.size(); index++) {
            MuseBeamEvent info = infos.get(index);
            if (info == null || !info.isTimed()) {
                flushMuseBeamGroup(activeGroup, infos, assignmentByIndex);
                activeGroup = new ArrayList<Integer>();
                continue;
            }
            if (cursorDiv > 0 && cursorDiv % resolvedBeatDiv == 0) {
                flushMuseBeamGroup(activeGroup, infos, assignmentByIndex);
                activeGroup = new ArrayList<Integer>();
            }
            if (!isBeamableMuseTimedEvent(info)) {
                flushMuseBeamGroup(activeGroup, infos, assignmentByIndex);
                activeGroup = new ArrayList<Integer>();
                continue;
            }
            if ("begin".equals(info.getExplicitMode())) {
                flushMuseBeamGroup(activeGroup, infos, assignmentByIndex);
                activeGroup = new ArrayList<Integer>();
                activeGroup.add(Integer.valueOf(index));
                cursorDiv += Math.max(0, info.getDurationDiv());
                continue;
            }
            if ("mid".equals(info.getExplicitMode())) {
                if (activeGroup.isEmpty()) {
                    MuseBeamEvent previous = index > 0 ? infos.get(index - 1) : null;
                    if (isBeamableMuseTimedEvent(previous)) {
                        activeGroup.add(Integer.valueOf(index - 1));
                    }
                }
                activeGroup.add(Integer.valueOf(index));
                cursorDiv += Math.max(0, info.getDurationDiv());
                continue;
            }
            activeGroup.add(Integer.valueOf(index));
            cursorDiv += Math.max(0, info.getDurationDiv());
        }
        flushMuseBeamGroup(activeGroup, infos, assignmentByIndex);
        return museBeamXmlByIndex(assignmentByIndex);
    }

    public static MuseBeamEvent buildMuseImportedBeamEventInfo(boolean timed, boolean chord, boolean grace,
            int durationDiv, Integer displayDurationDiv, int divisions, String explicitMode) {
        int displayDuration = displayDurationDiv == null ? durationDiv : displayDurationDiv.intValue();
        TypeAndDots info = timed ? divisionToTypeAndDots(divisions, displayDuration) : null;
        int levels = info == null ? 0 : beamLevelFromType(info.getType());
        return new MuseBeamEvent(timed, chord, grace, Math.max(0, durationDiv), levels, explicitMode);
    }

    public static String readMuseImportedBeamXmlByEventIndex(Map<Integer, String> beamXmlByEventIndex, int eventIndex) {
        if (beamXmlByEventIndex == null) {
            return "";
        }
        String xml = beamXmlByEventIndex.get(Integer.valueOf(Math.max(0, eventIndex)));
        return xml == null ? "" : xml;
    }

    private static boolean isBeamableMuseTimedEvent(MuseBeamEvent info) {
        return info != null && info.isTimed() && !info.isGrace() && info.getLevels() > 0;
    }

    private static void flushMuseBeamGroup(List<Integer> indices, List<MuseBeamEvent> infos,
            Map<Integer, BeamAssignment> assignmentByIndex) {
        List<Integer> chordIndices = new ArrayList<Integer>();
        if (indices != null) {
            for (Integer index : indices) {
                if (index == null || index.intValue() < 0 || index.intValue() >= infos.size()) {
                    continue;
                }
                MuseBeamEvent info = infos.get(index.intValue());
                if (info != null && info.isChord() && !info.isGrace()) {
                    chordIndices.add(index);
                }
            }
        }
        if (chordIndices.size() < 2) {
            return;
        }
        for (int groupIndex = 0; groupIndex < chordIndices.size(); groupIndex++) {
            Integer eventIndex = chordIndices.get(groupIndex);
            MuseBeamEvent info = infos.get(eventIndex.intValue());
            String state = groupIndex == 0 ? "begin" : (groupIndex == chordIndices.size() - 1 ? "end" : "continue");
            assignmentByIndex.put(eventIndex, new BeamAssignment(state, info.getLevels()));
        }
    }

    private static Map<Integer, String> museBeamXmlByIndex(Map<Integer, BeamAssignment> assignmentByIndex) {
        Map<Integer, String> out = new LinkedHashMap<Integer, String>();
        for (Map.Entry<Integer, BeamAssignment> entry : assignmentByIndex.entrySet()) {
            BeamAssignment assignment = entry.getValue();
            StringBuilder xml = new StringBuilder();
            for (int level = 1; level <= assignment.levels; level++) {
                xml.append("<beam number=\"").append(level).append("\">").append(assignment.state)
                        .append("</beam>");
            }
            if (xml.length() > 0) {
                out.put(entry.getKey(), xml.toString());
            }
        }
        return out;
    }

    public static OttavaShift parseOttavaSubtype(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        int size = normalized.contains("15") ? 15 : 8;
        String shiftType = normalized.contains("8vb") || normalized.contains("15mb") || normalized.contains("bassa")
                ? "down"
                : "up";
        return new OttavaShift(size, shiftType);
    }

    public static String parseMusicXmlOctaveShiftSubtype(String typeRaw, String sizeRaw) {
        String type = typeRaw == null ? "" : typeRaw.trim().toLowerCase(Locale.ROOT);
        if (type.length() == 0 || "stop".equals(type) || "continue".equals(type)) {
            return null;
        }
        int parsedSize = 0;
        try {
            parsedSize = Integer.parseInt(sizeRaw == null ? "" : sizeRaw.trim());
        } catch (NumberFormatException ex) {
            parsedSize = 0;
        }
        int resolvedSize = parsedSize > 8 ? 15 : 8;
        if ("down".equals(type)) {
            return resolvedSize == 15 ? "15mb" : "8vb";
        }
        return resolvedSize == 15 ? "15ma" : "8va";
    }

    public static String normalizeMusicXmlClefSign(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (value.length() == 0) {
            return null;
        }
        if (value.contains("C")) {
            return "C";
        }
        if (value.contains("F")) {
            return "F";
        }
        if (value.contains("G")) {
            return "G";
        }
        return null;
    }

    public static String readMusicXmlMeasureMuseConcertClefType(String signRaw, String lineRaw) {
        String sign = signRaw == null ? "" : signRaw.trim().toUpperCase(Locale.ROOT);
        int parsedLine = 0;
        try {
            parsedLine = Integer.parseInt(lineRaw == null ? "" : lineRaw.trim());
        } catch (NumberFormatException ex) {
            parsedLine = 0;
        }
        if ("C".equals(sign)) {
            int line = parsedLine > 0 ? Math.max(1, Math.min(5, Math.round(parsedLine))) : 3;
            return line == 3 ? "C3" : "C" + line;
        }
        if ("F".equals(sign)) {
            return "F";
        }
        if ("G".equals(sign)) {
            return "G";
        }
        return null;
    }

    public static String readMusicXmlMeasureClefSignFromValues(Collection<MusicXmlClefSource> clefs, int staffNo) {
        int targetStaffNo = Math.max(1, Math.round(staffNo));
        if (clefs != null) {
            for (MusicXmlClefSource clef : clefs) {
                if (clef == null || !clef.hasNumber() || clef.getNumber() == null
                        || clef.getNumber().intValue() != targetStaffNo) {
                    continue;
                }
                String sign = normalizeMusicXmlClefSign(clef.getSign());
                if (sign != null) {
                    return sign;
                }
            }
            if (targetStaffNo == 1) {
                for (MusicXmlClefSource clef : clefs) {
                    if (clef == null || clef.hasNumber()) {
                        continue;
                    }
                    String sign = normalizeMusicXmlClefSign(clef.getSign());
                    if (sign != null) {
                        return sign;
                    }
                }
            }
        }
        return null;
    }

    public static String readFirstExplicitClefInPartValues(Collection<MusicXmlMeasureClefSet> measureClefs,
            int staffNo) {
        if (measureClefs != null) {
            for (MusicXmlMeasureClefSet measureClef : measureClefs) {
                if (measureClef == null) {
                    continue;
                }
                String clef = readMusicXmlMeasureClefSignFromValues(measureClef.getClefs(), staffNo);
                if (clef != null) {
                    return clef;
                }
            }
        }
        return null;
    }

    public static String inferClefSignFromPitches(Collection<Integer> midiList) {
        if (midiList == null || midiList.isEmpty()) {
            return "G";
        }
        List<Integer> sorted = new ArrayList<Integer>();
        for (Integer midi : midiList) {
            if (midi != null) {
                sorted.add(midi);
            }
        }
        if (sorted.isEmpty()) {
            return "G";
        }
        Collections.sort(sorted);
        int mid = (int) Math.floor(sorted.size() / 2.0d);
        double median = sorted.size() % 2 == 1 ? sorted.get(mid).intValue()
                : (sorted.get(mid - 1).intValue() + sorted.get(mid).intValue()) / 2.0d;
        return median < 60 ? "F" : "G";
    }

    public static String inferClefSignFromPartName(String partName) {
        String name = partName == null ? "" : partName.trim().toLowerCase(Locale.ROOT);
        if (name.length() == 0) {
            return null;
        }
        if (name.matches(".*\\b(viola|vla)\\b.*")) {
            return "C";
        }
        if (name.matches(".*\\b(violoncello|cello|vc)\\b.*")) {
            return "F";
        }
        if (name.matches(".*\\b(contrabass|double\\s*bass|cb|bass)\\b.*")) {
            return "F";
        }
        return null;
    }

    public static String clefSignToMuseDefaultClef(String sign) {
        if ("F".equals(sign)) {
            return "F";
        }
        if ("C".equals(sign)) {
            return "C3";
        }
        return "G";
    }

    public static String clefSignToMuseConcertClefType(String sign) {
        if ("F".equals(sign)) {
            return "F";
        }
        if ("C".equals(sign)) {
            return "C3";
        }
        return "G";
    }

    public static String buildOctaveShiftDirectionXml(String type, OttavaState state) {
        String placement = "down".equals(state.getShiftType()) ? "below" : "above";
        return "<direction placement=\"" + placement + "\"><direction-type><octave-shift type=\"" + type
                + "\" size=\"" + state.getSize() + "\" number=\"" + state.getNumber()
                + "\"/></direction-type></direction>";
    }

    public static List<String> applyMuseOttavaSpannerTransition(String subtype, boolean hasStart, boolean hasStop,
            List<OttavaState> activeOttavaStates, MutableInt nextOttavaNumber) {
        List<OttavaState> active = activeOttavaStates == null ? new ArrayList<OttavaState>() : activeOttavaStates;
        MutableInt next = nextOttavaNumber == null ? new MutableInt(1) : nextOttavaNumber;
        List<String> directions = new ArrayList<String>();
        if (hasStop) {
            OttavaState state = active.isEmpty() ? new OttavaState(1, 8, "up") : active.remove(active.size() - 1);
            directions.add(buildOctaveShiftDirectionXml("stop", state));
        }
        if (hasStart) {
            OttavaShift parsed = parseOttavaSubtype(subtype);
            OttavaState state = new OttavaState(next.getCurrent(), parsed.getSize(), parsed.getShiftType());
            next.setCurrent(next.getCurrent() + 1);
            active.add(state);
            directions.add(buildOctaveShiftDirectionXml("start", state));
        }
        return directions;
    }

    public static int semitoneShiftForOttavaDisplay(OttavaState state) {
        int amount = state.getSize() == 15 ? 24 : 12;
        return "up".equals(state.getShiftType()) ? amount : -amount;
    }

    public static int semitoneShiftForActiveOttavaDisplay(Collection<OttavaState> activeOttavaStates) {
        int sum = 0;
        if (activeOttavaStates != null) {
            for (OttavaState state : activeOttavaStates) {
                if (state != null) {
                    sum += semitoneShiftForOttavaDisplay(state);
                }
            }
        }
        return sum;
    }

    public static int applyActiveOttavaDisplayShiftToMidi(int midi, Collection<OttavaState> activeOttavaStates) {
        return Math.max(0, Math.min(127, Math.round(midi + semitoneShiftForActiveOttavaDisplay(activeOttavaStates))));
    }

    public static ResolvedMuseScoreImportOptions resolveMuseScoreImportOptions(Boolean sourceMetadata,
            Boolean debugMetadata, Boolean normalizeCutTimeToTwoTwo, Boolean applyImplicitBeams) {
        return new ResolvedMuseScoreImportOptions(sourceMetadata == null || sourceMetadata.booleanValue(),
                debugMetadata == null || debugMetadata.booleanValue(),
                normalizeCutTimeToTwoTwo != null && normalizeCutTimeToTwoTwo.booleanValue(),
                applyImplicitBeams == null || applyImplicitBeams.booleanValue());
    }

    public static int readMuseTickRelativeDiv(String eventText, int measureStartDiv) {
        double parsed = 0.0d;
        String raw = eventText == null ? "" : eventText.trim();
        if (raw.length() > 0) {
            try {
                parsed = Double.parseDouble(raw);
            } catch (NumberFormatException ex) {
                parsed = 0.0d;
            }
        }
        int tickAbs = Math.max(0, (int) Math.round(parsed));
        return Math.max(0, tickAbs - measureStartDiv);
    }

    public static TupletDescriptor readMuseTupletDescriptor(String id, Integer normalNotes, Integer actualNotes,
            Integer numberType, Integer bracketType) {
        int normal = normalNotes == null ? 0 : Math.round(normalNotes.intValue());
        int actual = actualNotes == null ? 0 : Math.round(actualNotes.intValue());
        if (!(normal > 0 && actual > 0)) {
            return null;
        }
        String showNumber = null;
        if (numberType != null) {
            showNumber = numberType.intValue() == 2 ? "none" : "actual";
        }
        String bracket = bracketType != null ? (bracketType.intValue() == 2 ? "no" : "yes") : "yes";
        return new TupletDescriptor(id == null ? "" : id.trim(), actual, normal, showNumber, bracket);
    }

    public static int applyMuseInlineTupletStart(TupletDescriptor descriptor, List<Double> tupletScaleStack,
            List<TupletState> tupletStateStack, int nextTupletNumber) {
        tupletScaleStack.add(Double.valueOf(descriptor.getNormalNotes() / (double) descriptor.getActualNotes()));
        tupletStateStack.add(new TupletState(descriptor.getActualNotes(), descriptor.getNormalNotes(), nextTupletNumber,
                descriptor.getShowNumber(), descriptor.getBracket(), true));
        return nextTupletNumber + 1;
    }

    public static Integer applyMuseEndTuplet(List<Double> tupletScaleStack, List<TupletState> tupletStateStack) {
        if (!tupletScaleStack.isEmpty()) {
            tupletScaleStack.remove(tupletScaleStack.size() - 1);
        }
        if (tupletStateStack.isEmpty()) {
            return null;
        }
        TupletState ended = tupletStateStack.remove(tupletStateStack.size() - 1);
        return Integer.valueOf(ended.getNumber());
    }

    public static FinalizedTupletRef finalizeActiveMuseTupletRef(String activeTupletRefId,
            Map<String, Integer> tupletNumberById) {
        if (activeTupletRefId == null || activeTupletRefId.length() == 0) {
            return new FinalizedTupletRef(null, null);
        }
        Integer endedNo = tupletNumberById == null ? null : tupletNumberById.get(activeTupletRefId);
        return new FinalizedTupletRef(null, endedNo);
    }

    public static double currentTupletScale(Collection<Double> tupletScaleStack) {
        double scale = 1.0d;
        if (tupletScaleStack != null) {
            for (Double value : tupletScaleStack) {
                if (value != null) {
                    scale *= value.doubleValue();
                }
            }
        }
        return scale;
    }

    public static List<TupletStart> consumeTupletStarts(List<TupletState> tupletStateStack) {
        List<TupletStart> starts = new ArrayList<TupletStart>();
        if (tupletStateStack != null) {
            for (TupletState state : tupletStateStack) {
                if (state.isStartPending()) {
                    starts.add(new TupletStart(state.getNumber(), state.getShowNumber(), state.getBracket()));
                }
            }
            for (TupletState state : tupletStateStack) {
                state.markStartConsumed();
            }
        }
        return starts;
    }

    public static Integer appendTupletStopToLastTimedEvent(List<TimedEvent> events, int tupletNumber) {
        if (events == null) {
            return null;
        }
        for (int index = events.size() - 1; index >= 0; index--) {
            TimedEvent event = events.get(index);
            if (event == null) {
                continue;
            }
            event.getTupletStops().add(Integer.valueOf(tupletNumber));
            return Integer.valueOf(index);
        }
        return null;
    }

    public static int resolveTupletNumberById(String id, Map<String, Integer> tupletNumberById,
            MutableInt nextTupletNumberRef) {
        Integer existing = tupletNumberById.get(id);
        if (existing != null) {
            return existing.intValue();
        }
        int assigned = nextTupletNumberRef.getCurrent();
        nextTupletNumberRef.setCurrent(assigned + 1);
        tupletNumberById.put(id, Integer.valueOf(assigned));
        return assigned;
    }

    public static List<Integer> consumePendingTrillNumbers(List<Integer> pendingNumbers) {
        List<Integer> out = new ArrayList<Integer>();
        if (pendingNumbers != null) {
            out.addAll(pendingNumbers);
            pendingNumbers.clear();
        }
        return out;
    }

    public static MuseScoreImportEventRouting readMuseImportEventRouting(String tagName, Integer trackNo,
            int defaultVoiceNo, int localStaffIndex, Integer moveRaw) {
        String tag = tagName == null ? "" : tagName.trim().toLowerCase(Locale.ROOT);
        int voiceNo = trackNo != null ? Math.max(1, Math.min(4, (Math.max(0, trackNo.intValue()) % 4) + 1))
                : defaultVoiceNo;
        int movedStaffNo = moveRaw != null ? Math.max(1, localStaffIndex + 1 + moveRaw.intValue())
                : localStaffIndex + 1;
        return new MuseScoreImportEventRouting(tag, voiceNo, movedStaffNo);
    }

    public static TieFlags parseMuseTieFlags(boolean hasTie, boolean tieHasPrev, boolean tieHasNext,
            boolean hasEndSpanner) {
        boolean tieStart = hasTie && (tieHasNext || !tieHasPrev);
        boolean tieStop = hasEndSpanner || (hasTie && tieHasPrev);
        return new TieFlags(tieStart, tieStop);
    }

    public static String buildMuseImportedNoteTieXml(TieFlags tieFlags) {
        TieFlags safe = tieFlags == null ? new TieFlags(false, false) : tieFlags;
        StringBuilder xml = new StringBuilder();
        if (safe.isTieStart()) {
            xml.append("<tie type=\"start\"/>");
        }
        if (safe.isTieStop()) {
            xml.append("<tie type=\"stop\"/>");
        }
        return xml.toString();
    }

    public static String buildMuseImportedNoteNotationsXml(Collection<String> tupletNotationItems,
            Collection<Integer> slurStarts, Collection<Integer> slurStops, Collection<String> trillNotationItems,
            Collection<String> articulationTags, Collection<String> technicalTags, TieFlags tiedFlags,
            String fingeringText, Integer stringNumber) {
        StringBuilder items = new StringBuilder();
        appendStringItems(items, tupletNotationItems);
        if (slurStarts != null) {
            for (Integer number : slurStarts) {
                if (number != null) {
                    items.append("<slur type=\"start\" number=\"")
                            .append(Math.max(1, Math.round(number.intValue()))).append("\"/>");
                }
            }
        }
        if (slurStops != null) {
            for (Integer number : slurStops) {
                if (number != null) {
                    items.append("<slur type=\"stop\" number=\"")
                            .append(Math.max(1, Math.round(number.intValue()))).append("\"/>");
                }
            }
        }
        appendStringItems(items, trillNotationItems);
        String articulationXml = buildMuseImportedArticulationsXml(articulationTags);
        if (articulationXml.length() > 0) {
            items.append(articulationXml);
        }
        String technicalXml = buildMuseImportedTechnicalXml(technicalTags, fingeringText, stringNumber);
        if (technicalXml.length() > 0) {
            items.append(technicalXml);
        }
        TieFlags safeTied = tiedFlags == null ? new TieFlags(false, false) : tiedFlags;
        if (safeTied.isTieStart()) {
            items.append("<tied type=\"start\"/>");
        }
        if (safeTied.isTieStop()) {
            items.append("<tied type=\"stop\"/>");
        }
        return items.length() == 0 ? "" : "<notations>" + items.toString() + "</notations>";
    }

    private static void appendStringItems(StringBuilder out, Collection<String> items) {
        if (items == null) {
            return;
        }
        for (String item : items) {
            if (item != null && item.length() > 0) {
                out.append(item);
            }
        }
    }

    private static String buildMuseImportedArticulationsXml(Collection<String> articulationTags) {
        StringBuilder items = new StringBuilder();
        if (articulationTags != null) {
            for (String tag : articulationTags) {
                if (tag != null && tag.trim().length() > 0) {
                    items.append("<").append(tag.trim()).append("/>");
                }
            }
        }
        return items.length() == 0 ? "" : "<articulations>" + items.toString() + "</articulations>";
    }

    private static String buildMuseImportedTechnicalXml(Collection<String> technicalTags, String fingeringText,
            Integer stringNumber) {
        StringBuilder items = new StringBuilder();
        if (technicalTags != null) {
            for (String tag : technicalTags) {
                if (tag != null && tag.trim().length() > 0) {
                    items.append("<").append(tag.trim()).append("/>");
                }
            }
        }
        if (fingeringText != null && fingeringText.trim().length() > 0) {
            items.append("<fingering>").append(xmlEscape(fingeringText.trim())).append("</fingering>");
        }
        if (stringNumber != null && stringNumber.intValue() > 0) {
            items.append("<string>").append(Math.round(stringNumber.intValue())).append("</string>");
        }
        return items.length() == 0 ? "" : "<technical>" + items.toString() + "</technical>";
    }

    public static String parseMuseStringText(String directText, String nestedText) {
        String candidate = nestedText != null ? nestedText : directText;
        if (candidate == null) {
            return null;
        }
        String trimmed = candidate.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    public static TrillSpannerTransition parseTrillSpannerTransition(String type, boolean hasStart, boolean hasStop) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!"trill".equals(normalized)) {
            return new TrillSpannerTransition(false, false);
        }
        return new TrillSpannerTransition(hasStart, hasStop);
    }

    public static void applyMuseTrillSpannerTransition(TrillSpannerTransition transition,
            List<Integer> activeTrillNumbers, MutableInt nextTrillNumber, List<Integer> pendingTrillStarts,
            List<Integer> pendingTrillStops) {
        TrillSpannerTransition safe = transition == null ? new TrillSpannerTransition(false, false) : transition;
        List<Integer> active = activeTrillNumbers == null ? new ArrayList<Integer>() : activeTrillNumbers;
        MutableInt next = nextTrillNumber == null ? new MutableInt(1) : nextTrillNumber;
        if (safe.isStop()) {
            int number = 1;
            if (!active.isEmpty()) {
                number = active.remove(active.size() - 1).intValue();
            }
            if (pendingTrillStops != null) {
                pendingTrillStops.add(Integer.valueOf(number));
            }
        }
        if (safe.isStart()) {
            int number = next.getCurrent();
            next.setCurrent(number + 1);
            active.add(Integer.valueOf(number));
            if (pendingTrillStarts != null) {
                pendingTrillStarts.add(Integer.valueOf(number));
            }
        }
    }

    public static List<String> buildMuseImportedTrillNotationItems(Collection<Integer> trillStarts,
            Collection<Integer> trillStops, boolean trillMarkOnly, String accidentalMark) {
        List<String> items = new ArrayList<String>();
        String accidentalMarkXml = accidentalMark != null && accidentalMark.trim().length() > 0
                ? "<accidental-mark>" + xmlEscape(accidentalMark.trim()) + "</accidental-mark>"
                : "";
        List<Integer> starts = trillStarts == null ? Collections.<Integer>emptyList()
                : new ArrayList<Integer>(trillStarts);
        for (int index = 0; index < starts.size(); index++) {
            Integer number = starts.get(index);
            if (number == null) {
                continue;
            }
            items.add("<ornaments><trill-mark/>" + (index == 0 ? accidentalMarkXml : "")
                    + "<wavy-line type=\"start\" number=\"" + Math.max(1, Math.round(number.intValue()))
                    + "\"/></ornaments>");
        }
        if (trillStops != null) {
            for (Integer number : trillStops) {
                if (number != null) {
                    items.add("<ornaments><wavy-line type=\"stop\" number=\""
                            + Math.max(1, Math.round(number.intValue())) + "\"/></ornaments>");
                }
            }
        }
        if (starts.isEmpty() && trillMarkOnly) {
            items.add("<ornaments><trill-mark/>" + accidentalMarkXml + "</ornaments>");
        }
        return items;
    }

    public static MuseSlurTransition parseMuseSlurSpannerTransition(String type, boolean hasStart, boolean hasStop) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!"slur".equals(normalized)) {
            return new MuseSlurTransition(false, false);
        }
        return new MuseSlurTransition(hasStart, hasStop);
    }

    public static MuseSlurTransitions parseMuseChordSlurTransitions(Collection<MuseSlurElement> slurs,
            Collection<MuseSlurTransition> spanners, MuseImportSlurState state) {
        MuseImportSlurState safeState = state == null ? new MuseImportSlurState() : state;
        List<Integer> starts = new ArrayList<Integer>();
        List<Integer> stops = new ArrayList<Integer>();
        if (slurs != null) {
            for (MuseSlurElement slur : slurs) {
                if (slur == null) {
                    continue;
                }
                String type = slur.getType() == null ? "" : slur.getType().trim().toLowerCase(Locale.ROOT);
                int number = resolveMuseImportSlurNumber(slur.getId(), safeState);
                if ("start".equals(type)) {
                    starts.add(Integer.valueOf(number));
                    if (!safeState.getActiveSlurNumbers().contains(Integer.valueOf(number))) {
                        safeState.getActiveSlurNumbers().add(Integer.valueOf(number));
                    }
                } else if ("stop".equals(type)) {
                    stops.add(Integer.valueOf(number));
                    safeState.getActiveSlurNumbers().remove(Integer.valueOf(number));
                }
            }
        }
        if (spanners != null) {
            for (MuseSlurTransition spanner : spanners) {
                if (spanner == null) {
                    continue;
                }
                if (spanner.isStop()) {
                    int number = 1;
                    List<Integer> active = safeState.getActiveSlurNumbers();
                    if (!active.isEmpty()) {
                        number = active.remove(active.size() - 1).intValue();
                    }
                    stops.add(Integer.valueOf(number));
                }
                if (spanner.isStart()) {
                    int number = safeState.getNextSlurNumber();
                    safeState.setNextSlurNumber(number + 1);
                    safeState.getActiveSlurNumbers().add(Integer.valueOf(number));
                    starts.add(Integer.valueOf(number));
                }
            }
        }
        return new MuseSlurTransitions(starts, stops);
    }

    private static int resolveMuseImportSlurNumber(String rawId, MuseImportSlurState state) {
        String key = rawId == null ? "" : rawId.trim();
        if (key.length() == 0) {
            int number = state.getNextSlurNumber();
            state.setNextSlurNumber(number + 1);
            return number;
        }
        Integer direct = parsePositiveIntegerOrNull(key);
        if (direct != null) {
            return direct.intValue();
        }
        Integer mapped = state.getSlurKeyToNumber().get(key);
        if (mapped != null) {
            return mapped.intValue();
        }
        int number = state.getNextSlurNumber();
        state.setNextSlurNumber(number + 1);
        state.getSlurKeyToNumber().put(key, Integer.valueOf(number));
        return number;
    }

    private static Integer parsePositiveIntegerOrNull(String raw) {
        try {
            int parsed = Integer.parseInt(raw == null ? "" : raw.trim());
            return parsed > 0 ? Integer.valueOf(parsed) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static MuseScoreChordNotationSummary summarizeMuseChordNotations(Collection<String> articulationSubtypes,
            Collection<String> ornamentSubtypes) {
        List<String> articulationTags = new ArrayList<String>();
        List<String> technicalTags = new ArrayList<String>();
        if (articulationSubtypes != null) {
            for (String subtype : articulationSubtypes) {
                NotationTag mapped = museArticulationSubtypeToMusicXmlTag(subtype);
                if (mapped == null) {
                    continue;
                }
                if ("articulations".equals(mapped.getGroup())) {
                    articulationTags.add(mapped.getTag());
                } else if ("technical".equals(mapped.getGroup())) {
                    technicalTags.add(mapped.getTag());
                }
            }
        }
        boolean hasChordLocalTrillMark = false;
        if (ornamentSubtypes != null) {
            for (String subtype : ornamentSubtypes) {
                String normalized = subtype == null ? "" : subtype.trim().toLowerCase(Locale.ROOT);
                if (normalized.contains("trill")) {
                    hasChordLocalTrillMark = true;
                }
                NotationTag mapped = museOrnamentSubtypeToMusicXmlTag(normalized);
                if (mapped == null) {
                    continue;
                }
                if ("articulations".equals(mapped.getGroup())) {
                    articulationTags.add(mapped.getTag());
                } else if ("technical".equals(mapped.getGroup())) {
                    technicalTags.add(mapped.getTag());
                }
            }
        }
        return new MuseScoreChordNotationSummary(articulationTags, technicalTags, hasChordLocalTrillMark);
    }

    public static List<MuseScoreChordNote> parseMuseChordNotes(Collection<MuseScoreChordNoteInput> noteInputs,
            int ottavaDisplayShift) {
        List<MuseScoreChordNote> notes = new ArrayList<MuseScoreChordNote>();
        if (noteInputs == null) {
            return notes;
        }
        for (MuseScoreChordNoteInput input : noteInputs) {
            if (input == null || input.getMidi() == null) {
                continue;
            }
            int midi = Math.max(0, Math.min(127, Math.round(input.getMidi().intValue() + ottavaDisplayShift)));
            String accidentalText = museAccidentalSubtypeToMusicXml(input.getAccidentalSubtype());
            String tpcAccidentalText = museTpcToAccidentalText(input.getTpcRaw());
            String fingeringText = parseMuseStringText(input.getFingeringText(), null);
            String stringRaw = parseMuseStringText(input.getStringText(), null);
            Integer stringNumber = null;
            if (stringRaw != null) {
                try {
                    int parsed = Integer.parseInt(stringRaw);
                    if (parsed > 0) {
                        stringNumber = Integer.valueOf(parsed);
                    }
                } catch (NumberFormatException ex) {
                    stringNumber = null;
                }
            }
            notes.add(new MuseScoreChordNote(midi, accidentalText, tpcAccidentalText, input.getTieFlags().isTieStart(),
                    input.getTieFlags().isTieStop(), fingeringText, stringNumber));
        }
        return notes;
    }

    public static MuseImportedPitchXml buildMuseImportedPitchXml(MuseScoreChordNote note, int keyFifths, int staffNo,
            Map<String, Integer> previousAlterByPitchKey) {
        if (note == null) {
            return new MuseImportedPitchXml("", "");
        }
        String preferredAccidental = note.getAccidentalText() != null ? note.getAccidentalText()
                : note.getTpcAccidentalText();
        AccidentalSpelling.SpelledPitch pitch = AccidentalSpelling.midiToPitch(note.getMidi(),
                Integer.valueOf(keyFifths), preferredAccidental);
        String pitchXml = "<pitch><step>" + pitch.getStep() + "</step>"
                + (pitch.getAlter() != 0 ? "<alter>" + pitch.getAlter() + "</alter>" : "") + "<octave>"
                + pitch.getOctave() + "</octave></pitch>";
        String pitchKey = Math.max(1, staffNo) + ":" + pitch.getOctave() + ":" + pitch.getStep();
        String accidentalText = AccidentalSpelling.resolveAccidentalTextForPitch(pitch, keyFifths,
                previousAlterByPitchKey, pitchKey, preferredAccidental);
        String accidentalXml = accidentalText != null && accidentalText.trim().length() > 0
                ? "<accidental>" + xmlEscape(accidentalText.trim()) + "</accidental>"
                : "";
        return new MuseImportedPitchXml(pitchXml, accidentalXml);
    }

    public static List<MuseImportedPitchXml> buildMuseImportedVoicePitchXmlItems(
            Collection<MuseScoreChordNote> notes, int keyFifths, int staffNo,
            Map<String, Integer> previousAlterByPitchKey) {
        List<MuseImportedPitchXml> items = new ArrayList<MuseImportedPitchXml>();
        if (notes == null) {
            return items;
        }
        Map<String, Integer> state = previousAlterByPitchKey == null ? new LinkedHashMap<String, Integer>()
                : previousAlterByPitchKey;
        for (MuseScoreChordNote note : notes) {
            if (note != null) {
                items.add(buildMuseImportedPitchXml(note, keyFifths, staffNo, state));
            }
        }
        return items;
    }

    public static String buildMuseImportedRestNoteXml(int durationDiv, int partVoiceNo, String type, int dots,
            String timeModificationXml, String beamXml, int staffNo, Collection<String> notationItems) {
        String notationsXml = buildMuseImportedNotationItemsXml(notationItems);
        return "<note><rest/><duration>" + Math.max(0, durationDiv) + "</duration><voice>"
                + Math.max(1, partVoiceNo) + "</voice><type>" + xmlEscape(type == null ? "" : type)
                + "</type>" + repeatXml("<dot/>", dots) + safeXmlFragment(timeModificationXml)
                + safeXmlFragment(beamXml) + "<staff>" + Math.max(1, staffNo) + "</staff>" + notationsXml
                + "</note>";
    }

    public static String buildMuseImportedPitchedNoteXml(boolean chordFollow, boolean grace, boolean graceSlash,
            MuseImportedPitchXml pitch, TieFlags tieFlags, int durationDiv, int partVoiceNo, String type, int dots,
            String timeModificationXml, String beamXml, int staffNo, Collection<String> notationItems) {
        return buildMuseImportedPitchedNoteXml(chordFollow, grace, graceSlash, grace, pitch, tieFlags, durationDiv,
                partVoiceNo, type, dots, timeModificationXml, beamXml, staffNo, notationItems);
    }

    public static String buildMuseImportedPitchedNoteXml(boolean chordFollow, boolean grace, boolean graceSlash,
            boolean omitDuration, MuseImportedPitchXml pitch, TieFlags tieFlags, int durationDiv, int partVoiceNo,
            String type, int dots, String timeModificationXml, String beamXml, int staffNo,
            Collection<String> notationItems) {
        MuseImportedPitchXml safePitch = pitch == null ? new MuseImportedPitchXml("", "") : pitch;
        String graceXml = grace ? (graceSlash ? "<grace slash=\"yes\"/>" : "<grace/>") : "";
        String durationXml = omitDuration ? "" : "<duration>" + Math.max(0, durationDiv) + "</duration>";
        return "<note>" + (chordFollow ? "<chord/>" : "") + graceXml + safePitch.getPitchXml()
                + buildMuseImportedNoteTieXml(tieFlags) + durationXml + "<voice>" + Math.max(1, partVoiceNo)
                + "</voice><type>" + xmlEscape(type == null ? "" : type) + "</type>"
                + repeatXml("<dot/>", dots) + safeXmlFragment(timeModificationXml)
                + safePitch.getAccidentalXml() + safeXmlFragment(beamXml) + "<staff>" + Math.max(1, staffNo)
                + "</staff>" + buildMuseImportedNotationItemsXml(notationItems) + "</note>";
    }

    public static String buildMuseImportedRestEventXml(int durationDiv, Integer displayDurationDiv, int divisions,
            int partVoiceNo, int staffNo, TupletMusicXml tupletXml, String beamXml) {
        int displayDuration = displayDurationDiv == null ? durationDiv : displayDurationDiv.intValue();
        TypeAndDots info = divisionToTypeAndDots(divisions, displayDuration);
        TupletMusicXml tuplet = tupletXml == null ? new TupletMusicXml("", Collections.<String>emptyList())
                : tupletXml;
        return buildMuseImportedRestNoteXml(durationDiv, partVoiceNo, info.getType(), info.getDots(),
                tuplet.getTimeModificationXml(), beamXml, staffNo, tuplet.getNotationItems());
    }

    public static String buildMuseImportedChordEventXml(Collection<MuseScoreChordNote> notes, int keyFifths,
            int staffNo, Map<String, Integer> previousAlterByPitchKey, int durationDiv, Integer displayDurationDiv,
            int divisions, int partVoiceNo, boolean grace, boolean graceSlash, TupletMusicXml tupletXml,
            String beamXml, Collection<Integer> slurStarts, Collection<Integer> slurStops,
            Collection<String> trillNotationItems, Collection<String> articulationTags,
            Collection<String> technicalTags) {
        int displayDuration = displayDurationDiv == null ? durationDiv : displayDurationDiv.intValue();
        TypeAndDots info = divisionToTypeAndDots(divisions, displayDuration);
        TupletMusicXml tuplet = tupletXml == null ? new TupletMusicXml("", Collections.<String>emptyList())
                : tupletXml;
        List<MuseImportedPitchXml> pitches = buildMuseImportedVoicePitchXmlItems(notes, keyFifths, staffNo,
                previousAlterByPitchKey);
        List<MuseScoreChordNote> safeNotes = notes == null ? Collections.<MuseScoreChordNote>emptyList()
                : new ArrayList<MuseScoreChordNote>(notes);
        StringBuilder body = new StringBuilder();
        int pitchIndex = 0;
        for (int index = 0; index < safeNotes.size(); index++) {
            MuseScoreChordNote note = safeNotes.get(index);
            if (note == null) {
                continue;
            }
            boolean first = index == 0;
            TieFlags tieFlags = new TieFlags(note.isTieStart(), note.isTieStop());
            Collection<String> tupletItems = first ? tuplet.getNotationItems() : null;
            Collection<Integer> effectiveSlurStarts = first ? slurStarts : null;
            Collection<Integer> effectiveSlurStops = first ? slurStops : null;
            Collection<String> effectiveTrillItems = first ? trillNotationItems : null;
            Collection<String> effectiveArticulations = first ? articulationTags : null;
            Collection<String> effectiveTechnicalTags = first ? technicalTags : null;
            String notationsXml = buildMuseImportedNoteNotationsXml(tupletItems, effectiveSlurStarts,
                    effectiveSlurStops, effectiveTrillItems, effectiveArticulations, effectiveTechnicalTags, tieFlags,
                    note.getFingeringText(), note.getStringNumber());
            List<String> notationItems = new ArrayList<String>();
            if (notationsXml.startsWith("<notations>") && notationsXml.endsWith("</notations>")) {
                notationItems.add(notationsXml.substring("<notations>".length(), notationsXml.length()
                        - "</notations>".length()));
            }
            body.append(buildMuseImportedPitchedNoteXml(!first, grace && first, graceSlash, grace,
                    pitches.get(pitchIndex), tieFlags, durationDiv, partVoiceNo, info.getType(), info.getDots(),
                    first && !grace ? tuplet.getTimeModificationXml() : "", first ? beamXml : "", staffNo,
                    notationItems));
            pitchIndex++;
        }
        return body.toString();
    }

    public static String buildMuseImportedForwardXml(int leadDiv, int partVoiceNo, int staffNo) {
        int lead = Math.max(0, leadDiv);
        if (lead <= 0) {
            return "";
        }
        return "<forward><duration>" + lead + "</duration><voice>" + Math.max(1, partVoiceNo)
                + "</voice><staff>" + Math.max(1, staffNo) + "</staff></forward>";
    }

    public static boolean shouldClampMuseImportedTimedEvent(int occupiedDiv, int timedDurationDiv, int capacityDiv,
            int tupletToleranceDiv) {
        int duration = Math.max(0, timedDurationDiv);
        return duration > 0 && Math.max(0, occupiedDiv) + duration > Math.max(0, capacityDiv)
                + Math.max(0, tupletToleranceDiv);
    }

    public static String buildMuseImportedTailRestNoteXml(int occupiedDiv, int capacityDiv, int tupletToleranceDiv,
            int divisions, int partVoiceNo, int staffNo) {
        int occupied = Math.max(0, occupiedDiv);
        int capacity = Math.max(0, capacityDiv);
        int restDiv = capacity - occupied;
        if (occupied >= capacity || restDiv <= Math.max(0, tupletToleranceDiv)) {
            return "";
        }
        TypeAndDots info = divisionToTypeAndDots(divisions, restDiv);
        return buildMuseImportedRestNoteXml(restDiv, partVoiceNo, info.getType(), info.getDots(), "", "", staffNo,
                null);
    }

    public static String buildMuseImportedPlacedDirectionXml(int eventAtDiv, int occupiedDiv, int partVoiceNo,
            int staffNo, String directionXml) {
        int lead = Math.max(0, eventAtDiv - Math.max(0, occupiedDiv));
        return buildMuseImportedForwardXml(lead, partVoiceNo, staffNo)
                + withDirectionPlacement(directionXml, Math.max(1, staffNo), Math.max(1, partVoiceNo));
    }

    public static String buildMuseImportedPlacedDynamicXml(int eventAtDiv, int occupiedDiv, int partVoiceNo,
            int staffNo, String mark, Double soundDynamics) {
        return buildMuseImportedPlacedDirectionXml(eventAtDiv, occupiedDiv, partVoiceNo, staffNo,
                buildDynamicDirectionXml(mark, soundDynamics));
    }

    public static String buildMuseImportedPlacedBarlineXml(int eventAtDiv, int occupiedDiv, int partVoiceNo,
            int staffNo, String barlineXml) {
        int lead = Math.max(0, eventAtDiv - Math.max(0, occupiedDiv));
        return buildMuseImportedForwardXml(lead, partVoiceNo, staffNo) + safeXmlFragment(barlineXml);
    }

    public static MuseImportedVoiceCursorStep advanceMuseImportedVoiceCursorForEvent(Integer eventAtDivRaw,
            int occupiedDiv, int timedDurationDiv, int capacityDiv, int tupletToleranceDiv, int partVoiceNo,
            int staffNo) {
        int occupied = Math.max(0, occupiedDiv);
        int eventAtDiv = eventAtDivRaw == null ? occupied : Math.max(0, Math.round(eventAtDivRaw.intValue()));
        int lead = Math.max(0, eventAtDiv - occupied);
        int occupiedAfterLead = occupied + lead;
        int timedDuration = Math.max(0, timedDurationDiv);
        boolean clamped = shouldClampMuseImportedTimedEvent(occupiedAfterLead, timedDuration, capacityDiv,
                tupletToleranceDiv);
        int occupiedAfterTimed = clamped ? occupiedAfterLead : occupiedAfterLead + timedDuration;
        return new MuseImportedVoiceCursorStep(eventAtDiv,
                buildMuseImportedForwardXml(lead, partVoiceNo, staffNo), occupiedAfterLead, occupiedAfterTimed,
                clamped);
    }

    public static String buildMuseImportedVoiceXml(int beats, int beatType, int keyFifths, int staffNo,
            int partVoiceNo, int capacityDiv, int divisions, boolean applyImplicitBeams,
            Collection<MuseImportedVoiceEvent> voiceEvents) {
        List<MuseImportedVoiceEvent> events = voiceEvents == null ? Collections.<MuseImportedVoiceEvent>emptyList()
                : new ArrayList<MuseImportedVoiceEvent>(voiceEvents);
        int tupletTolerance = tupletRoundingToleranceByMuseImportedVoiceEvents(events);
        int baseBeatDiv = Math.max(1, Math.round((divisions * 4.0f) / Math.max(1, beatType)));
        int inferredBeamBeatDiv = beatType == 8 && beats >= 6 && beats % 3 == 0 ? baseBeatDiv * 3 : baseBeatDiv;
        List<MuseBeamEvent> beamEvents = new ArrayList<MuseBeamEvent>();
        for (MuseImportedVoiceEvent event : events) {
            beamEvents.add(buildMuseImportedBeamEventInfo(event != null && event.isTimed(),
                    event != null && event.isChord(), event != null && event.isGrace(),
                    event == null ? 0 : event.getDurationDiv(), event == null ? null : event.getDisplayDurationDiv(),
                    divisions, event == null ? null : event.getExplicitBeamMode()));
        }
        Map<Integer, String> beamXmlByEventIndex = buildMuseBeamXmlByEventInfo(beamEvents, inferredBeamBeatDiv,
                applyImplicitBeams);
        Map<String, Integer> accidentalStateByPitch = new LinkedHashMap<String, Integer>();
        StringBuilder body = new StringBuilder();
        int occupied = 0;
        for (int eventIndex = 0; eventIndex < events.size(); eventIndex++) {
            MuseImportedVoiceEvent event = events.get(eventIndex);
            if (event == null) {
                continue;
            }
            int eventStaffNo = event.getStaffNo() == null ? staffNo : Math.max(1, event.getStaffNo().intValue());
            if (event.isDynamic()) {
                int lead = Math.max(0, event.getEventAtDiv(occupied) - occupied);
                body.append(buildMuseImportedPlacedDynamicXml(event.getEventAtDiv(occupied), occupied, partVoiceNo,
                        eventStaffNo, event.getDynamicMark(), event.getSoundDynamics()));
                occupied += lead;
                continue;
            }
            if (event.isDirectionXml()) {
                int lead = Math.max(0, event.getEventAtDiv(occupied) - occupied);
                body.append(buildMuseImportedPlacedDirectionXml(event.getEventAtDiv(occupied), occupied, partVoiceNo,
                        eventStaffNo, event.getXml()));
                occupied += lead;
                continue;
            }
            if (event.isBarlineXml()) {
                int lead = Math.max(0, event.getEventAtDiv(occupied) - occupied);
                body.append(buildMuseImportedPlacedBarlineXml(event.getEventAtDiv(occupied), occupied, partVoiceNo,
                        eventStaffNo, event.getXml()));
                occupied += lead;
                continue;
            }
            MuseImportedVoiceCursorStep cursor = advanceMuseImportedVoiceCursorForEvent(event.getAtDiv(), occupied,
                    event.getDurationDiv(), capacityDiv, tupletTolerance, partVoiceNo, eventStaffNo);
            body.append(cursor.getForwardXml());
            if (cursor.isClamped()) {
                break;
            }
            occupied = cursor.getOccupiedAfterTimed();
            TupletMusicXml tuplet = buildTupletMusicXml(event.getTimeModification(), event.getTupletStarts(),
                    event.getTupletStops());
            String beamXml = readMuseImportedBeamXmlByEventIndex(beamXmlByEventIndex, eventIndex);
            if (event.isRest()) {
                body.append(buildMuseImportedRestEventXml(event.getDurationDiv(), event.getDisplayDurationDiv(),
                        divisions, partVoiceNo, eventStaffNo, tuplet, beamXml));
                continue;
            }
            body.append(buildMuseImportedChordEventXml(event.getNotes(), keyFifths, eventStaffNo,
                    accidentalStateByPitch, event.getDurationDiv(), event.getDisplayDurationDiv(), divisions,
                    partVoiceNo, event.isGrace(), event.isGraceSlash(), tuplet, beamXml, event.getSlurStarts(),
                    event.getSlurStops(), event.getTrillNotationItems(), event.getArticulationTags(),
                    event.getTechnicalTags()));
        }
        body.append(buildMuseImportedTailRestNoteXml(occupied, capacityDiv, tupletTolerance, divisions, partVoiceNo,
                staffNo));
        return body.toString();
    }

    public static String buildMuseImportedBackupXml(int durationDiv) {
        return "<backup><duration>" + Math.max(0, durationDiv) + "</duration></backup>";
    }

    public static String buildMuseImportedStaffVoicesXml(int beats, int beatType, int keyFifths, int staffNo,
            int capacityDiv, int divisions, boolean applyImplicitBeams, MuseImportedPartVoiceIdResolver resolver,
            Collection<MuseImportedVoiceEvent> staffEvents) {
        MuseImportedPartVoiceIdResolver safeResolver = resolver == null ? new MuseImportedPartVoiceIdResolver()
                : resolver;
        StringBuilder body = new StringBuilder();
        List<Integer> voiceNos = resolveMuseImportedTypedVoiceNos(staffEvents);
        for (int index = 0; index < voiceNos.size(); index++) {
            int voiceNo = voiceNos.get(index).intValue();
            int partVoiceNo = safeResolver.resolve(Math.max(1, staffNo), voiceNo);
            if (index > 0) {
                body.append(buildMuseImportedBackupXml(capacityDiv));
            }
            body.append(buildMuseImportedVoiceXml(beats, beatType, keyFifths, staffNo, partVoiceNo, capacityDiv,
                    divisions, applyImplicitBeams, collectMuseImportedTypedVoiceEvents(staffEvents, voiceNo)));
        }
        return body.toString();
    }

    public static String buildMuseImportedMeasureXml(ParsedMuseScorePart part, ParsedMuseScoreMeasure primaryMeasure,
            int partIndex, int measureIndex, int measureCount, boolean startsWithPickup, int divisions,
            String miscXml, boolean needsAttributes, boolean applyImplicitBeams,
            MuseImportedPartVoiceIdResolver resolver,
            Map<Integer, ? extends Collection<MuseImportedVoiceEvent>> staffEventsByStaffNo) {
        ParsedMuseScoreMeasure measure = primaryMeasure == null
                ? buildFallbackParsedMuseScoreMeasure(measureIndex + 1, 4, 4, null, Math.max(1, divisions * 4),
                        false, 0, "major")
                : primaryMeasure;
        ParsedMuseScorePart safePart = part == null
                ? new ParsedMuseScorePart("P1", "Music", null, Collections.<ParsedMuseScoreStaff>emptyList())
                : part;
        MuseImportedPartVoiceIdResolver safeResolver = resolver == null ? buildMuseImportedPartVoiceIdResolver(safePart)
                : resolver;
        int capacity = Math.max(1, Math.round(measure.getCapacityDiv()));
        StringBuilder body = new StringBuilder();
        body.append(buildMuseImportedMeasureHeaderXml(measure, safePart, partIndex, divisions, miscXml,
                needsAttributes));
        int staffCount = Math.max(1, safePart.getStaffs().size());
        for (int staffIndex = 0; staffIndex < staffCount; staffIndex++) {
            int staffNo = staffIndex + 1;
            if (staffIndex > 0) {
                body.append(buildMuseImportedBackupXml(capacity));
            }
            Collection<MuseImportedVoiceEvent> staffEvents = staffEventsByStaffNo == null ? null
                    : staffEventsByStaffNo.get(Integer.valueOf(staffNo));
            body.append(buildMuseImportedStaffVoicesXml(measure.getBeats(), measure.getBeatType(),
                    measure.getFifths(), staffNo, capacity, divisions, applyImplicitBeams, safeResolver,
                    staffEvents));
        }
        return finalizeMuseImportedMeasureXml(body.toString(), measure, measureIndex, measureCount,
                startsWithPickup);
    }

    public static String buildMuseImportedPartXml(ParsedMuseScorePart part, int partIndex, int divisions,
            String miscXml, boolean applyImplicitBeams, int initialBeats, int initialBeatType,
            String initialTimeSymbol, int initialFifths, String initialMode,
            Map<Integer, ? extends Map<Integer, ? extends Collection<MuseImportedVoiceEvent>>> eventsByMeasureStaff) {
        ParsedMuseScorePart safePart = part == null
                ? new ParsedMuseScorePart("P1", "Music", null, Collections.<ParsedMuseScoreStaff>emptyList())
                : part;
        MuseImportedPartVoiceIdResolver resolver = buildMuseImportedPartVoiceIdResolver(safePart);
        int prevBeats = Math.max(1, initialBeats);
        int prevBeatType = Math.max(1, initialBeatType);
        String prevTimeSymbol = initialTimeSymbol;
        int prevFifths = Math.max(-7, Math.min(7, initialFifths));
        String prevMode = initialMode == null || initialMode.length() == 0 ? "major" : initialMode;
        int measureCount = 1;
        for (ParsedMuseScoreStaff staff : safePart.getStaffs()) {
            if (staff != null) {
                measureCount = Math.max(measureCount, staff.getMeasures().size());
            }
        }
        boolean startsWithPickup = safePart.getStaffs().size() > 0 && safePart.getStaffs().get(0).getMeasures().size() > 0
                && safePart.getStaffs().get(0).getMeasures().get(0).isImplicit();
        StringBuilder measuresXml = new StringBuilder();
        for (int measureIndex = 0; measureIndex < measureCount; measureIndex++) {
            ParsedMuseScoreMeasure primaryMeasure = resolveMuseImportedPrimaryMeasure(safePart, measureIndex,
                    divisions, prevBeats, prevBeatType, prevTimeSymbol, prevFifths, prevMode);
            boolean needsAttributes = needsMuseImportedMeasureAttributes(measureIndex, primaryMeasure, prevBeats,
                    prevBeatType, prevTimeSymbol, prevFifths, prevMode);
            Map<Integer, ? extends Collection<MuseImportedVoiceEvent>> staffEvents = eventsByMeasureStaff == null ? null
                    : eventsByMeasureStaff.get(Integer.valueOf(measureIndex));
            measuresXml.append(buildMuseImportedMeasureXml(safePart, primaryMeasure, measureIndex == 0 ? partIndex : -1,
                    measureIndex, measureCount, startsWithPickup, divisions, measureIndex == 0 ? miscXml : "",
                    needsAttributes, applyImplicitBeams, resolver, staffEvents));
            prevBeats = primaryMeasure.getBeats();
            prevBeatType = primaryMeasure.getBeatType();
            prevTimeSymbol = primaryMeasure.getTimeSymbol();
            prevFifths = primaryMeasure.getFifths();
            prevMode = primaryMeasure.getMode();
        }
        return "<part id=\"" + safePart.getPartId() + "\">" + measuresXml.toString() + "</part>";
    }

    private static int tupletRoundingToleranceByMuseImportedVoiceEvents(Collection<MuseImportedVoiceEvent> events) {
        int tupletCount = 0;
        if (events != null) {
            for (MuseImportedVoiceEvent event : events) {
                if (event == null || !event.isTimed() || event.getDurationDiv() <= 0 || event.isGrace()
                        || event.getTimeModification() == null) {
                    continue;
                }
                tupletCount++;
            }
        }
        return tupletCount <= 0 ? 0 : (int) Math.floor(tupletCount / 2.0d);
    }

    public static List<MuseImportedVoiceEvent> collectMuseImportedTypedVoiceEvents(
            Collection<MuseImportedVoiceEvent> events, int voiceNo) {
        List<MuseImportedVoiceEvent> out = new ArrayList<MuseImportedVoiceEvent>();
        if (events != null) {
            for (MuseImportedVoiceEvent event : events) {
                if (event != null && event.getVoiceNo() == Math.max(1, voiceNo)) {
                    out.add(event);
                }
            }
        }
        Collections.sort(out, new java.util.Comparator<MuseImportedVoiceEvent>() {
            @Override
            public int compare(MuseImportedVoiceEvent left, MuseImportedVoiceEvent right) {
                return Integer.valueOf(left.getEventAtDiv(0)).compareTo(Integer.valueOf(right.getEventAtDiv(0)));
            }
        });
        return out;
    }

    public static List<Integer> resolveMuseImportedTypedVoiceNos(Collection<MuseImportedVoiceEvent> events) {
        java.util.Set<Integer> voiceNos = new java.util.TreeSet<Integer>();
        if (events != null) {
            for (MuseImportedVoiceEvent event : events) {
                if (event != null) {
                    voiceNos.add(Integer.valueOf(event.getVoiceNo()));
                }
            }
        }
        if (voiceNos.isEmpty()) {
            voiceNos.add(Integer.valueOf(1));
        }
        return new ArrayList<Integer>(voiceNos);
    }

    public static List<MuseImportedVoiceEvent> buildMuseImportedRestVoiceEvents(Collection<TimedEvent> events,
            int staffNo) {
        List<MuseImportedVoiceEvent> out = new ArrayList<MuseImportedVoiceEvent>();
        if (events != null) {
            for (TimedEvent event : events) {
                if (event == null) {
                    continue;
                }
                out.add(MuseImportedVoiceEvent.restForVoice(Integer.valueOf(event.getAtDiv()), event.getVoice(),
                        event.getDurationDiv(), null, Integer.valueOf(Math.max(1, staffNo)), null, null,
                        event.getTupletStops(), null));
            }
        }
        return out;
    }

    public static Map<Integer, Collection<MuseImportedVoiceEvent>> buildMuseImportedRestStaffEventsByStaffNo(
            ParsedMuseScorePart part, int measureIndex, ParsedMuseScoreMeasure primaryMeasure) {
        Map<Integer, Collection<MuseImportedVoiceEvent>> out = new LinkedHashMap<Integer, Collection<MuseImportedVoiceEvent>>();
        ParsedMuseScorePart safePart = part == null
                ? new ParsedMuseScorePart("P1", "Music", null, Collections.<ParsedMuseScoreStaff>emptyList())
                : part;
        int staffCount = Math.max(1, safePart.getStaffs().size());
        ParsedMuseScoreMeasure safePrimary = primaryMeasure == null
                ? buildFallbackParsedMuseScoreMeasure(measureIndex + 1, 4, 4, null, 4, false, 0, "major")
                : primaryMeasure;
        for (int staffIndex = 0; staffIndex < staffCount; staffIndex++) {
            ParsedMuseScoreMeasure measure = resolveMuseImportedStaffMeasure(safePart, staffIndex, measureIndex,
                    safePrimary);
            out.put(Integer.valueOf(staffIndex + 1),
                    buildMuseImportedRestVoiceEvents(measure.getEvents(), staffIndex + 1));
        }
        return out;
    }

    private static String buildMuseImportedNotationItemsXml(Collection<String> notationItems) {
        StringBuilder items = new StringBuilder();
        appendStringItems(items, notationItems);
        return items.length() == 0 ? "" : "<notations>" + items.toString() + "</notations>";
    }

    private static String repeatXml(String xml, int count) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < Math.max(0, count); index++) {
            out.append(xml);
        }
        return out.toString();
    }

    private static String safeXmlFragment(String xml) {
        return xml == null ? "" : xml;
    }

    public static boolean isIgnoredMuseImportTag(String tag) {
        String normalized = tag == null ? "" : tag.trim().toLowerCase(Locale.ROOT);
        return "timesig".equals(normalized) || "keysig".equals(normalized) || "layoutbreak".equals(normalized)
                || "clef".equals(normalized) || "beam".equals(normalized);
    }

    public static List<MuseScoreWarning> warnOnMuseImportMeasureOverflow(Collection<TimedEvent> events,
            int capacityDiv, int measureNo, int localStaffNo) {
        java.util.Map<Integer, Integer> occupiedByVoice = new java.util.LinkedHashMap<Integer, Integer>();
        if (events != null) {
            for (TimedEvent event : events) {
                if (event == null) {
                    continue;
                }
                Integer voice = Integer.valueOf(event.getVoice());
                int current = occupiedByVoice.containsKey(voice) ? occupiedByVoice.get(voice).intValue() : 0;
                occupiedByVoice.put(voice, Integer.valueOf(current + Math.max(0, Math.round(event.getDurationDiv()))));
            }
        }
        List<MuseScoreWarning> warnings = new ArrayList<MuseScoreWarning>();
        for (Map.Entry<Integer, Integer> entry : occupiedByVoice.entrySet()) {
            int voice = entry.getKey().intValue();
            int occupied = entry.getValue().intValue();
            if (occupied <= capacityDiv) {
                continue;
            }
            warnings.add(new MuseScoreWarning("MUSESCORE_IMPORT_WARNING",
                    "measure " + measureNo + " voice " + voice + ": overfull content (" + occupied + " > "
                            + capacityDiv + "); tail events are clamped.",
                    measureNo, localStaffNo, voice, null, "clamped", "overfull", null, occupied, capacityDiv));
        }
        return warnings;
    }

    public static ParsedMuseScoreMeasure buildParsedMuseScoreMeasure(int measureNo,
            MuseScoreImportMeasureContext measureContext, Collection<TimedEvent> events) {
        return new ParsedMuseScoreMeasure(measureNo, measureContext.getBeats(), measureContext.getBeatType(),
                measureContext.getTimeSymbol(), measureContext.isExplicitTimeSig(), measureContext.getCapacityDiv(),
                measureContext.isImplicit(), measureContext.getFifths(), measureContext.getMode(),
                measureContext.getTempoBpm(), measureContext.getTempoText(), measureContext.isRepeatForward(),
                measureContext.isRepeatBackward(), measureContext.isLeftDoubleBarline(), events);
    }

    public static ParsedMuseScoreMeasure buildFallbackParsedMuseScoreMeasure(int index, int beats, int beatType,
            String timeSymbol, int capacityDiv, boolean implicit, int fifths, String mode) {
        MuseScoreImportMeasureContext context = new MuseScoreImportMeasureContext(beats, beatType, timeSymbol, false,
                capacityDiv, implicit, fifths, mode, null, null, false, false, false);
        return buildParsedMuseScoreMeasure(index, context, Collections.singletonList(new TimedEvent(capacityDiv, false,
                false)));
    }

    public static String buildMuseScoreImportPartListXml(Collection<ParsedMuseScorePart> parsedByPart) {
        StringBuilder out = new StringBuilder();
        if (parsedByPart != null) {
            for (ParsedMuseScorePart part : parsedByPart) {
                out.append("<score-part id=\"").append(xmlEscape(part.getPartId())).append("\"><part-name>")
                        .append(xmlEscape(part.getPartName())).append("</part-name></score-part>");
            }
        }
        return out.toString();
    }

    public static String buildMuseScoreImportIdentificationXml(MuseScoreImportMetadata metadata) {
        StringBuilder creatorItems = new StringBuilder();
        if (metadata.getComposer() != null && metadata.getComposer().length() > 0) {
            creatorItems.append("<creator type=\"composer\">").append(xmlEscape(metadata.getComposer()))
                    .append("</creator>");
        }
        if (metadata.getArrangerMeta() != null && metadata.getArrangerMeta().length() > 0) {
            creatorItems.append("<creator type=\"arranger\">").append(xmlEscape(metadata.getArrangerMeta()))
                    .append("</creator>");
        }
        if (metadata.getLyricistMeta() != null && metadata.getLyricistMeta().length() > 0) {
            creatorItems.append("<creator type=\"lyricist\">").append(xmlEscape(metadata.getLyricistMeta()))
                    .append("</creator>");
        }
        if (metadata.getTranslatorMeta() != null && metadata.getTranslatorMeta().length() > 0) {
            creatorItems.append("<creator type=\"translator\">").append(xmlEscape(metadata.getTranslatorMeta()))
                    .append("</creator>");
        }
        String rightsXml = metadata.getCopyrightMeta() != null && metadata.getCopyrightMeta().length() > 0
                ? "<rights>" + xmlEscape(metadata.getCopyrightMeta()) + "</rights>"
                : "";
        String encodingXml = metadata.getCreationDateMeta() != null && metadata.getCreationDateMeta().length() > 0
                ? "<encoding><encoding-date>" + xmlEscape(metadata.getCreationDateMeta())
                        + "</encoding-date></encoding>"
                : "";
        if (creatorItems.length() == 0 && rightsXml.length() == 0 && encodingXml.length() == 0) {
            return "";
        }
        return "<identification>" + creatorItems.toString() + rightsXml + encodingXml + "</identification>";
    }

    public static String buildMuseScoreImportDocumentXml(Collection<ParsedMuseScorePart> parsedByPart,
            MuseScoreImportMetadata metadata, Collection<String> partXmlItems) {
        String identificationXml = buildMuseScoreImportIdentificationXml(metadata);
        String workNumberXml = metadata.getWorkNumberMeta() != null && metadata.getWorkNumberMeta().length() > 0
                ? "<work-number>" + xmlEscape(metadata.getWorkNumberMeta()) + "</work-number>"
                : "";
        String movementTitleXml = metadata.getMovementTitleMeta() != null
                && metadata.getMovementTitleMeta().length() > 0
                        ? "<movement-title>" + xmlEscape(metadata.getMovementTitleMeta()) + "</movement-title>"
                        : "";
        String movementNumberXml = metadata.getMovementNumberMeta() != null
                && metadata.getMovementNumberMeta().length() > 0
                        ? "<movement-number>" + xmlEscape(metadata.getMovementNumberMeta()) + "</movement-number>"
                        : "";
        String subtitleCreditXml = metadata.getSubtitleMeta() != null && metadata.getSubtitleMeta().length() > 0
                ? "<credit page=\"1\"><credit-type>subtitle</credit-type><credit-words>"
                        + xmlEscape(metadata.getSubtitleMeta()) + "</credit-words></credit>"
                : "";
        StringBuilder partXml = new StringBuilder();
        if (partXmlItems != null) {
            for (String item : partXmlItems) {
                partXml.append(item == null ? "" : item);
            }
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><score-partwise version=\"4.0\"><work><work-title>"
                + xmlEscape(metadata.getWorkTitle()) + "</work-title>" + workNumberXml + "</work>" + movementTitleXml
                + movementNumberXml + subtitleCreditXml + identificationXml + "<part-list>"
                + buildMuseScoreImportPartListXml(parsedByPart) + "</part-list>" + partXml.toString()
                + "</score-partwise>";
    }

    public static String buildMuseScoreImportDocumentXml(Collection<ParsedMuseScorePart> parsedByPart,
            MuseScoreImportMetadata metadata, int divisions, String miscXml, boolean applyImplicitBeams,
            int initialBeats, int initialBeatType, String initialTimeSymbol, int initialFifths, String initialMode,
            Map<Integer, ? extends Map<Integer, ? extends Map<Integer, ? extends Collection<MuseImportedVoiceEvent>>>> eventsByPartMeasureStaff) {
        List<String> partXmlItems = new ArrayList<String>();
        List<ParsedMuseScorePart> parts = parsedByPart == null ? Collections.<ParsedMuseScorePart>emptyList()
                : new ArrayList<ParsedMuseScorePart>(parsedByPart);
        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
            Map<Integer, ? extends Map<Integer, ? extends Collection<MuseImportedVoiceEvent>>> eventsByMeasureStaff = eventsByPartMeasureStaff == null
                    ? null
                    : eventsByPartMeasureStaff.get(Integer.valueOf(partIndex));
            partXmlItems.add(buildMuseImportedPartXml(parts.get(partIndex), partIndex, divisions, miscXml,
                    applyImplicitBeams, initialBeats, initialBeatType, initialTimeSymbol, initialFifths, initialMode,
                    eventsByMeasureStaff));
        }
        return buildMuseScoreImportDocumentXml(parts, metadata, partXmlItems);
    }

    public static String fractionFromDivisions(int durationDiv, int divisions) {
        int numeratorRaw = Math.max(1, Math.round(durationDiv));
        int denominatorRaw = Math.max(1, Math.round(divisions)) * 4;
        int gcd = gcdPositive(numeratorRaw, denominatorRaw);
        return Math.max(1, Math.round(numeratorRaw / (float) gcd)) + "/"
                + Math.max(1, Math.round(denominatorRaw / (float) gcd));
    }

    public static String makeMuseRestXml(int durationDiv, int displayDurationDiv, int divisions, String tupletRefId) {
        TypeAndDots duration = divisionToTypeAndDots(divisions,
                displayDurationDiv > 0 ? displayDurationDiv : durationDiv);
        StringBuilder xml = new StringBuilder();
        xml.append("<Rest>");
        xml.append("<durationType>").append(duration.getType()).append("</durationType>");
        if (tupletRefId != null && tupletRefId.trim().length() > 0) {
            xml.append("<Tuplet>").append(xmlEscape(tupletRefId.trim())).append("</Tuplet>");
        }
        if (duration.getDots() > 0) {
            xml.append("<dots>").append(duration.getDots()).append("</dots>");
        }
        xml.append("</Rest>");
        return xml.toString();
    }

    public static String makeMuseChordXml(MuseScoreExportChord chord) {
        int effectiveDuration = chord.getDisplayDurationDiv() > 0 ? chord.getDisplayDurationDiv()
                : (chord.getDurationDiv() > 0 ? chord.getDurationDiv()
                        : Math.max(1, Math.round(chord.getDivisions() / 4.0f)));
        TypeAndDots duration = divisionToTypeAndDots(chord.getDivisions(), effectiveDuration);
        StringBuilder xml = new StringBuilder();
        xml.append("<Chord>");
        if (chord.isGrace()) {
            xml.append(chord.isGraceSlash() ? "<acciaccatura/>" : "<grace/>");
        }
        xml.append("<durationType>").append(duration.getType()).append("</durationType>");
        if (chord.getTupletRefId() != null && chord.getTupletRefId().trim().length() > 0) {
            xml.append("<Tuplet>").append(xmlEscape(chord.getTupletRefId().trim())).append("</Tuplet>");
        }
        if (duration.getDots() > 0) {
            xml.append("<dots>").append(duration.getDots()).append("</dots>");
        }
        for (String subtype : chord.getOttavaStartSubtypes()) {
            xml.append("<Spanner type=\"Ottava\"><Ottava><subtype>").append(xmlEscape(subtype))
                    .append("</subtype></Ottava><next><location><fractions>1/1</fractions></location></next></Spanner>");
        }
        for (int index = 0; index < Math.max(0, Math.round(chord.getOttavaStopCount())); index++) {
            xml.append("<Spanner type=\"Ottava\"><prev><location><fractions>-1/1</fractions></location></prev></Spanner>");
        }
        for (Integer ignored : chord.getTrillStarts()) {
            xml.append("<Spanner type=\"Trill\"><Trill><subtype>trill</subtype></Trill><next><location><fractions>1/1</fractions></location></next></Spanner>");
        }
        for (Integer ignored : chord.getTrillStops()) {
            xml.append("<Spanner type=\"Trill\"><prev><location><fractions>-1/1</fractions></location></prev></Spanner>");
        }
        if (chord.isTrillMarkOnly() && chord.getTrillStarts().isEmpty()) {
            xml.append("<Ornament><subtype>ornamentTrill</subtype></Ornament>");
        }
        for (String span : chord.getSlurStopFractions()) {
            String normalized = span == null ? "" : span.trim();
            if (normalized.length() == 0) {
                normalized = fractionFromDivisions(Math.max(1,
                        Math.round(chord.getDisplayDurationDiv() > 0 ? chord.getDisplayDurationDiv()
                                : chord.getDurationDiv())),
                        chord.getDivisions());
            }
            xml.append("<Spanner type=\"Slur\"><prev><location><fractions>-").append(normalized)
                    .append("</fractions></location></prev></Spanner>");
        }
        for (String span : chord.getSlurStartFractions()) {
            String normalized = span == null ? "" : span.trim();
            if (normalized.length() == 0) {
                normalized = fractionFromDivisions(Math.max(1,
                        Math.round(chord.getDisplayDurationDiv() > 0 ? chord.getDisplayDurationDiv()
                                : chord.getDurationDiv())),
                        chord.getDivisions());
            }
            xml.append("<Spanner type=\"Slur\"><Slur/><next><location><fractions>").append(normalized)
                    .append("</fractions></location></next></Spanner>");
        }
        for (String subtype : chord.getArticulationSubtypes()) {
            xml.append("<Articulation><subtype>").append(xmlEscape(subtype)).append("</subtype></Articulation>");
        }
        for (MuseScoreExportNote note : chord.getNotes()) {
            xml.append("<Note>");
            xml.append("<pitch>").append(Math.max(0, Math.min(127, Math.round(note.getMidi())))).append("</pitch>");
            if (note.getAccidentalSubtype() != null) {
                xml.append("<Accidental><subtype>").append(xmlEscape(note.getAccidentalSubtype()))
                        .append("</subtype></Accidental>");
            }
            if (note.getFingeringText() != null && note.getFingeringText().trim().length() > 0) {
                xml.append("<Fingering>").append(xmlEscape(note.getFingeringText().trim())).append("</Fingering>");
            }
            if (note.getStringNumber() != null && note.getStringNumber().intValue() > 0) {
                xml.append("<String>").append(Math.round(note.getStringNumber().intValue())).append("</String>");
            }
            if (note.isTieStart()) {
                xml.append("<Tie/>");
            }
            if (note.isTieStop()) {
                xml.append("<endSpanner/>");
            }
            xml.append("</Note>");
        }
        xml.append("</Chord>");
        return xml.toString();
    }

    public static int normalizeMuseVoiceEventDuration(int rawDuration, int targetDivisions, int sourceDivisions) {
        int src = Math.max(1, Math.round(sourceDivisions));
        int dst = Math.max(1, Math.round(targetDivisions));
        return Math.max(0, Math.round((Math.max(0, rawDuration) * dst) / (float) src));
    }

    public static int processMuseVoiceEventBackupCursor(int cursorDiv, int rawDuration, int targetDivisions,
            int sourceDivisions) {
        int duration = normalizeMuseVoiceEventDuration(Math.max(0, Math.round(rawDuration)), targetDivisions,
                sourceDivisions);
        return Math.max(0, Math.max(0, cursorDiv) - duration);
    }

    public static int processMuseVoiceEventForwardCursor(int cursorDiv, int rawDuration, int targetDivisions,
            int sourceDivisions) {
        int duration = normalizeMuseVoiceEventDuration(Math.max(0, Math.round(rawDuration)), targetDivisions,
                sourceDivisions);
        return Math.max(0, cursorDiv) + duration;
    }

    public static List<MuseVoiceEvent> pushMuseVoiceEventList(
            Map<Integer, Map<Integer, List<MuseVoiceEvent>>> byStaff, int staffNo, int voiceNo) {
        if (byStaff == null) {
            return new ArrayList<MuseVoiceEvent>();
        }
        Integer staffKey = Integer.valueOf(Math.max(1, Math.round(staffNo)));
        Integer voiceKey = Integer.valueOf(Math.max(1, Math.round(voiceNo)));
        Map<Integer, List<MuseVoiceEvent>> byVoice = byStaff.get(staffKey);
        if (byVoice == null) {
            byVoice = new java.util.LinkedHashMap<Integer, List<MuseVoiceEvent>>();
            byStaff.put(staffKey, byVoice);
        }
        List<MuseVoiceEvent> events = byVoice.get(voiceKey);
        if (events == null) {
            events = new ArrayList<MuseVoiceEvent>();
            byVoice.put(voiceKey, events);
        }
        return events;
    }

    public static void processMuseVoiceEventDirectionMarks(List<MusePendingDirectionMarkEntry> pendingDirectionMarks,
            int cursorDiv, MusicXmlDirectionMarkPayload payload) {
        if (payload == null) {
            return;
        }
        queueMusePendingDirectionMarks(pendingDirectionMarks, payload.getStaffNo(), payload.getVoiceNo(), cursorDiv,
                payload.getMarks());
    }

    public static void processMuseVoiceEventMidBarlineMarks(List<MusePendingDirectionMarkEntry> pendingDirectionMarks,
            int cursorDiv, MusePendingDirectionMarks marks) {
        if (marks == null) {
            return;
        }
        queueMusePendingDirectionMarks(pendingDirectionMarks, 1, 1, cursorDiv, marks);
    }

    public static MuseVoiceEvent buildRestMuseVoiceEventFromMusicXmlValues(int atDiv, int durationDiv,
            TimeModification tupletTimeModification, Collection<Integer> tupletStarts,
            Collection<Integer> tupletStops, MusePendingDirectionMarks marks) {
        return new MuseVoiceEvent(atDiv, durationDiv, null, tupletStarts, tupletStops, tupletTimeModification, null,
                null, null, false, null, null, marks == null ? null : marks.getOttavaStartSubtypes(),
                marks == null ? 0 : marks.getOttavaStopCount(), false, false,
                marks != null && marks.getRepeatForwardCount() > 0,
                marks != null && marks.getRepeatBackwardCount() > 0);
    }

    public static MuseVoiceEvent buildChordMuseVoiceEventFromMusicXmlValues(int atDiv, int durationDiv,
            boolean grace, boolean graceSlash, TimeModification tupletTimeModification,
            Collection<Integer> tupletStarts, Collection<Integer> tupletStops, MuseScoreExportNote payloadNote,
            Collection<Integer> slurStarts, Collection<Integer> slurStops, Collection<Integer> trillStarts,
            Collection<Integer> trillStops, boolean trillMarkOnly, Collection<String> articulationSubtypes,
            MusePendingDirectionMarks marks) {
        if (payloadNote == null) {
            return null;
        }
        return new MuseVoiceEvent(atDiv, durationDiv, Collections.singletonList(payloadNote), tupletStarts, tupletStops,
                tupletTimeModification, slurStarts, slurStops, articulationSubtypes, trillMarkOnly, trillStarts,
                trillStops, marks == null ? null : marks.getOttavaStartSubtypes(),
                marks == null ? 0 : marks.getOttavaStopCount(), grace, graceSlash,
                marks != null && marks.getRepeatForwardCount() > 0,
                marks != null && marks.getRepeatBackwardCount() > 0);
    }

    public static int advanceMuseVoiceEventCursorAfterNote(int cursorDiv, boolean isChordFollow, boolean isGrace,
            int durationDiv) {
        if (isChordFollow || isGrace) {
            return Math.max(0, cursorDiv);
        }
        return Math.max(0, cursorDiv) + Math.max(0, Math.round(durationDiv));
    }

    public static MuseVoiceEvent mergeChordFollowMusicXmlNoteEvent(MuseVoiceEvent previous, boolean isChordFollow,
            boolean isRest, int cursorDiv, MuseScoreExportNote payloadNote, Collection<Integer> slurStarts,
            Collection<Integer> slurStops, Collection<Integer> trillStarts, Collection<Integer> trillStops,
            boolean trillMarkOnly, Collection<Integer> tupletStarts, Collection<Integer> tupletStops,
            TimeModification tupletTimeModification, Collection<String> articulationSubtypes, boolean isGrace,
            boolean isGraceSlash) {
        if (!isChordFollow || isRest || previous == null || previous.getPitches() == null || payloadNote == null) {
            return null;
        }
        if (previous.getAtDiv() + previous.getDurationDiv() != cursorDiv) {
            return null;
        }
        List<MuseScoreExportNote> pitches = new ArrayList<MuseScoreExportNote>(previous.getPitches());
        pitches.add(payloadNote);
        TimeModification mergedTimeModification = previous.getTupletTimeModification() != null
                ? previous.getTupletTimeModification()
                : tupletTimeModification;
        return new MuseVoiceEvent(previous.getAtDiv(), previous.getDurationDiv(), pitches,
                mergeUniqueNumbers(previous.getTupletStarts(), tupletStarts),
                mergeUniqueNumbers(previous.getTupletStops(), tupletStops), mergedTimeModification,
                mergeUniqueNumbers(previous.getSlurStarts(), slurStarts),
                mergeUniqueNumbers(previous.getSlurStops(), slurStops),
                mergeUniqueStrings(previous.getArticulationSubtypes(), articulationSubtypes),
                previous.isTrillMarkOnly() || trillMarkOnly,
                mergeUniqueNumbers(previous.getTrillStarts(), trillStarts),
                mergeUniqueNumbers(previous.getTrillStops(), trillStops), previous.getOttavaStartSubtypes(),
                previous.getOttavaStopCount(), previous.isGrace() || isGrace,
                previous.isGraceSlash() || isGraceSlash, previous.isRepeatForwardAtStart(),
                previous.isRepeatBackwardAtStart());
    }

    public static void queueMusePendingDirectionMarks(List<MusePendingDirectionMarkEntry> pendingDirectionMarks,
            int staffNo, int voiceNo, int atDiv, MusePendingDirectionMarks marks) {
        if (pendingDirectionMarks == null || marks == null) {
            return;
        }
        MusePendingDirectionMarks prev = null;
        for (MusePendingDirectionMarkEntry entry : pendingDirectionMarks) {
            if (entry.getStaffNo() == staffNo && entry.getVoiceNo() == voiceNo && entry.getAtDiv() == atDiv) {
                prev = entry.getMarks();
                break;
            }
        }
        if (prev == null) {
            prev = new MusePendingDirectionMarks(null, 0, 0, 0);
        }
        if (!marks.getOttavaStartSubtypes().isEmpty()) {
            prev.getOttavaStartSubtypes().addAll(marks.getOttavaStartSubtypes());
        }
        if (marks.getOttavaStopCount() != 0) {
            prev.setOttavaStopCount(prev.getOttavaStopCount() + marks.getOttavaStopCount());
        }
        if (marks.getRepeatForwardCount() != 0) {
            prev.setRepeatForwardCount(prev.getRepeatForwardCount() + marks.getRepeatForwardCount());
        }
        if (marks.getRepeatBackwardCount() != 0) {
            prev.setRepeatBackwardCount(prev.getRepeatBackwardCount() + marks.getRepeatBackwardCount());
        }
        for (MusePendingDirectionMarkEntry entry : pendingDirectionMarks) {
            if (entry.getStaffNo() == staffNo && entry.getVoiceNo() == voiceNo && entry.getAtDiv() == atDiv) {
                entry.setMarks(prev);
                return;
            }
        }
        pendingDirectionMarks.add(new MusePendingDirectionMarkEntry(staffNo, voiceNo, atDiv, prev));
    }

    public static MusePendingDirectionMarks consumeMusePendingDirectionMarks(
            List<MusePendingDirectionMarkEntry> pendingDirectionMarks, int staffNo, int voiceNo, int atDiv) {
        MusePendingDirectionMarks collected = new MusePendingDirectionMarks(null, 0, 0, 0);
        if (pendingDirectionMarks != null) {
            for (int index = pendingDirectionMarks.size() - 1; index >= 0; index--) {
                MusePendingDirectionMarkEntry entry = pendingDirectionMarks.get(index);
                if (entry.getStaffNo() != staffNo || entry.getVoiceNo() != voiceNo) {
                    continue;
                }
                if (entry.getAtDiv() > atDiv) {
                    continue;
                }
                if (!entry.getMarks().getOttavaStartSubtypes().isEmpty()) {
                    collected.getOttavaStartSubtypes().addAll(entry.getMarks().getOttavaStartSubtypes());
                }
                if (entry.getMarks().getOttavaStopCount() > 0) {
                    collected.setOttavaStopCount(collected.getOttavaStopCount() + entry.getMarks().getOttavaStopCount());
                }
                if (entry.getMarks().getRepeatForwardCount() > 0) {
                    collected.setRepeatForwardCount(
                            collected.getRepeatForwardCount() + entry.getMarks().getRepeatForwardCount());
                }
                if (entry.getMarks().getRepeatBackwardCount() > 0) {
                    collected.setRepeatBackwardCount(
                            collected.getRepeatBackwardCount() + entry.getMarks().getRepeatBackwardCount());
                }
                pendingDirectionMarks.remove(index);
            }
        }
        if (collected.getOttavaStartSubtypes().isEmpty() && collected.getOttavaStopCount() <= 0
                && collected.getRepeatForwardCount() <= 0 && collected.getRepeatBackwardCount() <= 0) {
            return null;
        }
        return collected;
    }

    public static void applyMuseTrailingDirectionMarks(Map<Integer, Map<Integer, List<MuseVoiceEvent>>> byStaff,
            Collection<MusePendingDirectionMarkEntry> pendingDirectionMarks) {
        if (byStaff == null || pendingDirectionMarks == null) {
            return;
        }
        for (MusePendingDirectionMarkEntry pending : pendingDirectionMarks) {
            Map<Integer, List<MuseVoiceEvent>> byVoice = byStaff.get(Integer.valueOf(pending.getStaffNo()));
            if (byVoice == null) {
                continue;
            }
            List<MuseVoiceEvent> events = byVoice.get(Integer.valueOf(pending.getVoiceNo()));
            if (events == null || events.isEmpty()) {
                continue;
            }
            MuseVoiceEvent last = events.get(events.size() - 1);
            last.setOttavaStartSubtypes(
                    mergeUniqueSubtypes(last.getOttavaStartSubtypes(), pending.getMarks().getOttavaStartSubtypes()));
            if (pending.getMarks().getOttavaStopCount() > 0) {
                last.setOttavaStopCount(last.getOttavaStopCount() + pending.getMarks().getOttavaStopCount());
            }
            if (pending.getMarks().getRepeatForwardCount() > 0) {
                last.setRepeatForwardAtStart(true);
            }
            if (pending.getMarks().getRepeatBackwardCount() > 0) {
                last.setRepeatBackwardAtStart(true);
            }
        }
    }

    public static List<TimedEvent> collectMuseImportedVoiceEvents(ParsedMuseScoreMeasure measure, int voiceNo) {
        List<TimedEvent> out = new ArrayList<TimedEvent>();
        if (measure != null) {
            for (TimedEvent event : measure.getEvents()) {
                if (Math.max(1, event.getVoice()) == voiceNo) {
                    out.add(event);
                }
            }
        }
        Collections.sort(out, new java.util.Comparator<TimedEvent>() {
            public int compare(TimedEvent left, TimedEvent right) {
                return Integer.valueOf(Math.max(0, left.getAtDiv())).compareTo(Integer.valueOf(Math.max(0,
                        right.getAtDiv())));
            }
        });
        return out;
    }

    public static MuseImportedVoiceEvent buildMuseImportedTypedRestEventFromTimedEvent(TimedEvent event,
            int staffNo) {
        TimedEvent safe = event == null ? new TimedEvent(0, false, false, 1, 0) : event;
        return MuseImportedVoiceEvent.restForVoice(Integer.valueOf(safe.getAtDiv()), safe.getVoice(),
                safe.getDurationDiv(), Integer.valueOf(safe.getDurationDiv()), Integer.valueOf(Math.max(1, staffNo)),
                null, null, safe.getTupletStops(), null);
    }

    public static List<MuseImportedVoiceEvent> collectMuseImportedTypedRestEvents(ParsedMuseScoreMeasure measure,
            int staffNo) {
        List<MuseImportedVoiceEvent> out = new ArrayList<MuseImportedVoiceEvent>();
        if (measure != null) {
            for (TimedEvent event : measure.getEvents()) {
                out.add(buildMuseImportedTypedRestEventFromTimedEvent(event, staffNo));
            }
        }
        Collections.sort(out, new java.util.Comparator<MuseImportedVoiceEvent>() {
            @Override
            public int compare(MuseImportedVoiceEvent left, MuseImportedVoiceEvent right) {
                int byVoice = Integer.valueOf(left.getVoiceNo()).compareTo(Integer.valueOf(right.getVoiceNo()));
                if (byVoice != 0) {
                    return byVoice;
                }
                return Integer.valueOf(left.getEventAtDiv(0)).compareTo(Integer.valueOf(right.getEventAtDiv(0)));
            }
        });
        return out;
    }

    public static boolean needsMuseImportedMeasureAttributes(int measureIndex, ParsedMuseScoreMeasure primaryMeasure,
            int prevBeats, int prevBeatType, String prevTimeSymbol, int prevFifths, String prevMode) {
        return measureIndex == 0 || primaryMeasure.getBeats() != prevBeats
                || primaryMeasure.getBeatType() != prevBeatType
                || !stringEquals(primaryMeasure.getTimeSymbol(), prevTimeSymbol)
                || primaryMeasure.isExplicitTimeSig()
                || primaryMeasure.getFifths() != prevFifths
                || !stringEquals(primaryMeasure.getMode(), prevMode);
    }

    public static ParsedMuseScoreMeasure resolveMuseImportedPrimaryMeasure(ParsedMuseScorePart part, int measureIndex,
            int divisions, int prevBeats, int prevBeatType, String prevTimeSymbol, int prevFifths, String prevMode) {
        if (part != null && part.getStaffs().size() > 0 && part.getStaffs().get(0).getMeasures().size() > measureIndex) {
            return part.getStaffs().get(0).getMeasures().get(measureIndex);
        }
        return buildFallbackParsedMuseScoreMeasure(measureIndex + 1, prevBeats, prevBeatType, prevTimeSymbol,
                Math.max(1, Math.round((divisions * 4.0f * prevBeats) / Math.max(1, prevBeatType))), false,
                prevFifths, prevMode);
    }

    public static ParsedMuseScoreMeasure resolveMuseImportedStaffMeasure(ParsedMuseScorePart part, int staffIndex,
            int measureIndex, ParsedMuseScoreMeasure primaryMeasure) {
        if (part != null && part.getStaffs().size() > staffIndex
                && part.getStaffs().get(staffIndex).getMeasures().size() > measureIndex) {
            return part.getStaffs().get(staffIndex).getMeasures().get(measureIndex);
        }
        return buildFallbackParsedMuseScoreMeasure(measureIndex + 1, primaryMeasure.getBeats(),
                primaryMeasure.getBeatType(), primaryMeasure.getTimeSymbol(), primaryMeasure.getCapacityDiv(),
                primaryMeasure.isImplicit(), primaryMeasure.getFifths(), primaryMeasure.getMode());
    }

    public static String finalizeMuseImportedMeasureXml(String body, ParsedMuseScoreMeasure primaryMeasure,
            int measureIndex, int measureCount, boolean startsWithPickup) {
        boolean isLastMeasure = measureIndex == measureCount - 1;
        String out = body == null ? "" : body;
        if (primaryMeasure.isRepeatBackward() || isLastMeasure) {
            out += "<barline location=\"right\">";
            if (isLastMeasure) {
                out += "<bar-style>light-heavy</bar-style>";
            }
            if (primaryMeasure.isRepeatBackward()) {
                out += "<repeat direction=\"backward\"/>";
            }
            out += "</barline>";
        }
        String implicitAttr = primaryMeasure.isImplicit() ? " implicit=\"yes\"" : "";
        int measureNumber = startsWithPickup ? measureIndex : measureIndex + 1;
        return "<measure number=\"" + measureNumber + "\"" + implicitAttr + ">" + out + "</measure>";
    }

    public static String buildMuseImportedMeasureHeaderXml(ParsedMuseScoreMeasure primaryMeasure,
            ParsedMuseScorePart part, int partIndex, int divisions, String miscXml, boolean needsAttributes) {
        StringBuilder body = new StringBuilder();
        if (needsAttributes) {
            String timeSymbolAttr = primaryMeasure.getTimeSymbol() != null ? " symbol=\"" + primaryMeasure.getTimeSymbol()
                    + "\"" : "";
            body.append("<attributes><divisions>").append(divisions).append("</divisions><key><fifths>")
                    .append(primaryMeasure.getFifths()).append("</fifths><mode>").append(primaryMeasure.getMode())
                    .append("</mode></key><time").append(timeSymbolAttr).append("><beats>")
                    .append(primaryMeasure.getBeats()).append("</beats><beat-type>").append(primaryMeasure.getBeatType())
                    .append("</beat-type></time>").append(buildTransposeXml(part.getTranspose()));
            if (part.getStaffs().size() > 1) {
                body.append("<staves>").append(part.getStaffs().size()).append("</staves>");
                for (int index = 0; index < part.getStaffs().size(); index++) {
                    ParsedMuseScoreStaff staff = part.getStaffs().get(index);
                    body.append("<clef number=\"").append(index + 1).append("\"><sign>").append(staff.getClefSign())
                            .append("</sign><line>").append(staff.getClefLine()).append("</line></clef>");
                }
            } else {
                ParsedMuseScoreStaff staff = part.getStaffs().isEmpty() ? null : part.getStaffs().get(0);
                body.append("<clef><sign>").append(staff == null ? "G" : staff.getClefSign()).append("</sign><line>")
                        .append(staff == null ? 2 : staff.getClefLine()).append("</line></clef>");
            }
            if (partIndex == 0 && miscXml != null && miscXml.length() > 0) {
                body.append("<miscellaneous>").append(miscXml).append("</miscellaneous>");
            }
            body.append("</attributes>");
        }
        if (primaryMeasure.isLeftDoubleBarline()) {
            body.append("<barline location=\"left\"><bar-style>light-light</bar-style></barline>");
        }
        if (primaryMeasure.isRepeatForward()) {
            body.append("<barline location=\"left\"><repeat direction=\"forward\"/></barline>");
        }
        if (primaryMeasure.getTempoText() != null && primaryMeasure.getTempoText().length() > 0) {
            body.append(buildWordsDirectionXml(primaryMeasure.getTempoText(), "above", primaryMeasure.getTempoBpm(),
                    null));
        } else if (primaryMeasure.getTempoBpm() != null) {
            body.append("<direction><direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>")
                    .append(primaryMeasure.getTempoBpm().intValue())
                    .append("</per-minute></metronome></direction-type><sound tempo=\"")
                    .append(primaryMeasure.getTempoBpm().intValue()).append("\"/></direction>");
        }
        return body.toString();
    }

    public static MuseImportedPartVoiceIdResolver buildMuseImportedPartVoiceIdResolver(ParsedMuseScorePart part) {
        MuseImportedPartVoiceIdResolver resolver = new MuseImportedPartVoiceIdResolver();
        if (part != null) {
            for (int staffIndex = 0; staffIndex < part.getStaffs().size(); staffIndex++) {
                int staffNo = staffIndex + 1;
                java.util.Set<Integer> voices = new java.util.TreeSet<Integer>();
                for (ParsedMuseScoreMeasure measure : part.getStaffs().get(staffIndex).getMeasures()) {
                    for (TimedEvent event : measure.getEvents()) {
                        voices.add(Integer.valueOf(Math.max(1, event.getVoice())));
                    }
                }
                if (voices.isEmpty()) {
                    voices.add(Integer.valueOf(1));
                }
                for (Integer voiceNo : voices) {
                    resolver.resolve(staffNo, voiceNo.intValue());
                }
            }
        }
        return resolver;
    }

    private static boolean stringEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static String normalizeToken(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToEmpty(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private static MusicXmlNumberSet parseMusicXmlTypedNumbers(Collection<MusicXmlTypedNumber> typedNumbers) {
        List<Integer> starts = new ArrayList<Integer>();
        List<Integer> stops = new ArrayList<Integer>();
        if (typedNumbers != null) {
            for (MusicXmlTypedNumber typedNumber : typedNumbers) {
                if (typedNumber == null) {
                    continue;
                }
                String type = typedNumber.getType() == null ? ""
                        : typedNumber.getType().trim().toLowerCase(Locale.ROOT);
                int number = typedNumber.parsePositiveNumberOrOne();
                if ("start".equals(type)) {
                    starts.add(Integer.valueOf(number));
                }
                if ("stop".equals(type)) {
                    stops.add(Integer.valueOf(number));
                }
            }
        }
        return new MusicXmlNumberSet(starts, stops);
    }

    private static int gcdPositive(int a, int b) {
        int x = Math.max(1, Math.abs(Math.round(a)));
        int y = Math.max(1, Math.abs(Math.round(b)));
        while (y != 0) {
            int t = x % y;
            x = y;
            y = t;
        }
        return Math.max(1, x);
    }

    private static String xmlEscape(String value) {
        String text = value == null ? "" : value;
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String joinWithSemicolon(List<String> values) {
        StringBuilder out = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                out.append(";");
            }
            out.append(values.get(index));
        }
        return out.toString();
    }

    private static List<String> mergeUniqueSubtypes(Collection<String> base, Collection<String> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return base == null ? null : new ArrayList<String>(base);
        }
        java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<String>();
        if (base != null) {
            merged.addAll(base);
        }
        merged.addAll(incoming);
        return new ArrayList<String>(merged);
    }

    private static String encodeUriComponent(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder out = new StringBuilder();
        for (byte raw : bytes) {
            int b = raw & 0xff;
            char ch = (char) b;
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')
                    || ch == '-' || ch == '_' || ch == '.' || ch == '!' || ch == '~' || ch == '*'
                    || ch == '\'' || ch == '(' || ch == ')') {
                out.append(ch);
            } else {
                out.append('%').append(String.format(Locale.ROOT, "%02X", Integer.valueOf(b)));
            }
        }
        return out.toString();
    }

    public static String museTpcToAccidentalText(String tpcRaw) {
        if (tpcRaw == null) {
            return null;
        }
        int tpc;
        try {
            tpc = Integer.parseInt(tpcRaw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
        int[] baseByStep = new int[] { 13, 14, 15, 16, 17, 18, 19 };
        for (int base : baseByStep) {
            int delta = tpc - base;
            if (delta % 7 != 0) {
                continue;
            }
            int alter = delta / 7;
            if (alter <= -2) {
                return "flat-flat";
            }
            if (alter == -1) {
                return "flat";
            }
            if (alter == 1) {
                return "sharp";
            }
            if (alter >= 2) {
                return "double-sharp";
            }
            return null;
        }
        return null;
    }

    public static final class TypeAndDots {
        private final String type;
        private final int dots;

        private TypeAndDots(String type, int dots) {
            this.type = type;
            this.dots = dots;
        }

        public String getType() {
            return type;
        }

        public int getDots() {
            return dots;
        }
    }

    public static final class MusicXmlTypedNumber {
        private final String type;
        private final String numberRaw;

        public MusicXmlTypedNumber(String type, String numberRaw) {
            this.type = type;
            this.numberRaw = numberRaw;
        }

        public String getType() {
            return type;
        }

        public String getNumberRaw() {
            return numberRaw;
        }

        private int parsePositiveNumberOrOne() {
            try {
                int parsed = Integer.parseInt(numberRaw == null ? "" : numberRaw.trim());
                return parsed > 0 ? Math.round(parsed) : 1;
            } catch (NumberFormatException ex) {
                return 1;
            }
        }
    }

    public static final class MusicXmlNumberSet {
        private final List<Integer> starts;
        private final List<Integer> stops;

        public MusicXmlNumberSet(Collection<Integer> starts, Collection<Integer> stops) {
            this.starts = starts == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(starts));
            this.stops = stops == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(stops));
        }

        public List<Integer> getStarts() {
            return starts;
        }

        public List<Integer> getStops() {
            return stops;
        }
    }

    public static final class MusicXmlOctaveShiftSource {
        private final String type;
        private final String size;

        public MusicXmlOctaveShiftSource(String type, String size) {
            this.type = type;
            this.size = size;
        }

        public String getType() {
            return type;
        }

        public String getSize() {
            return size;
        }
    }

    public static final class MusicXmlDirectionMarkPayload {
        private final int staffNo;
        private final int voiceNo;
        private final MusePendingDirectionMarks marks;

        public MusicXmlDirectionMarkPayload(int staffNo, int voiceNo, MusePendingDirectionMarks marks) {
            this.staffNo = Math.max(1, Math.round(staffNo));
            this.voiceNo = Math.max(1, Math.round(voiceNo));
            this.marks = marks == null ? new MusePendingDirectionMarks(null, 0, 0, 0) : marks;
        }

        public int getStaffNo() {
            return staffNo;
        }

        public int getVoiceNo() {
            return voiceNo;
        }

        public MusePendingDirectionMarks getMarks() {
            return marks;
        }
    }

    public static final class MusicXmlClefSource {
        private final String numberRaw;
        private final String sign;

        public MusicXmlClefSource(String numberRaw, String sign) {
            this.numberRaw = numberRaw;
            this.sign = sign;
        }

        public boolean hasNumber() {
            return numberRaw != null && numberRaw.trim().length() > 0;
        }

        public Integer getNumber() {
            if (!hasNumber()) {
                return null;
            }
            try {
                int parsed = Integer.parseInt(numberRaw.trim());
                return parsed > 0 ? Integer.valueOf(Math.round(parsed)) : null;
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        public String getSign() {
            return sign;
        }
    }

    public static final class MusicXmlMeasureClefSet {
        private final List<MusicXmlClefSource> clefs;

        public MusicXmlMeasureClefSet(Collection<MusicXmlClefSource> clefs) {
            this.clefs = clefs == null ? Collections.<MusicXmlClefSource>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MusicXmlClefSource>(clefs));
        }

        public List<MusicXmlClefSource> getClefs() {
            return clefs;
        }
    }

    public static final class MusicXmlPitchStaff {
        private final String step;
        private final String octave;
        private final String alter;
        private final String staff;
        private final boolean rest;

        public MusicXmlPitchStaff(String step, String octave, String alter, String staff, boolean rest) {
            this.step = step;
            this.octave = octave;
            this.alter = alter;
            this.staff = staff;
            this.rest = rest;
        }

        public String getStep() {
            return step;
        }

        public String getOctave() {
            return octave;
        }

        public String getAlter() {
            return alter;
        }

        public String getStaff() {
            return staff;
        }

        public boolean isRest() {
            return rest;
        }
    }

    public static final class MusicXmlDirectionWords {
        private final String text;
        private final String fontStyle;

        public MusicXmlDirectionWords(String text, String fontStyle) {
            this.text = text;
            this.fontStyle = fontStyle;
        }

        public String getText() {
            return text;
        }

        public String getFontStyle() {
            return fontStyle;
        }
    }

    public static final class MusicXmlDirectionSeedSource {
        private final String staff;
        private final String soundTempo;
        private final String metronomePerMinute;
        private final String beatUnit;
        private final int beatUnitDots;
        private final String soundDynamics;
        private final String soundDalsegno;
        private final String soundDaCapo;
        private final String soundFine;
        private final String soundToCoda;
        private final List<String> dynamicTags;
        private final boolean hasSegno;
        private final boolean hasCoda;
        private final List<MusicXmlDirectionWords> words;

        public MusicXmlDirectionSeedSource(String staff, String soundTempo, String metronomePerMinute,
                String beatUnit, int beatUnitDots, String soundDynamics, String soundDalsegno, String soundDaCapo,
                String soundFine, String soundToCoda, Collection<String> dynamicTags, boolean hasSegno,
                boolean hasCoda, Collection<MusicXmlDirectionWords> words) {
            this.staff = staff;
            this.soundTempo = soundTempo;
            this.metronomePerMinute = metronomePerMinute;
            this.beatUnit = beatUnit;
            this.beatUnitDots = beatUnitDots;
            this.soundDynamics = soundDynamics;
            this.soundDalsegno = soundDalsegno;
            this.soundDaCapo = soundDaCapo;
            this.soundFine = soundFine;
            this.soundToCoda = soundToCoda;
            this.dynamicTags = dynamicTags == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(dynamicTags));
            this.hasSegno = hasSegno;
            this.hasCoda = hasCoda;
            this.words = words == null ? Collections.<MusicXmlDirectionWords>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MusicXmlDirectionWords>(words));
        }

        public String getStaff() {
            return staff;
        }

        public String getSoundTempo() {
            return soundTempo;
        }

        public String getMetronomePerMinute() {
            return metronomePerMinute;
        }

        public String getBeatUnit() {
            return beatUnit;
        }

        public int getBeatUnitDots() {
            return beatUnitDots;
        }

        public String getSoundDynamics() {
            return soundDynamics;
        }

        public String getSoundDalsegno() {
            return soundDalsegno;
        }

        public String getSoundDaCapo() {
            return soundDaCapo;
        }

        public String getSoundFine() {
            return soundFine;
        }

        public String getSoundToCoda() {
            return soundToCoda;
        }

        public List<String> getDynamicTags() {
            return dynamicTags;
        }

        public boolean isHasSegno() {
            return hasSegno;
        }

        public boolean isHasCoda() {
            return hasCoda;
        }

        public List<MusicXmlDirectionWords> getWords() {
            return words;
        }
    }

    public static final class MuseDirectionSeed {
        private final String kind;
        private final double qps;
        private final String text;
        private final boolean followText;
        private final Boolean visible;
        private final String subtype;
        private final Integer velocity;
        private final boolean italic;
        private final String label;
        private final String jumpTo;
        private final String playUntil;
        private final String continueAt;

        private MuseDirectionSeed(String kind, double qps, String text, boolean followText, Boolean visible,
                String subtype, Integer velocity, boolean italic, String label, String jumpTo, String playUntil,
                String continueAt) {
            this.kind = kind;
            this.qps = qps;
            this.text = text;
            this.followText = followText;
            this.visible = visible;
            this.subtype = subtype;
            this.velocity = velocity;
            this.italic = italic;
            this.label = label;
            this.jumpTo = jumpTo;
            this.playUntil = playUntil;
            this.continueAt = continueAt;
        }

        public static MuseDirectionSeed tempo(double qps, String text, boolean followText, Boolean visible) {
            return new MuseDirectionSeed("tempo", qps, text, followText, visible, null, null, false, null, null,
                    null, null);
        }

        public static MuseDirectionSeed dynamic(String subtype, Integer velocity) {
            return new MuseDirectionSeed("dynamic", 0.0d, null, false, null, subtype, velocity, false, null, null,
                    null, null);
        }

        public static MuseDirectionSeed expression(String text, boolean italic) {
            return new MuseDirectionSeed("expression", 0.0d, text, false, null, null, null, italic, null, null,
                    null, null);
        }

        public static MuseDirectionSeed marker(String subtype, String label) {
            return new MuseDirectionSeed("marker", 0.0d, null, false, null, subtype, null, false, label, null,
                    null, null);
        }

        public static MuseDirectionSeed jump(String text, String jumpTo, String playUntil, String continueAt) {
            return new MuseDirectionSeed("jump", 0.0d, text, false, null, null, null, false, null, jumpTo,
                    playUntil, continueAt);
        }

        public String getKind() {
            return kind;
        }

        public double getQps() {
            return qps;
        }

        public String getText() {
            return text;
        }

        public boolean isFollowText() {
            return followText;
        }

        public Boolean getVisible() {
            return visible;
        }

        public String getSubtype() {
            return subtype;
        }

        public Integer getVelocity() {
            return velocity;
        }

        public boolean isItalic() {
            return italic;
        }

        public String getLabel() {
            return label;
        }

        public String getJumpTo() {
            return jumpTo;
        }

        public String getPlayUntil() {
            return playUntil;
        }

        public String getContinueAt() {
            return continueAt;
        }
    }

    public static final class MuseScoreExportMeasureContext {
        private final int measureSourceDivisions;
        private final int effectiveMeasureBeats;
        private final int effectiveMeasureBeatType;
        private final String measureTimeSymbol;
        private final int measureFifths;
        private final String targetClef;
        private final String measureClefType;
        private final boolean shouldWriteClef;
        private final boolean shouldWriteTime;
        private final boolean shouldWriteKey;
        private final boolean needsDoubleBarlineAtMeasureStart;
        private final List<MuseDirectionSeed> directionSeeds;
        private final boolean hasStartRepeat;
        private final boolean hasEndRepeat;

        public MuseScoreExportMeasureContext(int effectiveMeasureBeats, int effectiveMeasureBeatType,
                String measureTimeSymbol, int measureFifths, String targetClef, String measureClefType,
                boolean shouldWriteClef, boolean shouldWriteTime, boolean shouldWriteKey,
                boolean needsDoubleBarlineAtMeasureStart, Collection<MuseDirectionSeed> directionSeeds,
                boolean hasStartRepeat) {
            this(effectiveMeasureBeats, effectiveMeasureBeatType, measureTimeSymbol, measureFifths, targetClef,
                    measureClefType, shouldWriteClef, shouldWriteTime, shouldWriteKey,
                    needsDoubleBarlineAtMeasureStart, directionSeeds, hasStartRepeat, false);
        }

        public MuseScoreExportMeasureContext(int effectiveMeasureBeats, int effectiveMeasureBeatType,
                String measureTimeSymbol, int measureFifths, String targetClef, String measureClefType,
                boolean shouldWriteClef, boolean shouldWriteTime, boolean shouldWriteKey,
                boolean needsDoubleBarlineAtMeasureStart, Collection<MuseDirectionSeed> directionSeeds,
                boolean hasStartRepeat, boolean hasEndRepeat) {
            this(0, effectiveMeasureBeats, effectiveMeasureBeatType, measureTimeSymbol, measureFifths, targetClef,
                    measureClefType, shouldWriteClef, shouldWriteTime, shouldWriteKey,
                    needsDoubleBarlineAtMeasureStart, directionSeeds, hasStartRepeat, hasEndRepeat);
        }

        public MuseScoreExportMeasureContext(int measureSourceDivisions, int effectiveMeasureBeats,
                int effectiveMeasureBeatType, String measureTimeSymbol, int measureFifths, String targetClef,
                String measureClefType, boolean shouldWriteClef, boolean shouldWriteTime, boolean shouldWriteKey,
                boolean needsDoubleBarlineAtMeasureStart, Collection<MuseDirectionSeed> directionSeeds,
                boolean hasStartRepeat, boolean hasEndRepeat) {
            this.measureSourceDivisions = measureSourceDivisions;
            this.effectiveMeasureBeats = effectiveMeasureBeats;
            this.effectiveMeasureBeatType = effectiveMeasureBeatType;
            this.measureTimeSymbol = measureTimeSymbol;
            this.measureFifths = measureFifths;
            this.targetClef = targetClef;
            this.measureClefType = measureClefType;
            this.shouldWriteClef = shouldWriteClef;
            this.shouldWriteTime = shouldWriteTime;
            this.shouldWriteKey = shouldWriteKey;
            this.needsDoubleBarlineAtMeasureStart = needsDoubleBarlineAtMeasureStart;
            this.directionSeeds = directionSeeds == null ? Collections.<MuseDirectionSeed>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MuseDirectionSeed>(directionSeeds));
            this.hasStartRepeat = hasStartRepeat;
            this.hasEndRepeat = hasEndRepeat;
        }

        public int getMeasureSourceDivisions() {
            return measureSourceDivisions;
        }

        public int getEffectiveMeasureBeats() {
            return effectiveMeasureBeats;
        }

        public int getEffectiveMeasureBeatType() {
            return effectiveMeasureBeatType;
        }

        public String getMeasureTimeSymbol() {
            return measureTimeSymbol;
        }

        public int getMeasureFifths() {
            return measureFifths;
        }

        public String getTargetClef() {
            return targetClef;
        }

        public String getMeasureClefType() {
            return measureClefType;
        }

        public boolean isShouldWriteClef() {
            return shouldWriteClef;
        }

        public boolean isShouldWriteTime() {
            return shouldWriteTime;
        }

        public boolean isShouldWriteKey() {
            return shouldWriteKey;
        }

        public boolean isNeedsDoubleBarlineAtMeasureStart() {
            return needsDoubleBarlineAtMeasureStart;
        }

        public List<MuseDirectionSeed> getDirectionSeeds() {
            return directionSeeds;
        }

        public boolean isHasStartRepeat() {
            return hasStartRepeat;
        }

        public boolean isHasEndRepeat() {
            return hasEndRepeat;
        }
    }

    public static final class MuseScoreExportMeasureContextResult {
        private final MuseScoreExportMeasureContext measureContext;
        private final int capacityDiv;
        private final int renderCapacityDiv;
        private final String lenAttr;
        private final List<Integer> voiceNos;
        private final int usedDiv;

        public MuseScoreExportMeasureContextResult(MuseScoreExportMeasureContext measureContext, int capacityDiv,
                int renderCapacityDiv, String lenAttr, Collection<Integer> voiceNos, int usedDiv) {
            this.measureContext = measureContext;
            this.capacityDiv = Math.max(1, capacityDiv);
            this.renderCapacityDiv = Math.max(1, renderCapacityDiv);
            this.lenAttr = lenAttr;
            this.voiceNos = voiceNos == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(voiceNos));
            this.usedDiv = Math.max(0, usedDiv);
        }

        public MuseScoreExportMeasureContext getMeasureContext() {
            return measureContext;
        }

        public int getCapacityDiv() {
            return capacityDiv;
        }

        public int getRenderCapacityDiv() {
            return renderCapacityDiv;
        }

        public String getLenAttr() {
            return lenAttr;
        }

        public List<Integer> getVoiceNos() {
            return voiceNos;
        }

        public int getUsedDiv() {
            return usedDiv;
        }
    }

    public static final class MuseScoreExportStaffState {
        private final int currentSourceDivisions;
        private final int currentBeats;
        private final int currentBeatType;
        private final String currentTimeSymbol;
        private final int currentFifths;
        private final String currentClef;

        public MuseScoreExportStaffState(int currentSourceDivisions, int currentBeats, int currentBeatType,
                String currentTimeSymbol, int currentFifths, String currentClef) {
            this.currentSourceDivisions = Math.max(1, currentSourceDivisions);
            this.currentBeats = Math.max(1, currentBeats);
            this.currentBeatType = Math.max(1, currentBeatType);
            this.currentTimeSymbol = currentTimeSymbol;
            this.currentFifths = Math.max(-7, Math.min(7, currentFifths));
            this.currentClef = currentClef;
        }

        public int getCurrentSourceDivisions() {
            return currentSourceDivisions;
        }

        public int getCurrentBeats() {
            return currentBeats;
        }

        public int getCurrentBeatType() {
            return currentBeatType;
        }

        public String getCurrentTimeSymbol() {
            return currentTimeSymbol;
        }

        public int getCurrentFifths() {
            return currentFifths;
        }

        public String getCurrentClef() {
            return currentClef;
        }
    }

    public static final class MuseScoreExportStaffMeasure {
        private final MuseScoreExportMeasureContext measureContext;
        private final String lenAttr;
        private final int renderCapacityDiv;
        private final List<Integer> voiceNos;
        private final Map<Integer, List<MuseVoiceEvent>> eventsByVoice;

        public MuseScoreExportStaffMeasure(MuseScoreExportMeasureContext measureContext, String lenAttr,
                int renderCapacityDiv, Collection<Integer> voiceNos,
                Map<Integer, List<MuseVoiceEvent>> eventsByVoice) {
            this.measureContext = measureContext;
            this.lenAttr = lenAttr;
            this.renderCapacityDiv = Math.max(0, renderCapacityDiv);
            this.voiceNos = voiceNos == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(voiceNos));
            this.eventsByVoice = new java.util.LinkedHashMap<Integer, List<MuseVoiceEvent>>();
            if (eventsByVoice != null) {
                for (Map.Entry<Integer, List<MuseVoiceEvent>> entry : eventsByVoice.entrySet()) {
                    this.eventsByVoice.put(entry.getKey(),
                            entry.getValue() == null ? Collections.<MuseVoiceEvent>emptyList()
                                    : Collections.unmodifiableList(new ArrayList<MuseVoiceEvent>(entry.getValue())));
                }
            }
        }

        public MuseScoreExportMeasureContext getMeasureContext() {
            return measureContext;
        }

        public String getLenAttr() {
            return lenAttr;
        }

        public int getRenderCapacityDiv() {
            return renderCapacityDiv;
        }

        public List<Integer> getVoiceNos() {
            return voiceNos;
        }

        public Map<Integer, List<MuseVoiceEvent>> getEventsByVoice() {
            return eventsByVoice;
        }
    }

    public static final class MuseScoreExportPartName {
        private final String name;
        private final String abbreviation;

        public MuseScoreExportPartName(String name, String abbreviation) {
            this.name = name;
            this.abbreviation = abbreviation;
        }

        public String getName() {
            return name;
        }

        public String getAbbreviation() {
            return abbreviation;
        }
    }

    public static final class MuseScoreExportPartNameEntry {
        private final String id;
        private final String name;
        private final String abbreviation;

        public MuseScoreExportPartNameEntry(String id, String name, String abbreviation) {
            this.id = id;
            this.name = name;
            this.abbreviation = abbreviation;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getAbbreviation() {
            return abbreviation;
        }
    }

    public static final class MuseScoreExportCredit {
        private final String type;
        private final String words;

        public MuseScoreExportCredit(String type, String words) {
            this.type = type;
            this.words = words;
        }

        public String getType() {
            return type;
        }

        public String getWords() {
            return words;
        }
    }

    public static final class MuseScoreExportCreator {
        private final String type;
        private final String text;

        public MuseScoreExportCreator(String type, String text) {
            this.type = type;
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public String getText() {
            return text;
        }
    }

    public static final class MuseScoreExportPartIdentity {
        private final String partName;
        private final String partAbbreviation;

        public MuseScoreExportPartIdentity(String partName, String partAbbreviation) {
            this.partName = partName == null ? "" : partName;
            this.partAbbreviation = partAbbreviation == null ? "" : partAbbreviation;
        }

        public String getPartName() {
            return partName;
        }

        public String getPartAbbreviation() {
            return partAbbreviation;
        }
    }

    public static final class MuseScoreExportPartScaffold {
        private final List<Integer> staffIds;
        private final Transpose partTranspose;
        private final Map<Integer, String> initialClefByStaff;
        private final String partDefBodyXml;

        public MuseScoreExportPartScaffold(Collection<Integer> staffIds, Transpose partTranspose,
                Map<Integer, String> initialClefByStaff, String partDefBodyXml) {
            this.staffIds = staffIds == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(staffIds));
            this.partTranspose = partTranspose;
            this.initialClefByStaff = new java.util.LinkedHashMap<Integer, String>();
            if (initialClefByStaff != null) {
                this.initialClefByStaff.putAll(initialClefByStaff);
            }
            this.partDefBodyXml = partDefBodyXml == null ? "" : partDefBodyXml;
        }

        public List<Integer> getStaffIds() {
            return staffIds;
        }

        public Transpose getPartTranspose() {
            return partTranspose;
        }

        public Map<Integer, String> getInitialClefByStaff() {
            return initialClefByStaff;
        }

        public String getPartDefBodyXml() {
            return partDefBodyXml;
        }
    }

    public static final class MuseScoreExportPartResult {
        private final int nextStaffId;
        private final String partDefXml;
        private final List<String> staffsXml;

        public MuseScoreExportPartResult(int nextStaffId, String partDefXml, Collection<String> staffsXml) {
            this.nextStaffId = nextStaffId;
            this.partDefXml = partDefXml == null ? "" : partDefXml;
            this.staffsXml = staffsXml == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(staffsXml));
        }

        public int getNextStaffId() {
            return nextStaffId;
        }

        public String getPartDefXml() {
            return partDefXml;
        }

        public List<String> getStaffsXml() {
            return staffsXml;
        }
    }

    public static final class MuseScoreExportDocumentBody {
        private final List<String> partDefs;
        private final List<String> staffsXml;

        public MuseScoreExportDocumentBody(Collection<String> partDefs, Collection<String> staffsXml) {
            this.partDefs = partDefs == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(partDefs));
            this.staffsXml = staffsXml == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(staffsXml));
        }

        public List<String> getPartDefs() {
            return partDefs;
        }

        public List<String> getStaffsXml() {
            return staffsXml;
        }
    }

    public static final class MuseScoreExportMeasureVoiceXml {
        private final String xml;
        private final String targetClef;

        public MuseScoreExportMeasureVoiceXml(String xml, String targetClef) {
            this.xml = xml == null ? "" : xml;
            this.targetClef = targetClef;
        }

        public String getXml() {
            return xml;
        }

        public String getTargetClef() {
            return targetClef;
        }
    }

    public static final class MuseScoreExportSlurState {
        private int nextSlurId;
        private final Map<String, List<Integer>> slurActiveIdsBySource;

        public MuseScoreExportSlurState() {
            this(1, null);
        }

        public MuseScoreExportSlurState(int nextSlurId, Map<String, List<Integer>> slurActiveIdsBySource) {
            this.nextSlurId = Math.max(1, nextSlurId);
            this.slurActiveIdsBySource = new java.util.LinkedHashMap<String, List<Integer>>();
            if (slurActiveIdsBySource != null) {
                for (Map.Entry<String, List<Integer>> entry : slurActiveIdsBySource.entrySet()) {
                    this.slurActiveIdsBySource.put(entry.getKey(), new ArrayList<Integer>(entry.getValue()));
                }
            }
        }

        public int getNextSlurId() {
            return nextSlurId;
        }

        public void setNextSlurId(int nextSlurId) {
            this.nextSlurId = Math.max(1, nextSlurId);
        }

        public Map<String, List<Integer>> getSlurActiveIdsBySource() {
            return slurActiveIdsBySource;
        }
    }

    public static final class MuseScoreExportSlurFractions {
        private final List<String> slurStartFractions;
        private final List<String> slurStopFractions;

        public MuseScoreExportSlurFractions(Collection<String> slurStartFractions,
                Collection<String> slurStopFractions) {
            this.slurStartFractions = slurStartFractions == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(slurStartFractions));
            this.slurStopFractions = slurStopFractions == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(slurStopFractions));
        }

        public List<String> getSlurStartFractions() {
            return slurStartFractions;
        }

        public List<String> getSlurStopFractions() {
            return slurStopFractions;
        }
    }

    public static final class MuseScoreExportTupletRefState {
        private int nextTupletRefNo;
        private final Map<Integer, String> activeTupletRefByNumber;

        public MuseScoreExportTupletRefState() {
            this.nextTupletRefNo = 1;
            this.activeTupletRefByNumber = new java.util.LinkedHashMap<Integer, String>();
        }

        public int getNextTupletRefNo() {
            return nextTupletRefNo;
        }

        public void setNextTupletRefNo(int nextTupletRefNo) {
            this.nextTupletRefNo = Math.max(1, nextTupletRefNo);
        }

        public Map<Integer, String> getActiveTupletRefByNumber() {
            return activeTupletRefByNumber;
        }
    }

    public static final class MuseScoreExportTupletRef {
        private final String definitionXml;
        private final String tupletRefId;
        private final int displayDurationDiv;

        public MuseScoreExportTupletRef(String definitionXml, String tupletRefId, int displayDurationDiv) {
            this.definitionXml = definitionXml == null ? "" : definitionXml;
            this.tupletRefId = tupletRefId;
            this.displayDurationDiv = displayDurationDiv;
        }

        public String getDefinitionXml() {
            return definitionXml;
        }

        public String getTupletRefId() {
            return tupletRefId;
        }

        public int getDisplayDurationDiv() {
            return displayDurationDiv;
        }
    }

    public static final class MuseScoreExportMetadata {
        private final String title;
        private final String subtitle;
        private final String composer;
        private final String arranger;
        private final String lyricist;
        private final String translator;
        private final String rights;
        private final String workNumber;
        private final String movementTitle;
        private final String movementNumber;
        private final String creationDate;

        public MuseScoreExportMetadata(String title, String subtitle, String composer, String arranger,
                String lyricist, String translator, String rights, String workNumber, String movementTitle,
                String movementNumber, String creationDate) {
            this.title = title;
            this.subtitle = subtitle;
            this.composer = composer;
            this.arranger = arranger;
            this.lyricist = lyricist;
            this.translator = translator;
            this.rights = rights;
            this.workNumber = workNumber;
            this.movementTitle = movementTitle;
            this.movementNumber = movementNumber;
            this.creationDate = creationDate;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public String getComposer() {
            return composer;
        }

        public String getArranger() {
            return arranger;
        }

        public String getLyricist() {
            return lyricist;
        }

        public String getTranslator() {
            return translator;
        }

        public String getRights() {
            return rights;
        }

        public String getWorkNumber() {
            return workNumber;
        }

        public String getMovementTitle() {
            return movementTitle;
        }

        public String getMovementNumber() {
            return movementNumber;
        }

        public String getCreationDate() {
            return creationDate;
        }
    }

    public static final class MuseScoreExportNote {
        private final int midi;
        private final boolean tieStart;
        private final boolean tieStop;
        private final String accidentalSubtype;
        private final String fingeringText;
        private final Integer stringNumber;

        public MuseScoreExportNote(int midi, boolean tieStart, boolean tieStop, String accidentalSubtype,
                String fingeringText, Integer stringNumber) {
            this.midi = midi;
            this.tieStart = tieStart;
            this.tieStop = tieStop;
            this.accidentalSubtype = accidentalSubtype;
            this.fingeringText = fingeringText;
            this.stringNumber = stringNumber;
        }

        public int getMidi() {
            return midi;
        }

        public boolean isTieStart() {
            return tieStart;
        }

        public boolean isTieStop() {
            return tieStop;
        }

        public String getAccidentalSubtype() {
            return accidentalSubtype;
        }

        public String getFingeringText() {
            return fingeringText;
        }

        public Integer getStringNumber() {
            return stringNumber;
        }
    }

    public static final class MuseScoreExportChord {
        private final int durationDiv;
        private final int displayDurationDiv;
        private final int divisions;
        private final List<MuseScoreExportNote> notes;
        private final List<String> slurStartFractions;
        private final List<String> slurStopFractions;
        private final List<String> articulationSubtypes;
        private final boolean trillMarkOnly;
        private final List<Integer> trillStarts;
        private final List<Integer> trillStops;
        private final String tupletRefId;
        private final List<String> ottavaStartSubtypes;
        private final int ottavaStopCount;
        private final boolean grace;
        private final boolean graceSlash;

        public MuseScoreExportChord(int durationDiv, int displayDurationDiv, int divisions,
                Collection<MuseScoreExportNote> notes, Collection<String> slurStartFractions,
                Collection<String> slurStopFractions, Collection<String> articulationSubtypes, boolean trillMarkOnly,
                Collection<Integer> trillStarts, Collection<Integer> trillStops, String tupletRefId,
                Collection<String> ottavaStartSubtypes, int ottavaStopCount, boolean grace, boolean graceSlash) {
            this.durationDiv = durationDiv;
            this.displayDurationDiv = displayDurationDiv;
            this.divisions = divisions;
            this.notes = notes == null ? Collections.<MuseScoreExportNote>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MuseScoreExportNote>(notes));
            this.slurStartFractions = slurStartFractions == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(slurStartFractions));
            this.slurStopFractions = slurStopFractions == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(slurStopFractions));
            this.articulationSubtypes = articulationSubtypes == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(articulationSubtypes));
            this.trillMarkOnly = trillMarkOnly;
            this.trillStarts = trillStarts == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(trillStarts));
            this.trillStops = trillStops == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(trillStops));
            this.tupletRefId = tupletRefId;
            this.ottavaStartSubtypes = ottavaStartSubtypes == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(ottavaStartSubtypes));
            this.ottavaStopCount = ottavaStopCount;
            this.grace = grace;
            this.graceSlash = graceSlash;
        }

        public int getDurationDiv() {
            return durationDiv;
        }

        public int getDisplayDurationDiv() {
            return displayDurationDiv;
        }

        public int getDivisions() {
            return divisions;
        }

        public List<MuseScoreExportNote> getNotes() {
            return notes;
        }

        public List<String> getSlurStartFractions() {
            return slurStartFractions;
        }

        public List<String> getSlurStopFractions() {
            return slurStopFractions;
        }

        public List<String> getArticulationSubtypes() {
            return articulationSubtypes;
        }

        public boolean isTrillMarkOnly() {
            return trillMarkOnly;
        }

        public List<Integer> getTrillStarts() {
            return trillStarts;
        }

        public List<Integer> getTrillStops() {
            return trillStops;
        }

        public String getTupletRefId() {
            return tupletRefId;
        }

        public List<String> getOttavaStartSubtypes() {
            return ottavaStartSubtypes;
        }

        public int getOttavaStopCount() {
            return ottavaStopCount;
        }

        public boolean isGrace() {
            return grace;
        }

        public boolean isGraceSlash() {
            return graceSlash;
        }
    }

    public static final class MusePendingDirectionMarks {
        private final List<String> ottavaStartSubtypes;
        private int ottavaStopCount;
        private int repeatForwardCount;
        private int repeatBackwardCount;

        public MusePendingDirectionMarks(Collection<String> ottavaStartSubtypes, int ottavaStopCount,
                int repeatForwardCount, int repeatBackwardCount) {
            this.ottavaStartSubtypes = ottavaStartSubtypes == null ? new ArrayList<String>()
                    : new ArrayList<String>(ottavaStartSubtypes);
            this.ottavaStopCount = ottavaStopCount;
            this.repeatForwardCount = repeatForwardCount;
            this.repeatBackwardCount = repeatBackwardCount;
        }

        public List<String> getOttavaStartSubtypes() {
            return ottavaStartSubtypes;
        }

        public int getOttavaStopCount() {
            return ottavaStopCount;
        }

        public void setOttavaStopCount(int ottavaStopCount) {
            this.ottavaStopCount = ottavaStopCount;
        }

        public int getRepeatForwardCount() {
            return repeatForwardCount;
        }

        public void setRepeatForwardCount(int repeatForwardCount) {
            this.repeatForwardCount = repeatForwardCount;
        }

        public int getRepeatBackwardCount() {
            return repeatBackwardCount;
        }

        public void setRepeatBackwardCount(int repeatBackwardCount) {
            this.repeatBackwardCount = repeatBackwardCount;
        }
    }

    public static final class MusePendingDirectionMarkEntry {
        private final int staffNo;
        private final int voiceNo;
        private final int atDiv;
        private MusePendingDirectionMarks marks;

        public MusePendingDirectionMarkEntry(int staffNo, int voiceNo, int atDiv, MusePendingDirectionMarks marks) {
            this.staffNo = staffNo;
            this.voiceNo = voiceNo;
            this.atDiv = atDiv;
            this.marks = marks == null ? new MusePendingDirectionMarks(null, 0, 0, 0) : marks;
        }

        public int getStaffNo() {
            return staffNo;
        }

        public int getVoiceNo() {
            return voiceNo;
        }

        public int getAtDiv() {
            return atDiv;
        }

        public MusePendingDirectionMarks getMarks() {
            return marks;
        }

        public void setMarks(MusePendingDirectionMarks marks) {
            this.marks = marks == null ? new MusePendingDirectionMarks(null, 0, 0, 0) : marks;
        }
    }

    public static final class MuseVoiceEvent {
        private final int atDiv;
        private final int durationDiv;
        private final List<MuseScoreExportNote> pitches;
        private final List<Integer> tupletStarts;
        private final List<Integer> tupletStops;
        private final TimeModification tupletTimeModification;
        private final List<Integer> slurStarts;
        private final List<Integer> slurStops;
        private final List<String> articulationSubtypes;
        private final boolean trillMarkOnly;
        private final List<Integer> trillStarts;
        private final List<Integer> trillStops;
        private final boolean grace;
        private final boolean graceSlash;
        private List<String> ottavaStartSubtypes;
        private int ottavaStopCount;
        private boolean repeatForwardAtStart;
        private boolean repeatBackwardAtStart;

        public MuseVoiceEvent(int atDiv, int durationDiv, Collection<MuseScoreExportNote> pitches,
                Collection<String> ottavaStartSubtypes, int ottavaStopCount, boolean repeatForwardAtStart,
                boolean repeatBackwardAtStart) {
            this(atDiv, durationDiv, pitches, null, null, null, null, null, null, false, null, null,
                    ottavaStartSubtypes, ottavaStopCount, false, false, repeatForwardAtStart,
                    repeatBackwardAtStart);
        }

        public MuseVoiceEvent(int atDiv, int durationDiv, Collection<MuseScoreExportNote> pitches,
                Collection<Integer> tupletStarts, Collection<Integer> tupletStops,
                TimeModification tupletTimeModification, Collection<Integer> slurStarts,
                Collection<Integer> slurStops, Collection<String> articulationSubtypes, boolean trillMarkOnly,
                Collection<Integer> trillStarts, Collection<Integer> trillStops,
                Collection<String> ottavaStartSubtypes, int ottavaStopCount, boolean grace, boolean graceSlash,
                boolean repeatForwardAtStart, boolean repeatBackwardAtStart) {
            this.atDiv = atDiv;
            this.durationDiv = durationDiv;
            this.pitches = pitches == null ? null : Collections.unmodifiableList(new ArrayList<MuseScoreExportNote>(
                    pitches));
            this.tupletStarts = tupletStarts == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(tupletStarts));
            this.tupletStops = tupletStops == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(tupletStops));
            this.tupletTimeModification = tupletTimeModification;
            this.slurStarts = slurStarts == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(slurStarts));
            this.slurStops = slurStops == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(slurStops));
            this.articulationSubtypes = articulationSubtypes == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(articulationSubtypes));
            this.trillMarkOnly = trillMarkOnly;
            this.trillStarts = trillStarts == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(trillStarts));
            this.trillStops = trillStops == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(trillStops));
            this.ottavaStartSubtypes = ottavaStartSubtypes == null ? null : new ArrayList<String>(
                    ottavaStartSubtypes);
            this.ottavaStopCount = ottavaStopCount;
            this.grace = grace;
            this.graceSlash = graceSlash;
            this.repeatForwardAtStart = repeatForwardAtStart;
            this.repeatBackwardAtStart = repeatBackwardAtStart;
        }

        public int getAtDiv() {
            return atDiv;
        }

        public int getDurationDiv() {
            return durationDiv;
        }

        public List<MuseScoreExportNote> getPitches() {
            return pitches;
        }

        public List<Integer> getTupletStarts() {
            return tupletStarts;
        }

        public List<Integer> getTupletStops() {
            return tupletStops;
        }

        public TimeModification getTupletTimeModification() {
            return tupletTimeModification;
        }

        public List<Integer> getSlurStarts() {
            return slurStarts;
        }

        public List<Integer> getSlurStops() {
            return slurStops;
        }

        public List<String> getArticulationSubtypes() {
            return articulationSubtypes;
        }

        public boolean isTrillMarkOnly() {
            return trillMarkOnly;
        }

        public List<Integer> getTrillStarts() {
            return trillStarts;
        }

        public List<Integer> getTrillStops() {
            return trillStops;
        }

        public List<String> getOttavaStartSubtypes() {
            return ottavaStartSubtypes;
        }

        public void setOttavaStartSubtypes(List<String> ottavaStartSubtypes) {
            this.ottavaStartSubtypes = ottavaStartSubtypes;
        }

        public int getOttavaStopCount() {
            return ottavaStopCount;
        }

        public void setOttavaStopCount(int ottavaStopCount) {
            this.ottavaStopCount = ottavaStopCount;
        }

        public boolean isGrace() {
            return grace;
        }

        public boolean isGraceSlash() {
            return graceSlash;
        }

        public boolean isRepeatForwardAtStart() {
            return repeatForwardAtStart;
        }

        public void setRepeatForwardAtStart(boolean repeatForwardAtStart) {
            this.repeatForwardAtStart = repeatForwardAtStart;
        }

        public boolean isRepeatBackwardAtStart() {
            return repeatBackwardAtStart;
        }

        public void setRepeatBackwardAtStart(boolean repeatBackwardAtStart) {
            this.repeatBackwardAtStart = repeatBackwardAtStart;
        }
    }

    public static final class RepeatFlags {
        private final boolean repeatForward;
        private final boolean repeatBackward;

        private RepeatFlags(boolean repeatForward, boolean repeatBackward) {
            this.repeatForward = repeatForward;
            this.repeatBackward = repeatBackward;
        }

        public boolean isRepeatForward() {
            return repeatForward;
        }

        public boolean isRepeatBackward() {
            return repeatBackward;
        }
    }

    public static final class NotationTag {
        private final String group;
        private final String tag;

        private NotationTag(String group, String tag) {
            this.group = group;
            this.tag = tag;
        }

        public String getGroup() {
            return group;
        }

        public String getTag() {
            return tag;
        }
    }

    public static final class Transpose {
        private final Integer diatonic;
        private final Integer chromatic;

        public Transpose(Integer diatonic, Integer chromatic) {
            this.diatonic = diatonic;
            this.chromatic = chromatic;
        }

        public Integer getDiatonic() {
            return diatonic;
        }

        public Integer getChromatic() {
            return chromatic;
        }
    }

    public static final class Clef {
        private final String sign;
        private final int line;

        private Clef(String sign, int line) {
            this.sign = sign;
            this.line = line;
        }

        public String getSign() {
            return sign;
        }

        public int getLine() {
            return line;
        }
    }

    public static final class TimeModification {
        private final int actualNotes;
        private final int normalNotes;

        public TimeModification(int actualNotes, int normalNotes) {
            this.actualNotes = actualNotes;
            this.normalNotes = normalNotes;
        }

        public int getActualNotes() {
            return actualNotes;
        }

        public int getNormalNotes() {
            return normalNotes;
        }
    }

    public static final class TupletStart {
        private final int number;
        private final String showNumber;
        private final String bracket;

        public TupletStart(int number, String showNumber, String bracket) {
            this.number = number;
            this.showNumber = showNumber;
            this.bracket = bracket;
        }

        public int getNumber() {
            return number;
        }

        public String getShowNumber() {
            return showNumber;
        }

        public String getBracket() {
            return bracket;
        }
    }

    public static final class TupletMusicXml {
        private final String timeModificationXml;
        private final List<String> notationItems;

        private TupletMusicXml(String timeModificationXml, List<String> notationItems) {
            this.timeModificationXml = timeModificationXml;
            this.notationItems = Collections.unmodifiableList(new ArrayList<String>(notationItems));
        }

        public String getTimeModificationXml() {
            return timeModificationXml;
        }

        public List<String> getNotationItems() {
            return notationItems;
        }
    }

    public static final class TimedEvent {
        private final int durationDiv;
        private final boolean grace;
        private final boolean tupletTimeModification;
        private final int voice;
        private final int atDiv;
        private final List<Integer> tupletStops;

        public TimedEvent(int durationDiv, boolean grace, boolean tupletTimeModification) {
            this(durationDiv, grace, tupletTimeModification, 1);
        }

        public TimedEvent(int durationDiv, boolean grace, boolean tupletTimeModification, int voice) {
            this(durationDiv, grace, tupletTimeModification, voice, 0);
        }

        public TimedEvent(int durationDiv, boolean grace, boolean tupletTimeModification, int voice, int atDiv) {
            this.durationDiv = durationDiv;
            this.grace = grace;
            this.tupletTimeModification = tupletTimeModification;
            this.voice = Math.max(1, voice);
            this.atDiv = Math.max(0, atDiv);
            this.tupletStops = new ArrayList<Integer>();
        }

        public int getDurationDiv() {
            return durationDiv;
        }

        public boolean isGrace() {
            return grace;
        }

        public boolean hasTupletTimeModification() {
            return tupletTimeModification;
        }

        public int getVoice() {
            return voice;
        }

        public int getAtDiv() {
            return atDiv;
        }

        public List<Integer> getTupletStops() {
            return tupletStops;
        }
    }

    public static final class MuseBeamEvent {
        private final boolean timed;
        private final boolean chord;
        private final boolean grace;
        private final int durationDiv;
        private final int levels;
        private final String explicitMode;

        public MuseBeamEvent(boolean timed, boolean chord, boolean grace, int durationDiv, int levels,
                String explicitMode) {
            this.timed = timed;
            this.chord = chord;
            this.grace = grace;
            this.durationDiv = Math.max(0, durationDiv);
            this.levels = Math.max(0, levels);
            String normalized = explicitMode == null ? "" : explicitMode.trim().toLowerCase(Locale.ROOT);
            this.explicitMode = "begin".equals(normalized) || "mid".equals(normalized) ? normalized : "";
        }

        public boolean isTimed() {
            return timed;
        }

        public boolean isChord() {
            return chord;
        }

        public boolean isGrace() {
            return grace;
        }

        public int getDurationDiv() {
            return durationDiv;
        }

        public int getLevels() {
            return levels;
        }

        public String getExplicitMode() {
            return explicitMode;
        }
    }

    private static final class BeamAssignment {
        private final String state;
        private final int levels;

        private BeamAssignment(String state, int levels) {
            this.state = state;
            this.levels = Math.max(0, levels);
        }
    }

    public static final class OttavaShift {
        private final int size;
        private final String shiftType;

        private OttavaShift(int size, String shiftType) {
            this.size = size;
            this.shiftType = shiftType;
        }

        public int getSize() {
            return size;
        }

        public String getShiftType() {
            return shiftType;
        }
    }

    public static final class OttavaState {
        private final int number;
        private final int size;
        private final String shiftType;

        public OttavaState(int number, int size, String shiftType) {
            this.number = number;
            this.size = size;
            this.shiftType = shiftType;
        }

        public int getNumber() {
            return number;
        }

        public int getSize() {
            return size;
        }

        public String getShiftType() {
            return shiftType;
        }
    }

    public static final class ResolvedMuseScoreImportOptions {
        private final boolean sourceMetadata;
        private final boolean debugMetadata;
        private final boolean normalizeCutTimeToTwoTwo;
        private final boolean applyImplicitBeams;

        private ResolvedMuseScoreImportOptions(boolean sourceMetadata, boolean debugMetadata,
                boolean normalizeCutTimeToTwoTwo, boolean applyImplicitBeams) {
            this.sourceMetadata = sourceMetadata;
            this.debugMetadata = debugMetadata;
            this.normalizeCutTimeToTwoTwo = normalizeCutTimeToTwoTwo;
            this.applyImplicitBeams = applyImplicitBeams;
        }

        public boolean isSourceMetadata() {
            return sourceMetadata;
        }

        public boolean isDebugMetadata() {
            return debugMetadata;
        }

        public boolean isNormalizeCutTimeToTwoTwo() {
            return normalizeCutTimeToTwoTwo;
        }

        public boolean isApplyImplicitBeams() {
            return applyImplicitBeams;
        }
    }

    public static final class TupletDescriptor {
        private final String id;
        private final int actualNotes;
        private final int normalNotes;
        private final String showNumber;
        private final String bracket;

        private TupletDescriptor(String id, int actualNotes, int normalNotes, String showNumber, String bracket) {
            this.id = id;
            this.actualNotes = actualNotes;
            this.normalNotes = normalNotes;
            this.showNumber = showNumber;
            this.bracket = bracket;
        }

        public String getId() {
            return id;
        }

        public int getActualNotes() {
            return actualNotes;
        }

        public int getNormalNotes() {
            return normalNotes;
        }

        public String getShowNumber() {
            return showNumber;
        }

        public String getBracket() {
            return bracket;
        }
    }

    public static final class TupletState {
        private final int actualNotes;
        private final int normalNotes;
        private final int number;
        private final String showNumber;
        private final String bracket;
        private boolean startPending;

        private TupletState(int actualNotes, int normalNotes, int number, String showNumber, String bracket,
                boolean startPending) {
            this.actualNotes = actualNotes;
            this.normalNotes = normalNotes;
            this.number = number;
            this.showNumber = showNumber;
            this.bracket = bracket;
            this.startPending = startPending;
        }

        public int getActualNotes() {
            return actualNotes;
        }

        public int getNormalNotes() {
            return normalNotes;
        }

        public int getNumber() {
            return number;
        }

        public String getShowNumber() {
            return showNumber;
        }

        public String getBracket() {
            return bracket;
        }

        public boolean isStartPending() {
            return startPending;
        }

        private void markStartConsumed() {
            startPending = false;
        }
    }

    public static final class FinalizedTupletRef {
        private final String activeTupletRefId;
        private final Integer endedTupletNumber;

        private FinalizedTupletRef(String activeTupletRefId, Integer endedTupletNumber) {
            this.activeTupletRefId = activeTupletRefId;
            this.endedTupletNumber = endedTupletNumber;
        }

        public String getActiveTupletRefId() {
            return activeTupletRefId;
        }

        public Integer getEndedTupletNumber() {
            return endedTupletNumber;
        }
    }

    public static final class MutableInt {
        private int current;

        public MutableInt(int current) {
            this.current = current;
        }

        public int getCurrent() {
            return current;
        }

        public void setCurrent(int current) {
            this.current = current;
        }
    }

    public static final class MuseScoreImportEventRouting {
        private final String tag;
        private final int voiceNo;
        private final int movedStaffNo;

        private MuseScoreImportEventRouting(String tag, int voiceNo, int movedStaffNo) {
            this.tag = tag;
            this.voiceNo = voiceNo;
            this.movedStaffNo = movedStaffNo;
        }

        public String getTag() {
            return tag;
        }

        public int getVoiceNo() {
            return voiceNo;
        }

        public int getMovedStaffNo() {
            return movedStaffNo;
        }
    }

    public static final class TieFlags {
        private final boolean tieStart;
        private final boolean tieStop;

        private TieFlags(boolean tieStart, boolean tieStop) {
            this.tieStart = tieStart;
            this.tieStop = tieStop;
        }

        public boolean isTieStart() {
            return tieStart;
        }

        public boolean isTieStop() {
            return tieStop;
        }
    }

    public static final class TrillSpannerTransition {
        private final boolean start;
        private final boolean stop;

        private TrillSpannerTransition(boolean start, boolean stop) {
            this.start = start;
            this.stop = stop;
        }

        public boolean isStart() {
            return start;
        }

        public boolean isStop() {
            return stop;
        }
    }

    public static final class MuseSlurElement {
        private final String type;
        private final String id;

        public MuseSlurElement(String type, String id) {
            this.type = type;
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public String getId() {
            return id;
        }
    }

    public static final class MuseSlurTransition {
        private final boolean start;
        private final boolean stop;

        private MuseSlurTransition(boolean start, boolean stop) {
            this.start = start;
            this.stop = stop;
        }

        public boolean isStart() {
            return start;
        }

        public boolean isStop() {
            return stop;
        }
    }

    public static final class MuseSlurTransitions {
        private final List<Integer> starts;
        private final List<Integer> stops;

        private MuseSlurTransitions(Collection<Integer> starts, Collection<Integer> stops) {
            this.starts = starts == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(starts));
            this.stops = stops == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(stops));
        }

        public List<Integer> getStarts() {
            return starts;
        }

        public List<Integer> getStops() {
            return stops;
        }
    }

    public static final class MuseImportSlurState {
        private final List<Integer> activeSlurNumbers;
        private final Map<String, Integer> slurKeyToNumber;
        private int nextSlurNumber;

        public MuseImportSlurState() {
            this(1, null, null);
        }

        public MuseImportSlurState(int nextSlurNumber, Collection<Integer> activeSlurNumbers,
                Map<String, Integer> slurKeyToNumber) {
            this.nextSlurNumber = Math.max(1, nextSlurNumber);
            this.activeSlurNumbers = activeSlurNumbers == null ? new ArrayList<Integer>()
                    : new ArrayList<Integer>(activeSlurNumbers);
            this.slurKeyToNumber = slurKeyToNumber == null ? new LinkedHashMap<String, Integer>()
                    : new LinkedHashMap<String, Integer>(slurKeyToNumber);
        }

        public int getNextSlurNumber() {
            return nextSlurNumber;
        }

        public void setNextSlurNumber(int nextSlurNumber) {
            this.nextSlurNumber = Math.max(1, nextSlurNumber);
        }

        public List<Integer> getActiveSlurNumbers() {
            return activeSlurNumbers;
        }

        public Map<String, Integer> getSlurKeyToNumber() {
            return slurKeyToNumber;
        }
    }

    public static final class MuseScoreChordNotationSummary {
        private final List<String> articulationTags;
        private final List<String> technicalTags;
        private final boolean chordLocalTrillMark;

        private MuseScoreChordNotationSummary(List<String> articulationTags, List<String> technicalTags,
                boolean chordLocalTrillMark) {
            this.articulationTags = Collections.unmodifiableList(new ArrayList<String>(articulationTags));
            this.technicalTags = Collections.unmodifiableList(new ArrayList<String>(technicalTags));
            this.chordLocalTrillMark = chordLocalTrillMark;
        }

        public List<String> getArticulationTags() {
            return articulationTags;
        }

        public List<String> getTechnicalTags() {
            return technicalTags;
        }

        public boolean hasChordLocalTrillMark() {
            return chordLocalTrillMark;
        }
    }

    public static final class MuseScoreChordNoteInput {
        private final Integer midi;
        private final String accidentalSubtype;
        private final String tpcRaw;
        private final TieFlags tieFlags;
        private final String fingeringText;
        private final String stringText;

        public MuseScoreChordNoteInput(Integer midi, String accidentalSubtype, String tpcRaw, TieFlags tieFlags,
                String fingeringText, String stringText) {
            this.midi = midi;
            this.accidentalSubtype = accidentalSubtype;
            this.tpcRaw = tpcRaw;
            this.tieFlags = tieFlags == null ? new TieFlags(false, false) : tieFlags;
            this.fingeringText = fingeringText;
            this.stringText = stringText;
        }

        public Integer getMidi() {
            return midi;
        }

        public String getAccidentalSubtype() {
            return accidentalSubtype;
        }

        public String getTpcRaw() {
            return tpcRaw;
        }

        public TieFlags getTieFlags() {
            return tieFlags;
        }

        public String getFingeringText() {
            return fingeringText;
        }

        public String getStringText() {
            return stringText;
        }
    }

    public static final class MuseScoreChordNote {
        private final int midi;
        private final String accidentalText;
        private final String tpcAccidentalText;
        private final boolean tieStart;
        private final boolean tieStop;
        private final String fingeringText;
        private final Integer stringNumber;

        private MuseScoreChordNote(int midi, String accidentalText, String tpcAccidentalText, boolean tieStart,
                boolean tieStop, String fingeringText, Integer stringNumber) {
            this.midi = midi;
            this.accidentalText = accidentalText;
            this.tpcAccidentalText = tpcAccidentalText;
            this.tieStart = tieStart;
            this.tieStop = tieStop;
            this.fingeringText = fingeringText;
            this.stringNumber = stringNumber;
        }

        public int getMidi() {
            return midi;
        }

        public String getAccidentalText() {
            return accidentalText;
        }

        public String getTpcAccidentalText() {
            return tpcAccidentalText;
        }

        public boolean isTieStart() {
            return tieStart;
        }

        public boolean isTieStop() {
            return tieStop;
        }

        public String getFingeringText() {
            return fingeringText;
        }

        public Integer getStringNumber() {
            return stringNumber;
        }
    }

    public static final class MuseImportedPitchXml {
        private final String pitchXml;
        private final String accidentalXml;

        private MuseImportedPitchXml(String pitchXml, String accidentalXml) {
            this.pitchXml = pitchXml == null ? "" : pitchXml;
            this.accidentalXml = accidentalXml == null ? "" : accidentalXml;
        }

        public String getPitchXml() {
            return pitchXml;
        }

        public String getAccidentalXml() {
            return accidentalXml;
        }
    }

    public static final class MuseImportedVoiceEvent {
        private final String kind;
        private final Integer atDiv;
        private final int voiceNo;
        private final int durationDiv;
        private final Integer displayDurationDiv;
        private final Integer staffNo;
        private final List<MuseScoreChordNote> notes;
        private final boolean grace;
        private final boolean graceSlash;
        private final TimeModification timeModification;
        private final List<TupletStart> tupletStarts;
        private final List<Integer> tupletStops;
        private final List<Integer> slurStarts;
        private final List<Integer> slurStops;
        private final List<String> trillNotationItems;
        private final List<String> articulationTags;
        private final List<String> technicalTags;
        private final String explicitBeamMode;
        private final String xml;
        private final String dynamicMark;
        private final Double soundDynamics;

        private MuseImportedVoiceEvent(String kind, Integer atDiv, int voiceNo, int durationDiv,
                Integer displayDurationDiv, Integer staffNo, Collection<MuseScoreChordNote> notes, boolean grace,
                boolean graceSlash,
                TimeModification timeModification, Collection<TupletStart> tupletStarts,
                Collection<Integer> tupletStops, Collection<Integer> slurStarts, Collection<Integer> slurStops,
                Collection<String> trillNotationItems, Collection<String> articulationTags,
                Collection<String> technicalTags, String explicitBeamMode, String xml, String dynamicMark,
                Double soundDynamics) {
            this.kind = kind == null ? "" : kind;
            this.atDiv = atDiv == null ? null : Integer.valueOf(Math.max(0, atDiv.intValue()));
            this.voiceNo = Math.max(1, voiceNo);
            this.durationDiv = Math.max(0, durationDiv);
            this.displayDurationDiv = displayDurationDiv == null ? null
                    : Integer.valueOf(Math.max(0, displayDurationDiv.intValue()));
            this.staffNo = staffNo == null ? null : Integer.valueOf(Math.max(1, staffNo.intValue()));
            this.notes = notes == null ? Collections.<MuseScoreChordNote>emptyList()
                    : Collections.unmodifiableList(new ArrayList<MuseScoreChordNote>(notes));
            this.grace = grace;
            this.graceSlash = graceSlash;
            this.timeModification = timeModification;
            this.tupletStarts = tupletStarts == null ? Collections.<TupletStart>emptyList()
                    : Collections.unmodifiableList(new ArrayList<TupletStart>(tupletStarts));
            this.tupletStops = tupletStops == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(tupletStops));
            this.slurStarts = slurStarts == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(slurStarts));
            this.slurStops = slurStops == null ? Collections.<Integer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<Integer>(slurStops));
            this.trillNotationItems = trillNotationItems == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(trillNotationItems));
            this.articulationTags = articulationTags == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(articulationTags));
            this.technicalTags = technicalTags == null ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(new ArrayList<String>(technicalTags));
            this.explicitBeamMode = explicitBeamMode;
            this.xml = xml == null ? "" : xml;
            this.dynamicMark = dynamicMark;
            this.soundDynamics = soundDynamics;
        }

        public static MuseImportedVoiceEvent rest(Integer atDiv, int durationDiv, Integer displayDurationDiv,
                Integer staffNo, TimeModification timeModification, Collection<TupletStart> tupletStarts,
                Collection<Integer> tupletStops, String explicitBeamMode) {
            return restForVoice(atDiv, 1, durationDiv, displayDurationDiv, staffNo, timeModification, tupletStarts,
                    tupletStops, explicitBeamMode);
        }

        public static MuseImportedVoiceEvent restForVoice(Integer atDiv, int voiceNo, int durationDiv,
                Integer displayDurationDiv, Integer staffNo, TimeModification timeModification,
                Collection<TupletStart> tupletStarts, Collection<Integer> tupletStops, String explicitBeamMode) {
            return new MuseImportedVoiceEvent("rest", atDiv, voiceNo, durationDiv, displayDurationDiv, staffNo, null,
                    false,
                    false, timeModification, tupletStarts, tupletStops, null, null, null, null, null,
                    explicitBeamMode, null, null, null);
        }

        public static MuseImportedVoiceEvent chord(Integer atDiv, int durationDiv, Integer displayDurationDiv,
                Integer staffNo, Collection<MuseScoreChordNote> notes, boolean grace, boolean graceSlash,
                TimeModification timeModification, Collection<TupletStart> tupletStarts,
                Collection<Integer> tupletStops, Collection<Integer> slurStarts, Collection<Integer> slurStops,
                Collection<String> trillNotationItems, Collection<String> articulationTags,
                Collection<String> technicalTags, String explicitBeamMode) {
            return chordForVoice(atDiv, 1, durationDiv, displayDurationDiv, staffNo, notes, grace, graceSlash,
                    timeModification, tupletStarts, tupletStops, slurStarts, slurStops, trillNotationItems,
                    articulationTags, technicalTags, explicitBeamMode);
        }

        public static MuseImportedVoiceEvent chordForVoice(Integer atDiv, int voiceNo, int durationDiv,
                Integer displayDurationDiv, Integer staffNo, Collection<MuseScoreChordNote> notes, boolean grace,
                boolean graceSlash, TimeModification timeModification, Collection<TupletStart> tupletStarts,
                Collection<Integer> tupletStops, Collection<Integer> slurStarts, Collection<Integer> slurStops,
                Collection<String> trillNotationItems, Collection<String> articulationTags,
                Collection<String> technicalTags, String explicitBeamMode) {
            return new MuseImportedVoiceEvent("chord", atDiv, voiceNo, durationDiv, displayDurationDiv, staffNo, notes,
                    grace, graceSlash, timeModification, tupletStarts, tupletStops, slurStarts, slurStops,
                    trillNotationItems, articulationTags, technicalTags, explicitBeamMode, null, null, null);
        }

        public static MuseImportedVoiceEvent dynamic(Integer atDiv, Integer staffNo, String mark,
                Double soundDynamics) {
            return dynamicForVoice(atDiv, 1, staffNo, mark, soundDynamics);
        }

        public static MuseImportedVoiceEvent dynamicForVoice(Integer atDiv, int voiceNo, Integer staffNo, String mark,
                Double soundDynamics) {
            return new MuseImportedVoiceEvent("dynamic", atDiv, voiceNo, 0, null, staffNo, null, false, false, null, null,
                    null, null, null, null, null, null, null, null, mark, soundDynamics);
        }

        public static MuseImportedVoiceEvent directionXml(Integer atDiv, Integer staffNo, String xml) {
            return directionXmlForVoice(atDiv, 1, staffNo, xml);
        }

        public static MuseImportedVoiceEvent directionXmlForVoice(Integer atDiv, int voiceNo, Integer staffNo, String xml) {
            return new MuseImportedVoiceEvent("directionXml", atDiv, voiceNo, 0, null, staffNo, null, false, false, null,
                    null, null, null, null, null, null, null, null, xml, null, null);
        }

        public static MuseImportedVoiceEvent barlineXml(Integer atDiv, Integer staffNo, String xml) {
            return barlineXmlForVoice(atDiv, 1, staffNo, xml);
        }

        public static MuseImportedVoiceEvent barlineXmlForVoice(Integer atDiv, int voiceNo, Integer staffNo, String xml) {
            return new MuseImportedVoiceEvent("barlineXml", atDiv, voiceNo, 0, null, staffNo, null, false, false, null, null,
                    null, null, null, null, null, null, null, xml, null, null);
        }

        public String getKind() {
            return kind;
        }

        public boolean isTimed() {
            return isRest() || isChord();
        }

        public boolean isRest() {
            return "rest".equals(kind);
        }

        public boolean isChord() {
            return "chord".equals(kind);
        }

        public boolean isDynamic() {
            return "dynamic".equals(kind);
        }

        public boolean isDirectionXml() {
            return "directionXml".equals(kind);
        }

        public boolean isBarlineXml() {
            return "barlineXml".equals(kind);
        }

        public Integer getAtDiv() {
            return atDiv;
        }

        public int getVoiceNo() {
            return voiceNo;
        }

        public int getEventAtDiv(int occupiedDiv) {
            return atDiv == null ? Math.max(0, occupiedDiv) : Math.max(0, atDiv.intValue());
        }

        public int getDurationDiv() {
            return durationDiv;
        }

        public Integer getDisplayDurationDiv() {
            return displayDurationDiv;
        }

        public Integer getStaffNo() {
            return staffNo;
        }

        public List<MuseScoreChordNote> getNotes() {
            return notes;
        }

        public boolean isGrace() {
            return grace;
        }

        public boolean isGraceSlash() {
            return graceSlash;
        }

        public TimeModification getTimeModification() {
            return timeModification;
        }

        public List<TupletStart> getTupletStarts() {
            return tupletStarts;
        }

        public List<Integer> getTupletStops() {
            return tupletStops;
        }

        public List<Integer> getSlurStarts() {
            return slurStarts;
        }

        public List<Integer> getSlurStops() {
            return slurStops;
        }

        public List<String> getTrillNotationItems() {
            return trillNotationItems;
        }

        public List<String> getArticulationTags() {
            return articulationTags;
        }

        public List<String> getTechnicalTags() {
            return technicalTags;
        }

        public String getExplicitBeamMode() {
            return explicitBeamMode;
        }

        public String getXml() {
            return xml;
        }

        public String getDynamicMark() {
            return dynamicMark;
        }

        public Double getSoundDynamics() {
            return soundDynamics;
        }
    }

    public static final class MuseImportedVoiceCursorStep {
        private final int eventAtDiv;
        private final String forwardXml;
        private final int occupiedAfterLead;
        private final int occupiedAfterTimed;
        private final boolean clamped;

        private MuseImportedVoiceCursorStep(int eventAtDiv, String forwardXml, int occupiedAfterLead,
                int occupiedAfterTimed, boolean clamped) {
            this.eventAtDiv = Math.max(0, eventAtDiv);
            this.forwardXml = forwardXml == null ? "" : forwardXml;
            this.occupiedAfterLead = Math.max(0, occupiedAfterLead);
            this.occupiedAfterTimed = Math.max(0, occupiedAfterTimed);
            this.clamped = clamped;
        }

        public int getEventAtDiv() {
            return eventAtDiv;
        }

        public String getForwardXml() {
            return forwardXml;
        }

        public int getOccupiedAfterLead() {
            return occupiedAfterLead;
        }

        public int getOccupiedAfterTimed() {
            return occupiedAfterTimed;
        }

        public boolean isClamped() {
            return clamped;
        }
    }

    public static final class MuseScoreWarning {
        private final String code;
        private final String message;
        private final Integer measure;
        private final Integer staff;
        private final Integer voice;
        private final Integer atDiv;
        private final String action;
        private final String reason;
        private final String tag;
        private final Integer occupiedDiv;
        private final Integer capacityDiv;

        private MuseScoreWarning(String code, String message, Integer measure, Integer staff, Integer voice,
                Integer atDiv, String action, String reason, String tag, Integer occupiedDiv, Integer capacityDiv) {
            this.code = code;
            this.message = message;
            this.measure = measure;
            this.staff = staff;
            this.voice = voice;
            this.atDiv = atDiv;
            this.action = action;
            this.reason = reason;
            this.tag = tag;
            this.occupiedDiv = occupiedDiv;
            this.capacityDiv = capacityDiv;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public Integer getMeasure() {
            return measure;
        }

        public Integer getStaff() {
            return staff;
        }

        public Integer getVoice() {
            return voice;
        }

        public Integer getAtDiv() {
            return atDiv;
        }

        public String getAction() {
            return action;
        }

        public String getReason() {
            return reason;
        }

        public String getTag() {
            return tag;
        }

        public Integer getOccupiedDiv() {
            return occupiedDiv;
        }

        public Integer getCapacityDiv() {
            return capacityDiv;
        }
    }

    public static final class MuseScoreImportMeasureContext {
        private final int beats;
        private final int beatType;
        private final String timeSymbol;
        private final boolean explicitTimeSig;
        private final int capacityDiv;
        private final boolean implicit;
        private final int fifths;
        private final String mode;
        private final Integer tempoBpm;
        private final String tempoText;
        private final boolean repeatForward;
        private final boolean repeatBackward;
        private final boolean leftDoubleBarline;

        public MuseScoreImportMeasureContext(int beats, int beatType, String timeSymbol, boolean explicitTimeSig,
                int capacityDiv, boolean implicit, int fifths, String mode, Integer tempoBpm, String tempoText,
                boolean repeatForward, boolean repeatBackward, boolean leftDoubleBarline) {
            this.beats = beats;
            this.beatType = beatType;
            this.timeSymbol = timeSymbol;
            this.explicitTimeSig = explicitTimeSig;
            this.capacityDiv = capacityDiv;
            this.implicit = implicit;
            this.fifths = fifths;
            this.mode = mode;
            this.tempoBpm = tempoBpm;
            this.tempoText = tempoText;
            this.repeatForward = repeatForward;
            this.repeatBackward = repeatBackward;
            this.leftDoubleBarline = leftDoubleBarline;
        }

        public int getBeats() {
            return beats;
        }

        public int getBeatType() {
            return beatType;
        }

        public String getTimeSymbol() {
            return timeSymbol;
        }

        public boolean isExplicitTimeSig() {
            return explicitTimeSig;
        }

        public int getCapacityDiv() {
            return capacityDiv;
        }

        public boolean isImplicit() {
            return implicit;
        }

        public int getFifths() {
            return fifths;
        }

        public String getMode() {
            return mode;
        }

        public Integer getTempoBpm() {
            return tempoBpm;
        }

        public String getTempoText() {
            return tempoText;
        }

        public boolean isRepeatForward() {
            return repeatForward;
        }

        public boolean isRepeatBackward() {
            return repeatBackward;
        }

        public boolean isLeftDoubleBarline() {
            return leftDoubleBarline;
        }
    }

    public static final class ParsedMuseScoreMeasure {
        private final int index;
        private final int beats;
        private final int beatType;
        private final String timeSymbol;
        private final boolean explicitTimeSig;
        private final int capacityDiv;
        private final boolean implicit;
        private final int fifths;
        private final String mode;
        private final Integer tempoBpm;
        private final String tempoText;
        private final boolean repeatForward;
        private final boolean repeatBackward;
        private final boolean leftDoubleBarline;
        private final List<TimedEvent> events;

        private ParsedMuseScoreMeasure(int index, int beats, int beatType, String timeSymbol, boolean explicitTimeSig,
                int capacityDiv, boolean implicit, int fifths, String mode, Integer tempoBpm, String tempoText,
                boolean repeatForward, boolean repeatBackward, boolean leftDoubleBarline,
                Collection<TimedEvent> events) {
            this.index = index;
            this.beats = beats;
            this.beatType = beatType;
            this.timeSymbol = timeSymbol;
            this.explicitTimeSig = explicitTimeSig;
            this.capacityDiv = capacityDiv;
            this.implicit = implicit;
            this.fifths = fifths;
            this.mode = mode;
            this.tempoBpm = tempoBpm;
            this.tempoText = tempoText;
            this.repeatForward = repeatForward;
            this.repeatBackward = repeatBackward;
            this.leftDoubleBarline = leftDoubleBarline;
            this.events = Collections.unmodifiableList(new ArrayList<TimedEvent>(events));
        }

        public int getIndex() {
            return index;
        }

        public int getBeats() {
            return beats;
        }

        public int getBeatType() {
            return beatType;
        }

        public String getTimeSymbol() {
            return timeSymbol;
        }

        public boolean isExplicitTimeSig() {
            return explicitTimeSig;
        }

        public int getCapacityDiv() {
            return capacityDiv;
        }

        public boolean isImplicit() {
            return implicit;
        }

        public int getFifths() {
            return fifths;
        }

        public String getMode() {
            return mode;
        }

        public Integer getTempoBpm() {
            return tempoBpm;
        }

        public String getTempoText() {
            return tempoText;
        }

        public boolean isRepeatForward() {
            return repeatForward;
        }

        public boolean isRepeatBackward() {
            return repeatBackward;
        }

        public boolean isLeftDoubleBarline() {
            return leftDoubleBarline;
        }

        public List<TimedEvent> getEvents() {
            return events;
        }
    }

    public static final class ParsedMuseScoreStaff {
        private final String sourceStaffId;
        private final String clefSign;
        private final int clefLine;
        private final List<ParsedMuseScoreMeasure> measures;

        public ParsedMuseScoreStaff(String sourceStaffId, String clefSign, int clefLine,
                Collection<ParsedMuseScoreMeasure> measures) {
            this.sourceStaffId = sourceStaffId;
            this.clefSign = clefSign;
            this.clefLine = clefLine;
            this.measures = Collections.unmodifiableList(new ArrayList<ParsedMuseScoreMeasure>(measures));
        }

        public String getSourceStaffId() {
            return sourceStaffId;
        }

        public String getClefSign() {
            return clefSign;
        }

        public int getClefLine() {
            return clefLine;
        }

        public List<ParsedMuseScoreMeasure> getMeasures() {
            return measures;
        }
    }

    public static final class ParsedMuseScorePart {
        private final String partId;
        private final String partName;
        private final Transpose transpose;
        private final List<ParsedMuseScoreStaff> staffs;

        public ParsedMuseScorePart(String partId, String partName, Transpose transpose,
                Collection<ParsedMuseScoreStaff> staffs) {
            this.partId = partId;
            this.partName = partName;
            this.transpose = transpose;
            this.staffs = Collections.unmodifiableList(new ArrayList<ParsedMuseScoreStaff>(staffs));
        }

        public String getPartId() {
            return partId;
        }

        public String getPartName() {
            return partName;
        }

        public Transpose getTranspose() {
            return transpose;
        }

        public List<ParsedMuseScoreStaff> getStaffs() {
            return staffs;
        }
    }

    public static final class MuseScoreImportMetadata {
        private final String workTitle;
        private final String subtitleMeta;
        private final String movementTitleMeta;
        private final String movementNumberMeta;
        private final String workNumberMeta;
        private final String composer;
        private final String arrangerMeta;
        private final String lyricistMeta;
        private final String translatorMeta;
        private final String copyrightMeta;
        private final String creationDateMeta;

        public MuseScoreImportMetadata(String workTitle, String composer, String arrangerMeta, String lyricistMeta,
                String translatorMeta, String copyrightMeta, String creationDateMeta) {
            this(workTitle, "", "", "", "", composer, arrangerMeta, lyricistMeta, translatorMeta, copyrightMeta,
                    creationDateMeta);
        }

        public MuseScoreImportMetadata(String workTitle, String subtitleMeta, String movementTitleMeta,
                String movementNumberMeta, String workNumberMeta, String composer, String arrangerMeta,
                String lyricistMeta, String translatorMeta, String copyrightMeta, String creationDateMeta) {
            this.workTitle = workTitle;
            this.subtitleMeta = subtitleMeta;
            this.movementTitleMeta = movementTitleMeta;
            this.movementNumberMeta = movementNumberMeta;
            this.workNumberMeta = workNumberMeta;
            this.composer = composer;
            this.arrangerMeta = arrangerMeta;
            this.lyricistMeta = lyricistMeta;
            this.translatorMeta = translatorMeta;
            this.copyrightMeta = copyrightMeta;
            this.creationDateMeta = creationDateMeta;
        }

        public String getWorkTitle() {
            return workTitle;
        }

        public String getSubtitleMeta() {
            return subtitleMeta;
        }

        public String getMovementTitleMeta() {
            return movementTitleMeta;
        }

        public String getMovementNumberMeta() {
            return movementNumberMeta;
        }

        public String getWorkNumberMeta() {
            return workNumberMeta;
        }

        public String getComposer() {
            return composer;
        }

        public String getArrangerMeta() {
            return arrangerMeta;
        }

        public String getLyricistMeta() {
            return lyricistMeta;
        }

        public String getTranslatorMeta() {
            return translatorMeta;
        }

        public String getCopyrightMeta() {
            return copyrightMeta;
        }

        public String getCreationDateMeta() {
            return creationDateMeta;
        }
    }

    public static final class MuseImportedPartVoiceIdResolver {
        private final Map<String, Integer> voiceIdByStaffLocal = new java.util.LinkedHashMap<String, Integer>();
        private int nextVoiceId = 1;

        private MuseImportedPartVoiceIdResolver() {
        }

        public int resolve(int staffNo, int localVoiceNo) {
            String key = staffNo + ":" + Math.max(1, localVoiceNo);
            Integer existing = voiceIdByStaffLocal.get(key);
            if (existing != null) {
                return existing.intValue();
            }
            int assigned = nextVoiceId;
            nextVoiceId++;
            voiceIdByStaffLocal.put(key, Integer.valueOf(assigned));
            return assigned;
        }

        public Map<String, Integer> getVoiceIdByStaffLocal() {
            return Collections.unmodifiableMap(voiceIdByStaffLocal);
        }
    }

    private static final class TypeCandidate {
        private final String type;
        private final int div;

        private TypeCandidate(String type, int div) {
            this.type = type;
            this.div = div;
        }
    }
}
