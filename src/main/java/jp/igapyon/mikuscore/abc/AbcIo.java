/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.abc;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AbcIo {
    private static final Fraction DEFAULT_UNIT = new Fraction(1, 8);
    private static final Fraction DEFAULT_RATIO = new Fraction(1, 1);
    private static final Pattern ABC_META_PARAM_PATTERN = Pattern.compile("([A-Za-z][A-Za-z0-9_-]*)=([^\\s]+)");
    private static final Pattern ABC_META_DIRECTIVE_PATTERN = Pattern
            .compile("^%@mks\\s+(trill|key|measure|transpose)\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEADER_FIELD_PATTERN = Pattern.compile("^([A-Za-z]):\\s*(.*)$");
    private static final Pattern VOICE_DIRECTIVE_PATTERN = Pattern.compile("^(\\S+)\\s*(.*)$");
    private static final Pattern USER_DEFINED_DECORATION_PATTERN = Pattern.compile("^(\\S)(?:\\s*=\\s*|\\s+)(.+)$");
    private static final Pattern VOICE_BARE_CLEF_PATTERN = Pattern
            .compile("^\\s*(bass|treble|alto|tenor|c3|c4)(?=\\s|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VOICE_ATTR_PATTERN = Pattern
            .compile("([A-Za-z][A-Za-z0-9_-]*)\\s*=\\s*(\"([^\"]*)\"|(\\S+))");
    private static final Pattern ABC_TEMPO_FRACTION_PATTERN = Pattern
            .compile("(\\d+)\\s*/\\s*(\\d+)\\s*=\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern ABC_TEMPO_EQUALS_PATTERN = Pattern.compile("=\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern ABC_TEMPO_NUMBER_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)$");

    private AbcIo() {
    }

    public static int gcd(int a, int b) {
        int x = Math.abs(a);
        int y = Math.abs(b);
        while (y != 0) {
            int t = x % y;
            x = y;
            y = t;
        }
        return x == 0 ? 1 : x;
    }

    public static Fraction reduceFraction(int num, int den) {
        return reduceFraction(num, den, DEFAULT_RATIO);
    }

    public static Fraction reduceFraction(int num, int den, Fraction fallback) {
        if (den == 0) {
            return fallback.copy();
        }
        int sign = den < 0 ? -1 : 1;
        int n = num * sign;
        int d = den * sign;
        int g = gcd(n, d);
        return new Fraction(n / g, d / g);
    }

    public static Fraction multiplyFractions(Fraction a, Fraction b) {
        return reduceFraction(a.getNum() * b.getNum(), a.getDen() * b.getDen(), DEFAULT_RATIO);
    }

    public static Fraction divideFractions(Fraction a, Fraction b) {
        return reduceFraction(a.getNum() * b.getDen(), a.getDen() * b.getNum(), DEFAULT_RATIO);
    }

    public static Fraction parseFractionText(String text) {
        return parseFractionText(text, DEFAULT_UNIT);
    }

    public static Fraction parseFractionText(String text, Fraction fallback) {
        String value = text == null ? "" : text.trim();
        if (!value.matches("^\\d+/\\d+$")) {
            return fallback.copy();
        }
        String[] parts = value.split("/", 2);
        int num = parseInt(parts[0], 0);
        int den = parseInt(parts[1], 0);
        if (num == 0 || den == 0) {
            return fallback.copy();
        }
        return reduceFraction(num, den, fallback);
    }

    public static Fraction parseAbcLengthToken(String token, int lineNo) {
        String value = token == null ? "" : token;
        if (value.length() == 0) {
            return new Fraction(1, 1);
        }
        if (value.matches("^/+$")) {
            return new Fraction(1, 1 << value.length());
        }
        if (value.matches("^\\d+$")) {
            return new Fraction(parseInt(value, 1), 1);
        }
        if (value.matches("^\\d+/$")) {
            return new Fraction(parseInt(value.substring(0, value.length() - 1), 1), 2);
        }
        if (value.matches("^/\\d+$")) {
            return new Fraction(1, parseInt(value.substring(1), 1));
        }
        if (value.matches("^\\d+/\\d+$")) {
            String[] parts = value.split("/", 2);
            return reduceFraction(parseInt(parts[0], 1), parseInt(parts[1], 1));
        }
        throw new IllegalArgumentException("line " + lineNo + ": Could not parse length token: " + value);
    }

    public static String abcLengthTokenFromFraction(Fraction ratio) {
        Fraction reduced = reduceFraction(ratio.getNum(), ratio.getDen(), DEFAULT_RATIO);
        if (reduced.getNum() == reduced.getDen()) {
            return "";
        }
        if (reduced.getDen() == 1) {
            return String.valueOf(reduced.getNum());
        }
        if (reduced.getNum() == 1 && reduced.getDen() == 2) {
            return "/";
        }
        if (reduced.getNum() == 1) {
            return "/" + reduced.getDen();
        }
        return reduced.getNum() + "/" + reduced.getDen();
    }

    public static String abcPitchFromStepOctave(String step, int octave) {
        String upperStep = step == null ? "" : step.toUpperCase();
        if (!upperStep.matches("^[A-G]$")) {
            return "C";
        }
        if (octave >= 5) {
            return upperStep.toLowerCase() + repeat("'", octave - 5);
        }
        return upperStep + repeat(",", Math.max(0, 4 - octave));
    }

    public static String accidentalFromAlter(int alter) {
        if (alter == 0) {
            return "";
        }
        if (alter > 0) {
            return repeat("^", Math.min(2, alter));
        }
        return repeat("_", Math.min(2, Math.abs(alter)));
    }

    public static String keyFromFifthsMode(int fifths, String mode) {
        String[] major = { "Cb", "Gb", "Db", "Ab", "Eb", "Bb", "F", "C", "G", "D", "A", "E", "B", "F#",
                "C#" };
        String[] minor = { "Abm", "Ebm", "Bbm", "Fm", "Cm", "Gm", "Dm", "Am", "Em", "Bm", "F#m", "C#m",
                "G#m", "D#m", "A#m" };
        int idx = fifths + 7;
        if (idx < 0 || idx >= major.length) {
            return "C";
        }
        return "minor".equalsIgnoreCase(mode == null ? "" : mode) ? minor[idx] : major[idx];
    }

    public static String fractionToAbcTempoUnit(Fraction fraction) {
        Fraction reduced = reduceFraction(fraction.getNum(), fraction.getDen(), new Fraction(1, 4));
        return reduced.getNum() + "/" + reduced.getDen();
    }

    public static Integer parseTempoFromQ(String rawQ, List<String> warnings) {
        String raw = trimToEmpty(rawQ);
        if (raw.length() == 0) {
            return null;
        }
        String withoutQuoted = raw.replaceAll("\"[^\"]*\"", " ").trim();
        Matcher fractionMatcher = ABC_TEMPO_FRACTION_PATTERN.matcher(withoutQuoted);
        if (fractionMatcher.find()) {
            double num = parseDouble(fractionMatcher.group(1), Double.NaN);
            double den = parseDouble(fractionMatcher.group(2), Double.NaN);
            double bpm = parseDouble(fractionMatcher.group(3), Double.NaN);
            if (num > 0 && den > 0 && !Double.isNaN(bpm) && bpm > 0) {
                double quarterBpm = bpm * ((4 * num) / den);
                return Integer.valueOf(clampRoundedTempo(quarterBpm));
            }
        }
        Matcher equalsMatcher = ABC_TEMPO_EQUALS_PATTERN.matcher(withoutQuoted);
        if (equalsMatcher.find()) {
            double bpm = parseDouble(equalsMatcher.group(1), Double.NaN);
            if (!Double.isNaN(bpm) && bpm > 0) {
                return Integer.valueOf(clampRoundedTempo(bpm));
            }
        }
        Matcher numberMatcher = ABC_TEMPO_NUMBER_PATTERN.matcher(withoutQuoted);
        if (numberMatcher.find()) {
            double bpm = parseDouble(numberMatcher.group(1), Double.NaN);
            if (!Double.isNaN(bpm) && bpm > 0) {
                return Integer.valueOf(clampRoundedTempo(bpm));
            }
        }
        warnings.add("Q: unsupported tempo format; ignored: " + rawQ);
        return null;
    }

    public static AbcMeter parseMeter(String raw, List<String> warnings) {
        String normalized = trimToEmpty(raw);
        if ("C".equals(normalized)) {
            return new AbcMeter(4, 4);
        }
        if ("C|".equals(normalized)) {
            return new AbcMeter(2, 2);
        }
        Matcher matcher = Pattern.compile("^(\\d+)/(\\d+)$").matcher(normalized);
        if (!matcher.find()) {
            warnings.add("Invalid meter M: format; defaulted to 4/4: " + raw);
            return new AbcMeter(4, 4);
        }
        return new AbcMeter(parseInt(matcher.group(1), 4), parseInt(matcher.group(2), 4));
    }

    public static Fraction parseFraction(String raw, String fieldName, List<String> warnings) {
        Fraction parsed = parseFractionText(raw, DEFAULT_UNIT);
        if (parsed.getNum() == 1 && parsed.getDen() == 8
                && !(raw == null ? "" : raw).matches("^\\s*\\d+/\\d+\\s*$")) {
            warnings.add(fieldName + " has invalid format; defaulted to 1/8: " + raw);
            return parsed;
        }
        Matcher matcher = Pattern.compile("^\\s*(\\d+)/(\\d+)\\s*$").matcher(raw == null ? "" : raw);
        if (!matcher.find() || parseInt(matcher.group(1), 0) == 0 || parseInt(matcher.group(2), 0) == 0) {
            warnings.add(fieldName + " has invalid value; defaulted to 1/8: " + raw);
            return new Fraction(1, 8);
        }
        return parsed;
    }

    public static AbcKeyInfo parseKey(String raw, List<String> warnings) {
        String key = raw == null ? "" : raw.trim();
        Integer fifths = fifthsFromAbcKey(key);
        if (fifths != null) {
            return new AbcKeyInfo(fifths.intValue());
        }
        warnings.add("K: unsupported key; defaulted to C: " + key);
        return new AbcKeyInfo(0);
    }

    public static AbcVoiceMeasureMetaByIndex buildAbcVoiceMeasureMetaByIndex(String voiceId,
            List<?> normalizedMeasures, Map<String, Integer> keyHintFifthsByKey,
            Map<String, Map<Integer, AbcMeasureMeta>> notationMeasureMetaByVoice,
            Map<String, AbcMeasureMeta> measureMetaByKey, Map<String, Map<Integer, AbcMeter>> meterByMeasureByVoice,
            Map<String, Map<Integer, Integer>> tempoByMeasureByVoice) {
        Map<Integer, Integer> keyByMeasure = new LinkedHashMap<Integer, Integer>();
        Map<Integer, AbcMeter> meterByMeasure = new LinkedHashMap<Integer, AbcMeter>();
        Map<Integer, Integer> tempoByMeasure = new LinkedHashMap<Integer, Integer>();
        Map<Integer, AbcMeasureMeta> measureMetaByIndex = new LinkedHashMap<Integer, AbcMeasureMeta>();
        int measureCount = normalizedMeasures == null ? 0 : normalizedMeasures.size();
        String normalizedVoiceId = trimToEmpty(voiceId).length() == 0 ? "1" : trimToEmpty(voiceId);
        for (int measureNo = 1; measureNo <= measureCount; measureNo++) {
            Integer hinted = keyHintFifthsByKey == null ? null : keyHintFifthsByKey.get(normalizedVoiceId + "#"
                    + measureNo);
            if (hinted != null) {
                keyByMeasure.put(Integer.valueOf(measureNo), hinted);
            }
            AbcMeasureMeta notationMeta = getNestedMapValue(notationMeasureMetaByVoice, normalizedVoiceId, measureNo);
            AbcMeasureMeta hintedMeta = measureMetaByKey == null ? null : measureMetaByKey.get(normalizedVoiceId + "#"
                    + measureNo);
            AbcMeter meterHint = getNestedMapValue(meterByMeasureByVoice, normalizedVoiceId, measureNo);
            Integer tempoHint = getNestedMapValue(tempoByMeasureByVoice, normalizedVoiceId, measureNo);
            if (notationMeta != null || hintedMeta != null) {
                String number = firstNonEmpty(hintedMeta == null ? "" : hintedMeta.getNumber(),
                        notationMeta == null ? "" : notationMeta.getNumber(), String.valueOf(measureNo));
                boolean implicit = hintedMeta != null ? hintedMeta.isImplicit()
                        : (notationMeta != null && notationMeta.isImplicit());
                boolean repeatStart = (notationMeta != null && notationMeta.isRepeatStart())
                        || (hintedMeta != null && hintedMeta.isRepeatStart());
                boolean repeatEnd = (notationMeta != null && notationMeta.isRepeatEnd())
                        || (hintedMeta != null && hintedMeta.isRepeatEnd());
                Integer repeatTimes = hintedMeta != null && hintedMeta.getRepeatTimes() != null
                        ? hintedMeta.getRepeatTimes()
                        : (notationMeta == null ? null : notationMeta.getRepeatTimes());
                String endingStart = firstNonEmpty(notationMeta == null ? "" : notationMeta.getEndingStart(),
                        hintedMeta == null ? "" : hintedMeta.getEndingStart(), "");
                String endingStop = firstNonEmpty(notationMeta == null ? "" : notationMeta.getEndingStop(),
                        hintedMeta == null ? "" : hintedMeta.getEndingStop(), "");
                String endingStopType = firstNonEmpty(hintedMeta == null ? "" : hintedMeta.getEndingStopType(),
                        notationMeta == null ? "" : notationMeta.getEndingStopType(), "");
                measureMetaByIndex.put(Integer.valueOf(measureNo), new AbcMeasureMeta(number, implicit, repeatStart,
                        repeatEnd, repeatTimes, endingStart, endingStop, endingStopType));
            }
            if (meterHint != null) {
                meterByMeasure.put(Integer.valueOf(measureNo),
                        new AbcMeter(meterHint.getBeats(), meterHint.getBeatType()));
            }
            if (tempoHint != null) {
                tempoByMeasure.put(Integer.valueOf(measureNo), Integer.valueOf(clampRoundedTempo(tempoHint)));
            }
        }
        return new AbcVoiceMeasureMetaByIndex(keyByMeasure, meterByMeasure, tempoByMeasure, measureMetaByIndex);
    }

    public static String xmlEscape(String text) {
        String value = text == null ? "" : text;
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public static String clefXmlFromAbcClef(String rawClef) {
        String clef = trimToEmpty(rawClef).toLowerCase();
        if ("bass".equals(clef) || "f".equals(clef)) {
            return "<clef><sign>F</sign><line>4</line></clef>";
        }
        if ("alto".equals(clef) || "c3".equals(clef)) {
            return "<clef><sign>C</sign><line>3</line></clef>";
        }
        if ("tenor".equals(clef) || "c4".equals(clef)) {
            return "<clef><sign>C</sign><line>4</line></clef>";
        }
        return "<clef><sign>G</sign><line>2</line></clef>";
    }

    public static String buildAbcGroupedStaffClefXml(List<AbcParsedStaffVoice> staffVoices) {
        StringBuilder builder = new StringBuilder();
        if (staffVoices == null) {
            return "";
        }
        for (AbcParsedStaffVoice staffVoice : staffVoices) {
            String clefXml = clefXmlFromAbcClef(staffVoice == null ? "" : staffVoice.getClef());
            int staff = staffVoice == null ? 1 : staffVoice.getStaff();
            builder.append(clefXml.replace("<clef>", "<clef number=\"" + staff + "\">"));
        }
        return builder.toString();
    }

    public static boolean hasAbcGroupedStaffVoices(AbcParsedPartHeader part) {
        return part != null && part.getStaffVoices() != null && part.getStaffVoices().size() > 1;
    }

    public static String buildAbcGroupedStaffMeasureNotesXml(List<AbcParsedStaffVoice> staffVoices, int measureIndex,
            int currentMeasureDurationDiv, AbcMeasureNotesXmlBuilder buildMeasureNotesXml) {
        if (staffVoices == null || buildMeasureNotesXml == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int staffIndex = 0; staffIndex < staffVoices.size(); staffIndex++) {
            AbcParsedStaffVoice staffVoice = staffVoices.get(staffIndex);
            List<AbcMeasureNote> staffNotes = staffVoice == null
                    ? new ArrayList<AbcMeasureNote>()
                    : staffVoice.getMeasure(measureIndex);
            String xml = buildMeasureNotesXml.build(staffNotes,
                    staffVoice == null ? Integer.valueOf(1) : Integer.valueOf(staffVoice.getStaff()));
            if (staffIndex > 0) {
                builder.append("<backup><duration>").append(currentMeasureDurationDiv).append("</duration></backup>");
            }
            builder.append(xml);
        }
        return builder.toString();
    }

    public static String buildAbcPartTransposeXml(AbcTransposeMeta transpose) {
        if (transpose == null || (transpose.getChromatic() == null && transpose.getDiatonic() == null)) {
            return "";
        }
        return "<transpose>"
                + (transpose.getDiatonic() != null ? "<diatonic>" + Math.round(transpose.getDiatonic()) + "</diatonic>"
                        : "")
                + (transpose.getChromatic() != null
                        ? "<chromatic>" + Math.round(transpose.getChromatic()) + "</chromatic>"
                        : "")
                + "</transpose>";
    }

    public static String buildAbcTempoDirectionXml(Integer tempo, boolean include) {
        if (!include || tempo == null) {
            return "";
        }
        return "<direction><direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>" + tempo
                + "</per-minute></metronome></direction-type><sound tempo=\"" + tempo + "\"/></direction>";
    }

    public static AbcMeasureHeaderXml buildAbcMeasureHeaderXml(AbcParsedPartHeader part, int partIndex,
            int measureIndex, int currentPartFifths, AbcMeter currentPartMeter, Integer currentPartTempo,
            Integer hintedFifths, AbcMeter hintedMeter) {
        AbcMeter meter = currentPartMeter == null ? new AbcMeter(4, 4) : currentPartMeter;
        if (measureIndex == 0) {
            boolean grouped = hasAbcGroupedStaffVoices(part);
            String headerXml = "<attributes>"
                    + "<divisions>960</divisions>"
                    + "<key><fifths>" + Math.round(currentPartFifths) + "</fifths></key>"
                    + "<time><beats>" + Math.round(meter.getBeats()) + "</beats><beat-type>"
                    + Math.round(meter.getBeatType()) + "</beat-type></time>"
                    + (grouped ? "<staves>" + part.getStaffVoices().size() + "</staves>" : "")
                    + buildAbcPartTransposeXml(part == null ? null : part.getTranspose())
                    + (grouped ? buildAbcGroupedStaffClefXml(part.getStaffVoices())
                            : clefXmlFromAbcClef(part == null ? "" : part.getClef()))
                    + "</attributes>"
                    + buildAbcTempoDirectionXml(currentPartTempo, partIndex == 0);
            return new AbcMeasureHeaderXml(headerXml, "");
        }
        String headerXml = hintedFifths != null || hintedMeter != null
                ? "<attributes>"
                        + (hintedFifths != null
                                ? "<key><fifths>" + Math.round(currentPartFifths) + "</fifths></key>"
                                : "")
                        + (hintedMeter != null
                                ? "<time><beats>" + Math.round(meter.getBeats()) + "</beats><beat-type>"
                                        + Math.round(meter.getBeatType()) + "</beat-type></time>"
                                : "")
                        + "</attributes>"
                : "";
        return new AbcMeasureHeaderXml(headerXml, "");
    }

    public static String buildAbcMeasureTempoDirectionXml(Integer hintedTempo, int partIndex, int measureIndex) {
        return buildAbcTempoDirectionXml(hintedTempo, measureIndex > 0 && partIndex == 0);
    }

    public static String buildAbcMeasureRepeatStartXml(AbcMeasureMeta measureMeta) {
        StringBuilder chunks = new StringBuilder();
        if (measureMeta != null && trimToEmpty(measureMeta.getEndingStart()).length() > 0) {
            chunks.append("<ending number=\"").append(xmlEscape(measureMeta.getEndingStart()))
                    .append("\" type=\"start\"/>");
        }
        if (measureMeta != null && measureMeta.isRepeatStart()) {
            chunks.append("<repeat direction=\"forward\" winged=\"none\"/>");
        }
        return chunks.length() > 0 ? "<barline location=\"left\">" + chunks.toString() + "</barline>" : "";
    }

    public static String buildAbcMeasureRepeatEndXml(AbcMeasureMeta measureMeta) {
        StringBuilder chunks = new StringBuilder();
        if (measureMeta != null && trimToEmpty(measureMeta.getEndingStop()).length() > 0) {
            chunks.append("<ending number=\"").append(xmlEscape(measureMeta.getEndingStop())).append("\" type=\"")
                    .append(trimToEmpty(measureMeta.getEndingStopType()).length() > 0
                            ? xmlEscape(measureMeta.getEndingStopType())
                            : "stop")
                    .append("\"/>");
        }
        if (measureMeta != null && measureMeta.isRepeatEnd()) {
            chunks.append("<repeat direction=\"backward\" winged=\"none\"");
            if (measureMeta.getRepeatTimes() != null && measureMeta.getRepeatTimes().intValue() > 1) {
                chunks.append(" times=\"").append(Math.round(measureMeta.getRepeatTimes())).append("\"");
            }
            chunks.append("/>");
        }
        return chunks.length() > 0 ? "<barline location=\"right\">" + chunks.toString() + "</barline>" : "";
    }

    public static String buildAbcMeasureXml(int measureNo, String measureNumberText, boolean implicit,
            String repeatStartXml, String headerXml, String tempoDirectionXml, String debugMiscXml,
            String diagMiscXml, String sourceMiscXml, String notesXml, String repeatEndXml) {
        String xmlMeasureNumber = xmlEscape(trimToEmpty(measureNumberText).length() > 0 ? measureNumberText
                : String.valueOf(measureNo));
        String implicitAttr = implicit ? " implicit=\"yes\"" : "";
        return "<measure number=\"" + xmlMeasureNumber + "\"" + implicitAttr + ">"
                + nullToEmpty(repeatStartXml)
                + nullToEmpty(headerXml)
                + nullToEmpty(tempoDirectionXml)
                + nullToEmpty(debugMiscXml)
                + nullToEmpty(diagMiscXml)
                + nullToEmpty(sourceMiscXml)
                + nullToEmpty(notesXml)
                + nullToEmpty(repeatEndXml)
                + "</measure>";
    }

    public static String buildAbcMeasureDebugMiscXml(List<AbcMeasureNote> notes, int measureNo) {
        if (notes == null || notes.isEmpty()) {
            return "";
        }
        StringBuilder xml = new StringBuilder("<attributes><miscellaneous>");
        xml.append("<miscellaneous-field name=\"mks:dbg:abc:meta:count\">").append(toHex(notes.size(), 4))
                .append("</miscellaneous-field>");
        for (int index = 0; index < notes.size(); index++) {
            AbcMeasureNote note = notes.get(index);
            String voice = normalizeVoiceForMusicXml(note == null ? "" : note.getVoice());
            String step = note == null || note.isRest() ? "R" : normalizeStep(note.getStep());
            int octave = note == null || note.getOctave() == null ? 4
                    : Math.max(0, Math.min(9, note.getOctave().intValue()));
            int alter = note == null || note.getAlter() == null ? 0 : note.getAlter().intValue();
            int duration = note == null || note.isGrace() ? 0 : Math.max(1, note.getDuration());
            String payload = "idx=" + toHex(index, 4)
                    + ";m=" + toHex(measureNo, 4)
                    + ";v=" + xmlEscape(voice)
                    + ";r=" + (note != null && note.isRest() ? "1" : "0")
                    + ";g=" + (note != null && note.isGrace() ? "1" : "0")
                    + ";ch=" + (note != null && note.isChord() ? "1" : "0")
                    + ";st=" + step
                    + ";al=" + alter
                    + ";oc=" + toHex(octave, 2)
                    + ";dd=" + toHex(duration, 4)
                    + ";tp=" + xmlEscape(normalizeTypeForMusicXml(note == null ? "" : note.getType()));
            xml.append("<miscellaneous-field name=\"mks:dbg:abc:meta:")
                    .append(padLeft(index + 1, 4))
                    .append("\">")
                    .append(payload)
                    .append("</miscellaneous-field>");
        }
        xml.append("</miscellaneous></attributes>");
        return xml.toString();
    }

    public static String buildAbcSourceMiscXml(String abcSource) {
        String source = abcSource == null ? "" : abcSource;
        if (source.length() == 0) {
            return "";
        }
        String encoded = source.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
        int chunkSize = 240;
        int maxChunks = 512;
        List<String> chunks = new ArrayList<String>();
        for (int index = 0; index < encoded.length() && chunks.size() < maxChunks; index += chunkSize) {
            chunks.add(encoded.substring(index, Math.min(encoded.length(), index + chunkSize)));
        }
        boolean truncated = joinStrings(chunks).length() < encoded.length();
        StringBuilder xml = new StringBuilder("<attributes><miscellaneous>");
        xml.append("<miscellaneous-field name=\"mks:src:abc:raw-encoding\">escape-v1</miscellaneous-field>");
        xml.append("<miscellaneous-field name=\"mks:src:abc:raw-length\">").append(xmlEscape(String.valueOf(source.length())))
                .append("</miscellaneous-field>");
        xml.append("<miscellaneous-field name=\"mks:src:abc:raw-encoded-length\">")
                .append(xmlEscape(String.valueOf(encoded.length()))).append("</miscellaneous-field>");
        xml.append("<miscellaneous-field name=\"mks:src:abc:raw-chunks\">").append(xmlEscape(String.valueOf(chunks.size())))
                .append("</miscellaneous-field>");
        xml.append("<miscellaneous-field name=\"mks:src:abc:raw-truncated\">").append(truncated ? "1" : "0")
                .append("</miscellaneous-field>");
        for (int index = 0; index < chunks.size(); index++) {
            xml.append("<miscellaneous-field name=\"mks:src:abc:raw-").append(padLeft(index + 1, 4)).append("\">")
                    .append(xmlEscape(chunks.get(index))).append("</miscellaneous-field>");
        }
        xml.append("</miscellaneous></attributes>");
        return xml.toString();
    }

    public static String buildAbcDiagMiscXml(List<AbcImportDiagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) {
            return "";
        }
        int maxEntries = Math.min(256, diagnostics.size());
        StringBuilder xml = new StringBuilder("<attributes><miscellaneous>");
        xml.append("<miscellaneous-field name=\"mks:diag:count\">").append(maxEntries).append("</miscellaneous-field>");
        for (int index = 0; index < maxEntries; index++) {
            AbcImportDiagnostic item = diagnostics.get(index);
            List<String> payload = new ArrayList<String>();
            payload.add("level=" + item.getLevel());
            payload.add("code=" + item.getCode());
            payload.add("fmt=" + item.getFmt());
            if (item.getMeasure() != null) {
                payload.add("measure=" + Math.max(1, item.getMeasure().intValue()));
            }
            if (trimToEmpty(item.getVoiceId()).length() > 0) {
                payload.add("voice=" + xmlEscape(item.getVoiceId()));
            }
            if (trimToEmpty(item.getAction()).length() > 0) {
                payload.add("action=" + xmlEscape(item.getAction()));
            }
            if (trimToEmpty(item.getMessage()).length() > 0) {
                payload.add("message=" + xmlEscape(item.getMessage()));
            }
            if (item.getMovedEvents() != null) {
                payload.add("movedEvents=" + Math.max(0, item.getMovedEvents().intValue()));
            }
            xml.append("<miscellaneous-field name=\"mks:diag:").append(padLeft(index + 1, 4)).append("\">")
                    .append(joinSemicolon(payload)).append("</miscellaneous-field>");
        }
        xml.append("</miscellaneous></attributes>");
        return xml.toString();
    }

    public static AbcRenderedMeasureMiscXml buildAbcRenderedMeasureMiscXml(AbcRenderedMeasureMiscContext context) {
        if (context == null) {
            return new AbcRenderedMeasureMiscXml("", "", "");
        }
        List<AbcImportDiagnostic> filteredDiagnostics = new ArrayList<AbcImportDiagnostic>();
        if (context.getDiagnostics() != null) {
            String partVoiceId = context.getPart() == null ? "" : context.getPart().getVoiceId();
            for (AbcImportDiagnostic diagnostic : context.getDiagnostics()) {
                if (diagnostic == null || trimToEmpty(diagnostic.getVoiceId()).length() == 0
                        || trimToEmpty(diagnostic.getVoiceId()).equals(partVoiceId)) {
                    filteredDiagnostics.add(diagnostic);
                }
            }
        }
        String debugMiscXml = context.isDebugMetadata()
                ? buildAbcMeasureDebugMiscXml(context.getNotes(), context.getMeasureNo())
                : "";
        String diagMiscXml = context.getPartIndex() == 0 && context.getMeasureNo() == 1
                ? buildAbcDiagMiscXml(filteredDiagnostics)
                : "";
        String sourceMiscXml = context.isSourceMetadata() && context.getPartIndex() == 0 && context.getMeasureNo() == 1
                ? buildAbcSourceMiscXml(context.getAbcSource())
                : "";
        return new AbcRenderedMeasureMiscXml(debugMiscXml, diagMiscXml, sourceMiscXml);
    }

    public static String buildAbcRenderedPartMeasureXml(AbcRenderedPartMeasureContext context) {
        if (context == null) {
            return "";
        }
        AbcMeasureHeaderXml header = buildAbcMeasureHeaderXml(context.getPartHeader(), context.getPartIndex(),
                context.getMeasureIndex(), context.getCurrentPartFifths(), context.getCurrentPartMeter(),
                context.getCurrentPartTempo(), context.getHintedFifths(), context.getHintedMeter());
        String tempoDirectionXml = header.getTempoDirectionXml().length() > 0
                ? header.getTempoDirectionXml()
                : buildAbcMeasureTempoDirectionXml(context.getHintedTempo(), context.getPartIndex(),
                        context.getMeasureIndex());
        String notesXml = hasAbcGroupedStaffVoices(context.getPartHeader())
                ? buildAbcGroupedStaffMeasureNotesXml(context.getPartHeader().getStaffVoices(),
                        context.getMeasureIndex(), context.getCurrentMeasureDurationDiv(),
                        context.getBuildMeasureNotesXml())
                : context.getBuildMeasureNotesXml().build(context.getNotes(), null);
        String repeatStartXml = buildAbcMeasureRepeatStartXml(context.getMeasureMeta());
        String repeatEndXml = buildAbcMeasureRepeatEndXml(context.getMeasureMeta());
        AbcRenderedMeasureMiscXml miscXml = buildAbcRenderedMeasureMiscXml(new AbcRenderedMeasureMiscContext(
                context.getPart(), context.getPartIndex(), context.getMeasureNo(), context.getNotes(),
                context.isDebugMetadata(), context.isSourceMetadata(), context.getDiagnostics(),
                context.getAbcSource()));
        String measureNumber = context.getMeasureMeta() != null && trimToEmpty(context.getMeasureMeta().getNumber()).length() > 0
                ? context.getMeasureMeta().getNumber()
                : String.valueOf(context.getMeasureNo());
        return buildAbcMeasureXml(context.getMeasureNo(), measureNumber,
                context.getMeasureMeta() != null && context.getMeasureMeta().isImplicit()
                        || context.isInferredImplicitPickup(),
                repeatStartXml, header.getHeaderXml(), tempoDirectionXml, miscXml.getDebugMiscXml(),
                miscXml.getDiagMiscXml(), miscXml.getSourceMiscXml(), notesXml, repeatEndXml);
    }

    public static String buildAbcPartXml(AbcParsedPart part, int partIndex, int measureCount, int defaultFifths,
            int beats, int beatType, Integer tempoBpm, boolean debugMetadata, boolean sourceMetadata,
            List<AbcImportDiagnostic> diagnostics, String abcSource, AbcMeasureNotesXmlBuilder buildMeasureNotesXml) {
        AbcParsedPart safePart = part == null ? new AbcParsedPart("P1", "Voice 1") : part;
        StringBuilder measuresXml = new StringBuilder();
        AbcPartRenderState state = createInitialAbcPartRenderState(defaultFifths, beats, beatType, tempoBpm);
        AbcParsedPartRenderData renderData = safePart.toRenderData();
        AbcParsedPartHeader header = safePart.toPartHeader();
        for (int index = 0; index < measureCount; index++) {
            int measureNo = index + 1;
            AbcPartMeasureRenderContext measureContext = buildAbcPartMeasureRenderContext(renderData, index, state,
                    beats, beatType);
            state = measureContext.getNextState();
            measuresXml.append(buildAbcRenderedPartMeasureXml(new AbcRenderedPartMeasureContext(header, renderData,
                    partIndex, index, measureNo, measureContext.getNotes(), measureContext.getMeasureMeta(),
                    measureContext.getHintedFifths(), measureContext.getHintedMeter(),
                    measureContext.getHintedTempo(), state.getCurrentPartFifths(), state.getCurrentPartMeter(),
                    state.getCurrentPartTempo(), measureContext.getCurrentMeasureDurationDiv(),
                    measureContext.isInferredImplicitPickup(), debugMetadata, sourceMetadata, diagnostics, abcSource,
                    buildMeasureNotesXml)));
        }
        return "<part id=\"" + xmlEscape(safePart.getPartId()) + "\">" + measuresXml.toString() + "</part>";
    }

    public static String buildAbcPartListXml(List<AbcParsedPart> resolvedParts) {
        if (resolvedParts == null || resolvedParts.isEmpty()) {
            resolvedParts = java.util.Arrays.asList(new AbcParsedPart("P1", "Voice 1"));
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < resolvedParts.size(); index++) {
            AbcParsedPart part = resolvedParts.get(index) == null ? new AbcParsedPart("P" + (index + 1), "Voice "
                    + (index + 1)) : resolvedParts.get(index);
            int rawChannel = (index % 16) + 1;
            int midiChannel = rawChannel == 10 ? 11 : rawChannel;
            String partId = trimToEmpty(part.getPartId()).length() == 0 ? "P" + (index + 1) : part.getPartId();
            String partName = trimToEmpty(part.getPartName()).length() == 0 ? partId : part.getPartName();
            builder.append("<score-part id=\"").append(xmlEscape(partId)).append("\">")
                    .append("<part-name>").append(xmlEscape(partName)).append("</part-name>")
                    .append("<midi-instrument id=\"").append(xmlEscape(partId)).append("-I1\">")
                    .append("<midi-channel>").append(midiChannel).append("</midi-channel>")
                    .append("<midi-program>6</midi-program>")
                    .append("</midi-instrument>")
                    .append("</score-part>");
        }
        return builder.toString();
    }

    public static String buildAbcPartBodyXml(List<AbcParsedPart> resolvedParts, int measureCount, int defaultFifths,
            int beats, int beatType, Integer tempoBpm, boolean debugMetadata, boolean sourceMetadata,
            List<AbcImportDiagnostic> diagnostics, String abcSource, AbcMeasureNotesXmlBuilder buildMeasureNotesXml) {
        if (resolvedParts == null || resolvedParts.isEmpty()) {
            resolvedParts = java.util.Arrays.asList(new AbcParsedPart("P1", "Voice 1"));
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < resolvedParts.size(); index++) {
            builder.append(buildAbcPartXml(resolvedParts.get(index), index, measureCount, defaultFifths, beats,
                    beatType, tempoBpm, debugMetadata, sourceMetadata, diagnostics, abcSource, buildMeasureNotesXml));
        }
        return builder.toString();
    }

    public static String buildAbcScorePartwiseXmlDocument(String title, String composer, String partListXml,
            String partBodyXml) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<score-partwise version=\"4.0\">"
                + "<work><work-title>" + xmlEscape(title) + "</work-title></work>"
                + "<identification><creator type=\"composer\">" + xmlEscape(composer)
                + "</creator></identification>"
                + "<part-list>" + nullToEmpty(partListXml) + "</part-list>"
                + nullToEmpty(partBodyXml)
                + "</score-partwise>";
    }

    public static String buildAbcEmptyMeasureNotesXml(int measureDurationDiv, String emptyMeasureRestType,
            Integer staffOverride) {
        return "<note><rest/><duration>" + Math.max(1, measureDurationDiv)
                + "</duration><voice>1</voice><type>" + normalizeTypeForMusicXml(emptyMeasureRestType) + "</type>"
                + (staffOverride != null ? "<staff>" + Math.max(1, staffOverride.intValue()) + "</staff>" : "")
                + "</note>";
    }

    public static Map<Integer, String> buildAbcBeamXmlByNoteIndex(List<AbcMeasureNote> notes, int beatDiv) {
        Map<Integer, String> out = new LinkedHashMap<Integer, String>();
        if (notes == null || notes.isEmpty()) {
            return out;
        }
        Map<String, List<AbcBeamNoteEvent>> byVoice = new LinkedHashMap<String, List<AbcBeamNoteEvent>>();
        for (int index = 0; index < notes.size(); index++) {
            AbcMeasureNote note = notes.get(index);
            String voice = normalizeVoiceForMusicXml(note == null ? "" : note.getVoice());
            if (!byVoice.containsKey(voice)) {
                byVoice.put(voice, new ArrayList<AbcBeamNoteEvent>());
            }
            byVoice.get(voice).add(new AbcBeamNoteEvent(note, index));
        }
        for (List<AbcBeamNoteEvent> events : byVoice.values()) {
            List<AbcBeamNoteEvent> primary = new ArrayList<AbcBeamNoteEvent>();
            for (AbcBeamNoteEvent event : events) {
                if (event.getNote() == null || !event.getNote().isChord()) {
                    primary.add(event);
                }
            }
            Map<Integer, AbcBeamAssignment> assignments = computeAbcBeamAssignments(primary, beatDiv, true);
            for (Map.Entry<Integer, AbcBeamAssignment> entry : assignments.entrySet()) {
                AbcBeamAssignment assignment = entry.getValue();
                if (assignment == null || assignment.getLevels() <= 0) {
                    continue;
                }
                StringBuilder beamXml = new StringBuilder();
                for (int level = 1; level <= assignment.getLevels(); level++) {
                    beamXml.append("<beam number=\"").append(level).append("\">").append(assignment.getState())
                            .append("</beam>");
                }
                AbcBeamNoteEvent target = primary.get(entry.getKey().intValue());
                out.put(Integer.valueOf(target.getNoteIndex()), beamXml.toString());
            }
        }
        return out;
    }

    public static String buildAbcNotePitchOrRestXml(AbcMeasureNote note) {
        if (note == null || note.isRest()) {
            return "<rest/>";
        }
        String step = normalizeStep(note.getStep());
        int octave = note.getOctave() == null ? 4 : Math.max(0, Math.min(9, note.getOctave().intValue()));
        String alterXml = note.getAlter() != null && note.getAlter().intValue() != 0
                ? "<alter>" + Math.round(note.getAlter().intValue()) + "</alter>"
                : "";
        return "<pitch><step>" + step + "</step>" + alterXml + "<octave>" + octave + "</octave></pitch>";
    }

    public static String buildAbcNoteAccidentalXml(AbcMeasureNote note) {
        if (note == null || trimToEmpty(note.getAccidentalText()).length() == 0) {
            return "";
        }
        List<String> attrs = new ArrayList<String>();
        if (note.isAccidentalEditorial()) {
            attrs.add("editorial=\"yes\"");
        }
        if (note.isAccidentalCautionary()) {
            attrs.add("cautionary=\"yes\"");
        }
        String attrText = attrs.isEmpty() ? "" : " " + joinStringsWithSeparator(attrs, " ");
        return "<accidental" + attrText + ">" + xmlEscape(note.getAccidentalText()) + "</accidental>";
    }

    public static String buildAbcNoteLyricXml(AbcMeasureNote note) {
        if (note == null || trimToEmpty(note.getLyricText()).length() == 0) {
            return "";
        }
        return "<lyric><syllabic>" + xmlEscape(trimToEmpty(note.getLyricSyllabic()).length() == 0 ? "single"
                : note.getLyricSyllabic()) + "</syllabic><text>" + xmlEscape(note.getLyricText()) + "</text>"
                + (note.isLyricExtend() ? "<extend/>" : "") + "</lyric>";
    }

    public static String buildAbcNoteTimeModificationXml(AbcMeasureNote note) {
        if (note == null || note.getTimeModificationActual() == null || note.getTimeModificationNormal() == null
                || note.getTimeModificationActual().intValue() <= 0 || note.getTimeModificationNormal().intValue() <= 0) {
            return "";
        }
        return "<time-modification><actual-notes>" + Math.round(note.getTimeModificationActual().intValue())
                + "</actual-notes><normal-notes>" + Math.round(note.getTimeModificationNormal().intValue())
                + "</normal-notes></time-modification>";
    }

    public static String buildAbcNoteHarmonyAndWordsDirectionXml(AbcMeasureNote note) {
        if (note == null || note.isChord()) {
            return "";
        }
        StringBuilder chunks = new StringBuilder();
        for (String annotation : note.getAnnotations()) {
            if (trimToEmpty(annotation).length() == 0) {
                continue;
            }
            chunks.append("<direction><direction-type><words>").append(xmlEscape(annotation))
                    .append("</words></direction-type></direction>");
        }
        return chunks.toString();
    }

    public static String buildAbcNoteControlDirectionXml(AbcMeasureNote note) {
        if (note == null || note.isChord()) {
            return "";
        }
        StringBuilder chunks = new StringBuilder();
        if (note.isSegno()) {
            chunks.append("<direction><direction-type><segno/></direction-type></direction>");
        }
        if (note.isCoda()) {
            chunks.append("<direction><direction-type><coda/></direction-type></direction>");
        }
        if (trimToEmpty(note.getRehearsalMark()).length() > 0) {
            chunks.append("<direction><direction-type><rehearsal>")
                    .append(xmlEscape(note.getRehearsalMark()))
                    .append("</rehearsal></direction-type></direction>");
        }
        if (note.isFine()) {
            chunks.append("<direction><sound fine=\"yes\"/></direction>");
        }
        if (note.isDaCapo()) {
            chunks.append("<direction><sound dacapo=\"yes\"/></direction>");
        }
        if (note.isDalSegno()) {
            chunks.append("<direction><sound dalsegno=\"segno\"/></direction>");
        }
        if (note.isToCoda()) {
            chunks.append("<direction><sound tocoda=\"coda\"/></direction>");
        }
        if (note.isCrescendoStart()) {
            chunks.append("<direction><direction-type><wedge type=\"crescendo\"/></direction-type></direction>");
        }
        if (note.isDiminuendoStart()) {
            chunks.append("<direction><direction-type><wedge type=\"diminuendo\"/></direction-type></direction>");
        }
        if (note.isCrescendoStop() || note.isDiminuendoStop()) {
            chunks.append("<direction><direction-type><wedge type=\"stop\"/></direction-type></direction>");
        }
        if (trimToEmpty(note.getDynamicMark()).length() > 0) {
            chunks.append("<direction><direction-type><dynamics><")
                    .append(xmlEscape(note.getDynamicMark()))
                    .append("/></dynamics></direction-type></direction>");
        }
        if (note.isSfz()) {
            chunks.append("<direction><direction-type><dynamics><sfz/></dynamics></direction-type></direction>");
        }
        return chunks.toString();
    }

    public static String buildAbcNoteLeadingDirectionXml(AbcMeasureNote note) {
        return buildAbcNoteHarmonyAndWordsDirectionXml(note) + buildAbcNoteControlDirectionXml(note);
    }

    public static String buildAbcNoteCoreXml(AbcMeasureNote note, int noteIndex, Integer staffOverride,
            Map<Integer, String> beamXmlByNoteIndex) {
        AbcMeasureNote safeNote = note == null ? new AbcMeasureNote("1", 1, false, false, true, "C",
                Integer.valueOf(4), Integer.valueOf(0), "quarter") : note;
        StringBuilder chunks = new StringBuilder();
        chunks.append("<note>");
        if (safeNote.isChord()) {
            chunks.append("<chord/>");
        }
        if (safeNote.isGrace()) {
            chunks.append(safeNote.isGraceSlash() ? "<grace slash=\"yes\"/>" : "<grace/>");
        }
        chunks.append(buildAbcNotePitchOrRestXml(safeNote));
        if (!safeNote.isGrace()) {
            chunks.append("<duration>").append(Math.max(1, safeNote.getDuration())).append("</duration>");
        }
        chunks.append("<voice>").append(xmlEscape(normalizeVoiceForMusicXml(safeNote.getVoice()))).append("</voice>");
        Integer staffNumber = staffOverride != null ? staffOverride : safeNote.getStaff();
        if (staffNumber != null) {
            chunks.append("<staff>").append(Math.max(1, staffNumber.intValue())).append("</staff>");
        }
        chunks.append(buildAbcNoteLyricXml(safeNote));
        chunks.append("<type>").append(normalizeTypeForMusicXml(safeNote.getType())).append("</type>");
        if (!safeNote.isChord() && beamXmlByNoteIndex != null && beamXmlByNoteIndex.containsKey(Integer.valueOf(noteIndex))) {
            chunks.append(beamXmlByNoteIndex.get(Integer.valueOf(noteIndex)));
        }
        chunks.append(buildAbcNoteTimeModificationXml(safeNote));
        chunks.append(buildAbcNoteAccidentalXml(safeNote));
        if (safeNote.isTieStart()) {
            chunks.append("<tie type=\"start\"/>");
        }
        if (safeNote.isTieStop()) {
            chunks.append("<tie type=\"stop\"/>");
        }
        return chunks.toString();
    }

    public static String buildAbcNoteNotationsXml(AbcMeasureNote note) {
        if (note == null || !(note.isTieStart() || note.isTieStop())) {
            return "";
        }
        StringBuilder chunks = new StringBuilder("<notations>");
        if (note.isTieStart()) {
            chunks.append("<tied type=\"start\"/>");
        }
        if (note.isTieStop()) {
            chunks.append("<tied type=\"stop\"/>");
        }
        chunks.append("</notations>");
        return chunks.toString();
    }

    public static String buildAbcNoteXml(AbcMeasureNote note, int noteIndex, Integer staffOverride,
            Map<Integer, String> beamXmlByNoteIndex) {
        return buildAbcNoteLeadingDirectionXml(note)
                + buildAbcNoteCoreXml(note, noteIndex, staffOverride, beamXmlByNoteIndex)
                + buildAbcNoteNotationsXml(note)
                + "</note>";
    }

    public static String buildAbcMeasureNotesXml(List<AbcMeasureNote> notes, int measureDurationDiv,
            String emptyMeasureRestType, int beatDiv, Integer staffOverride) {
        if (notes == null || notes.isEmpty()) {
            return buildAbcEmptyMeasureNotesXml(measureDurationDiv, emptyMeasureRestType, staffOverride);
        }
        Map<Integer, String> beamXmlByNoteIndex = buildAbcBeamXmlByNoteIndex(notes, beatDiv);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < notes.size(); index++) {
            builder.append(buildAbcNoteXml(notes.get(index), index, staffOverride, beamXmlByNoteIndex));
        }
        return builder.toString();
    }

    public static List<AbcParsedPart> resolveAbcParsedPartsForExport(List<AbcParsedPart> parts) {
        List<AbcParsedPart> safeParts = parts == null || parts.isEmpty()
                ? java.util.Arrays.asList(new AbcParsedPart("P1", "Voice 1", "", "", null,
                        new ArrayList<AbcParsedStaffVoice>(),
                        java.util.Arrays.asList(new ArrayList<AbcMeasureNote>()),
                        new LinkedHashMap<Integer, Integer>(), new LinkedHashMap<Integer, AbcMeter>(),
                        new LinkedHashMap<Integer, Integer>(), new LinkedHashMap<Integer, AbcMeasureMeta>()))
                : parts;
        List<AbcParsedPart> resolved = new ArrayList<AbcParsedPart>();
        for (AbcParsedPart part : safeParts) {
            AbcParsedPart safePart = part == null ? new AbcParsedPart("P1", "Voice 1") : part;
            resolved.add(safePart.withClef(resolveAbcImportClef(safePart)));
        }
        return resolved;
    }

    public static AbcMusicXmlExportContext buildAbcMusicXmlExportContext(AbcParsedResult parsed) {
        AbcParsedResult safeParsed = parsed == null ? new AbcParsedResult(new AbcParsedMeta(), null, null, null)
                : parsed;
        AbcParsedMeta meta = safeParsed.getMeta() == null ? new AbcParsedMeta() : safeParsed.getMeta();
        List<AbcParsedPart> resolvedParts = resolveAbcParsedPartsForExport(safeParsed.getParts());
        int measureCount = 1;
        for (AbcParsedPart part : resolvedParts) {
            measureCount = Math.max(measureCount, part == null ? 0 : part.getMeasures().size());
        }
        int beats = meta.getMeter() == null ? 4 : meta.getMeter().getBeats();
        int beatType = meta.getMeter() == null ? 4 : meta.getMeter().getBeatType();
        int defaultFifths = meta.getKeyInfo() == null ? 0 : meta.getKeyInfo().getFifths();
        int divisions = 960;
        int beatDiv = Math.max(1, (int) Math.round((divisions * 4.0) / Math.max(1, Math.round(beatType))));
        int measureDurationDiv = Math.max(1,
                (int) Math.round((divisions * 4.0 * Math.max(1, Math.round(beats)))
                        / Math.max(1, Math.round(beatType))));
        String emptyMeasureRestType = normalizeTypeForMusicXml(typeFromDuration(measureDurationDiv, divisions));
        Integer tempoBpm = meta.getTempoBpm() != null && meta.getTempoBpm().intValue() > 0
                ? Integer.valueOf(clampRoundedTempo(meta.getTempoBpm().intValue()))
                : null;
        return new AbcMusicXmlExportContext(resolvedParts, measureCount,
                trimToEmpty(meta.getTitle()).length() == 0 ? "mikuscore" : meta.getTitle(),
                trimToEmpty(meta.getComposer()).length() == 0 ? "Unknown" : meta.getComposer(),
                beats, beatType, defaultFifths, divisions, beatDiv, measureDurationDiv, emptyMeasureRestType,
                tempoBpm);
    }

    public static String buildMusicXmlFromAbcParsed(AbcParsedResult parsed, String abcSource,
            AbcImportOptions options, AbcMeasureNotesXmlBuilder buildMeasureNotesXml) {
        AbcImportOptions safeOptions = options == null ? new AbcImportOptions() : options;
        boolean debugMetadata = safeOptions.getDebugMetadata() == null ? true
                : safeOptions.getDebugMetadata().booleanValue();
        boolean sourceMetadata = safeOptions.getSourceMetadata() == null ? true
                : safeOptions.getSourceMetadata().booleanValue();
        boolean debugPrettyPrint = safeOptions.getDebugPrettyPrint() == null ? debugMetadata
                : safeOptions.getDebugPrettyPrint().booleanValue();
        AbcMusicXmlExportContext exportContext = buildAbcMusicXmlExportContext(parsed);
        AbcMeasureNotesXmlBuilder notesXmlBuilder = buildMeasureNotesXml == null ? new AbcMeasureNotesXmlBuilder() {
            public String build(List<AbcMeasureNote> notes, Integer staffNumber) {
                return buildAbcMeasureNotesXml(notes, exportContext.getMeasureDurationDiv(),
                        exportContext.getEmptyMeasureRestType(), exportContext.getBeatDiv(), staffNumber);
            }
        } : buildMeasureNotesXml;
        String partListXml = buildAbcPartListXml(exportContext.getResolvedParts());
        String partBodyXml = buildAbcPartBodyXml(exportContext.getResolvedParts(), exportContext.getMeasureCount(),
                exportContext.getDefaultFifths(), exportContext.getBeats(), exportContext.getBeatType(),
                exportContext.getTempoBpm(), debugMetadata, sourceMetadata,
                parsed == null ? null : parsed.getDiagnostics(), abcSource, notesXmlBuilder);
        String xml = buildAbcScorePartwiseXmlDocument(exportContext.getTitle(), exportContext.getComposer(),
                partListXml, partBodyXml);
        return debugPrettyPrint ? prettyPrintXml(xml) : xml;
    }

    public static String buildMusicXmlFromAbcParsed(AbcParsedResult parsed, String abcSource,
            AbcImportOptions options) {
        return buildMusicXmlFromAbcParsed(parsed, abcSource, options, null);
    }

    public static AbcPartRenderState createInitialAbcPartRenderState(int defaultFifths, int beats, int beatType,
            Integer tempoBpm) {
        return new AbcPartRenderState(Math.max(-7, Math.min(7, (int) Math.round(defaultFifths))),
                new AbcMeter((int) Math.round(beats), (int) Math.round(beatType)), tempoBpm);
    }

    public static AbcPartMeasureRenderContext buildAbcPartMeasureRenderContext(AbcParsedPartRenderData part,
            int measureIndex, AbcPartRenderState state, int beats, int beatType) {
        int measureNo = measureIndex + 1;
        List<AbcMeasureNote> notes = part == null ? new ArrayList<AbcMeasureNote>() : part.getMeasure(measureIndex);
        AbcMeasureMeta measureMeta = part == null ? null : part.getMeasureMetaByIndex().get(Integer.valueOf(measureNo));
        Integer rawHintedFifths = part == null ? null : part.getKeyByMeasure().get(Integer.valueOf(measureNo));
        Integer hintedFifths = rawHintedFifths == null ? null
                : Integer.valueOf(Math.max(-7, Math.min(7, (int) Math.round(rawHintedFifths.intValue()))));
        AbcMeter hintedMeter = part == null ? null : part.getMeterByMeasure().get(Integer.valueOf(measureNo));
        Integer rawHintedTempo = part == null ? null : part.getTempoByMeasure().get(Integer.valueOf(measureNo));
        Integer hintedTempo = rawHintedTempo == null ? null : Integer.valueOf(clampRoundedTempo(rawHintedTempo));
        AbcPartRenderState normalizedState = state == null
                ? createInitialAbcPartRenderState(0, beats, beatType, null)
                : state;
        AbcMeter nextMeter = hintedMeter != null
                ? new AbcMeter(Math.max(1, (int) Math.round(hintedMeter.getBeats() == 0 ? beats
                        : hintedMeter.getBeats())),
                        Math.max(1, (int) Math.round(hintedMeter.getBeatType() == 0 ? beatType
                                : hintedMeter.getBeatType())))
                : normalizedState.getCurrentPartMeter();
        AbcPartRenderState nextState = new AbcPartRenderState(
                hintedFifths != null ? hintedFifths.intValue() : normalizedState.getCurrentPartFifths(),
                nextMeter,
                hintedTempo != null ? hintedTempo : normalizedState.getCurrentPartTempo());
        int currentMeasureDurationDiv = Math.max(1,
                (int) Math.round((960.0 * 4.0 * Math.max(1, Math.round(nextState.getCurrentPartMeter().getBeats())))
                        / Math.max(1, Math.round(nextState.getCurrentPartMeter().getBeatType()))));
        int currentMeasureContentDiv = estimateAbcMeasureContentDiv(notes);
        boolean inferredImplicitPickup = measureIndex == 0
                && !(measureMeta != null && measureMeta.isImplicit())
                && currentMeasureContentDiv > 0
                && currentMeasureContentDiv < currentMeasureDurationDiv;
        return new AbcPartMeasureRenderContext(notes, measureMeta, hintedFifths, hintedMeter, hintedTempo, nextState,
                currentMeasureDurationDiv, inferredImplicitPickup);
    }

    public static boolean isAbcjsWrapperLine(String text) {
        String value = text == null ? "" : text.trim();
        return value.matches("^\\[\\s*/?\\s*abcjs(?:-[A-Za-z0-9_-]+)?(?:\\s+[^\\]]*)?\\]$");
    }

    public static int estimateAbcMeasureContentDiv(List<AbcMeasureNote> notes) {
        java.util.Map<String, Integer> byVoice = new java.util.LinkedHashMap<String, Integer>();
        java.util.Map<String, Integer> lastStartByVoice = new java.util.LinkedHashMap<String, Integer>();
        if (notes == null) {
            return 0;
        }
        for (AbcMeasureNote note : notes) {
            if (note == null || note.isGrace()) {
                continue;
            }
            String voice = note.getVoice() == null || note.getVoice().length() == 0 ? "1" : note.getVoice();
            int durationDiv = Math.max(0, note.getDuration());
            if (durationDiv <= 0) {
                continue;
            }
            int current = byVoice.containsKey(voice) ? byVoice.get(voice) : 0;
            if (note.isChord()) {
                int startDiv = lastStartByVoice.containsKey(voice) ? lastStartByVoice.get(voice) : current;
                byVoice.put(voice, Math.max(current, startDiv + durationDiv));
                continue;
            }
            lastStartByVoice.put(voice, current);
            byVoice.put(voice, current + durationDiv);
        }
        int maxDiv = 0;
        for (Integer value : byVoice.values()) {
            maxDiv = Math.max(maxDiv, value.intValue());
        }
        return maxDiv;
    }

    public static Integer fifthsFromAbcKey(String raw) {
        String normalized = raw == null ? "" : raw.trim().replaceAll("\\s+", "");
        String[] keys = { "C", "G", "D", "A", "E", "B", "F#", "C#", "F", "Bb", "Eb", "Ab", "Db", "Gb", "Cb",
                "Am", "Em", "Bm", "F#m", "C#m", "G#m", "D#m", "A#m", "Dm", "Gm", "Cm", "Fm", "Bbm", "Ebm",
                "Abm" };
        int[] fifths = { 0, 1, 2, 3, 4, 5, 6, 7, -1, -2, -3, -4, -5, -6, -7, 0, 1, 2, 3, 4, 5, 6, 7, -1, -2,
                -3, -4, -5, -6, -7 };
        for (int index = 0; index < keys.length; index++) {
            if (keys[index].equals(normalized)) {
                return Integer.valueOf(fifths[index]);
            }
        }
        return null;
    }

    public static Map<String, String> parseAbcMetaParams(String raw) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        Matcher matcher = ABC_META_PARAM_PATTERN.matcher(raw == null ? "" : raw);
        while (matcher.find()) {
            params.put(matcher.group(1).toLowerCase(), matcher.group(2));
        }
        return params;
    }

    public static boolean applyAbcTrillMeta(Map<String, String> params, Map<String, String> trillWidthHintByKey) {
        String voiceId = trimToEmpty(params.get("voice"));
        int measureNo = parseInt(params.get("measure"), Integer.MIN_VALUE);
        int eventNo = parseInt(params.get("event"), Integer.MIN_VALUE);
        String upper = trimToEmpty(params.get("upper"));
        if (voiceId.length() > 0 && measureNo > 0 && eventNo > 0 && upper.length() > 0) {
            trillWidthHintByKey.put(voiceId + "#" + measureNo + "#" + eventNo, upper);
            return true;
        }
        return false;
    }

    public static boolean applyAbcKeyMeta(Map<String, String> params, Map<String, Integer> keyHintFifthsByKey) {
        String voiceId = trimToEmpty(params.get("voice"));
        int measureNo = parseInt(params.get("measure"), Integer.MIN_VALUE);
        int fifths = parseInt(params.get("fifths"), Integer.MIN_VALUE);
        if (voiceId.length() > 0 && measureNo > 0 && fifths != Integer.MIN_VALUE) {
            String key = voiceId + "#" + measureNo;
            if (!keyHintFifthsByKey.containsKey(key)) {
                keyHintFifthsByKey.put(key, Integer.valueOf(Math.max(-7, Math.min(7, fifths))));
            }
            return true;
        }
        return false;
    }

    public static boolean applyAbcMeasureMeta(Map<String, String> params, Map<String, AbcMeasureMeta> measureMetaByKey) {
        String voiceId = trimToEmpty(params.get("voice"));
        int measureNo = parseInt(params.get("measure"), Integer.MIN_VALUE);
        if (voiceId.length() == 0 || measureNo <= 0) {
            return false;
        }
        String number = trimToEmpty(params.get("number"));
        String repeatRaw = trimToEmpty(params.get("repeat")).toLowerCase();
        String leftRepeatRaw = trimToEmpty(params.get("left-repeat")).toLowerCase();
        String rightRepeatRaw = trimToEmpty(params.get("right-repeat")).toLowerCase();
        int repeatTimesRaw = parseInt(params.get("times"), Integer.MIN_VALUE);
        String endingStart = trimToEmpty(params.get("ending-start"));
        String endingStop = trimToEmpty(params.get("ending-stop"));
        String endingStopTypeRaw = trimToEmpty(params.get("ending-type")).toLowerCase();
        String endingStopType = "discontinue".equals(endingStopTypeRaw) || "stop".equals(endingStopTypeRaw)
                ? endingStopTypeRaw
                : (endingStop.length() > 0 ? "stop" : "");
        measureMetaByKey.put(voiceId + "#" + measureNo,
                new AbcMeasureMeta(number.length() > 0 ? number : String.valueOf(measureNo),
                        isTruthy(params.get("implicit")),
                        isTruthy(leftRepeatRaw) || "forward".equals(repeatRaw),
                        isTruthy(rightRepeatRaw) || "backward".equals(repeatRaw),
                        repeatTimesRaw > 1 ? Integer.valueOf(repeatTimesRaw) : null,
                        endingStart,
                        endingStop,
                        endingStopType));
        return true;
    }

    public static boolean applyAbcTransposeMeta(Map<String, String> params,
            Map<String, AbcTransposeMeta> transposeHintByVoiceId) {
        String voiceId = trimToEmpty(params.get("voice"));
        Integer chromatic = parseIntegerOrNull(params.get("chromatic"));
        Integer diatonic = parseIntegerOrNull(params.get("diatonic"));
        if (voiceId.length() == 0 || (chromatic == null && diatonic == null)) {
            return false;
        }
        transposeHintByVoiceId.put(voiceId, new AbcTransposeMeta(chromatic, diatonic));
        return true;
    }

    public static boolean handleAbcMetaDirectiveLine(String rawTrimmed, Map<String, String> trillWidthHintByKey,
            Map<String, Integer> keyHintFifthsByKey, Map<String, AbcMeasureMeta> measureMetaByKey,
            Map<String, AbcTransposeMeta> transposeHintByVoiceId) {
        Matcher matcher = ABC_META_DIRECTIVE_PATTERN.matcher(rawTrimmed == null ? "" : rawTrimmed);
        if (!matcher.find()) {
            return false;
        }
        String kind = matcher.group(1).toLowerCase();
        Map<String, String> params = parseAbcMetaParams(matcher.group(2));
        if ("trill".equals(kind)) {
            return applyAbcTrillMeta(params, trillWidthHintByKey);
        }
        if ("key".equals(kind)) {
            return applyAbcKeyMeta(params, keyHintFifthsByKey);
        }
        if ("measure".equals(kind)) {
            return applyAbcMeasureMeta(params, measureMetaByKey);
        }
        if ("transpose".equals(kind)) {
            return applyAbcTransposeMeta(params, transposeHintByVoiceId);
        }
        return false;
    }

    public static boolean isAbcStructuredDirectiveLine(String rawTrimmed) {
        String value = rawTrimmed == null ? "" : rawTrimmed;
        return value.matches("^%@mks\\s+.*")
                || value.matches("^%%\\s*.*")
                || value.matches("^[A-Za-z]:\\s*(.*)$");
    }

    public static boolean handleAbcUnsupportedContinuedFieldLine(String raw, String rawTrimmed, int lineNo,
            AbcImportLineState lineState, List<String> warnings) {
        if (lineState.getPendingUnsupportedContinuedFieldName().length() == 0
                || lineState.isBodyStarted()
                || isAbcStructuredDirectiveLine(rawTrimmed)) {
            return false;
        }
        warnings.add("line " + lineNo + ": Skipped unsupported continued field text for "
                + lineState.getPendingUnsupportedContinuedFieldName() + ": " + rawTrimmed);
        if (!(raw == null ? "" : raw).matches(".*\\\\\\s*$")) {
            lineState.setPendingUnsupportedContinuedFieldName("");
        }
        return true;
    }

    public static void clearAbcPendingUnsupportedContinuedFieldOnStructuredLine(String rawTrimmed,
            AbcImportLineState lineState) {
        if (lineState.getPendingUnsupportedContinuedFieldName().length() > 0
                && !lineState.isBodyStarted()
                && isAbcStructuredDirectiveLine(rawTrimmed)) {
            lineState.setPendingUnsupportedContinuedFieldName("");
        }
    }

    public static void ensureAbcDeclaredVoice(AbcImportVoiceRegistry registry, String voiceId) {
        String normalized = trimToEmpty(voiceId);
        if (normalized.length() == 0) {
            return;
        }
        if (!registry.getDeclaredVoiceIds().contains(normalized)) {
            registry.getDeclaredVoiceIds().add(normalized);
        }
    }

    public static AbcAppendBodyTextResult appendAbcBodyTextEntries(String rawBodyText, int lineNo, String voiceId,
            AbcImportVoiceRegistry registry, List<AbcImportBodyEntry> bodyEntries) {
        return appendAbcBodyTextEntries(rawBodyText, lineNo, voiceId, registry, bodyEntries,
                new InlineVoiceSplitter() {
                    public AbcInlineVoiceSplitResult split(String text, String initialVoiceId) {
                        return splitBodyTextByInlineVoice(text, initialVoiceId);
                    }
                }, new OverlaySplitter() {
                    public List<AbcOverlaySegment> split(String text, String baseVoiceId) {
                        return splitBodyTextByOverlay(text, baseVoiceId);
                    }
                });
    }

    public static AbcAppendBodyTextResult appendAbcBodyTextEntries(String rawBodyText, int lineNo, String voiceId,
            AbcImportVoiceRegistry registry, List<AbcImportBodyEntry> bodyEntries,
            InlineVoiceSplitter splitBodyTextByInlineVoice, OverlaySplitter splitBodyTextByOverlay) {
        String normalizedBodyText = (rawBodyText == null ? "" : rawBodyText).replaceFirst("\\\\\\s*$", "");
        String normalizedVoiceId = trimToEmpty(voiceId).length() == 0 ? "1" : trimToEmpty(voiceId);
        if (normalizedBodyText.trim().length() == 0) {
            return new AbcAppendBodyTextResult(false, normalizedVoiceId);
        }
        AbcInlineVoiceSplitResult inlineVoiceResult = splitBodyTextByInlineVoice.split(normalizedBodyText,
                normalizedVoiceId);
        for (AbcInlineVoiceSegment segment : inlineVoiceResult.getSegments()) {
            List<AbcOverlaySegment> overlaySegments = splitBodyTextByOverlay.split(segment.getText(),
                    segment.getVoiceId());
            for (AbcOverlaySegment overlaySegment : overlaySegments) {
                ensureAbcDeclaredVoice(registry, overlaySegment.getVoiceId());
                if (overlaySegment.getOverlayIndex() > 0) {
                    String overlayLabel = "overlay " + (overlaySegment.getOverlayIndex() + 1);
                    String baseVoiceId = segment.getVoiceId();
                    String baseName = registry.getVoiceNameById().get(baseVoiceId);
                    registry.getVoiceNameById().put(overlaySegment.getVoiceId(),
                            baseName != null && baseName.length() > 0
                                    ? baseName + " " + overlayLabel
                                    : "Voice " + baseVoiceId + " " + overlayLabel);
                    if (registry.getVoiceClefById().containsKey(baseVoiceId)
                            && !registry.getVoiceClefById().containsKey(overlaySegment.getVoiceId())) {
                        registry.getVoiceClefById().put(overlaySegment.getVoiceId(),
                                registry.getVoiceClefById().get(baseVoiceId));
                    }
                    if (registry.getVoiceTransposeById().containsKey(baseVoiceId)
                            && !registry.getVoiceTransposeById().containsKey(overlaySegment.getVoiceId())) {
                        AbcTransposeMeta transpose = registry.getVoiceTransposeById().get(baseVoiceId);
                        registry.getVoiceTransposeById().put(overlaySegment.getVoiceId(),
                                transpose == null ? null : new AbcTransposeMeta(transpose.getChromatic(),
                                        transpose.getDiatonic()));
                    }
                }
                bodyEntries.add(new AbcImportBodyEntry(overlaySegment.getText(), lineNo, overlaySegment.getVoiceId()));
            }
        }
        String finalVoiceId = trimToEmpty(inlineVoiceResult.getFinalVoiceId());
        return new AbcAppendBodyTextResult(true, finalVoiceId.length() == 0 ? normalizedVoiceId : finalVoiceId);
    }

    public static AbcInlineVoiceSplitResult splitBodyTextByInlineVoice(String text, String initialVoiceId) {
        List<AbcInlineVoiceSegment> segments = new ArrayList<AbcInlineVoiceSegment>();
        String activeVoiceId = trimToEmpty(initialVoiceId).length() == 0 ? "1" : trimToEmpty(initialVoiceId);
        StringBuilder buffer = new StringBuilder();
        String raw = text == null ? "" : text;
        int idx = 0;
        while (idx < raw.length()) {
            if (raw.charAt(idx) == '[') {
                AbcParser.AbcParsedBracketToken bracketToken = AbcParser.parseAbcBracketTokenAt(raw, idx);
                if (bracketToken != null && "inline-field".equals(bracketToken.getKind())
                        && "V".equals(bracketToken.getInlineField().getFieldName())) {
                    AbcParser.AbcParsedInlineField inlineField = bracketToken.getInlineField();
                    if (buffer.toString().trim().length() > 0) {
                        segments.add(new AbcInlineVoiceSegment(activeVoiceId, buffer.toString()));
                    }
                    buffer.setLength(0);
                    Matcher voiceMatcher = Pattern.compile("^(\\S+)").matcher(inlineField.getFieldValue());
                    if (voiceMatcher.find()) {
                        activeVoiceId = voiceMatcher.group(1);
                    } else {
                        buffer.append(raw.substring(idx, inlineField.getNextIdx()));
                    }
                    idx = inlineField.getNextIdx();
                    continue;
                }
            }
            buffer.append(raw.charAt(idx));
            idx++;
        }
        if (buffer.toString().trim().length() > 0) {
            segments.add(new AbcInlineVoiceSegment(activeVoiceId, buffer.toString()));
        }
        return new AbcInlineVoiceSplitResult(segments, activeVoiceId);
    }

    public static List<AbcOverlaySegment> splitBodyTextByOverlay(String text, String baseVoiceId) {
        String raw = text == null ? "" : text;
        String normalizedBaseVoiceId = trimToEmpty(baseVoiceId).length() == 0 ? "1" : trimToEmpty(baseVoiceId);
        List<StringBuilder> overlayBuffers = new ArrayList<StringBuilder>();
        overlayBuffers.add(new StringBuilder());
        StringBuilder completedMeasureSkeleton = new StringBuilder();
        int activeOverlayIndex = 0;
        int idx = 0;
        while (idx < raw.length()) {
            char ch = raw.charAt(idx);
            if (ch == '"' || ch == '!' || ch == '+') {
                AbcParser.AbcParsedDelimitedSpan token = AbcParser.parseAbcDelimitedSpanAt(raw, idx, ch);
                if (token == null) {
                    idx++;
                    continue;
                }
                ensureOverlayBuffer(overlayBuffers, activeOverlayIndex, completedMeasureSkeleton);
                overlayBuffers.get(activeOverlayIndex).append(token.getText());
                idx = token.getNextIdx();
                continue;
            }
            AbcParser.AbcParsedBarlineToken barlineToken = AbcParser.parseAbcBarlineTokenAt(raw, idx);
            if (barlineToken != null) {
                String tokenText = raw.substring(idx, barlineToken.getNextIdx());
                if (barlineToken.isEndsMeasure()) {
                    for (int overlayIndex = 0; overlayIndex < overlayBuffers.size(); overlayIndex++) {
                        ensureOverlayBuffer(overlayBuffers, overlayIndex, completedMeasureSkeleton);
                        overlayBuffers.get(overlayIndex).append(tokenText);
                    }
                    completedMeasureSkeleton.append(tokenText);
                    activeOverlayIndex = 0;
                } else {
                    ensureOverlayBuffer(overlayBuffers, activeOverlayIndex, completedMeasureSkeleton);
                    overlayBuffers.get(activeOverlayIndex).append(tokenText);
                }
                idx = barlineToken.getNextIdx();
                continue;
            }
            if (ch == '&') {
                activeOverlayIndex++;
                ensureOverlayBuffer(overlayBuffers, activeOverlayIndex, completedMeasureSkeleton);
                idx++;
                continue;
            }
            ensureOverlayBuffer(overlayBuffers, activeOverlayIndex, completedMeasureSkeleton);
            overlayBuffers.get(activeOverlayIndex).append(ch);
            idx++;
        }
        List<AbcOverlaySegment> segments = new ArrayList<AbcOverlaySegment>();
        for (int overlayIndex = 0; overlayIndex < overlayBuffers.size(); overlayIndex++) {
            String segmentText = overlayBuffers.get(overlayIndex).toString();
            if (segmentText.trim().length() == 0) {
                continue;
            }
            String overlayVoiceId = overlayIndex == 0 ? normalizedBaseVoiceId : normalizedBaseVoiceId + "_ov"
                    + (overlayIndex + 1);
            segments.add(new AbcOverlaySegment(overlayVoiceId, segmentText, overlayIndex));
        }
        return segments;
    }

    public static AbcParsedVoiceDirectiveTail parseVoiceDirectiveTail(String raw) {
        if (raw == null || raw.length() == 0) {
            return new AbcParsedVoiceDirectiveTail("", "", null, "", "", new ArrayList<String>());
        }
        String bodyText = raw;
        String name = "";
        String clef = "";
        AbcTransposeMeta transpose = null;
        List<String> unsupportedKeys = new ArrayList<String>();
        Matcher bareClefMatch = VOICE_BARE_CLEF_PATTERN.matcher(bodyText);
        if (bareClefMatch.find()) {
            clef = trimToEmpty(bareClefMatch.group(1)).toLowerCase();
            bodyText = bodyText.substring(bareClefMatch.group(0).length());
        }
        Matcher attrMatcher = VOICE_ATTR_PATTERN.matcher(bodyText);
        StringBuffer replacedBody = new StringBuffer();
        while (attrMatcher.find()) {
            String lowerKey = attrMatcher.group(1).toLowerCase();
            String attrValue = attrMatcher.group(3) != null ? attrMatcher.group(3) : attrMatcher.group(4);
            if ("name".equals(lowerKey)) {
                name = attrValue == null ? "" : attrValue;
            } else if ("clef".equals(lowerKey)) {
                clef = trimToEmpty(attrValue).toLowerCase();
            } else if ("transpose".equals(lowerKey)) {
                int parsed = parseInt(trimToEmpty(attrValue), Integer.MIN_VALUE);
                if (parsed >= -24 && parsed <= 24) {
                    transpose = new AbcTransposeMeta(Integer.valueOf(parsed), null);
                }
            } else {
                unsupportedKeys.add(lowerKey);
            }
            attrMatcher.appendReplacement(replacedBody, " ");
        }
        attrMatcher.appendTail(replacedBody);
        bodyText = replacedBody.toString().trim();
        String skippedText = "";
        Matcher firstTokenMatch = Pattern.compile("^(\\S+)").matcher(bodyText);
        String firstToken = firstTokenMatch.find() ? firstTokenMatch.group(1) : "";
        if (firstToken.length() > 0
                && firstToken.matches("^[A-Za-z][A-Za-z0-9_-]*$")
                && Pattern.compile("[^A-Ga-gzZxX]").matcher(firstToken).find()) {
            skippedText = firstToken;
            bodyText = bodyText.substring(firstToken.length()).trim();
        }
        return new AbcParsedVoiceDirectiveTail(name.trim(), clef.trim(), transpose, bodyText, skippedText,
                unsupportedKeys);
    }

    private static void ensureOverlayBuffer(List<StringBuilder> overlayBuffers, int overlayIndex,
            StringBuilder completedMeasureSkeleton) {
        while (overlayBuffers.size() <= overlayIndex) {
            overlayBuffers.add(new StringBuilder(completedMeasureSkeleton.toString()));
        }
    }

    public static void applyAbcVoiceDirective(String value, int lineNo, AbcImportLineState lineState,
            AbcImportVoiceRegistry voiceRegistry, List<String> warnings, Map<String, String> userDefinedDecorationBySymbol,
            VoiceDirectiveTailParser parseVoiceDirectiveTail, DecorationSymbolExpander expandUserDefinedDecorationSymbols,
            BodyTextPusher pushBodyText) {
        Matcher matcher = VOICE_DIRECTIVE_PATTERN.matcher(value == null ? "" : value);
        if (!matcher.find()) {
            return;
        }
        lineState.setCurrentVoiceId(matcher.group(1));
        ensureAbcDeclaredVoice(voiceRegistry, lineState.getCurrentVoiceId());
        AbcParsedVoiceDirectiveTail parsedVoice = parseVoiceDirectiveTail.parse(matcher.group(2).trim());
        if (parsedVoice.getName().length() > 0) {
            voiceRegistry.getVoiceNameById().put(lineState.getCurrentVoiceId(), parsedVoice.getName());
        }
        if (parsedVoice.getClef().length() > 0) {
            voiceRegistry.getVoiceClefById().put(lineState.getCurrentVoiceId(), parsedVoice.getClef());
        }
        if (parsedVoice.getTranspose() != null) {
            voiceRegistry.getVoiceTransposeById().put(lineState.getCurrentVoiceId(), parsedVoice.getTranspose());
        }
        if (parsedVoice.getSkippedText().length() > 0) {
            warnings.add("line " + lineNo + ": Skipped unsupported V: directive tail token: "
                    + parsedVoice.getSkippedText());
        }
        for (String unsupportedKey : parsedVoice.getUnsupportedKeys()) {
            warnings.add("line " + lineNo + ": Skipped unsupported V: property: " + unsupportedKey);
        }
        if (parsedVoice.getBodyText().length() > 0) {
            String expandedBodyText = expandUserDefinedDecorationSymbols.expand(parsedVoice.getBodyText(),
                    userDefinedDecorationBySymbol);
            pushBodyText.push(expandedBodyText, lineNo, lineState.getCurrentVoiceId());
        }
    }

    public static boolean handleAbcHeaderFieldLine(String key, String value, boolean valueHasContinuation, int lineNo,
            AbcImportLineState lineState, Map<String, String> headers,
            Map<String, List<AbcLyricEntry>> lyricEntriesByVoice, Set<String> supportedStandaloneBodyFieldNames,
            AbcImportVoiceRegistry voiceRegistry, List<String> warnings, Map<String, String> userDefinedDecorationBySymbol,
            VoiceDirectiveTailParser parseVoiceDirectiveTail, UserDefinedDecorationParser parseUserDefinedDecoration,
            DecorationSymbolExpander expandUserDefinedDecorationSymbols, BodyTextPusher pushBodyText) {
        if ("w".equals(key)) {
            if (!lyricEntriesByVoice.containsKey(lineState.getCurrentVoiceId())) {
                lyricEntriesByVoice.put(lineState.getCurrentVoiceId(), new ArrayList<AbcLyricEntry>());
            }
            lyricEntriesByVoice.get(lineState.getCurrentVoiceId()).add(new AbcLyricEntry(value, lineNo));
            return true;
        }
        if (lineState.isBodyStarted() && supportedStandaloneBodyFieldNames.contains(key)) {
            pushBodyText.push("[" + key + ":" + value + "]", lineNo, lineState.getCurrentVoiceId());
            lineState.setBodyStarted(true);
            return true;
        }
        if ("V".equals(key)) {
            applyAbcVoiceDirective(value, lineNo, lineState, voiceRegistry, warnings, userDefinedDecorationBySymbol,
                    parseVoiceDirectiveTail, expandUserDefinedDecorationSymbols, pushBodyText);
            if (!lineState.isBodyStarted() && valueHasContinuation) {
                warnings.add("line " + lineNo
                        + ": Unsupported continued field after V:; following continuation text will be skipped.");
                lineState.setPendingUnsupportedContinuedFieldName("V:");
            }
            return true;
        }
        if (lineState.isBodyStarted()) {
            warnings.add("line " + lineNo + ": Skipped unsupported standalone body field: " + key + ":" + value);
            return true;
        }
        if ("U".equals(key)) {
            AbcUserDefinedDecoration parsedUserDefinedDecoration = parseUserDefinedDecoration.parse(value);
            if (parsedUserDefinedDecoration != null) {
                userDefinedDecorationBySymbol.put(parsedUserDefinedDecoration.getSymbol(),
                        parsedUserDefinedDecoration.getDecoration());
            }
            return true;
        }
        headers.put(key, value);
        if (!lineState.isBodyStarted() && valueHasContinuation) {
            warnings.add("line " + lineNo + ": Unsupported continued field after " + key
                    + ":; following continuation text will be skipped.");
            lineState.setPendingUnsupportedContinuedFieldName(key + ":");
        }
        return true;
    }

    public static void processAbcImportLine(String raw, int lineNo, AbcImportLineProcessorContext context) {
        String rawText = raw == null ? "" : raw;
        String rawTrimmed = rawText.trim();
        if (rawTrimmed.length() == 0) {
            context.getLineState().setPendingUnsupportedContinuedFieldName("");
            return;
        }
        if (isAbcjsWrapperLine(rawTrimmed)) {
            context.getWarnings().add("line " + lineNo + ": Skipped unsupported abcjs wrapper line: " + rawTrimmed);
            context.getLineState().setPendingUnsupportedContinuedFieldName("");
            return;
        }
        if (handleAbcUnsupportedContinuedFieldLine(rawText, rawTrimmed, lineNo, context.getLineState(),
                context.getWarnings())) {
            return;
        }
        clearAbcPendingUnsupportedContinuedFieldOnStructuredLine(rawTrimmed, context.getLineState());
        if (handleAbcMetaDirectiveLine(rawTrimmed, context.getTrillWidthHintByKey(), context.getKeyHintFifthsByKey(),
                context.getMeasureMetaByKey(), context.getTransposeHintByVoiceId())) {
            return;
        }
        Matcher scoreMatcher = Pattern.compile("^%%\\s*score\\s+(.+)$", Pattern.CASE_INSENSITIVE)
                .matcher(rawTrimmed);
        if (scoreMatcher.find()) {
            context.getLineState().setScoreDirective(scoreMatcher.group(1).trim());
            return;
        }
        String noComment = rawText.split("%", 2)[0];
        String trimmed = noComment.trim();
        if (rawTrimmed.matches("^%%\\s*.*")) {
            context.getWarnings().add("line " + lineNo + ": Skipped unsupported ABC directive: " + rawTrimmed);
            return;
        }
        Matcher headerMatcher = HEADER_FIELD_PATTERN.matcher(trimmed);
        if (headerMatcher.find() && headerMatcher.group(1).matches("^[A-Za-z]$")) {
            String key = headerMatcher.group(1);
            String rawValue = headerMatcher.group(2);
            boolean valueHasContinuation = rawValue.matches(".*\\\\\\s*$");
            String value = rawValue.replaceFirst("\\\\\\s*$", "").trim();
            handleAbcHeaderFieldLine(key, value, valueHasContinuation, lineNo, context.getLineState(),
                    context.getHeaders(), context.getLyricEntriesByVoice(),
                    context.getSupportedStandaloneBodyFieldNames(), context.getVoiceRegistry(), context.getWarnings(),
                    context.getUserDefinedDecorationBySymbol(), context.getParseVoiceDirectiveTail(),
                    context.getParseUserDefinedDecoration(), context.getExpandUserDefinedDecorationSymbols(),
                    context.getPushBodyText());
            return;
        }
        String expandedBodyText = context.getExpandUserDefinedDecorationSymbols().expand(noComment,
                context.getUserDefinedDecorationBySymbol());
        context.getPushBodyText().push(expandedBodyText, lineNo, context.getLineState().getCurrentVoiceId());
    }

    public static AbcUserDefinedDecoration parseUserDefinedDecoration(String rawValue) {
        String text = trimToEmpty(rawValue);
        Matcher match = USER_DEFINED_DECORATION_PATTERN.matcher(text);
        if (!match.find()) {
            return null;
        }
        String symbol = match.group(1);
        String rhs = match.group(2).trim();
        if (symbol.length() == 0 || rhs.length() == 0) {
            return null;
        }
        Matcher wrapped = Pattern.compile("^[!+](.+)[!+]$").matcher(rhs);
        String decoration = (wrapped.find() ? wrapped.group(1) : rhs).trim();
        return decoration.length() == 0 ? null : new AbcUserDefinedDecoration(symbol, decoration);
    }

    public static String expandUserDefinedDecorationSymbols(String text, Map<String, String> userDefinedDecorationBySymbol) {
        String raw = text == null ? "" : text;
        if (raw.length() == 0 || userDefinedDecorationBySymbol == null || userDefinedDecorationBySymbol.isEmpty()) {
            return raw;
        }
        StringBuilder out = new StringBuilder();
        int idx = 0;
        while (idx < raw.length()) {
            char ch = raw.charAt(idx);
            if (ch == '"' || ch == '!' || ch == '+') {
                AbcParser.AbcParsedDelimitedSpan token = AbcParser.parseAbcDelimitedSpanAt(raw, idx, ch);
                if (token == null) {
                    out.append(ch);
                    idx++;
                    continue;
                }
                out.append(token.getText());
                idx = token.getNextIdx();
                continue;
            }
            String key = String.valueOf(ch);
            if (userDefinedDecorationBySymbol.containsKey(key)) {
                out.append("!").append(userDefinedDecorationBySymbol.get(key)).append("!");
                idx++;
                continue;
            }
            out.append(ch);
            idx++;
        }
        return out.toString();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static Integer parseIntegerOrNull(String value) {
        try {
            return Integer.valueOf(Integer.parseInt(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static int clampRoundedTempo(double value) {
        return Math.max(20, Math.min(300, (int) Math.round(value)));
    }

    private static <T> T getNestedMapValue(Map<String, Map<Integer, T>> source, String voiceId, int measureNo) {
        Map<Integer, T> byMeasure = source == null ? null : source.get(voiceId);
        return byMeasure == null ? null : byMeasure.get(Integer.valueOf(measureNo));
    }

    private static String firstNonEmpty(String first, String second, String fallback) {
        String normalizedFirst = trimToEmpty(first);
        if (normalizedFirst.length() > 0) {
            return normalizedFirst;
        }
        String normalizedSecond = trimToEmpty(second);
        if (normalizedSecond.length() > 0) {
            return normalizedSecond;
        }
        return trimToEmpty(fallback);
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private static String toHex(int value, int width) {
        int safe = Math.max(0, value);
        String hex = Integer.toHexString(safe).toUpperCase();
        while (hex.length() < width) {
            hex = "0" + hex;
        }
        return "0x" + hex;
    }

    private static String padLeft(int value, int width) {
        String text = String.valueOf(value);
        while (text.length() < width) {
            text = "0" + text;
        }
        return text;
    }

    private static String normalizeStep(String step) {
        String value = trimToEmpty(step).toUpperCase();
        return value.matches("^[A-G]$") ? value : "C";
    }

    private static String normalizeTypeForMusicXml(String type) {
        String raw = trimToEmpty(type);
        if (raw.length() == 0) {
            return "quarter";
        }
        if ("16th".equals(raw) || "32nd".equals(raw) || "64th".equals(raw) || "128th".equals(raw)) {
            return raw;
        }
        if ("whole".equals(raw) || "half".equals(raw) || "quarter".equals(raw) || "eighth".equals(raw)) {
            return raw;
        }
        return "quarter";
    }

    private static String typeFromDuration(int duration, int divisionsPerQuarter) {
        double whole = ((double) duration) / (4.0 * Math.max(1, divisionsPerQuarter));
        if (whole >= 1.0) {
            return "whole";
        }
        if (whole >= 0.5) {
            return "half";
        }
        if (whole >= 0.25) {
            return "quarter";
        }
        if (whole >= 0.125) {
            return "eighth";
        }
        if (whole >= 0.0625) {
            return "16th";
        }
        return "32nd";
    }

    private static String resolveAbcImportClef(AbcParsedPart part) {
        String explicit = part == null ? "" : trimToEmpty(part.getClef()).toLowerCase();
        if (explicit.length() > 0) {
            return explicit;
        }
        List<Integer> keys = new ArrayList<Integer>();
        if (part != null) {
            for (List<AbcMeasureNote> measure : part.getMeasures()) {
                if (measure == null) {
                    continue;
                }
                for (AbcMeasureNote note : measure) {
                    Integer midi = noteToMidiForAbcClefInference(note);
                    if (midi != null) {
                        keys.add(midi);
                    }
                }
            }
        }
        if (keys.isEmpty()) {
            return "";
        }
        return "F".equals(chooseSingleClefByKeys(keys)) ? "bass" : "treble";
    }

    private static Integer noteToMidiForAbcClefInference(AbcMeasureNote note) {
        if (note == null || note.isRest()) {
            return null;
        }
        String step = normalizeStep(note.getStep());
        int octave = note.getOctave() == null ? 4 : (int) Math.round(note.getOctave().intValue());
        int alter = note.getAlter() == null ? 0 : (int) Math.round(note.getAlter().intValue());
        return Integer.valueOf((octave + 1) * 12 + midiByStepForAbcImport(step) + alter);
    }

    private static int midiByStepForAbcImport(String step) {
        if ("D".equals(step)) {
            return 2;
        }
        if ("E".equals(step)) {
            return 4;
        }
        if ("F".equals(step)) {
            return 5;
        }
        if ("G".equals(step)) {
            return 7;
        }
        if ("A".equals(step)) {
            return 9;
        }
        if ("B".equals(step)) {
            return 11;
        }
        return 0;
    }

    private static String chooseSingleClefByKeys(List<Integer> keys) {
        if (keys == null || keys.isEmpty()) {
            return "G";
        }
        List<Integer> sorted = new ArrayList<Integer>(keys);
        java.util.Collections.sort(sorted);
        int minKey = sorted.get(0).intValue();
        if (minKey >= 55) {
            return "G";
        }
        int median = sorted.get(sorted.size() / 2).intValue();
        return median < 60 ? "F" : "G";
    }

    private static String prettyPrintXml(String xml) {
        String compact = String.valueOf(xml == null ? "" : xml).replaceAll(">\\s+<", "><").trim();
        if (compact.length() == 0) {
            return "";
        }
        String[] split = compact.replaceAll("(>)(<)(/?)", "$1\n$2$3").split("\n");
        int indent = 0;
        StringBuilder out = new StringBuilder();
        for (String rawToken : split) {
            String token = rawToken.trim();
            if (token.length() == 0) {
                continue;
            }
            if (token.startsWith("</")) {
                indent = Math.max(0, indent - 1);
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            for (int index = 0; index < indent; index++) {
                out.append("  ");
            }
            out.append(token);
            boolean opening = token.matches("^<[^!?/][^>]*>$");
            boolean selfClosing = token.endsWith("/>");
            if (opening && !selfClosing) {
                indent++;
            }
        }
        return out.toString();
    }

    private static String normalizeVoiceForMusicXml(String voice) {
        String raw = trimToEmpty(voice);
        if (raw.length() == 0) {
            return "1";
        }
        if (raw.matches("^[1-9]\\d*$")) {
            return raw;
        }
        Matcher matcher = Pattern.compile("\\d+").matcher(raw);
        if (!matcher.find()) {
            return "1";
        }
        int value = parseInt(matcher.group(0), 1);
        return value <= 0 ? "1" : String.valueOf(value);
    }

    private static String joinStrings(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            builder.append(value);
        }
        return builder.toString();
    }

    private static String joinSemicolon(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(";");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static String joinStringsWithSeparator(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(separator == null ? "" : separator);
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isTruthy(String value) {
        String normalized = trimToEmpty(value).toLowerCase();
        return "1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized);
    }

    private static Map<Integer, AbcBeamAssignment> computeAbcBeamAssignments(List<AbcBeamNoteEvent> events,
            int beatDiv, boolean splitAtBeatBoundaryWhenImplicit) {
        Map<Integer, AbcBeamAssignment> assignmentByIndex = new LinkedHashMap<Integer, AbcBeamAssignment>();
        List<AbcBeamEventInfo> infos = new ArrayList<AbcBeamEventInfo>();
        if (events == null) {
            return assignmentByIndex;
        }
        for (AbcBeamNoteEvent event : events) {
            infos.add(resolveAbcBeamEventInfo(event == null ? null : event.getNote()));
        }

        boolean hasExplicitBeamMode = false;
        for (AbcBeamEventInfo info : infos) {
            if (info.isTimed() && ("begin".equals(info.getExplicitMode()) || "mid".equals(info.getExplicitMode()))) {
                hasExplicitBeamMode = true;
                break;
            }
        }
        if (!hasExplicitBeamMode) {
            List<Integer> currentGroup = new ArrayList<Integer>();
            int cursorDiv = 0;
            int resolvedBeatDiv = Math.max(1, Math.round(beatDiv));
            for (int index = 0; index < infos.size(); index++) {
                AbcBeamEventInfo info = infos.get(index);
                if (splitAtBeatBoundaryWhenImplicit && info.isTimed()) {
                    boolean startsAtBeatBoundary = cursorDiv > 0 && cursorDiv % resolvedBeatDiv == 0;
                    if (startsAtBeatBoundary) {
                        flushAbcBeamGroup(infos, currentGroup, assignmentByIndex);
                        currentGroup.clear();
                    }
                }
                if (!info.isChord() || !isAbcBeamableTimedEvent(info)) {
                    flushAbcBeamGroup(infos, currentGroup, assignmentByIndex);
                    currentGroup.clear();
                    if (info.isTimed()) {
                        cursorDiv += Math.max(0, info.getDurationDiv());
                    }
                    continue;
                }
                currentGroup.add(Integer.valueOf(index));
                if (info.isTimed()) {
                    cursorDiv += Math.max(0, info.getDurationDiv());
                }
            }
            flushAbcBeamGroup(infos, currentGroup, assignmentByIndex);
            return assignmentByIndex;
        }

        List<Integer> activeGroup = new ArrayList<Integer>();
        int cursorDiv = 0;
        int resolvedBeatDiv = Math.max(1, Math.round(beatDiv));
        for (int index = 0; index < infos.size(); index++) {
            AbcBeamEventInfo info = infos.get(index);
            if (!info.isTimed()) {
                flushAbcBeamGroup(infos, activeGroup, assignmentByIndex);
                activeGroup.clear();
                continue;
            }
            boolean startsAtBeatBoundary = cursorDiv > 0 && cursorDiv % resolvedBeatDiv == 0;
            if (startsAtBeatBoundary) {
                flushAbcBeamGroup(infos, activeGroup, assignmentByIndex);
                activeGroup.clear();
            }
            if (!isAbcBeamableTimedEvent(info)) {
                flushAbcBeamGroup(infos, activeGroup, assignmentByIndex);
                activeGroup.clear();
                continue;
            }
            if ("begin".equals(info.getExplicitMode())) {
                flushAbcBeamGroup(infos, activeGroup, assignmentByIndex);
                activeGroup.clear();
                activeGroup.add(Integer.valueOf(index));
                cursorDiv += Math.max(0, info.getDurationDiv());
                continue;
            }
            if ("mid".equals(info.getExplicitMode())) {
                if (activeGroup.isEmpty()) {
                    AbcBeamEventInfo previous = index > 0 ? infos.get(index - 1) : null;
                    if (isAbcBeamableTimedEvent(previous)) {
                        activeGroup.add(Integer.valueOf(index - 1));
                    }
                    activeGroup.add(Integer.valueOf(index));
                } else {
                    activeGroup.add(Integer.valueOf(index));
                }
                cursorDiv += Math.max(0, info.getDurationDiv());
                continue;
            }
            if (activeGroup.isEmpty()) {
                activeGroup.add(Integer.valueOf(index));
            } else {
                activeGroup.add(Integer.valueOf(index));
            }
            cursorDiv += Math.max(0, info.getDurationDiv());
        }
        flushAbcBeamGroup(infos, activeGroup, assignmentByIndex);
        return assignmentByIndex;
    }

    private static AbcBeamEventInfo resolveAbcBeamEventInfo(AbcMeasureNote note) {
        String type = normalizeTypeForMusicXml(note == null ? "" : note.getType());
        return new AbcBeamEventInfo(true, note != null && !note.isRest(), note != null && note.isGrace(),
                note != null && note.isGrace() ? 0 : Math.max(1, note == null ? 1 : note.getDuration()),
                beamLevelsFromType(type), note == null ? "" : note.getBeamMode());
    }

    private static boolean isAbcBeamableTimedEvent(AbcBeamEventInfo info) {
        return info != null && info.isTimed() && !info.isGrace() && info.getLevels() > 0;
    }

    private static void flushAbcBeamGroup(List<AbcBeamEventInfo> infos, List<Integer> indices,
            Map<Integer, AbcBeamAssignment> assignmentByIndex) {
        List<Integer> chordIndices = new ArrayList<Integer>();
        for (Integer index : indices) {
            AbcBeamEventInfo info = infos.get(index.intValue());
            if (info.isChord() && !info.isGrace()) {
                chordIndices.add(index);
            }
        }
        if (chordIndices.size() < 2) {
            return;
        }
        for (int groupIndex = 0; groupIndex < chordIndices.size(); groupIndex++) {
            int index = chordIndices.get(groupIndex).intValue();
            AbcBeamEventInfo info = infos.get(index);
            if (info.getLevels() <= 0) {
                continue;
            }
            String state = groupIndex == 0 ? "begin" : (groupIndex == chordIndices.size() - 1 ? "end" : "continue");
            assignmentByIndex.put(Integer.valueOf(index), new AbcBeamAssignment(state, info.getLevels()));
        }
    }

    private static int beamLevelsFromType(String typeText) {
        String type = trimToEmpty(typeText).toLowerCase();
        if ("eighth".equals(type)) {
            return 1;
        }
        if ("16th".equals(type)) {
            return 2;
        }
        if ("32nd".equals(type)) {
            return 3;
        }
        if ("64th".equals(type)) {
            return 4;
        }
        return 0;
    }

    private static String repeat(String text, int count) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            builder.append(text);
        }
        return builder.toString();
    }

    public static final class Fraction {
        private final int num;
        private final int den;

        public Fraction(int num, int den) {
            this.num = num;
            this.den = den;
        }

        public int getNum() {
            return num;
        }

        public int getDen() {
            return den;
        }

        private Fraction copy() {
            return new Fraction(num, den);
        }
    }

    public static final class AbcMeter {
        private final int beats;
        private final int beatType;

        public AbcMeter(int beats, int beatType) {
            this.beats = beats;
            this.beatType = beatType;
        }

        public int getBeats() {
            return beats;
        }

        public int getBeatType() {
            return beatType;
        }
    }

    public static final class AbcKeyInfo {
        private final int fifths;

        public AbcKeyInfo(int fifths) {
            this.fifths = fifths;
        }

        public int getFifths() {
            return fifths;
        }
    }

    public static final class AbcVoiceMeasureMetaByIndex {
        private final Map<Integer, Integer> keyByMeasure;
        private final Map<Integer, AbcMeter> meterByMeasure;
        private final Map<Integer, Integer> tempoByMeasure;
        private final Map<Integer, AbcMeasureMeta> measureMetaByIndex;

        public AbcVoiceMeasureMetaByIndex(Map<Integer, Integer> keyByMeasure, Map<Integer, AbcMeter> meterByMeasure,
                Map<Integer, Integer> tempoByMeasure, Map<Integer, AbcMeasureMeta> measureMetaByIndex) {
            this.keyByMeasure = keyByMeasure == null ? new LinkedHashMap<Integer, Integer>() : keyByMeasure;
            this.meterByMeasure = meterByMeasure == null ? new LinkedHashMap<Integer, AbcMeter>() : meterByMeasure;
            this.tempoByMeasure = tempoByMeasure == null ? new LinkedHashMap<Integer, Integer>() : tempoByMeasure;
            this.measureMetaByIndex = measureMetaByIndex == null ? new LinkedHashMap<Integer, AbcMeasureMeta>()
                    : measureMetaByIndex;
        }

        public Map<Integer, Integer> getKeyByMeasure() {
            return keyByMeasure;
        }

        public Map<Integer, AbcMeter> getMeterByMeasure() {
            return meterByMeasure;
        }

        public Map<Integer, Integer> getTempoByMeasure() {
            return tempoByMeasure;
        }

        public Map<Integer, AbcMeasureMeta> getMeasureMetaByIndex() {
            return measureMetaByIndex;
        }
    }

    public static final class AbcParsedStaffVoice {
        private final String voiceId;
        private final int staff;
        private final String clef;
        private final List<List<AbcMeasureNote>> measures;

        public AbcParsedStaffVoice(int staff, String clef) {
            this("", staff, clef, new ArrayList<List<AbcMeasureNote>>());
        }

        public AbcParsedStaffVoice(String voiceId, int staff, String clef, List<List<AbcMeasureNote>> measures) {
            this.voiceId = trimToEmpty(voiceId);
            this.staff = staff;
            this.clef = trimToEmpty(clef);
            this.measures = measures == null ? new ArrayList<List<AbcMeasureNote>>() : measures;
        }

        public String getVoiceId() {
            return voiceId;
        }

        public int getStaff() {
            return staff;
        }

        public String getClef() {
            return clef;
        }

        public List<AbcMeasureNote> getMeasure(int measureIndex) {
            if (measureIndex < 0 || measureIndex >= measures.size() || measures.get(measureIndex) == null) {
                return new ArrayList<AbcMeasureNote>();
            }
            return measures.get(measureIndex);
        }

        public List<List<AbcMeasureNote>> getMeasures() {
            return measures;
        }
    }

    public static final class AbcParsedPartHeader {
        private final String clef;
        private final AbcTransposeMeta transpose;
        private final List<AbcParsedStaffVoice> staffVoices;

        public AbcParsedPartHeader(String clef, AbcTransposeMeta transpose, List<AbcParsedStaffVoice> staffVoices) {
            this.clef = trimToEmpty(clef);
            this.transpose = transpose;
            this.staffVoices = staffVoices == null ? new ArrayList<AbcParsedStaffVoice>() : staffVoices;
        }

        public String getClef() {
            return clef;
        }

        public AbcTransposeMeta getTranspose() {
            return transpose;
        }

        public List<AbcParsedStaffVoice> getStaffVoices() {
            return staffVoices;
        }
    }

    public static final class AbcMeasureHeaderXml {
        private final String headerXml;
        private final String tempoDirectionXml;

        public AbcMeasureHeaderXml(String headerXml, String tempoDirectionXml) {
            this.headerXml = headerXml == null ? "" : headerXml;
            this.tempoDirectionXml = tempoDirectionXml == null ? "" : tempoDirectionXml;
        }

        public String getHeaderXml() {
            return headerXml;
        }

        public String getTempoDirectionXml() {
            return tempoDirectionXml;
        }
    }

    public static final class AbcParsedPartRenderData {
        private final String voiceId;
        private final List<List<AbcMeasureNote>> measures;
        private final Map<Integer, Integer> keyByMeasure;
        private final Map<Integer, AbcMeter> meterByMeasure;
        private final Map<Integer, Integer> tempoByMeasure;
        private final Map<Integer, AbcMeasureMeta> measureMetaByIndex;

        public AbcParsedPartRenderData(List<List<AbcMeasureNote>> measures, Map<Integer, Integer> keyByMeasure,
                Map<Integer, AbcMeter> meterByMeasure, Map<Integer, Integer> tempoByMeasure,
                Map<Integer, AbcMeasureMeta> measureMetaByIndex) {
            this("", measures, keyByMeasure, meterByMeasure, tempoByMeasure, measureMetaByIndex);
        }

        public AbcParsedPartRenderData(String voiceId, List<List<AbcMeasureNote>> measures,
                Map<Integer, Integer> keyByMeasure, Map<Integer, AbcMeter> meterByMeasure,
                Map<Integer, Integer> tempoByMeasure, Map<Integer, AbcMeasureMeta> measureMetaByIndex) {
            this.voiceId = trimToEmpty(voiceId);
            this.measures = measures == null ? new ArrayList<List<AbcMeasureNote>>() : measures;
            this.keyByMeasure = keyByMeasure == null ? new LinkedHashMap<Integer, Integer>() : keyByMeasure;
            this.meterByMeasure = meterByMeasure == null ? new LinkedHashMap<Integer, AbcMeter>() : meterByMeasure;
            this.tempoByMeasure = tempoByMeasure == null ? new LinkedHashMap<Integer, Integer>() : tempoByMeasure;
            this.measureMetaByIndex = measureMetaByIndex == null ? new LinkedHashMap<Integer, AbcMeasureMeta>()
                    : measureMetaByIndex;
        }

        public String getVoiceId() {
            return voiceId;
        }

        public List<AbcMeasureNote> getMeasure(int measureIndex) {
            if (measureIndex < 0 || measureIndex >= measures.size() || measures.get(measureIndex) == null) {
                return new ArrayList<AbcMeasureNote>();
            }
            return measures.get(measureIndex);
        }

        public List<List<AbcMeasureNote>> getMeasures() {
            return measures;
        }

        public Map<Integer, Integer> getKeyByMeasure() {
            return keyByMeasure;
        }

        public Map<Integer, AbcMeter> getMeterByMeasure() {
            return meterByMeasure;
        }

        public Map<Integer, Integer> getTempoByMeasure() {
            return tempoByMeasure;
        }

        public Map<Integer, AbcMeasureMeta> getMeasureMetaByIndex() {
            return measureMetaByIndex;
        }
    }

    public static final class AbcParsedPart {
        private final String partId;
        private final String partName;
        private final String voiceId;
        private final String clef;
        private final AbcTransposeMeta transpose;
        private final List<AbcParsedStaffVoice> staffVoices;
        private final List<List<AbcMeasureNote>> measures;
        private final Map<Integer, Integer> keyByMeasure;
        private final Map<Integer, AbcMeter> meterByMeasure;
        private final Map<Integer, Integer> tempoByMeasure;
        private final Map<Integer, AbcMeasureMeta> measureMetaByIndex;

        public AbcParsedPart(String partId, String partName) {
            this(partId, partName, "", "", null, new ArrayList<AbcParsedStaffVoice>(),
                    new ArrayList<List<AbcMeasureNote>>(), new LinkedHashMap<Integer, Integer>(),
                    new LinkedHashMap<Integer, AbcMeter>(), new LinkedHashMap<Integer, Integer>(),
                    new LinkedHashMap<Integer, AbcMeasureMeta>());
        }

        public AbcParsedPart(String partId, String partName, String voiceId, String clef,
                AbcTransposeMeta transpose, List<AbcParsedStaffVoice> staffVoices,
                List<List<AbcMeasureNote>> measures, Map<Integer, Integer> keyByMeasure,
                Map<Integer, AbcMeter> meterByMeasure, Map<Integer, Integer> tempoByMeasure,
                Map<Integer, AbcMeasureMeta> measureMetaByIndex) {
            this.partId = trimToEmpty(partId).length() == 0 ? "P1" : trimToEmpty(partId);
            this.partName = trimToEmpty(partName);
            this.voiceId = trimToEmpty(voiceId);
            this.clef = trimToEmpty(clef);
            this.transpose = transpose;
            this.staffVoices = staffVoices == null ? new ArrayList<AbcParsedStaffVoice>() : staffVoices;
            this.measures = measures == null ? new ArrayList<List<AbcMeasureNote>>() : measures;
            this.keyByMeasure = keyByMeasure == null ? new LinkedHashMap<Integer, Integer>() : keyByMeasure;
            this.meterByMeasure = meterByMeasure == null ? new LinkedHashMap<Integer, AbcMeter>() : meterByMeasure;
            this.tempoByMeasure = tempoByMeasure == null ? new LinkedHashMap<Integer, Integer>() : tempoByMeasure;
            this.measureMetaByIndex = measureMetaByIndex == null ? new LinkedHashMap<Integer, AbcMeasureMeta>()
                    : measureMetaByIndex;
        }

        public String getPartId() {
            return partId;
        }

        public String getPartName() {
            return partName;
        }

        public String getVoiceId() {
            return voiceId;
        }

        public String getClef() {
            return clef;
        }

        public AbcTransposeMeta getTranspose() {
            return transpose;
        }

        public List<AbcParsedStaffVoice> getStaffVoices() {
            return staffVoices;
        }

        public List<List<AbcMeasureNote>> getMeasures() {
            return measures;
        }

        public Map<Integer, Integer> getKeyByMeasure() {
            return keyByMeasure;
        }

        public Map<Integer, AbcMeter> getMeterByMeasure() {
            return meterByMeasure;
        }

        public Map<Integer, Integer> getTempoByMeasure() {
            return tempoByMeasure;
        }

        public Map<Integer, AbcMeasureMeta> getMeasureMetaByIndex() {
            return measureMetaByIndex;
        }

        public AbcParsedPartHeader toPartHeader() {
            return new AbcParsedPartHeader(clef, transpose, staffVoices);
        }

        public AbcParsedPartRenderData toRenderData() {
            return new AbcParsedPartRenderData(voiceId, measures, keyByMeasure, meterByMeasure, tempoByMeasure,
                    measureMetaByIndex);
        }

        public AbcParsedPart withClef(String resolvedClef) {
            return new AbcParsedPart(partId, partName, voiceId, resolvedClef, transpose, staffVoices, measures,
                    keyByMeasure, meterByMeasure, tempoByMeasure, measureMetaByIndex);
        }
    }

    public static final class AbcParsedMeta {
        private final String title;
        private final String composer;
        private final AbcMeter meter;
        private final AbcKeyInfo keyInfo;
        private final Integer tempoBpm;

        public AbcParsedMeta() {
            this("mikuscore", "Unknown", new AbcMeter(4, 4), new AbcKeyInfo(0), null);
        }

        public AbcParsedMeta(String title, String composer, AbcMeter meter, AbcKeyInfo keyInfo, Integer tempoBpm) {
            this.title = trimToEmpty(title);
            this.composer = trimToEmpty(composer);
            this.meter = meter == null ? new AbcMeter(4, 4) : meter;
            this.keyInfo = keyInfo == null ? new AbcKeyInfo(0) : keyInfo;
            this.tempoBpm = tempoBpm;
        }

        public String getTitle() {
            return title;
        }

        public String getComposer() {
            return composer;
        }

        public AbcMeter getMeter() {
            return meter;
        }

        public AbcKeyInfo getKeyInfo() {
            return keyInfo;
        }

        public Integer getTempoBpm() {
            return tempoBpm;
        }
    }

    public static final class AbcParsedResult {
        private final AbcParsedMeta meta;
        private final List<AbcParsedPart> parts;
        private final List<String> warnings;
        private final List<AbcImportDiagnostic> diagnostics;

        public AbcParsedResult(AbcParsedMeta meta, List<AbcParsedPart> parts, List<String> warnings,
                List<AbcImportDiagnostic> diagnostics) {
            this.meta = meta == null ? new AbcParsedMeta() : meta;
            this.parts = parts == null ? new ArrayList<AbcParsedPart>() : parts;
            this.warnings = warnings == null ? new ArrayList<String>() : warnings;
            this.diagnostics = diagnostics == null ? new ArrayList<AbcImportDiagnostic>() : diagnostics;
        }

        public AbcParsedMeta getMeta() {
            return meta;
        }

        public List<AbcParsedPart> getParts() {
            return parts;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public List<AbcImportDiagnostic> getDiagnostics() {
            return diagnostics;
        }
    }

    public static final class AbcImportOptions {
        private final Boolean debugMetadata;
        private final Boolean debugPrettyPrint;
        private final Boolean sourceMetadata;
        private final Boolean overfullCompatibilityMode;

        public AbcImportOptions() {
            this(null, null, null, null);
        }

        public AbcImportOptions(Boolean debugMetadata, Boolean debugPrettyPrint, Boolean sourceMetadata,
                Boolean overfullCompatibilityMode) {
            this.debugMetadata = debugMetadata;
            this.debugPrettyPrint = debugPrettyPrint;
            this.sourceMetadata = sourceMetadata;
            this.overfullCompatibilityMode = overfullCompatibilityMode;
        }

        public Boolean getDebugMetadata() {
            return debugMetadata;
        }

        public Boolean getDebugPrettyPrint() {
            return debugPrettyPrint;
        }

        public Boolean getSourceMetadata() {
            return sourceMetadata;
        }

        public Boolean getOverfullCompatibilityMode() {
            return overfullCompatibilityMode;
        }
    }

    public static final class AbcMusicXmlExportContext {
        private final List<AbcParsedPart> resolvedParts;
        private final int measureCount;
        private final String title;
        private final String composer;
        private final int beats;
        private final int beatType;
        private final int defaultFifths;
        private final int divisions;
        private final int beatDiv;
        private final int measureDurationDiv;
        private final String emptyMeasureRestType;
        private final Integer tempoBpm;

        public AbcMusicXmlExportContext(List<AbcParsedPart> resolvedParts, int measureCount, String title,
                String composer, int beats, int beatType, int defaultFifths, int divisions, int beatDiv,
                int measureDurationDiv, String emptyMeasureRestType, Integer tempoBpm) {
            this.resolvedParts = resolvedParts == null ? new ArrayList<AbcParsedPart>() : resolvedParts;
            this.measureCount = measureCount;
            this.title = trimToEmpty(title);
            this.composer = trimToEmpty(composer);
            this.beats = beats;
            this.beatType = beatType;
            this.defaultFifths = defaultFifths;
            this.divisions = divisions;
            this.beatDiv = beatDiv;
            this.measureDurationDiv = measureDurationDiv;
            this.emptyMeasureRestType = trimToEmpty(emptyMeasureRestType);
            this.tempoBpm = tempoBpm;
        }

        public List<AbcParsedPart> getResolvedParts() {
            return resolvedParts;
        }

        public int getMeasureCount() {
            return measureCount;
        }

        public String getTitle() {
            return title;
        }

        public String getComposer() {
            return composer;
        }

        public int getBeats() {
            return beats;
        }

        public int getBeatType() {
            return beatType;
        }

        public int getDefaultFifths() {
            return defaultFifths;
        }

        public int getDivisions() {
            return divisions;
        }

        public int getBeatDiv() {
            return beatDiv;
        }

        public int getMeasureDurationDiv() {
            return measureDurationDiv;
        }

        public String getEmptyMeasureRestType() {
            return emptyMeasureRestType;
        }

        public Integer getTempoBpm() {
            return tempoBpm;
        }
    }

    public static final class AbcPartRenderState {
        private final int currentPartFifths;
        private final AbcMeter currentPartMeter;
        private final Integer currentPartTempo;

        public AbcPartRenderState(int currentPartFifths, AbcMeter currentPartMeter, Integer currentPartTempo) {
            this.currentPartFifths = currentPartFifths;
            this.currentPartMeter = currentPartMeter == null ? new AbcMeter(4, 4) : currentPartMeter;
            this.currentPartTempo = currentPartTempo;
        }

        public int getCurrentPartFifths() {
            return currentPartFifths;
        }

        public AbcMeter getCurrentPartMeter() {
            return currentPartMeter;
        }

        public Integer getCurrentPartTempo() {
            return currentPartTempo;
        }
    }

    public static final class AbcPartMeasureRenderContext {
        private final List<AbcMeasureNote> notes;
        private final AbcMeasureMeta measureMeta;
        private final Integer hintedFifths;
        private final AbcMeter hintedMeter;
        private final Integer hintedTempo;
        private final AbcPartRenderState nextState;
        private final int currentMeasureDurationDiv;
        private final boolean inferredImplicitPickup;

        public AbcPartMeasureRenderContext(List<AbcMeasureNote> notes, AbcMeasureMeta measureMeta,
                Integer hintedFifths, AbcMeter hintedMeter, Integer hintedTempo, AbcPartRenderState nextState,
                int currentMeasureDurationDiv, boolean inferredImplicitPickup) {
            this.notes = notes == null ? new ArrayList<AbcMeasureNote>() : notes;
            this.measureMeta = measureMeta;
            this.hintedFifths = hintedFifths;
            this.hintedMeter = hintedMeter;
            this.hintedTempo = hintedTempo;
            this.nextState = nextState;
            this.currentMeasureDurationDiv = currentMeasureDurationDiv;
            this.inferredImplicitPickup = inferredImplicitPickup;
        }

        public List<AbcMeasureNote> getNotes() {
            return notes;
        }

        public AbcMeasureMeta getMeasureMeta() {
            return measureMeta;
        }

        public Integer getHintedFifths() {
            return hintedFifths;
        }

        public AbcMeter getHintedMeter() {
            return hintedMeter;
        }

        public Integer getHintedTempo() {
            return hintedTempo;
        }

        public AbcPartRenderState getNextState() {
            return nextState;
        }

        public int getCurrentMeasureDurationDiv() {
            return currentMeasureDurationDiv;
        }

        public boolean isInferredImplicitPickup() {
            return inferredImplicitPickup;
        }
    }

    public static final class AbcImportDiagnostic {
        private final String level;
        private final String code;
        private final String fmt;
        private final String message;
        private final String voiceId;
        private final Integer measure;
        private final String action;
        private final Integer movedEvents;

        public AbcImportDiagnostic(String level, String code, String fmt, String message, String voiceId,
                Integer measure, String action, Integer movedEvents) {
            this.level = trimToEmpty(level).length() == 0 ? "warn" : trimToEmpty(level);
            this.code = trimToEmpty(code);
            this.fmt = trimToEmpty(fmt).length() == 0 ? "abc" : trimToEmpty(fmt);
            this.message = trimToEmpty(message);
            this.voiceId = trimToEmpty(voiceId);
            this.measure = measure;
            this.action = trimToEmpty(action);
            this.movedEvents = movedEvents;
        }

        public String getLevel() {
            return level;
        }

        public String getCode() {
            return code;
        }

        public String getFmt() {
            return fmt;
        }

        public String getMessage() {
            return message;
        }

        public String getVoiceId() {
            return voiceId;
        }

        public Integer getMeasure() {
            return measure;
        }

        public String getAction() {
            return action;
        }

        public Integer getMovedEvents() {
            return movedEvents;
        }
    }

    public static final class AbcRenderedMeasureMiscContext {
        private final AbcParsedPartRenderData part;
        private final int partIndex;
        private final int measureNo;
        private final List<AbcMeasureNote> notes;
        private final boolean debugMetadata;
        private final boolean sourceMetadata;
        private final List<AbcImportDiagnostic> diagnostics;
        private final String abcSource;

        public AbcRenderedMeasureMiscContext(AbcParsedPartRenderData part, int partIndex, int measureNo,
                List<AbcMeasureNote> notes, boolean debugMetadata, boolean sourceMetadata,
                List<AbcImportDiagnostic> diagnostics, String abcSource) {
            this.part = part;
            this.partIndex = partIndex;
            this.measureNo = measureNo;
            this.notes = notes == null ? new ArrayList<AbcMeasureNote>() : notes;
            this.debugMetadata = debugMetadata;
            this.sourceMetadata = sourceMetadata;
            this.diagnostics = diagnostics == null ? new ArrayList<AbcImportDiagnostic>() : diagnostics;
            this.abcSource = abcSource == null ? "" : abcSource;
        }

        public AbcParsedPartRenderData getPart() {
            return part;
        }

        public int getPartIndex() {
            return partIndex;
        }

        public int getMeasureNo() {
            return measureNo;
        }

        public List<AbcMeasureNote> getNotes() {
            return notes;
        }

        public boolean isDebugMetadata() {
            return debugMetadata;
        }

        public boolean isSourceMetadata() {
            return sourceMetadata;
        }

        public List<AbcImportDiagnostic> getDiagnostics() {
            return diagnostics;
        }

        public String getAbcSource() {
            return abcSource;
        }
    }

    public static final class AbcRenderedMeasureMiscXml {
        private final String debugMiscXml;
        private final String diagMiscXml;
        private final String sourceMiscXml;

        public AbcRenderedMeasureMiscXml(String debugMiscXml, String diagMiscXml, String sourceMiscXml) {
            this.debugMiscXml = debugMiscXml == null ? "" : debugMiscXml;
            this.diagMiscXml = diagMiscXml == null ? "" : diagMiscXml;
            this.sourceMiscXml = sourceMiscXml == null ? "" : sourceMiscXml;
        }

        public String getDebugMiscXml() {
            return debugMiscXml;
        }

        public String getDiagMiscXml() {
            return diagMiscXml;
        }

        public String getSourceMiscXml() {
            return sourceMiscXml;
        }
    }

    public static final class AbcRenderedPartMeasureContext {
        private final AbcParsedPartHeader partHeader;
        private final AbcParsedPartRenderData part;
        private final int partIndex;
        private final int measureIndex;
        private final int measureNo;
        private final List<AbcMeasureNote> notes;
        private final AbcMeasureMeta measureMeta;
        private final Integer hintedFifths;
        private final AbcMeter hintedMeter;
        private final Integer hintedTempo;
        private final int currentPartFifths;
        private final AbcMeter currentPartMeter;
        private final Integer currentPartTempo;
        private final int currentMeasureDurationDiv;
        private final boolean inferredImplicitPickup;
        private final boolean debugMetadata;
        private final boolean sourceMetadata;
        private final List<AbcImportDiagnostic> diagnostics;
        private final String abcSource;
        private final AbcMeasureNotesXmlBuilder buildMeasureNotesXml;

        public AbcRenderedPartMeasureContext(AbcParsedPartHeader partHeader, AbcParsedPartRenderData part,
                int partIndex, int measureIndex, int measureNo, List<AbcMeasureNote> notes,
                AbcMeasureMeta measureMeta, Integer hintedFifths, AbcMeter hintedMeter, Integer hintedTempo,
                int currentPartFifths, AbcMeter currentPartMeter, Integer currentPartTempo,
                int currentMeasureDurationDiv, boolean inferredImplicitPickup, boolean debugMetadata,
                boolean sourceMetadata, List<AbcImportDiagnostic> diagnostics, String abcSource,
                AbcMeasureNotesXmlBuilder buildMeasureNotesXml) {
            this.partHeader = partHeader == null
                    ? new AbcParsedPartHeader("", null, new ArrayList<AbcParsedStaffVoice>())
                    : partHeader;
            this.part = part;
            this.partIndex = partIndex;
            this.measureIndex = measureIndex;
            this.measureNo = measureNo;
            this.notes = notes == null ? new ArrayList<AbcMeasureNote>() : notes;
            this.measureMeta = measureMeta;
            this.hintedFifths = hintedFifths;
            this.hintedMeter = hintedMeter;
            this.hintedTempo = hintedTempo;
            this.currentPartFifths = currentPartFifths;
            this.currentPartMeter = currentPartMeter == null ? new AbcMeter(4, 4) : currentPartMeter;
            this.currentPartTempo = currentPartTempo;
            this.currentMeasureDurationDiv = currentMeasureDurationDiv;
            this.inferredImplicitPickup = inferredImplicitPickup;
            this.debugMetadata = debugMetadata;
            this.sourceMetadata = sourceMetadata;
            this.diagnostics = diagnostics == null ? new ArrayList<AbcImportDiagnostic>() : diagnostics;
            this.abcSource = abcSource == null ? "" : abcSource;
            this.buildMeasureNotesXml = buildMeasureNotesXml == null ? new AbcMeasureNotesXmlBuilder() {
                public String build(List<AbcMeasureNote> notes, Integer staffNumber) {
                    return "";
                }
            } : buildMeasureNotesXml;
        }

        public AbcParsedPartHeader getPartHeader() {
            return partHeader;
        }

        public AbcParsedPartRenderData getPart() {
            return part;
        }

        public int getPartIndex() {
            return partIndex;
        }

        public int getMeasureIndex() {
            return measureIndex;
        }

        public int getMeasureNo() {
            return measureNo;
        }

        public List<AbcMeasureNote> getNotes() {
            return notes;
        }

        public AbcMeasureMeta getMeasureMeta() {
            return measureMeta;
        }

        public Integer getHintedFifths() {
            return hintedFifths;
        }

        public AbcMeter getHintedMeter() {
            return hintedMeter;
        }

        public Integer getHintedTempo() {
            return hintedTempo;
        }

        public int getCurrentPartFifths() {
            return currentPartFifths;
        }

        public AbcMeter getCurrentPartMeter() {
            return currentPartMeter;
        }

        public Integer getCurrentPartTempo() {
            return currentPartTempo;
        }

        public int getCurrentMeasureDurationDiv() {
            return currentMeasureDurationDiv;
        }

        public boolean isInferredImplicitPickup() {
            return inferredImplicitPickup;
        }

        public boolean isDebugMetadata() {
            return debugMetadata;
        }

        public boolean isSourceMetadata() {
            return sourceMetadata;
        }

        public List<AbcImportDiagnostic> getDiagnostics() {
            return diagnostics;
        }

        public String getAbcSource() {
            return abcSource;
        }

        public AbcMeasureNotesXmlBuilder getBuildMeasureNotesXml() {
            return buildMeasureNotesXml;
        }
    }

    private static final class AbcBeamNoteEvent {
        private final AbcMeasureNote note;
        private final int noteIndex;

        private AbcBeamNoteEvent(AbcMeasureNote note, int noteIndex) {
            this.note = note;
            this.noteIndex = noteIndex;
        }

        private AbcMeasureNote getNote() {
            return note;
        }

        private int getNoteIndex() {
            return noteIndex;
        }
    }

    private static final class AbcBeamEventInfo {
        private final boolean timed;
        private final boolean chord;
        private final boolean grace;
        private final int durationDiv;
        private final int levels;
        private final String explicitMode;

        private AbcBeamEventInfo(boolean timed, boolean chord, boolean grace, int durationDiv, int levels,
                String explicitMode) {
            this.timed = timed;
            this.chord = chord;
            this.grace = grace;
            this.durationDiv = durationDiv;
            this.levels = levels;
            this.explicitMode = trimToEmpty(explicitMode);
        }

        private boolean isTimed() {
            return timed;
        }

        private boolean isChord() {
            return chord;
        }

        private boolean isGrace() {
            return grace;
        }

        private int getDurationDiv() {
            return durationDiv;
        }

        private int getLevels() {
            return levels;
        }

        private String getExplicitMode() {
            return explicitMode;
        }
    }

    private static final class AbcBeamAssignment {
        private final String state;
        private final int levels;

        private AbcBeamAssignment(String state, int levels) {
            this.state = trimToEmpty(state);
            this.levels = levels;
        }

        private String getState() {
            return state;
        }

        private int getLevels() {
            return levels;
        }
    }

    public static final class AbcMeasureNote {
        private final String voice;
        private final int duration;
        private final boolean chord;
        private final boolean grace;
        private final boolean rest;
        private final String step;
        private final Integer octave;
        private final Integer alter;
        private final String type;
        private final Integer staff;
        private final String accidentalText;
        private final boolean accidentalEditorial;
        private final boolean accidentalCautionary;
        private final boolean tieStart;
        private final boolean tieStop;
        private final boolean graceSlash;
        private final String beamMode;
        private final String lyricText;
        private final String lyricSyllabic;
        private final boolean lyricExtend;
        private final Integer timeModificationActual;
        private final Integer timeModificationNormal;
        private final List<String> annotations;
        private final boolean segno;
        private final boolean coda;
        private final String rehearsalMark;
        private final boolean fine;
        private final boolean daCapo;
        private final boolean dalSegno;
        private final boolean toCoda;
        private final boolean crescendoStart;
        private final boolean crescendoStop;
        private final boolean diminuendoStart;
        private final boolean diminuendoStop;
        private final String dynamicMark;
        private final boolean sfz;

        public AbcMeasureNote(String voice, int duration, boolean chord, boolean grace) {
            this(voice, duration, chord, grace, false, "C", Integer.valueOf(4), Integer.valueOf(0), "quarter");
        }

        public AbcMeasureNote(String voice, int duration, boolean chord, boolean grace, boolean rest, String step,
                Integer octave, Integer alter, String type) {
            this(voice, duration, chord, grace, rest, step, octave, alter, type, null, "", false, false, false, false,
                    false);
        }

        public AbcMeasureNote(String voice, int duration, boolean chord, boolean grace, boolean rest, String step,
                Integer octave, Integer alter, String type, Integer staff, String accidentalText,
                boolean accidentalEditorial, boolean accidentalCautionary, boolean tieStart, boolean tieStop,
                boolean graceSlash) {
            this(voice, duration, chord, grace, rest, step, octave, alter, type, staff, accidentalText,
                    accidentalEditorial, accidentalCautionary, tieStart, tieStop, graceSlash, "");
        }

        public AbcMeasureNote(String voice, int duration, boolean chord, boolean grace, boolean rest, String step,
                Integer octave, Integer alter, String type, Integer staff, String accidentalText,
                boolean accidentalEditorial, boolean accidentalCautionary, boolean tieStart, boolean tieStop,
                boolean graceSlash, String beamMode) {
            this(voice, duration, chord, grace, rest, step, octave, alter, type, staff, accidentalText,
                    accidentalEditorial, accidentalCautionary, tieStart, tieStop, graceSlash, beamMode, "", "single",
                    false, null, null);
        }

        public AbcMeasureNote(String voice, int duration, boolean chord, boolean grace, boolean rest, String step,
                Integer octave, Integer alter, String type, Integer staff, String accidentalText,
                boolean accidentalEditorial, boolean accidentalCautionary, boolean tieStart, boolean tieStop,
                boolean graceSlash, String beamMode, String lyricText, String lyricSyllabic, boolean lyricExtend,
                Integer timeModificationActual, Integer timeModificationNormal) {
            this(voice, duration, chord, grace, rest, step, octave, alter, type, staff, accidentalText,
                    accidentalEditorial, accidentalCautionary, tieStart, tieStop, graceSlash, beamMode, lyricText,
                    lyricSyllabic, lyricExtend, timeModificationActual, timeModificationNormal,
                    new ArrayList<String>(), false, false, "", false, false, false, false, false, false, false,
                    false, "", false);
        }

        public AbcMeasureNote(String voice, int duration, boolean chord, boolean grace, boolean rest, String step,
                Integer octave, Integer alter, String type, Integer staff, String accidentalText,
                boolean accidentalEditorial, boolean accidentalCautionary, boolean tieStart, boolean tieStop,
                boolean graceSlash, String beamMode, String lyricText, String lyricSyllabic, boolean lyricExtend,
                Integer timeModificationActual, Integer timeModificationNormal, List<String> annotations,
                boolean segno, boolean coda, String rehearsalMark, boolean fine, boolean daCapo, boolean dalSegno,
                boolean toCoda, boolean crescendoStart, boolean crescendoStop, boolean diminuendoStart,
                boolean diminuendoStop, String dynamicMark, boolean sfz) {
            this.voice = voice;
            this.duration = duration;
            this.chord = chord;
            this.grace = grace;
            this.rest = rest;
            this.step = trimToEmpty(step);
            this.octave = octave;
            this.alter = alter;
            this.type = trimToEmpty(type);
            this.staff = staff;
            this.accidentalText = trimToEmpty(accidentalText);
            this.accidentalEditorial = accidentalEditorial;
            this.accidentalCautionary = accidentalCautionary;
            this.tieStart = tieStart;
            this.tieStop = tieStop;
            this.graceSlash = graceSlash;
            this.beamMode = trimToEmpty(beamMode);
            this.lyricText = trimToEmpty(lyricText);
            this.lyricSyllabic = trimToEmpty(lyricSyllabic);
            this.lyricExtend = lyricExtend;
            this.timeModificationActual = timeModificationActual;
            this.timeModificationNormal = timeModificationNormal;
            this.annotations = annotations == null ? new ArrayList<String>() : annotations;
            this.segno = segno;
            this.coda = coda;
            this.rehearsalMark = trimToEmpty(rehearsalMark);
            this.fine = fine;
            this.daCapo = daCapo;
            this.dalSegno = dalSegno;
            this.toCoda = toCoda;
            this.crescendoStart = crescendoStart;
            this.crescendoStop = crescendoStop;
            this.diminuendoStart = diminuendoStart;
            this.diminuendoStop = diminuendoStop;
            this.dynamicMark = trimToEmpty(dynamicMark);
            this.sfz = sfz;
        }

        public String getVoice() {
            return voice;
        }

        public int getDuration() {
            return duration;
        }

        public boolean isChord() {
            return chord;
        }

        public boolean isGrace() {
            return grace;
        }

        public boolean isRest() {
            return rest;
        }

        public String getStep() {
            return step;
        }

        public Integer getOctave() {
            return octave;
        }

        public Integer getAlter() {
            return alter;
        }

        public String getType() {
            return type;
        }

        public Integer getStaff() {
            return staff;
        }

        public String getAccidentalText() {
            return accidentalText;
        }

        public boolean isAccidentalEditorial() {
            return accidentalEditorial;
        }

        public boolean isAccidentalCautionary() {
            return accidentalCautionary;
        }

        public boolean isTieStart() {
            return tieStart;
        }

        public boolean isTieStop() {
            return tieStop;
        }

        public boolean isGraceSlash() {
            return graceSlash;
        }

        public String getBeamMode() {
            return beamMode;
        }

        public String getLyricText() {
            return lyricText;
        }

        public String getLyricSyllabic() {
            return lyricSyllabic;
        }

        public boolean isLyricExtend() {
            return lyricExtend;
        }

        public Integer getTimeModificationActual() {
            return timeModificationActual;
        }

        public Integer getTimeModificationNormal() {
            return timeModificationNormal;
        }

        public List<String> getAnnotations() {
            return annotations;
        }

        public boolean isSegno() {
            return segno;
        }

        public boolean isCoda() {
            return coda;
        }

        public String getRehearsalMark() {
            return rehearsalMark;
        }

        public boolean isFine() {
            return fine;
        }

        public boolean isDaCapo() {
            return daCapo;
        }

        public boolean isDalSegno() {
            return dalSegno;
        }

        public boolean isToCoda() {
            return toCoda;
        }

        public boolean isCrescendoStart() {
            return crescendoStart;
        }

        public boolean isCrescendoStop() {
            return crescendoStop;
        }

        public boolean isDiminuendoStart() {
            return diminuendoStart;
        }

        public boolean isDiminuendoStop() {
            return diminuendoStop;
        }

        public String getDynamicMark() {
            return dynamicMark;
        }

        public boolean isSfz() {
            return sfz;
        }
    }

    public static final class AbcMeasureMeta {
        private final String number;
        private final boolean implicit;
        private final boolean repeatStart;
        private final boolean repeatEnd;
        private final Integer repeatTimes;
        private final String endingStart;
        private final String endingStop;
        private final String endingStopType;

        public AbcMeasureMeta(String number, boolean implicit, boolean repeatStart, boolean repeatEnd,
                Integer repeatTimes, String endingStart, String endingStop, String endingStopType) {
            this.number = number;
            this.implicit = implicit;
            this.repeatStart = repeatStart;
            this.repeatEnd = repeatEnd;
            this.repeatTimes = repeatTimes;
            this.endingStart = endingStart;
            this.endingStop = endingStop;
            this.endingStopType = endingStopType;
        }

        public String getNumber() {
            return number;
        }

        public boolean isImplicit() {
            return implicit;
        }

        public boolean isRepeatStart() {
            return repeatStart;
        }

        public boolean isRepeatEnd() {
            return repeatEnd;
        }

        public Integer getRepeatTimes() {
            return repeatTimes;
        }

        public String getEndingStart() {
            return endingStart;
        }

        public String getEndingStop() {
            return endingStop;
        }

        public String getEndingStopType() {
            return endingStopType;
        }
    }

    public static final class AbcTransposeMeta {
        private final Integer chromatic;
        private final Integer diatonic;

        public AbcTransposeMeta(Integer chromatic, Integer diatonic) {
            this.chromatic = chromatic;
            this.diatonic = diatonic;
        }

        public Integer getChromatic() {
            return chromatic;
        }

        public Integer getDiatonic() {
            return diatonic;
        }
    }

    public interface BodyTextPusher {
        void push(String rawBodyText, int lineNo, String voiceId);
    }

    public interface VoiceDirectiveTailParser {
        AbcParsedVoiceDirectiveTail parse(String raw);
    }

    public interface UserDefinedDecorationParser {
        AbcUserDefinedDecoration parse(String raw);
    }

    public interface DecorationSymbolExpander {
        String expand(String text, Map<String, String> symbolMap);
    }

    public interface InlineVoiceSplitter {
        AbcInlineVoiceSplitResult split(String text, String initialVoiceId);
    }

    public interface OverlaySplitter {
        List<AbcOverlaySegment> split(String text, String baseVoiceId);
    }

    public interface AbcMeasureNotesXmlBuilder {
        String build(List<AbcMeasureNote> notes, Integer staffNumber);
    }

    public static final class AbcImportVoiceRegistry {
        private final List<String> declaredVoiceIds = new ArrayList<String>();
        private final Map<String, String> voiceNameById = new LinkedHashMap<String, String>();
        private final Map<String, String> voiceClefById = new LinkedHashMap<String, String>();
        private final Map<String, AbcTransposeMeta> voiceTransposeById = new LinkedHashMap<String, AbcTransposeMeta>();

        public List<String> getDeclaredVoiceIds() {
            return declaredVoiceIds;
        }

        public Map<String, String> getVoiceNameById() {
            return voiceNameById;
        }

        public Map<String, String> getVoiceClefById() {
            return voiceClefById;
        }

        public Map<String, AbcTransposeMeta> getVoiceTransposeById() {
            return voiceTransposeById;
        }
    }

    public static final class AbcImportLineState {
        private String currentVoiceId = "1";
        private String scoreDirective = "";
        private boolean bodyStarted;
        private String pendingUnsupportedContinuedFieldName = "";

        public String getCurrentVoiceId() {
            return currentVoiceId;
        }

        public void setCurrentVoiceId(String currentVoiceId) {
            this.currentVoiceId = trimToEmpty(currentVoiceId).length() == 0 ? "1" : trimToEmpty(currentVoiceId);
        }

        public String getScoreDirective() {
            return scoreDirective;
        }

        public void setScoreDirective(String scoreDirective) {
            this.scoreDirective = trimToEmpty(scoreDirective);
        }

        public boolean isBodyStarted() {
            return bodyStarted;
        }

        public void setBodyStarted(boolean bodyStarted) {
            this.bodyStarted = bodyStarted;
        }

        public String getPendingUnsupportedContinuedFieldName() {
            return pendingUnsupportedContinuedFieldName;
        }

        public void setPendingUnsupportedContinuedFieldName(String pendingUnsupportedContinuedFieldName) {
            this.pendingUnsupportedContinuedFieldName = trimToEmpty(pendingUnsupportedContinuedFieldName);
        }
    }

    public static final class AbcLyricEntry {
        private final String text;
        private final int lineNo;

        public AbcLyricEntry(String text, int lineNo) {
            this.text = text;
            this.lineNo = lineNo;
        }

        public String getText() {
            return text;
        }

        public int getLineNo() {
            return lineNo;
        }
    }

    public static final class AbcImportBodyEntry {
        private final String text;
        private final int lineNo;
        private final String voiceId;

        public AbcImportBodyEntry(String text, int lineNo, String voiceId) {
            this.text = text == null ? "" : text;
            this.lineNo = lineNo;
            this.voiceId = trimToEmpty(voiceId).length() == 0 ? "1" : trimToEmpty(voiceId);
        }

        public String getText() {
            return text;
        }

        public int getLineNo() {
            return lineNo;
        }

        public String getVoiceId() {
            return voiceId;
        }
    }

    public static final class AbcAppendBodyTextResult {
        private final boolean appended;
        private final String finalVoiceId;

        public AbcAppendBodyTextResult(boolean appended, String finalVoiceId) {
            this.appended = appended;
            this.finalVoiceId = trimToEmpty(finalVoiceId).length() == 0 ? "1" : trimToEmpty(finalVoiceId);
        }

        public boolean isAppended() {
            return appended;
        }

        public String getFinalVoiceId() {
            return finalVoiceId;
        }
    }

    public static final class AbcInlineVoiceSplitResult {
        private final List<AbcInlineVoiceSegment> segments;
        private final String finalVoiceId;

        public AbcInlineVoiceSplitResult(List<AbcInlineVoiceSegment> segments, String finalVoiceId) {
            this.segments = segments == null ? new ArrayList<AbcInlineVoiceSegment>() : segments;
            this.finalVoiceId = trimToEmpty(finalVoiceId).length() == 0 ? "1" : trimToEmpty(finalVoiceId);
        }

        public List<AbcInlineVoiceSegment> getSegments() {
            return segments;
        }

        public String getFinalVoiceId() {
            return finalVoiceId;
        }
    }

    public static final class AbcInlineVoiceSegment {
        private final String voiceId;
        private final String text;

        public AbcInlineVoiceSegment(String voiceId, String text) {
            this.voiceId = trimToEmpty(voiceId).length() == 0 ? "1" : trimToEmpty(voiceId);
            this.text = text == null ? "" : text;
        }

        public String getVoiceId() {
            return voiceId;
        }

        public String getText() {
            return text;
        }
    }

    public static final class AbcOverlaySegment {
        private final String voiceId;
        private final String text;
        private final int overlayIndex;

        public AbcOverlaySegment(String voiceId, String text, int overlayIndex) {
            this.voiceId = trimToEmpty(voiceId).length() == 0 ? "1" : trimToEmpty(voiceId);
            this.text = text == null ? "" : text;
            this.overlayIndex = overlayIndex;
        }

        public String getVoiceId() {
            return voiceId;
        }

        public String getText() {
            return text;
        }

        public int getOverlayIndex() {
            return overlayIndex;
        }
    }

    public static final class AbcParsedVoiceDirectiveTail {
        private final String name;
        private final String clef;
        private final AbcTransposeMeta transpose;
        private final String bodyText;
        private final String skippedText;
        private final List<String> unsupportedKeys;

        public AbcParsedVoiceDirectiveTail(String name, String clef, AbcTransposeMeta transpose, String bodyText,
                String skippedText, List<String> unsupportedKeys) {
            this.name = trimToEmpty(name);
            this.clef = trimToEmpty(clef);
            this.transpose = transpose;
            this.bodyText = trimToEmpty(bodyText);
            this.skippedText = trimToEmpty(skippedText);
            this.unsupportedKeys = unsupportedKeys == null ? new ArrayList<String>() : unsupportedKeys;
        }

        public String getName() {
            return name;
        }

        public String getClef() {
            return clef;
        }

        public AbcTransposeMeta getTranspose() {
            return transpose;
        }

        public String getBodyText() {
            return bodyText;
        }

        public String getSkippedText() {
            return skippedText;
        }

        public List<String> getUnsupportedKeys() {
            return unsupportedKeys;
        }
    }

    public static final class AbcUserDefinedDecoration {
        private final String symbol;
        private final String decoration;

        public AbcUserDefinedDecoration(String symbol, String decoration) {
            this.symbol = trimToEmpty(symbol);
            this.decoration = trimToEmpty(decoration);
        }

        public String getSymbol() {
            return symbol;
        }

        public String getDecoration() {
            return decoration;
        }
    }

    public static final class AbcImportLineProcessorContext {
        private final AbcImportLineState lineState;
        private final List<String> warnings;
        private final Map<String, String> headers;
        private final Map<String, List<AbcLyricEntry>> lyricEntriesByVoice;
        private final Set<String> supportedStandaloneBodyFieldNames;
        private final AbcImportVoiceRegistry voiceRegistry;
        private final Map<String, String> userDefinedDecorationBySymbol;
        private final Map<String, String> trillWidthHintByKey;
        private final Map<String, Integer> keyHintFifthsByKey;
        private final Map<String, AbcMeasureMeta> measureMetaByKey;
        private final Map<String, AbcTransposeMeta> transposeHintByVoiceId;
        private final BodyTextPusher pushBodyText;
        private final VoiceDirectiveTailParser parseVoiceDirectiveTail;
        private final UserDefinedDecorationParser parseUserDefinedDecoration;
        private final DecorationSymbolExpander expandUserDefinedDecorationSymbols;

        public AbcImportLineProcessorContext(AbcImportLineState lineState, List<String> warnings,
                Map<String, String> headers, Map<String, List<AbcLyricEntry>> lyricEntriesByVoice,
                Set<String> supportedStandaloneBodyFieldNames, AbcImportVoiceRegistry voiceRegistry,
                Map<String, String> userDefinedDecorationBySymbol, Map<String, String> trillWidthHintByKey,
                Map<String, Integer> keyHintFifthsByKey, Map<String, AbcMeasureMeta> measureMetaByKey,
                Map<String, AbcTransposeMeta> transposeHintByVoiceId, BodyTextPusher pushBodyText,
                VoiceDirectiveTailParser parseVoiceDirectiveTail,
                UserDefinedDecorationParser parseUserDefinedDecoration,
                DecorationSymbolExpander expandUserDefinedDecorationSymbols) {
            this.lineState = lineState;
            this.warnings = warnings;
            this.headers = headers;
            this.lyricEntriesByVoice = lyricEntriesByVoice;
            this.supportedStandaloneBodyFieldNames = supportedStandaloneBodyFieldNames;
            this.voiceRegistry = voiceRegistry;
            this.userDefinedDecorationBySymbol = userDefinedDecorationBySymbol;
            this.trillWidthHintByKey = trillWidthHintByKey;
            this.keyHintFifthsByKey = keyHintFifthsByKey;
            this.measureMetaByKey = measureMetaByKey;
            this.transposeHintByVoiceId = transposeHintByVoiceId;
            this.pushBodyText = pushBodyText;
            this.parseVoiceDirectiveTail = parseVoiceDirectiveTail;
            this.parseUserDefinedDecoration = parseUserDefinedDecoration;
            this.expandUserDefinedDecorationSymbols = expandUserDefinedDecorationSymbols;
        }

        public AbcImportLineState getLineState() {
            return lineState;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public Map<String, List<AbcLyricEntry>> getLyricEntriesByVoice() {
            return lyricEntriesByVoice;
        }

        public Set<String> getSupportedStandaloneBodyFieldNames() {
            return supportedStandaloneBodyFieldNames;
        }

        public AbcImportVoiceRegistry getVoiceRegistry() {
            return voiceRegistry;
        }

        public Map<String, String> getUserDefinedDecorationBySymbol() {
            return userDefinedDecorationBySymbol;
        }

        public Map<String, String> getTrillWidthHintByKey() {
            return trillWidthHintByKey;
        }

        public Map<String, Integer> getKeyHintFifthsByKey() {
            return keyHintFifthsByKey;
        }

        public Map<String, AbcMeasureMeta> getMeasureMetaByKey() {
            return measureMetaByKey;
        }

        public Map<String, AbcTransposeMeta> getTransposeHintByVoiceId() {
            return transposeHintByVoiceId;
        }

        public BodyTextPusher getPushBodyText() {
            return pushBodyText;
        }

        public VoiceDirectiveTailParser getParseVoiceDirectiveTail() {
            return parseVoiceDirectiveTail;
        }

        public UserDefinedDecorationParser getParseUserDefinedDecoration() {
            return parseUserDefinedDecoration;
        }

        public DecorationSymbolExpander getExpandUserDefinedDecorationSymbols() {
            return expandUserDefinedDecorationSymbols;
        }
    }
}
