/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.abc;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jp.igapyon.mikuscore.core.StaffClefPolicy;

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
    private static final String[] ABC_MUSICXML_DIRECTION_DYNAMICS = { "pppp", "ppp", "pp", "p", "mp", "mf", "f",
            "ff", "fff", "ffff", "fp", "fz", "rfz", "sf", "sfp", "sfz" };

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
        for (String chordSymbol : note.getChordSymbols()) {
            chunks.append(buildHarmonyXmlFromChordSymbol(chordSymbol));
        }
        for (String annotation : note.getAnnotations()) {
            if (trimToEmpty(annotation).length() == 0) {
                continue;
            }
            chunks.append("<direction><direction-type><words>").append(xmlEscape(annotation))
                    .append("</words></direction-type></direction>");
        }
        return chunks.toString();
    }

    public static String abcQuotedTextEscape(String text) {
        return trimToEmpty(text == null ? "" : text.replace('"', '\'').replaceAll("\\s+", " "));
    }

    public static String normalizeChordToken(String raw) {
        return trimToEmpty(raw).replace("♯", "#").replace("♭", "b").replaceAll("\\s+", "");
    }

    public static boolean isLikelyAbcChordSymbol(String raw) {
        return normalizeChordToken(raw).matches("^[A-G](?:#|b)?(?:[^/\\s\"]*)?(?:/[A-G](?:#|b)?)?$");
    }

    public static String xmlHarmonyKindFromChordSuffix(String suffixRaw) {
        String suffix = trimToEmpty(suffixRaw).toLowerCase();
        if (suffix.length() == 0) {
            return "major";
        }
        if ("m".equals(suffix) || "min".equals(suffix)) {
            return "minor";
        }
        if ("6".equals(suffix)) {
            return "major-sixth";
        }
        if ("m6".equals(suffix) || "min6".equals(suffix)) {
            return "minor-sixth";
        }
        if ("7".equals(suffix)) {
            return "dominant";
        }
        if ("7sus4".equals(suffix) || "sus4".equals(suffix)) {
            return "suspended-fourth";
        }
        if ("9".equals(suffix)) {
            return "dominant-ninth";
        }
        if ("11".equals(suffix)) {
            return "dominant-11th";
        }
        if ("13".equals(suffix)) {
            return "dominant-13th";
        }
        if ("maj7".equals(suffix)) {
            return "major-seventh";
        }
        if ("maj9".equals(suffix)) {
            return "major-ninth";
        }
        if ("m9".equals(suffix) || "min9".equals(suffix)) {
            return "minor-ninth";
        }
        if ("m7".equals(suffix) || "min7".equals(suffix)) {
            return "minor-seventh";
        }
        if ("dim".equals(suffix)) {
            return "diminished";
        }
        if ("dim7".equals(suffix)) {
            return "diminished-seventh";
        }
        if ("aug".equals(suffix) || "+".equals(suffix)) {
            return "augmented";
        }
        if ("sus2".equals(suffix)) {
            return "suspended-second";
        }
        if ("m7b5".equals(suffix) || "min7b5".equals(suffix) || "ø".equals(suffix)) {
            return "half-diminished";
        }
        return "";
    }

    public static String buildHarmonyXmlFromChordSymbol(String raw) {
        String normalized = normalizeChordToken(raw);
        Matcher matcher = Pattern.compile("^([A-G](?:#|b)?)([^/]*)?(?:/([A-G](?:#|b)?))?$").matcher(normalized);
        if (!matcher.find()) {
            return "";
        }
        AbcHarmonyPitch root = xmlHarmonyRootFromChordToken(matcher.group(1));
        if (root == null) {
            return "";
        }
        String suffix = matcher.group(2) == null ? "" : matcher.group(2);
        AbcHarmonyPitch bass = matcher.group(3) == null ? null : xmlHarmonyRootFromChordToken(matcher.group(3));
        String kind = xmlHarmonyKindFromChordSuffix(suffix);
        if (kind.length() == 0) {
            return "";
        }
        StringBuilder xml = new StringBuilder();
        xml.append("<harmony>");
        xml.append("<root><root-step>").append(xmlEscape(root.getStep())).append("</root-step>");
        if (root.getAlter() != 0) {
            xml.append("<root-alter>").append(root.getAlter()).append("</root-alter>");
        }
        xml.append("</root>");
        if (bass != null) {
            xml.append("<bass><bass-step>").append(xmlEscape(bass.getStep())).append("</bass-step>");
            if (bass.getAlter() != 0) {
                xml.append("<bass-alter>").append(bass.getAlter()).append("</bass-alter>");
            }
            xml.append("</bass>");
        }
        xml.append("<kind text=\"").append(xmlEscape(normalized)).append("\">").append(kind).append("</kind>");
        xml.append("</harmony>");
        return xml.toString();
    }

    public static String abcChordSymbolFromHarmony(Element harmony) {
        if (harmony == null) {
            return "";
        }
        Element root = directChild(harmony, "root");
        String rootStep = directChildText(root, "root-step").trim().toUpperCase();
        if (!rootStep.matches("^[A-G]$")) {
            return "";
        }
        int rootAlter = parseInt(directChildText(root, "root-alter"), 0);
        Element kindNode = directChild(harmony, "kind");
        String kindTextAttr = kindNode == null ? "" : trimToEmpty(kindNode.getAttribute("text"));
        if (kindTextAttr.length() > 0) {
            return abcQuotedTextEscape(kindTextAttr);
        }
        String kindValue = elementText(kindNode).trim().toLowerCase();
        String rootToken = rootStep + (rootAlter == 1 ? "#" : (rootAlter == -1 ? "b" : ""));
        String suffix;
        if ("major".equals(kindValue)) {
            suffix = "";
        } else if ("minor".equals(kindValue)) {
            suffix = "m";
        } else if ("major-sixth".equals(kindValue)) {
            suffix = "6";
        } else if ("minor-sixth".equals(kindValue)) {
            suffix = "m6";
        } else if ("dominant".equals(kindValue)) {
            suffix = "7";
        } else if ("dominant-11th".equals(kindValue)) {
            suffix = "11";
        } else if ("dominant-13th".equals(kindValue)) {
            suffix = "13";
        } else if ("dominant-ninth".equals(kindValue)) {
            suffix = "9";
        } else if ("major-seventh".equals(kindValue)) {
            suffix = "maj7";
        } else if ("major-ninth".equals(kindValue)) {
            suffix = "maj9";
        } else if ("minor-ninth".equals(kindValue)) {
            suffix = "m9";
        } else if ("minor-seventh".equals(kindValue)) {
            suffix = "m7";
        } else if ("diminished".equals(kindValue)) {
            suffix = "dim";
        } else if ("diminished-seventh".equals(kindValue)) {
            suffix = "dim7";
        } else if ("augmented".equals(kindValue)) {
            suffix = "aug";
        } else if ("suspended-fourth".equals(kindValue)) {
            suffix = "sus4";
        } else if ("suspended-second".equals(kindValue)) {
            suffix = "sus2";
        } else if ("half-diminished".equals(kindValue)) {
            suffix = "m7b5";
        } else {
            suffix = "";
        }
        Element bass = directChild(harmony, "bass");
        String bassStep = directChildText(bass, "bass-step").trim().toUpperCase();
        int bassAlter = parseInt(directChildText(bass, "bass-alter"), 0);
        String bassToken = bassStep.matches("^[A-G]$")
                ? "/" + bassStep + (bassAlter == 1 ? "#" : (bassAlter == -1 ? "b" : ""))
                : "";
        return rootToken + suffix + bassToken;
    }

    public static String abcLyricTokenFromMusicXml(String text, String syllabic) {
        String normalized = trimToEmpty(text).replaceAll("\\s+", "~");
        String mode = trimToEmpty(syllabic).length() == 0 ? "single" : trimToEmpty(syllabic).toLowerCase();
        if (normalized.length() == 0) {
            return "*";
        }
        if ("begin".equals(mode) || "middle".equals(mode)) {
            return normalized + "-";
        }
        return normalized;
    }

    public static String abcClefFromMusicXmlPart(Element part) {
        Element firstClef = firstDescendantByPath(part, new String[] { "measure", "attributes", "clef" });
        if (firstClef == null) {
            return "";
        }
        String sign = directChildText(firstClef, "sign").trim().toUpperCase();
        int line = parseInt(directChildText(firstClef, "line"), 0);
        if ("F".equals(sign) && line == 4) {
            return "bass";
        }
        if ("G".equals(sign) && line == 2) {
            return "treble";
        }
        if ("C".equals(sign) && line == 3) {
            return "alto";
        }
        if ("C".equals(sign) && line == 4) {
            return "tenor";
        }
        return "";
    }

    public static String abcClefFromMusicXmlClef(Element clef) {
        String sign = directChildText(clef, "sign").trim().toUpperCase();
        int line = parseInt(directChildText(clef, "line"), 0);
        if ("F".equals(sign) && line == 4) {
            return "bass";
        }
        if ("G".equals(sign) && line == 2) {
            return "treble";
        }
        if ("C".equals(sign) && line == 3) {
            return "alto";
        }
        if ("C".equals(sign) && line == 4) {
            return "tenor";
        }
        return "";
    }

    public static String resolveMusicXmlPartLaneClef(Element part, String staff) {
        String normalizedStaff = trimToEmpty(staff);
        if (normalizedStaff.length() == 0) {
            return abcClefFromMusicXmlPart(part);
        }
        for (Element measure : directChildren(part, "measure")) {
            Element attributes = directChild(measure, "attributes");
            if (attributes == null) {
                continue;
            }
            for (Element clef : directChildren(attributes, "clef")) {
                if (!normalizedStaff.equals(trimToEmpty(clef.getAttribute("number")))) {
                    continue;
                }
                String abcClef = abcClefFromMusicXmlClef(clef);
                if (abcClef.length() > 0) {
                    return abcClef;
                }
            }
        }
        return abcClefFromMusicXmlPart(part);
    }

    public static List<AbcMusicXmlLaneDef> collectMusicXmlPartLaneDefs(Element part, String partId, String partName) {
        String safePartId = trimToEmpty(partId).length() == 0 ? "P1" : trimToEmpty(partId);
        String safePartName = trimToEmpty(partName).length() == 0 ? safePartId : trimToEmpty(partName);
        Map<String, AbcMusicXmlLaneDef> lanesByKey = new LinkedHashMap<String, AbcMusicXmlLaneDef>();
        if (part != null) {
            for (Element measure : directChildren(part, "measure")) {
                for (Element note : directChildren(measure, "note")) {
                    String staffText = directChildText(note, "staff").trim();
                    String voiceText = directChildText(note, "voice").trim();
                    String staff = staffText.length() == 0 ? null : staffText;
                    String voice = voiceText.length() == 0 ? "1" : voiceText;
                    String key = (staff == null ? "" : staff) + "::" + (voice == null ? "" : voice);
                    if (!lanesByKey.containsKey(key)) {
                        lanesByKey.put(key, new AbcMusicXmlLaneDef(staff, voice, "", "", "", ""));
                    }
                }
            }
        }
        List<AbcMusicXmlLaneDef> raw = new ArrayList<AbcMusicXmlLaneDef>(lanesByKey.values());
        if (raw.isEmpty()) {
            raw.add(new AbcMusicXmlLaneDef(null, null, "", "", "", ""));
        }
        raw.sort((a, b) -> {
            int staffCompare = compareMusicXmlLaneToken(a.getStaff(), b.getStaff(), true);
            if (staffCompare != 0) {
                return staffCompare;
            }
            return compareMusicXmlLaneToken(a.getVoice(), b.getVoice(), true);
        });
        List<AbcMusicXmlLaneDef> resolved = new ArrayList<AbcMusicXmlLaneDef>();
        for (int i = 0; i < raw.size(); i++) {
            AbcMusicXmlLaneDef lane = raw.get(i);
            String voiceId;
            if (raw.size() == 1) {
                voiceId = safePartId;
            } else {
                String staffSuffix = lane.getStaff() == null ? "" : "_s" + lane.getStaff();
                String voiceSuffix = lane.getVoice() == null ? "" : "_v" + lane.getVoice();
                voiceId = safePartId + staffSuffix + (voiceSuffix.length() > 0 ? voiceSuffix : "_l" + (i + 1));
            }
            String normalizedVoiceId = voiceId.replaceAll("[^A-Za-z0-9_.-]", "_");
            String laneName;
            if (raw.size() <= 1) {
                laneName = safePartName;
            } else if (lane.getStaff() != null && lane.getVoice() != null) {
                laneName = safePartName + " (Staff " + lane.getStaff() + " Voice " + lane.getVoice() + ")";
            } else if (lane.getStaff() != null) {
                laneName = safePartName + " (Staff " + lane.getStaff() + ")";
            } else if (lane.getVoice() != null) {
                laneName = safePartName + " (Voice " + lane.getVoice() + ")";
            } else {
                laneName = safePartName + " (Lane)";
            }
            String clef = resolveMusicXmlPartLaneClef(part, lane.getStaff());
            resolved.add(new AbcMusicXmlLaneDef(lane.getStaff(), lane.getVoice(), voiceId, normalizedVoiceId, laneName,
                    clef));
        }
        return resolved;
    }

    public static List<String> buildMusicXmlPartTransposeMetaLines(Element part, String normalizedVoiceId) {
        List<String> lines = new ArrayList<String>();
        String safeVoiceId = trimToEmpty(normalizedVoiceId);
        for (Element measure : directChildren(part, "measure")) {
            Element attributes = directChild(measure, "attributes");
            Element transpose = directChild(attributes, "transpose");
            if (transpose == null) {
                continue;
            }
            Double chromatic = parseOptionalNumber(directChildText(transpose, "chromatic"));
            Double diatonic = parseOptionalNumber(directChildText(transpose, "diatonic"));
            if (chromatic != null || diatonic != null) {
                StringBuilder line = new StringBuilder("%@mks transpose voice=").append(safeVoiceId);
                if (chromatic != null) {
                    line.append(" chromatic=").append(Math.round(chromatic.doubleValue()));
                }
                if (diatonic != null) {
                    line.append(" diatonic=").append(Math.round(diatonic.doubleValue()));
                }
                lines.add(line.toString());
            }
            break;
        }
        return lines;
    }

    public static List<String> buildMusicXmlMeasureMetaLines(String normalizedVoiceId, Element measure,
            int safeMeasureNumber) {
        List<String> lines = new ArrayList<String>();
        String safeVoiceId = trimToEmpty(normalizedVoiceId);
        String safeMeasureText = String.valueOf(safeMeasureNumber);
        String rawMeasureNumber = trimToEmpty(measure == null ? "" : measure.getAttribute("number"));
        if (rawMeasureNumber.length() == 0) {
            rawMeasureNumber = safeMeasureText;
        }
        String implicitAttr = trimToEmpty(measure == null ? "" : measure.getAttribute("implicit")).toLowerCase();
        boolean isImplicit = "yes".equals(implicitAttr) || "true".equals(implicitAttr) || "1".equals(implicitAttr);
        Element rightBarline = directChildWithAttribute(measure, "barline", "location", "right");
        Element rightRepeat = directChild(rightBarline, "repeat");
        Element rightEnding = directChild(rightBarline, "ending");
        String rightRepeatDir = trimToEmpty(rightRepeat == null ? "" : rightRepeat.getAttribute("direction"))
                .toLowerCase();
        boolean hasRightRepeat = "backward".equals(rightRepeatDir);
        int repeatTimes = parseInt(trimToEmpty(rightRepeat == null ? "" : rightRepeat.getAttribute("times")),
                Integer.MIN_VALUE);
        String rightEndingNumber = trimToEmpty(rightEnding == null ? "" : rightEnding.getAttribute("number"));
        String rightEndingType = trimToEmpty(rightEnding == null ? "" : rightEnding.getAttribute("type"))
                .toLowerCase();
        if (isImplicit || !rawMeasureNumber.equals(safeMeasureText) || (hasRightRepeat && repeatTimes > 2)
                || (rightEndingNumber.length() > 0 && "discontinue".equals(rightEndingType))) {
            StringBuilder line = new StringBuilder("%@mks measure voice=").append(safeVoiceId)
                    .append(" measure=").append(safeMeasureNumber)
                    .append(" number=").append(rawMeasureNumber)
                    .append(" implicit=").append(isImplicit ? 1 : 0);
            if (hasRightRepeat && repeatTimes > 2) {
                line.append(" times=").append(Math.round(repeatTimes));
            }
            if (rightEndingNumber.length() > 0 && "discontinue".equals(rightEndingType)) {
                line.append(" ending-stop=").append(rightEndingNumber);
                line.append(" ending-type=").append(rightEndingType);
            }
            lines.add(line.toString());
        }
        return lines;
    }

    public static List<String> buildMusicXmlMeasureDiagMetaLines(String normalizedVoiceId, Element measure,
            int safeMeasureNumber) {
        List<String> lines = new ArrayList<String>();
        String safeVoiceId = trimToEmpty(normalizedVoiceId);
        Map<String, String> byName = new LinkedHashMap<String, String>();
        Element attributes = directChild(measure, "attributes");
        Element miscellaneous = directChild(attributes, "miscellaneous");
        for (Element field : directChildren(miscellaneous, "miscellaneous-field")) {
            String name = trimToEmpty(field.getAttribute("name"));
            if (!name.startsWith("mks:diag:")) {
                continue;
            }
            byName.put(name, elementText(field));
        }
        List<String> names = new ArrayList<String>(byName.keySet());
        names.sort((a, b) -> {
            boolean countA = "mks:diag:count".equals(a);
            boolean countB = "mks:diag:count".equals(b);
            if (countA && !countB) {
                return -1;
            }
            if (!countA && countB) {
                return 1;
            }
            return a.compareTo(b);
        });
        for (String name : names) {
            lines.add("%@mks diag voice=" + safeVoiceId + " measure=" + safeMeasureNumber + " name=" + name
                    + " enc=uri-v1 value=" + encodeUriComponent(byName.get(name)));
        }
        return lines;
    }

    public static AbcMusicXmlMeasureState updateMusicXmlMeasureState(Element measure, double currentDivisions,
            int currentFifths, double currentBeats, double currentBeatType, Integer lastEmittedKeyFifths) {
        double nextDivisions = currentDivisions;
        int nextFifths = currentFifths;
        double nextBeats = currentBeats;
        double nextBeatType = currentBeatType;
        Element attributes = directChild(measure, "attributes");
        Double parsedDivisions = parseOptionalNumber(directChildText(attributes, "divisions"));
        if (parsedDivisions != null && parsedDivisions.doubleValue() > 0) {
            nextDivisions = parsedDivisions.doubleValue();
        }
        Element key = directChild(attributes, "key");
        Double parsedFifths = parseOptionalNumber(directChildText(key, "fifths"));
        if (parsedFifths != null) {
            nextFifths = (int) Math.round(parsedFifths.doubleValue());
        }
        boolean needsInlineKeyChange = lastEmittedKeyFifths == null
                || lastEmittedKeyFifths.intValue() != nextFifths;
        Element time = directChild(attributes, "time");
        Double parsedBeats = parseOptionalNumber(directChildText(time, "beats"));
        if (parsedBeats != null && parsedBeats.doubleValue() > 0) {
            nextBeats = parsedBeats.doubleValue();
        }
        Double parsedBeatType = parseOptionalNumber(directChildText(time, "beat-type"));
        if (parsedBeatType != null && parsedBeatType.doubleValue() > 0) {
            nextBeatType = parsedBeatType.doubleValue();
        }
        return new AbcMusicXmlMeasureState(nextDivisions, nextFifths, nextBeats, nextBeatType, needsInlineKeyChange,
                keySignatureAlterByStep(nextFifths), new LinkedHashMap<String, Integer>());
    }

    public static AbcMusicXmlDirectionTokens collectMusicXmlDirectionTokens(Element direction, String activeWedgeType) {
        List<String> words = new ArrayList<String>();
        List<String> decorations = new ArrayList<String>();
        String nextActiveWedgeType = trimToEmpty(activeWedgeType).toLowerCase();
        for (Element directionType : directChildren(direction, "direction-type")) {
            for (Element rehearsal : directChildren(directionType, "rehearsal")) {
                String text = abcQuotedTextEscape(elementText(rehearsal));
                if (text.length() > 0) {
                    decorations.add("!rehearsal:" + text + "!");
                }
            }
            for (Element wordsNode : directChildren(directionType, "words")) {
                String text = abcQuotedTextEscape(elementText(wordsNode));
                if (text.length() > 0) {
                    words.add(text);
                }
            }
            if (directChild(directionType, "segno") != null) {
                decorations.add("!segno!");
            }
            if (directChild(directionType, "coda") != null) {
                decorations.add("!coda!");
            }
            for (Element wedge : directChildren(directionType, "wedge")) {
                String wedgeType = trimToEmpty(wedge.getAttribute("type")).toLowerCase();
                if ("crescendo".equals(wedgeType)) {
                    decorations.add("!crescendo(!");
                    nextActiveWedgeType = "crescendo";
                } else if ("diminuendo".equals(wedgeType)) {
                    decorations.add("!diminuendo(!");
                    nextActiveWedgeType = "diminuendo";
                } else if ("stop".equals(wedgeType)) {
                    decorations.add("diminuendo".equals(nextActiveWedgeType) ? "!diminuendo)!" : "!crescendo)!");
                    nextActiveWedgeType = "";
                }
            }
            for (Element dynamics : directChildren(directionType, "dynamics")) {
                for (String dynamicName : ABC_MUSICXML_DIRECTION_DYNAMICS) {
                    if (directChild(dynamics, dynamicName) != null) {
                        decorations.add("!" + dynamicName + "!");
                    }
                }
            }
        }
        boolean hasDaCapo = false;
        boolean hasToCoda = false;
        boolean hasDalSegno = false;
        boolean hasFine = false;
        for (Element sound : directChildren(direction, "sound")) {
            hasFine = hasFine || "yes".equals(trimToEmpty(sound.getAttribute("fine")).toLowerCase());
            hasDaCapo = hasDaCapo || "yes".equals(trimToEmpty(sound.getAttribute("dacapo")).toLowerCase());
            hasToCoda = hasToCoda || trimToEmpty(sound.getAttribute("tocoda")).length() > 0;
            hasDalSegno = hasDalSegno || trimToEmpty(sound.getAttribute("dalsegno")).length() > 0;
        }
        if (hasFine) {
            decorations.add("!fine!");
        }
        if (hasDaCapo && hasToCoda) {
            decorations.add("!dacoda!");
        } else if (hasDaCapo) {
            decorations.add("!dacapo!");
        }
        if (hasDalSegno) {
            decorations.add("!dalsegno!");
        }
        if (hasToCoda && !hasDaCapo) {
            decorations.add("!tocoda!");
        }
        return new AbcMusicXmlDirectionTokens(words, decorations, nextActiveWedgeType);
    }

    public static boolean isMusicXmlNoteInLane(Element note, AbcMusicXmlLaneDef lane) {
        if (note == null) {
            return false;
        }
        if (lane != null && lane.getStaff() != null) {
            String noteStaff = directChildText(note, "staff").trim();
            if (!noteStaff.equals(lane.getStaff())) {
                return false;
            }
        }
        if (lane != null && lane.getVoice() != null) {
            String noteVoiceRaw = directChildText(note, "voice").trim();
            String noteVoice = noteVoiceRaw.length() == 0 ? "1" : noteVoiceRaw;
            if (!noteVoice.equals(lane.getVoice())) {
                return false;
            }
        }
        return true;
    }

    public static AbcMusicXmlNoteTiming resolveMusicXmlNoteTiming(Element note, double currentDivisions) {
        boolean chord = directChild(note, "chord") != null;
        boolean grace = directChild(note, "grace") != null;
        double duration = parseDouble(directChildText(note, "duration"), 0);
        if (!grace && (!Double.isFinite(duration) || duration <= 0)) {
            return new AbcMusicXmlNoteTiming(chord, false, 0, false);
        }
        int noteDuration = grace
                ? (Double.isFinite(duration) && duration > 0 ? (int) Math.round(duration)
                        : (int) Math.round(currentDivisions / 2.0))
                : (int) Math.round(duration);
        return new AbcMusicXmlNoteTiming(chord, grace, noteDuration, true);
    }

    public static AbcMusicXmlNoteOrnaments collectMusicXmlNoteOrnaments(Element note) {
        Element notations = directChild(note, "notations");
        Element ornaments = directChild(notations, "ornaments");
        boolean hasTrillMark = directChild(ornaments, "trill-mark") != null;
        boolean hasTurn = directChild(ornaments, "turn") != null;
        boolean hasInvertedTurn = directChild(ornaments, "inverted-turn") != null;
        boolean hasTurnSlash = hasMusicXmlTurnSlash(ornaments);
        boolean hasDelayedTurn = directChild(ornaments, "delayed-turn") != null;
        boolean hasMordent = directChild(ornaments, "mordent") != null;
        boolean hasInvertedMordent = directChild(ornaments, "inverted-mordent") != null;
        Element tremoloNode = directChild(ornaments, "tremolo");
        boolean hasSchleifer = directChild(ornaments, "schleifer") != null;
        boolean hasShake = directChild(ornaments, "shake") != null;
        boolean hasGlissandoStart = directChildWithAttribute(notations, "glissando", "type", "start") != null;
        boolean hasGlissandoStop = directChildWithAttribute(notations, "glissando", "type", "stop") != null;
        boolean hasSlideStart = directChildWithAttribute(notations, "slide", "type", "start") != null;
        boolean hasSlideStop = directChildWithAttribute(notations, "slide", "type", "stop") != null;
        boolean hasArpeggiate = directChild(notations, "arpeggiate") != null;
        boolean hasWavyLineStart = false;
        boolean hasWavyLineStop = false;
        for (Element wavyLine : directChildren(ornaments, "wavy-line")) {
            String type = trimToEmpty(wavyLine.getAttribute("type")).toLowerCase();
            if (type.length() == 0 || "start".equals(type)) {
                hasWavyLineStart = true;
            }
            if ("stop".equals(type)) {
                hasWavyLineStop = true;
            }
        }
        boolean hasTrill = hasTrillMark || hasWavyLineStart;
        String turnType = hasInvertedTurn ? "inverted-turn" : (hasTurn ? "turn" : "");
        String mordentType = hasInvertedMordent ? "inverted-mordent" : (hasMordent ? "mordent" : "");
        String tremoloTypeRaw = trimToEmpty(tremoloNode == null ? "" : tremoloNode.getAttribute("type")).toLowerCase();
        String tremoloType = "single".equals(tremoloTypeRaw) || "start".equals(tremoloTypeRaw)
                || "stop".equals(tremoloTypeRaw) ? tremoloTypeRaw : "";
        int tremoloMarks = Math.max(1, Math.min(8, parseInt(elementText(tremoloNode), 0)));
        String trillAccidentalText = directChildText(ornaments, "accidental-mark").trim();
        return new AbcMusicXmlNoteOrnaments(hasTrill, hasTrillMark, hasWavyLineStart, hasWavyLineStop,
                trillAccidentalText, turnType, hasTurnSlash, hasDelayedTurn, mordentType, tremoloType,
                Integer.valueOf(tremoloMarks), hasGlissandoStart, hasGlissandoStop, hasSlideStart, hasSlideStop,
                hasSchleifer, hasShake, hasArpeggiate);
    }

    public static AbcMusicXmlPitchToken resolveMusicXmlNotePitchToken(Element note, Map<String, Integer> keyAlterByStep,
            Map<String, Integer> measureAccidentalByStepOctave) {
        Map<String, Integer> safeMeasureAccidentals = measureAccidentalByStepOctave == null
                ? new LinkedHashMap<String, Integer>()
                : measureAccidentalByStepOctave;
        if (directChild(note, "rest") != null) {
            return new AbcMusicXmlPitchToken("z", safeMeasureAccidentals, false, false, "", 0);
        }
        Element pitch = directChild(note, "pitch");
        String step = directChildText(pitch, "step").trim();
        String upperStep = step.toUpperCase().matches("^[A-G]$") ? step.toUpperCase() : "C";
        double octaveRaw = parseDouble(directChildText(pitch, "octave"), 4);
        int safeOctave = Double.isFinite(octaveRaw) ? Math.max(0, Math.min(9, (int) Math.round(octaveRaw))) : 4;
        String stepOctaveKey = upperStep + safeOctave;
        String alterRaw = directChildText(pitch, "alter").trim();
        Integer explicitAlter = alterRaw.length() > 0 && Double.isFinite(parseDouble(alterRaw, Double.NaN))
                ? Integer.valueOf((int) Math.round(parseDouble(alterRaw, 0)))
                : null;
        Element accidentalNode = directChild(note, "accidental");
        String accidentalText = elementText(accidentalNode);
        Integer accidentalAlter = accidentalTextToAlter(accidentalText);
        boolean accidentalEditorial = "yes".equals(trimToEmpty(accidentalNode == null ? ""
                : accidentalNode.getAttribute("editorial")).toLowerCase());
        boolean accidentalCautionary = "yes".equals(trimToEmpty(accidentalNode == null ? ""
                : accidentalNode.getAttribute("cautionary")).toLowerCase());
        Integer keyAlterValue = keyAlterByStep == null ? null : keyAlterByStep.get(upperStep);
        int keyAlter = keyAlterValue == null ? 0 : keyAlterValue.intValue();
        Integer currentAlterValue = safeMeasureAccidentals.containsKey(stepOctaveKey)
                ? safeMeasureAccidentals.get(stepOctaveKey)
                : Integer.valueOf(keyAlter);
        int currentAlter = currentAlterValue == null ? 0 : currentAlterValue.intValue();
        int targetAlter = explicitAlter == null ? 0 : explicitAlter.intValue();
        if (accidentalAlter != null) {
            targetAlter = accidentalAlter.intValue();
        }
        boolean shouldEmitAccidental = targetAlter != currentAlter
                || (accidentalAlter != null && accidentalAlter.intValue() != 0);
        String accidental = shouldEmitAccidental ? (targetAlter == 0 ? "=" : accidentalFromAlter(targetAlter)) : "";
        safeMeasureAccidentals.put(stepOctaveKey, Integer.valueOf(targetAlter));
        String token = accidental + abcPitchFromStepOctave(upperStep, safeOctave);
        if (accidentalEditorial && accidental.length() > 0) {
            token = "!editorial!" + token;
        }
        if (accidentalCautionary && accidental.length() > 0) {
            token = "!courtesy!" + token;
        }
        return new AbcMusicXmlPitchToken(token, safeMeasureAccidentals, accidentalEditorial, accidentalCautionary,
                stepOctaveKey, targetAlter);
    }

    public static String buildMusicXmlNoteOrnamentPrefix(AbcMusicXmlNoteOrnaments ornaments) {
        if (ornaments == null) {
            return "";
        }
        String trillPrefix = ornaments.isWavyLineStop() ? "!trill)!"
                : (ornaments.hasTrillMark() && ornaments.isWavyLineStart()
                        ? "!trill(!"
                        : (ornaments.isTrill() ? "!trill!" : ""));
        String turnPrefix;
        if ("inverted-turn".equals(ornaments.getTurnType())) {
            turnPrefix = ornaments.isDelayedTurn() ? "!delayedinvertedturn!"
                    : (ornaments.isTurnSlash() ? "!invertedturnx!" : "!invertedturn!");
        } else if ("turn".equals(ornaments.getTurnType())) {
            turnPrefix = ornaments.isDelayedTurn() ? "!delayedturn!"
                    : (ornaments.isTurnSlash() ? "!turnx!" : "!turn!");
        } else {
            turnPrefix = "";
        }
        String mordentPrefix = "inverted-mordent".equals(ornaments.getMordentType()) ? "!pralltriller!"
                : ("mordent".equals(ornaments.getMordentType()) ? "!mordent!" : "");
        String tremoloPrefix = trimToEmpty(ornaments.getTremoloType()).length() > 0
                ? "!tremolo-" + ornaments.getTremoloType() + "-" + Math.max(1,
                        Math.min(8, ornaments.getTremoloMarks() == null ? 1 : ornaments.getTremoloMarks().intValue()))
                        + "!"
                : "";
        String glissandoPrefix = ornaments.isGlissandoStart() ? "!gliss-start!"
                : (ornaments.isGlissandoStop() ? "!gliss-stop!" : "");
        String slidePrefix = ornaments.isSlideStart() ? "!slide!" : (ornaments.isSlideStop() ? "!slide-stop!" : "");
        return trillPrefix + turnPrefix + mordentPrefix + tremoloPrefix + glissandoPrefix + slidePrefix
                + (ornaments.isSchleifer() ? "!schleifer!" : "")
                + (ornaments.isShake() ? "!shake!" : "")
                + (ornaments.isArpeggiate() ? "!arpeggio!" : "");
    }

    public static AbcMusicXmlNoteArticulations collectMusicXmlNoteArticulations(Element note) {
        Element notations = directChild(note, "notations");
        Element articulations = directChild(notations, "articulations");
        String phraseMarkText = "";
        for (Element other : directChildren(articulations, "other-articulation")) {
            String text = elementText(other).toLowerCase();
            if ("shortphrase".equals(text) || "mediumphrase".equals(text) || "longphrase".equals(text)) {
                phraseMarkText = text;
                break;
            }
        }
        return new AbcMusicXmlNoteArticulations(
                directChild(articulations, "staccato") != null,
                directChild(articulations, "staccatissimo") != null,
                directChild(articulations, "accent") != null,
                directChild(articulations, "tenuto") != null,
                directChild(articulations, "stress") != null,
                directChild(articulations, "unstress") != null,
                directChild(articulations, "strong-accent") != null,
                directChild(articulations, "breath-mark") != null,
                directChild(articulations, "caesura") != null,
                phraseMarkText);
    }

    public static String buildMusicXmlNoteArticulationPrefix(AbcMusicXmlNoteArticulations articulations) {
        if (articulations == null) {
            return "";
        }
        String phraseMark = articulations.getPhraseMarkText();
        String phraseMarkPrefix = "shortphrase".equals(phraseMark) || "mediumphrase".equals(phraseMark)
                || "longphrase".equals(phraseMark) ? "!" + phraseMark + "!" : "";
        return (articulations.isStaccatissimo() ? "!wedge!" : (articulations.isStaccato() ? "!staccato!" : ""))
                + (articulations.isAccent() ? "!accent!" : "")
                + (articulations.isTenuto() ? "!tenuto!" : "")
                + (articulations.isStress() ? "!stress!" : "")
                + (articulations.isUnstress() ? "!unstress!" : "")
                + (articulations.isStrongAccent() ? "!marcato!" : "")
                + (articulations.isBreathMark() ? "!breath!" : "")
                + (articulations.isCaesura() ? "!caesura!" : "")
                + phraseMarkPrefix;
    }

    public static AbcMusicXmlNoteTechnical collectMusicXmlNoteTechnical(Element note) {
        Element technical = directChild(directChild(note, "notations"), "technical");
        return new AbcMusicXmlNoteTechnical(
                directChild(technical, "up-bow") != null,
                directChild(technical, "down-bow") != null,
                directChild(technical, "double-tongue") != null,
                directChild(technical, "triple-tongue") != null,
                directChild(technical, "heel") != null,
                directChild(technical, "toe") != null,
                directChildTexts(technical, "fingering"),
                directChildTexts(technical, "string"),
                directChildTexts(technical, "pluck"),
                directChild(technical, "open-string") != null,
                directChild(technical, "snap-pizzicato") != null,
                directChild(technical, "harmonic") != null,
                directChild(technical, "stopped") != null,
                directChild(technical, "thumb-position") != null);
    }

    public static String buildMusicXmlNoteTechnicalPrefix(AbcMusicXmlNoteTechnical technical) {
        if (technical == null) {
            return "";
        }
        StringBuilder prefix = new StringBuilder();
        prefix.append(technical.isUpBow() ? "!upbow!" : "");
        prefix.append(technical.isDownBow() ? "!downbow!" : "");
        prefix.append(technical.isDoubleTongue() ? "!doubletongue!" : "");
        prefix.append(technical.isTripleTongue() ? "!tripletongue!" : "");
        prefix.append(technical.isHeel() ? "!heel!" : "");
        prefix.append(technical.isToe() ? "!toe!" : "");
        for (String value : technical.getFingerings()) {
            prefix.append(trimToEmpty(value).matches("^[0-5]$") ? "!" + trimToEmpty(value) + "!"
                    : "!fingering:" + trimToEmpty(value) + "!");
        }
        for (String value : technical.getStrings()) {
            prefix.append("!string:").append(trimToEmpty(value)).append("!");
        }
        for (String value : technical.getPlucks()) {
            prefix.append("!pluck:").append(trimToEmpty(value)).append("!");
        }
        prefix.append(technical.isOpenString() ? "!open!" : "");
        prefix.append(technical.isSnapPizzicato() ? "!snap!" : "");
        prefix.append(technical.isHarmonic() ? "!harmonic!" : "");
        prefix.append(technical.isStopped() ? "!stopped!" : "");
        prefix.append(technical.isThumbPosition() ? "!thumb!" : "");
        return prefix.toString();
    }

    public static Integer accidentalTextToAlter(String text) {
        String normalized = trimToEmpty(text).toLowerCase();
        if (normalized.length() == 0) {
            return null;
        }
        if ("sharp".equals(normalized)) {
            return Integer.valueOf(1);
        }
        if ("flat".equals(normalized)) {
            return Integer.valueOf(-1);
        }
        if ("natural".equals(normalized)) {
            return Integer.valueOf(0);
        }
        if ("double-sharp".equals(normalized)) {
            return Integer.valueOf(2);
        }
        if ("flat-flat".equals(normalized)) {
            return Integer.valueOf(-2);
        }
        return null;
    }

    public static Double parseOptionalNumber(String text) {
        String raw = trimToEmpty(text);
        if (raw.length() == 0) {
            return null;
        }
        double parsed = parseDouble(raw, Double.NaN);
        return Double.isNaN(parsed) ? null : Double.valueOf(parsed);
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
        if (note == null || !(note.isTieStart() || note.isTieStop() || note.isSlurStart() || note.isSlurStop()
                || note.isTupletStart() || note.isTupletStop() || note.isTrill() || note.isTrillLineStop()
                || trimToEmpty(note.getTurnType()).length() > 0 || note.isDelayedTurn()
                || trimToEmpty(note.getMordentType()).length() > 0 || trimToEmpty(note.getTremoloType()).length() > 0
                || note.isGlissandoStart() || note.isGlissandoStop() || note.isSlideStart() || note.isSlideStop()
                || note.isSchleifer() || note.isShake() || note.isArpeggiate() || note.isStaccato()
                || note.isStaccatissimo() || note.isAccent() || note.isTenuto() || note.isStress()
                || note.isUnstress() || trimToEmpty(note.getFermataType()).length() > 0 || note.isStrongAccent()
                || note.isBreathMark() || note.isCaesura() || trimToEmpty(note.getPhraseMark()).length() > 0
                || note.isUpBow() || note.isDownBow() || note.isDoubleTongue() || note.isTripleTongue()
                || note.isHeel() || note.isToe() || !note.getFingerings().isEmpty() || !note.getStrings().isEmpty()
                || !note.getPlucks().isEmpty() || note.isOpenString() || note.isSnapPizzicato() || note.isHarmonic()
                || note.isStopped() || note.isThumbPosition())) {
            return "";
        }
        StringBuilder chunks = new StringBuilder("<notations>");
        if (note.isTieStart()) {
            chunks.append("<tied type=\"start\"/>");
        }
        if (note.isTieStop()) {
            chunks.append("<tied type=\"stop\"/>");
        }
        if (note.isSlurStart()) {
            chunks.append("<slur type=\"start\"/>");
        }
        if (note.isSlurStop()) {
            chunks.append("<slur type=\"stop\"/>");
        }
        if (note.isTupletStart()) {
            chunks.append("<tuplet type=\"start\"/>");
        }
        if (note.isTupletStop()) {
            chunks.append("<tuplet type=\"stop\"/>");
        }
        chunks.append(buildAbcNoteOrnamentsXml(note));
        chunks.append(buildAbcNoteArticulationsXml(note));
        chunks.append(buildAbcNoteTechnicalXml(note));
        if (trimToEmpty(note.getFermataType()).length() > 0) {
            chunks.append("<fermata>").append("inverted".equals(note.getFermataType()) ? "inverted" : "normal")
                    .append("</fermata>");
        }
        chunks.append("</notations>");
        return chunks.toString();
    }

    public static String buildAbcNoteOrnamentsXml(AbcMeasureNote note) {
        if (note == null) {
            return "";
        }
        StringBuilder chunks = new StringBuilder();
        if (note.isTrill() || note.isTrillLineStop()) {
            StringBuilder trillParts = new StringBuilder();
            if (note.isTrill()) {
                trillParts.append("<trill-mark/>");
            }
            if (note.isTrillLineStop()) {
                trillParts.append("<wavy-line type=\"stop\"/>");
            } else if (note.isTrillLineStart()) {
                trillParts.append("<wavy-line type=\"start\"/>");
            }
            if (trimToEmpty(note.getTrillAccidentalText()).length() > 0) {
                trillParts.append("<accidental-mark>").append(xmlEscape(note.getTrillAccidentalText()))
                        .append("</accidental-mark>");
            }
            chunks.append("<ornaments>").append(trillParts).append("</ornaments>");
        }
        if (trimToEmpty(note.getTurnType()).length() > 0) {
            String tag = "inverted-turn".equals(note.getTurnType()) ? "inverted-turn" : "turn";
            chunks.append("<ornaments><").append(tag);
            if (note.isTurnSlash()) {
                chunks.append(" slash=\"yes\"");
            }
            chunks.append("/>");
            if (note.isDelayedTurn()) {
                chunks.append("<delayed-turn/>");
            }
            chunks.append("</ornaments>");
        }
        if (trimToEmpty(note.getMordentType()).length() > 0) {
            String tag = "inverted-mordent".equals(note.getMordentType()) ? "inverted-mordent" : "mordent";
            chunks.append("<ornaments><").append(tag).append("/></ornaments>");
        }
        if (trimToEmpty(note.getTremoloType()).length() > 0) {
            int marks = Math.max(1, Math.min(8, note.getTremoloMarks() == null ? 1 : note.getTremoloMarks().intValue()));
            chunks.append("<ornaments><tremolo type=\"").append(xmlEscape(note.getTremoloType())).append("\">")
                    .append(marks).append("</tremolo></ornaments>");
        }
        if (note.isGlissandoStart()) {
            chunks.append("<glissando type=\"start\" number=\"1\">wavy</glissando>");
        }
        if (note.isGlissandoStop()) {
            chunks.append("<glissando type=\"stop\" number=\"1\">wavy</glissando>");
        }
        if (note.isSlideStart()) {
            chunks.append("<slide type=\"start\" number=\"1\"/>");
        }
        if (note.isSlideStop()) {
            chunks.append("<slide type=\"stop\" number=\"1\"/>");
        }
        if (note.isSchleifer()) {
            chunks.append("<ornaments><schleifer/></ornaments>");
        }
        if (note.isShake()) {
            chunks.append("<ornaments><shake/></ornaments>");
        }
        if (note.isArpeggiate()) {
            chunks.append("<arpeggiate/>");
        }
        return chunks.toString();
    }

    public static String buildAbcNoteArticulationsXml(AbcMeasureNote note) {
        if (note == null) {
            return "";
        }
        StringBuilder articulationParts = new StringBuilder();
        if (note.isStaccato()) {
            articulationParts.append("<staccato/>");
        }
        if (note.isStaccatissimo()) {
            articulationParts.append("<staccatissimo/>");
        }
        if (note.isAccent()) {
            articulationParts.append("<accent/>");
        }
        if (note.isTenuto()) {
            articulationParts.append("<tenuto/>");
        }
        if (note.isStress()) {
            articulationParts.append("<stress/>");
        }
        if (note.isUnstress()) {
            articulationParts.append("<unstress/>");
        }
        if (note.isStrongAccent()) {
            articulationParts.append("<strong-accent/>");
        }
        if (note.isBreathMark()) {
            articulationParts.append("<breath-mark/>");
        }
        if (note.isCaesura()) {
            articulationParts.append("<caesura/>");
        }
        if (trimToEmpty(note.getPhraseMark()).length() > 0) {
            articulationParts.append("<other-articulation>").append(xmlEscape(note.getPhraseMark()))
                    .append("</other-articulation>");
        }
        return articulationParts.length() == 0 ? "" : "<articulations>" + articulationParts + "</articulations>";
    }

    public static String buildAbcNoteTechnicalXml(AbcMeasureNote note) {
        if (note == null) {
            return "";
        }
        StringBuilder technicalParts = new StringBuilder();
        if (note.isUpBow()) {
            technicalParts.append("<up-bow/>");
        }
        if (note.isDownBow()) {
            technicalParts.append("<down-bow/>");
        }
        if (note.isDoubleTongue()) {
            technicalParts.append("<double-tongue/>");
        }
        if (note.isTripleTongue()) {
            technicalParts.append("<triple-tongue/>");
        }
        if (note.isHeel()) {
            technicalParts.append("<heel/>");
        }
        if (note.isToe()) {
            technicalParts.append("<toe/>");
        }
        for (String fingering : note.getFingerings()) {
            if (trimToEmpty(fingering).length() > 0) {
                technicalParts.append("<fingering>").append(xmlEscape(fingering)).append("</fingering>");
            }
        }
        for (String stringText : note.getStrings()) {
            if (trimToEmpty(stringText).length() > 0) {
                technicalParts.append("<string>").append(xmlEscape(stringText)).append("</string>");
            }
        }
        for (String pluckText : note.getPlucks()) {
            if (trimToEmpty(pluckText).length() > 0) {
                technicalParts.append("<pluck>").append(xmlEscape(pluckText)).append("</pluck>");
            }
        }
        if (note.isOpenString()) {
            technicalParts.append("<open-string/>");
        }
        if (note.isSnapPizzicato()) {
            technicalParts.append("<snap-pizzicato/>");
        }
        if (note.isHarmonic()) {
            technicalParts.append("<harmonic/>");
        }
        if (note.isStopped()) {
            technicalParts.append("<stopped/>");
        }
        if (note.isThumbPosition()) {
            technicalParts.append("<thumb-position/>");
        }
        return technicalParts.length() == 0 ? "" : "<technical>" + technicalParts + "</technical>";
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

    public static AbcVoiceStores createAbcVoiceStores() {
        return new AbcVoiceStores();
    }

    public static List<List<AbcMeasureNote>> ensureAbcVoiceMeasures(AbcVoiceStores stores, String voiceId) {
        AbcVoiceStores safeStores = stores == null ? new AbcVoiceStores() : stores;
        String key = trimToEmpty(voiceId);
        if (!safeStores.getMeasuresByVoice().containsKey(key)) {
            List<List<AbcMeasureNote>> measures = new ArrayList<List<AbcMeasureNote>>();
            measures.add(new ArrayList<AbcMeasureNote>());
            safeStores.getMeasuresByVoice().put(key, measures);
        }
        return safeStores.getMeasuresByVoice().get(key);
    }

    public static AbcMeasureMeta ensureAbcNotationMeasureMeta(AbcVoiceStores stores, String voiceId, int measureNo) {
        AbcVoiceStores safeStores = stores == null ? new AbcVoiceStores() : stores;
        String key = trimToEmpty(voiceId);
        if (!safeStores.getNotationMeasureMetaByVoice().containsKey(key)) {
            safeStores.getNotationMeasureMetaByVoice().put(key, new LinkedHashMap<Integer, AbcMeasureMeta>());
        }
        Map<Integer, AbcMeasureMeta> byMeasure = safeStores.getNotationMeasureMetaByVoice().get(key);
        Integer measureKey = Integer.valueOf(measureNo);
        if (!byMeasure.containsKey(measureKey)) {
            byMeasure.put(measureKey,
                    new AbcMeasureMeta(String.valueOf(measureNo), false, false, false, null, "", "", ""));
        }
        return byMeasure.get(measureKey);
    }

    public static Map<Integer, AbcMeter> ensureAbcMeterByMeasure(AbcVoiceStores stores, String voiceId) {
        AbcVoiceStores safeStores = stores == null ? new AbcVoiceStores() : stores;
        String key = trimToEmpty(voiceId);
        if (!safeStores.getMeterByMeasureByVoice().containsKey(key)) {
            safeStores.getMeterByMeasureByVoice().put(key, new LinkedHashMap<Integer, AbcMeter>());
        }
        return safeStores.getMeterByMeasureByVoice().get(key);
    }

    public static Map<Integer, Integer> ensureAbcTempoByMeasure(AbcVoiceStores stores, String voiceId) {
        AbcVoiceStores safeStores = stores == null ? new AbcVoiceStores() : stores;
        String key = trimToEmpty(voiceId);
        if (!safeStores.getTempoByMeasureByVoice().containsKey(key)) {
            safeStores.getTempoByMeasureByVoice().put(key, new LinkedHashMap<Integer, Integer>());
        }
        return safeStores.getTempoByMeasureByVoice().get(key);
    }

    public static void finalizeAbcActiveEndings(AbcVoiceStores stores) {
        if (stores == null) {
            return;
        }
        for (String voiceId : new ArrayList<String>(stores.getMeasuresByVoice().keySet())) {
            List<List<AbcMeasureNote>> measures = stores.getMeasuresByVoice().get(voiceId);
            if (measures == null) {
                continue;
            }
            while (measures.size() > 1 && measures.get(measures.size() - 1).isEmpty()) {
                measures.remove(measures.size() - 1);
            }
            String activeEndingMarker = trimToEmpty(stores.getActiveEndingByVoice().get(voiceId));
            if (activeEndingMarker.length() == 0) {
                continue;
            }
            int lastMeasureNo = measures.size();
            if (lastMeasureNo >= 1) {
                AbcMeasureMeta measureMeta = ensureAbcNotationMeasureMeta(stores, voiceId, lastMeasureNo);
                if (trimToEmpty(measureMeta.getEndingStop()).length() == 0) {
                    measureMeta.setEndingStop(activeEndingMarker);
                    measureMeta.setEndingStopType("stop");
                }
            }
        }
    }

    public static List<AbcLyricToken> tokenizeAbcLyricLine(String text) {
        String raw = trimToEmpty(text);
        List<AbcLyricToken> tokens = new ArrayList<AbcLyricToken>();
        if (raw.length() == 0) {
            return tokens;
        }
        String[] chunks = raw.replace('|', ' ').split("\\s+");
        boolean pendingHyphenWord = false;
        for (String chunk : chunks) {
            String value = trimToEmpty(chunk);
            if (value.length() == 0) {
                continue;
            }
            if ("*".equals(value)) {
                tokens.add(new AbcLyricToken("skip", "", ""));
                continue;
            }
            if ("_".equals(value)) {
                tokens.add(new AbcLyricToken("extend", "", ""));
                continue;
            }
            String normalized = value.replace('~', ' ');
            if (normalized.endsWith("-") && normalized.length() > 1) {
                tokens.add(new AbcLyricToken("text", normalized.substring(0, normalized.length() - 1),
                        pendingHyphenWord ? "middle" : "begin"));
                pendingHyphenWord = true;
                continue;
            }
            String[] rawParts = normalized.split("-");
            List<String> parts = new ArrayList<String>();
            for (String part : rawParts) {
                if (part.length() > 0) {
                    parts.add(part);
                }
            }
            if (parts.size() <= 1) {
                tokens.add(new AbcLyricToken("text", normalized, pendingHyphenWord ? "end" : "single"));
                pendingHyphenWord = false;
                continue;
            }
            for (int index = 0; index < parts.size(); index++) {
                String syllabic = index == 0 ? "begin" : (index == parts.size() - 1 ? "end" : "middle");
                tokens.add(new AbcLyricToken("text", parts.get(index), syllabic));
            }
            pendingHyphenWord = false;
        }
        return tokens;
    }

    public static void applyAbcLyricsToMeasures(Map<String, List<AbcLyricEntry>> lyricEntriesByVoice,
            Map<String, List<List<AbcMeasureNote>>> measuresByVoice) {
        if (lyricEntriesByVoice == null || measuresByVoice == null) {
            return;
        }
        for (String voiceId : lyricEntriesByVoice.keySet()) {
            List<List<AbcMeasureNote>> measures = measuresByVoice.get(voiceId);
            if (measures == null || measures.isEmpty()) {
                continue;
            }
            List<AbcMeasureNote> lyricTargets = new ArrayList<AbcMeasureNote>();
            for (List<AbcMeasureNote> measure : measures) {
                if (measure == null) {
                    continue;
                }
                for (AbcMeasureNote note : measure) {
                    if (note != null && !note.isRest() && !note.isGrace() && !note.isChord()) {
                        lyricTargets.add(note);
                    }
                }
            }
            if (lyricTargets.isEmpty()) {
                continue;
            }
            int cursor = 0;
            List<AbcLyricEntry> lyricEntries = lyricEntriesByVoice.get(voiceId);
            if (lyricEntries == null) {
                continue;
            }
            for (AbcLyricEntry lyricEntry : lyricEntries) {
                List<AbcLyricToken> tokens = tokenizeAbcLyricLine(lyricEntry == null ? "" : lyricEntry.getText());
                for (AbcLyricToken token : tokens) {
                    if (cursor >= lyricTargets.size()) {
                        break;
                    }
                    if ("skip".equals(token.getType())) {
                        cursor += 1;
                        continue;
                    }
                    if ("extend".equals(token.getType())) {
                        AbcMeasureNote target = lyricTargets.get(Math.max(0, cursor - 1));
                        if (target != null) {
                            target.setLyricExtend(true);
                        }
                        continue;
                    }
                    AbcMeasureNote target = lyricTargets.get(cursor);
                    if (target != null) {
                        target.setLyricText(token.getText());
                        target.setLyricSyllabic(token.getSyllabic());
                    }
                    cursor += 1;
                }
            }
        }
    }

    public static Map<String, Integer> keySignatureAlterByStep(int fifths) {
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        String[] steps = { "C", "D", "E", "F", "G", "A", "B" };
        for (String step : steps) {
            map.put(step, Integer.valueOf(0));
        }
        String[] sharpOrder = { "F", "C", "G", "D", "A", "E", "B" };
        String[] flatOrder = { "B", "E", "A", "D", "G", "C", "F" };
        int safeFifths = Math.max(-7, Math.min(7, fifths));
        if (safeFifths > 0) {
            for (int i = 0; i < safeFifths; i++) {
                map.put(sharpOrder[i], Integer.valueOf(1));
            }
        } else if (safeFifths < 0) {
            for (int i = 0; i < Math.abs(safeFifths); i++) {
                map.put(flatOrder[i], Integer.valueOf(-1));
            }
        }
        return map;
    }

    public static AbcBodyFieldResult applyAbcBodyField(String fieldName, String fieldValue,
            AbcBodyFieldContext context) {
        AbcBodyFieldContext safeContext = context == null ? new AbcBodyFieldContext() : context;
        String normalizedFieldName = trimToEmpty(fieldName);
        int activeKeyFifths = safeContext.getActiveKeyFifths();
        Map<String, Integer> activeKeySignatureAccidentals = keySignatureAlterByStep(activeKeyFifths);
        Fraction activeUnitLength = safeContext.getActiveUnitLength() == null ? DEFAULT_UNIT
                : safeContext.getActiveUnitLength();
        AbcMeter activeMeter = safeContext.getActiveMeter() == null ? new AbcMeter(4, 4)
                : safeContext.getActiveMeter();
        Integer activeTempoBpm = safeContext.getActiveTempoBpm();
        Map<String, Integer> measureAccidentals = safeContext.getMeasureAccidentals() == null
                ? new LinkedHashMap<String, Integer>()
                : safeContext.getMeasureAccidentals();
        List<String> warnings = safeContext.getWarnings() == null ? new ArrayList<String>() : safeContext.getWarnings();
        AbcVoiceStores voiceStores = safeContext.getVoiceStores() == null ? createAbcVoiceStores()
                : safeContext.getVoiceStores();
        String entryVoiceId = trimToEmpty(safeContext.getEntryVoiceId()).length() == 0 ? "1"
                : trimToEmpty(safeContext.getEntryVoiceId());
        int currentMeasureNo = Math.max(1, safeContext.getCurrentMeasureNo());

        if ("K".equals(normalizedFieldName)) {
            AbcKeyInfo inlineKeyInfo = parseKey(trimToEmpty(fieldValue).length() == 0 ? "C" : fieldValue, warnings);
            activeKeyFifths = inlineKeyInfo.getFifths();
            activeKeySignatureAccidentals = keySignatureAlterByStep(activeKeyFifths);
            voiceStores.getCurrentKeyFifthsByVoice().put(entryVoiceId, Integer.valueOf(activeKeyFifths));
            if (safeContext.getKeyHintFifthsByKey() != null) {
                safeContext.getKeyHintFifthsByKey().put(entryVoiceId + "#" + currentMeasureNo,
                        Integer.valueOf(activeKeyFifths));
            }
            measureAccidentals = new LinkedHashMap<String, Integer>();
            return new AbcBodyFieldResult(true, activeKeyFifths, activeKeySignatureAccidentals, activeUnitLength,
                    activeMeter, activeTempoBpm, measureAccidentals);
        }
        if ("L".equals(normalizedFieldName)) {
            activeUnitLength = parseFraction(trimToEmpty(fieldValue).length() == 0 ? "1/8" : fieldValue, "L",
                    warnings);
            return new AbcBodyFieldResult(true, activeKeyFifths, activeKeySignatureAccidentals, activeUnitLength,
                    activeMeter, activeTempoBpm, measureAccidentals);
        }
        if ("M".equals(normalizedFieldName)) {
            activeMeter = parseMeter(trimToEmpty(fieldValue).length() == 0 ? "4/4" : fieldValue, warnings);
            ensureAbcMeterByMeasure(voiceStores, entryVoiceId).put(Integer.valueOf(currentMeasureNo),
                    new AbcMeter(activeMeter.getBeats(), activeMeter.getBeatType()));
            return new AbcBodyFieldResult(true, activeKeyFifths, activeKeySignatureAccidentals, activeUnitLength,
                    activeMeter, activeTempoBpm, measureAccidentals);
        }
        if ("Q".equals(normalizedFieldName)) {
            activeTempoBpm = parseTempoFromQ(trimToEmpty(fieldValue), warnings);
            if (activeTempoBpm != null) {
                ensureAbcTempoByMeasure(voiceStores, entryVoiceId).put(Integer.valueOf(currentMeasureNo),
                        Integer.valueOf(clampRoundedTempo(activeTempoBpm.intValue())));
            }
            return new AbcBodyFieldResult(true, activeKeyFifths, activeKeySignatureAccidentals, activeUnitLength,
                    activeMeter, activeTempoBpm, measureAccidentals);
        }
        return new AbcBodyFieldResult(false, activeKeyFifths, activeKeySignatureAccidentals, activeUnitLength,
                activeMeter, activeTempoBpm, measureAccidentals);
    }

    public static boolean processAbcBarlineEntry(AbcParser.AbcParsedBarlineToken barlineToken,
            AbcBarlineEntryContext context) {
        if (barlineToken == null || context == null) {
            return false;
        }
        AbcParser.AbcParsedRepeatEndingMarker bareRepeatEndingMarker = barlineToken.isEndsMeasure()
                ? AbcParser.parseAbcBareRepeatEndingMarkerAt(context.getText(), barlineToken.getNextIdx())
                : null;
        if (barlineToken.isRepeatEnd()) {
            context.markRepeatEnd();
        }
        if (barlineToken.isRepeatStart()) {
            context.markRepeatStart();
        }
        if ((barlineToken.isEndingStop() || bareRepeatEndingMarker != null)
                && trimToEmpty(context.getActiveEndingMarker()).length() > 0) {
            context.stopActiveEndingAtMeasure(context.getCurrentMeasureNo());
        }
        if (barlineToken.isEndsMeasure()
                && (context.getCurrentMeasureLength() > 0 || context.getMeasuresLength() == 0)) {
            context.advanceToNextMeasure();
        }
        if (barlineToken.isEndsMeasure()) {
            context.clearMeasureAccidentals();
            context.clearLastNote();
        }
        if (bareRepeatEndingMarker != null) {
            return context.startEndingAtCurrentMeasure(bareRepeatEndingMarker.getMarker(),
                    bareRepeatEndingMarker.getNextIdx());
        }
        context.setIdx(barlineToken.getNextIdx());
        context.resetBeamContext();
        return true;
    }

    public static boolean processAbcNonPlayableBodyEntry(AbcParser.AbcParsedBodyEntry bodyEntry,
            AbcNonPlayableBodyEntryContext context) {
        if (bodyEntry == null || context == null) {
            return false;
        }
        if ("barline".equals(bodyEntry.getKind())) {
            return context.handleBarlineToken(bodyEntry.getBarlineToken());
        }
        if ("standalone-body-field".equals(bodyEntry.getKind())) {
            AbcParser.AbcParsedStandaloneBodyField standaloneBodyField = bodyEntry.getStandaloneBodyField();
            if (!context.applyBodyField(standaloneBodyField.getFieldName(), standaloneBodyField.getFieldValue())) {
                context.warnBody("Skipped unsupported standalone body field token: " + standaloneBodyField.getToken());
            }
            context.setIdx(standaloneBodyField.getNextIdx());
            return true;
        }
        if ("unsupported-body-token".equals(bodyEntry.getKind())) {
            AbcParser.AbcParsedUnsupportedBodyToken unsupportedBodyToken = bodyEntry.getUnsupportedBodyToken();
            context.warnBody("Skipped unsupported body token: " + unsupportedBodyToken.getToken());
            context.setIdx(unsupportedBodyToken.getNextIdx());
            return true;
        }
        if ("unsupported-body-number".equals(bodyEntry.getKind())) {
            AbcParser.AbcParsedUnsupportedBodyToken unsupportedBodyNumber = bodyEntry.getUnsupportedBodyNumber();
            context.warnBody("Skipped unsupported body number token: " + unsupportedBodyNumber.getToken());
            context.setIdx(unsupportedBodyNumber.getNextIdx());
            return true;
        }
        return false;
    }

    public static boolean processAbcSimpleBodyToken(AbcParser.AbcParsedBodyToken bodyToken,
            AbcSimpleBodyTokenHandlerContext context) {
        if (bodyToken == null || context == null) {
            return false;
        }
        String kind = bodyToken.getKind();
        if ("broken-rhythm".equals(kind)) {
            return context.handleBrokenRhythmBodyToken(bodyToken);
        }
        if ("decoration".equals(kind)) {
            return context.handleDecorationBodyToken(bodyToken, context.getChar());
        }
        if ("paren".equals(kind)) {
            return context.handleParenBodyToken(bodyToken);
        }
        if ("quoted-string".equals(kind)) {
            return context.handleQuotedStringBodyToken(bodyToken);
        }
        if ("single-char-shorthand".equals(kind)) {
            return context.handleSingleCharShorthandBodyToken(bodyToken, context.getChar());
        }
        if ("slur-stop".equals(kind)) {
            return context.handleSlurStopBodyToken(bodyToken);
        }
        if ("tie".equals(kind)) {
            return context.handleTieBodyToken(bodyToken);
        }
        return false;
    }

    public static boolean processAbcBracketBodyToken(AbcParser.AbcParsedBodyToken bodyToken,
            AbcBracketBodyTokenContext context) {
        if (bodyToken == null || context == null || !"bracket".equals(bodyToken.getKind())) {
            return false;
        }
        AbcParser.AbcParsedBracketToken bracketToken = bodyToken.getBracketToken();
        if ("inline-field".equals(bracketToken.getKind())) {
            return context.handleInlineFieldBracketToken(bracketToken);
        }
        if ("repeat-ending".equals(bracketToken.getKind())) {
            return context.handleRepeatEndingBracketToken(bracketToken);
        }
        AbcParser.AbcParsedPlayableEvent playableEvent = AbcParser.parseAbcPlayableEventAt(context.getText(),
                context.getIdx());
        return context.handlePlayableEvent(playableEvent, true);
    }

    public static AbcGraceGroupProcessResult processAbcGraceGroup(AbcGraceGroupContext context) {
        if (context == null || !"{".equals(context.getChar())) {
            return new AbcGraceGroupProcessResult(false, context == null ? 0 : context.getIdx());
        }
        AbcParser.AbcParsedGraceGroup graceResult = AbcParser.parseAbcGraceGroupAt(context.getText(),
                context.getIdx(), context.getLineNo(), context.getWarnings());
        if (graceResult == null) {
            context.warnBody("Failed to parse grace group; skipped.");
            return new AbcGraceGroupProcessResult(true, context.getIdx() + 1);
        }
        context.appendGraceNotes(graceResult.getNotes());
        return new AbcGraceGroupProcessResult(true, graceResult.getNextIdx());
    }

    public static boolean processAbcBodyFallback(AbcBodyFallbackContext context) {
        if (context == null) {
            return false;
        }
        if (context.handleClosingNotation(context.getChar())) {
            return true;
        }
        if (context.handleUnsupportedPunctuation(context.getChar())) {
            return true;
        }
        if (context.getBodyEntry() == null) {
            context.throwBodyParseError();
        }
        return false;
    }

    public static void applyAbcPendingStateToPlayableNote(AbcPendingPlayableNoteContext context) {
        if (context == null || context.getNote() == null) {
            return;
        }
        AbcPendingPlayableNoteOptions options = context.getOptions() == null ? new AbcPendingPlayableNoteOptions()
                : context.getOptions();
        boolean applySlurStart = options.getApplySlurStart() == null ? true
                : options.getApplySlurStart().booleanValue();
        boolean applyTieStop = options.getApplyTieStop() == null ? true : options.getApplyTieStop().booleanValue();
        String trillHint = trimToEmpty(options.getTrillHint());

        context.applyPendingOrnamentState(context.getNote(), applySlurStart, trillHint);
        context.applyPendingArticulationState(context.getNote());
        context.applyPendingDirectionState(context.getNote());
        context.applyPendingTechnicalState(context.getNote());

        if (applyTieStop && context.hasPendingTieToNext() && !context.getNote().isRest()) {
            context.getNote().setTieStop(true);
            context.clearPendingTieToNext();
        } else if (applyTieStop && context.getNote().isRest() && context.hasPendingTieToNext()) {
            context.warnBody("tie(-) was followed by a rest; tie removed.");
            context.clearPendingTieToNext();
        }
    }

    public static void applyAbcPendingNoteValue(AbcPendingNoteValueContext context) {
        if (context != null && context.getNote() != null && !context.getNote().isRest() && context.isPending()) {
            context.apply();
            context.clear();
        }
    }

    public static void applyAbcPendingNoteOptionalValue(AbcPendingNoteOptionalValueContext context) {
        if (context != null && context.getNote() != null && !context.getNote().isRest()
                && !context.isEmpty(context.getValue())) {
            context.apply(context.getValue());
            context.clear();
        }
    }

    public static void applyAbcPendingNoteArray(AbcPendingNoteArrayContext context) {
        if (context != null && context.getNote() != null && !context.getNote().isRest()
                && !context.getValues().isEmpty()) {
            context.apply(context.getValues());
            context.clear();
        }
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

    public static AbcParsedResult parseForMusicXml(String source, AbcImportOptions options) {
        final List<String> warnings = new ArrayList<String>();
        final Map<String, String> trillWidthHintByKey = new LinkedHashMap<String, String>();
        final Map<String, Integer> keyHintFifthsByKey = new LinkedHashMap<String, Integer>();
        final Map<String, AbcMeasureMeta> measureMetaByKey = new LinkedHashMap<String, AbcMeasureMeta>();
        final Map<String, AbcTransposeMeta> transposeHintByVoiceId = new LinkedHashMap<String, AbcTransposeMeta>();
        final Map<String, String> headers = new LinkedHashMap<String, String>();
        final List<AbcImportBodyEntry> bodyEntries = new ArrayList<AbcImportBodyEntry>();
        final Map<String, List<AbcLyricEntry>> lyricEntriesByVoice =
                new LinkedHashMap<String, List<AbcLyricEntry>>();
        final AbcImportVoiceRegistry voiceRegistry = new AbcImportVoiceRegistry();
        final Map<String, String> userDefinedDecorationBySymbol = new LinkedHashMap<String, String>();
        final Set<String> supportedStandaloneBodyFieldNames = new java.util.LinkedHashSet<String>();
        supportedStandaloneBodyFieldNames.add("K");
        supportedStandaloneBodyFieldNames.add("L");
        supportedStandaloneBodyFieldNames.add("M");
        supportedStandaloneBodyFieldNames.add("Q");
        final AbcImportLineState lineState = new AbcImportLineState();

        BodyTextPusher pushBodyText = new BodyTextPusher() {
            public void push(String rawBodyText, int lineNo, String voiceId) {
                AbcAppendBodyTextResult result = appendAbcBodyTextEntries(rawBodyText, lineNo, voiceId,
                        voiceRegistry, bodyEntries);
                if (result.isAppended()) {
                    lineState.setBodyStarted(true);
                }
                lineState.setCurrentVoiceId(result.getFinalVoiceId());
            }
        };

        String[] lines = String.valueOf(source == null ? "" : source).split("\\n", -1);
        AbcImportLineProcessorContext lineContext = new AbcImportLineProcessorContext(lineState, warnings, headers,
                lyricEntriesByVoice, supportedStandaloneBodyFieldNames, voiceRegistry, userDefinedDecorationBySymbol,
                trillWidthHintByKey, keyHintFifthsByKey, measureMetaByKey, transposeHintByVoiceId, pushBodyText,
                new VoiceDirectiveTailParser() {
                    public AbcParsedVoiceDirectiveTail parse(String raw) {
                        return parseVoiceDirectiveTail(raw);
                    }
                },
                new UserDefinedDecorationParser() {
                    public AbcUserDefinedDecoration parse(String rawValue) {
                        return parseUserDefinedDecoration(rawValue);
                    }
                },
                new DecorationSymbolExpander() {
                    public String expand(String text, Map<String, String> userDefinedDecorationBySymbol) {
                        return expandUserDefinedDecorationSymbols(text, userDefinedDecorationBySymbol);
                    }
                });
        for (int index = 0; index < lines.length; index++) {
            processAbcImportLine(lines[index], index + 1, lineContext);
        }
        if (bodyEntries.isEmpty()) {
            throw new IllegalArgumentException("Body not found. Please provide ABC note content. (line 1)");
        }

        AbcMeter meter = parseMeter(firstNonEmpty(headers.get("M"), "", "4/4"), warnings);
        Fraction unitLength = parseFraction(firstNonEmpty(headers.get("L"), "", "1/8"), "L", warnings);
        AbcKeyInfo keyInfo = parseKey(firstNonEmpty(headers.get("K"), "", "C"), warnings);
        Integer tempoBpm = parseTempoFromQ(headers.get("Q") == null ? "" : headers.get("Q"), warnings);
        AbcVoiceStores voiceStores = createAbcVoiceStores();
        int noteCount = 0;

        for (AbcImportBodyEntry entry : bodyEntries) {
            noteCount += importAbcBodyEntryToVoiceStores(entry, voiceStores, unitLength, meter, keyInfo, tempoBpm,
                    keyHintFifthsByKey, trillWidthHintByKey, warnings);
        }

        List<AbcImportDiagnostic> diagnostics = new ArrayList<AbcImportDiagnostic>();
        for (String warning : warnings) {
            diagnostics.add(new AbcImportDiagnostic("warn", "ABC_IMPORT_WARNING", "abc", warning, "", null, "",
                    null));
        }

        finalizeAbcActiveEndings(voiceStores);
        applyAbcLyricsToMeasures(lyricEntriesByVoice, voiceStores.getMeasuresByVoice());
        if (noteCount == 0) {
            throw new IllegalArgumentException("No notes or rests were found. (line 1)");
        }

        boolean overfullCompatibilityMode = options == null || options.getOverfullCompatibilityMode() == null
                || options.getOverfullCompatibilityMode().booleanValue();
        if (overfullCompatibilityMode) {
            normalizeAbcVoiceStoresToMeasureCapacity(voiceStores, measureCapacityDiv(meter), diagnostics);
        }

        List<AbcParsedPart> parts = buildAbcParsedParts(voiceRegistry, voiceStores, keyHintFifthsByKey,
                measureMetaByKey, transposeHintByVoiceId, lineState.getScoreDirective());
        int measureCount = 0;
        for (AbcParsedPart part : parts) {
            measureCount = Math.max(measureCount, part.getMeasures().size());
        }
        return new AbcParsedResult(new AbcParsedMeta(firstNonEmpty(headers.get("T"), "", "mikuscore"),
                firstNonEmpty(headers.get("C"), "", "Unknown"), meter, keyInfo, tempoBpm), parts, warnings,
                diagnostics);
    }

    public static String musicXmlFromAbc(String source, AbcImportOptions options) {
        return buildMusicXmlFromAbcParsed(parseForMusicXml(source, options), source, options);
    }

    private static boolean isUnsupportedBodyPunctuation(char ch) {
        return ";`?@#$*".indexOf(ch) >= 0;
    }

    public static String musicXmlToAbc(String source) {
        Element root = parseMusicXmlRootElement(source);
        if (root == null) {
            throw new IllegalArgumentException("Invalid MusicXML input.");
        }
        List<Element> parts = directChildren(root, "part");
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("MusicXML part not found.");
        }

        String title = firstNonEmpty(
                directChildText(firstDescendantByPath(root, new String[] { "work" }), "work-title"),
                directChildText(root, "movement-title"), "mikuscore");
        String composer = firstNonEmpty(findMusicXmlCreator(root, "composer"), "", "Unknown");
        Element firstMeasure = firstDescendantByPath(root, new String[] { "part", "measure" });
        Element firstAttributes = directChild(firstMeasure, "attributes");
        int beats = Math.max(1, (int) Math.round(parseDouble(directChildText(directChild(firstAttributes, "time"),
                "beats"), 4)));
        int beatType = Math.max(1, (int) Math.round(parseDouble(directChildText(directChild(firstAttributes, "time"),
                "beat-type"), 4)));
        int fifths = (int) Math.round(parseDouble(directChildText(directChild(firstAttributes, "key"), "fifths"), 0));
        String key = keyFromFifthsMode(Math.max(-7, Math.min(7, fifths)), "major");
        AbcTempoHeader tempoHeader = findMusicXmlTempoHeader(root);

        Map<String, String> partNameById = collectMusicXmlPartNames(root);
        List<String> headerLines = new ArrayList<String>();
        List<String> bodyLines = new ArrayList<String>();
        List<String> metaLines = new ArrayList<String>();
        headerLines.add("X:1");
        headerLines.add("T:" + title);
        headerLines.add("C:" + composer);
        headerLines.add("M:" + beats + "/" + beatType);
        headerLines.add("L:1/8");
        if (tempoHeader != null) {
            headerLines.add("Q:" + tempoHeader.getUnit() + "=" + tempoHeader.getBpm());
        }
        headerLines.add("K:" + key);

        int partIndex = 0;
        for (Element part : parts) {
            partIndex++;
            String partId = firstNonEmpty(trimToEmpty(part.getAttribute("id")), "", "P" + partIndex);
            String partName = firstNonEmpty(partNameById.get(partId), "", partId);
            List<AbcMusicXmlLaneDef> lanes = collectMusicXmlPartLaneDefs(part, partId, partName);
            for (AbcMusicXmlLaneDef lane : lanes) {
                String clefSuffix = trimToEmpty(lane.getClef()).length() > 0 ? " clef=" + lane.getClef() : "";
                headerLines.add("V:" + lane.getNormalizedVoiceId() + " name=\"" + abcQuotedTextEscape(lane.getLaneName())
                        + "\"" + clefSuffix);
                metaLines.addAll(buildMusicXmlPartTransposeMetaLines(part, lane.getNormalizedVoiceId()));
                bodyLines.add("V:" + lane.getNormalizedVoiceId());
                bodyLines.add(buildMusicXmlPartLaneBody(part, lane, fifths, metaLines));
            }
        }
        String metaBlock = metaLines.isEmpty() ? "\n" : "\n" + joinStringsWithSeparator(metaLines, "\n") + "\n";
        return joinStringsWithSeparator(headerLines, "\n") + "\n\n" + joinStringsWithSeparator(bodyLines, "\n")
                + metaBlock;
    }

    private static Element parseMusicXmlRootElement(String source) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(String.valueOf(source == null ? "" : source).getBytes("UTF-8")));
            return document == null ? null : document.getDocumentElement();
        } catch (Exception ex) {
            return null;
        }
    }

    private static String buildMusicXmlPartLaneBody(Element part, AbcMusicXmlLaneDef lane, int defaultFifths,
            List<String> metaLines) {
        List<String> measureTexts = new ArrayList<String>();
        double currentDivisions = 480;
        int currentFifths = defaultFifths;
        double currentBeats = 4;
        double currentBeatType = 4;
        Integer lastEmittedKeyFifths = Integer.valueOf(defaultFifths);
        List<String> lyricTokens = new ArrayList<String>();
        boolean pendingLyricExtension = false;
        String activeWedgeType = "";
        for (Element measure : directChildren(part, "measure")) {
            AbcMusicXmlMeasureState state = updateMusicXmlMeasureState(measure, currentDivisions, currentFifths,
                    currentBeats, currentBeatType, lastEmittedKeyFifths);
            currentDivisions = state.getDivisions();
            currentFifths = state.getFifths();
            currentBeats = state.getBeats();
            currentBeatType = state.getBeatType();
            int safeMeasureNumber = measureTexts.size() + 1;
            if (metaLines != null) {
                metaLines.addAll(buildMusicXmlMeasureMetaLines(lane.getNormalizedVoiceId(), measure, safeMeasureNumber));
                metaLines.addAll(buildMusicXmlMeasureDiagMetaLines(lane.getNormalizedVoiceId(), measure,
                        safeMeasureNumber));
            }
            List<String> tokens = new ArrayList<String>();
            List<String> pendingChordPitches = new ArrayList<String>();
            String pendingChordLength = "";
            String pendingChordPrefix = "";
            boolean pendingChordTie = false;
            boolean pendingChordSlurStop = false;
            List<String> pendingGraceTokens = new ArrayList<String>();
            List<String> pendingHarmonySymbols = new ArrayList<String>();
            List<String> pendingDirectionWords = new ArrayList<String>();
            List<String> pendingDirectionDecorations = new ArrayList<String>();
            int activeTupletActual = 0;
            int activeTupletNormal = 0;
            int activeTupletRemaining = 0;
            int eventNo = 0;
            for (Element child : directChildren(measure)) {
                if ("harmony".equals(child.getTagName())) {
                    String chordSymbol = abcChordSymbolFromHarmony(child);
                    if (chordSymbol.length() > 0) {
                        pendingHarmonySymbols.add(chordSymbol);
                    }
                    continue;
                }
                if ("direction".equals(child.getTagName())) {
                    AbcMusicXmlDirectionTokens directionTokens = collectMusicXmlDirectionTokens(child, activeWedgeType);
                    pendingDirectionWords.addAll(directionTokens.getWords());
                    pendingDirectionDecorations.addAll(directionTokens.getDecorations());
                    activeWedgeType = directionTokens.getActiveWedgeType();
                    continue;
                }
                if (!"note".equals(child.getTagName()) || !isMusicXmlNoteInLane(child, lane)) {
                    continue;
                }
                AbcMusicXmlNoteTiming timing = resolveMusicXmlNoteTiming(child, currentDivisions);
                if (!timing.isPlayable()) {
                    continue;
                }
                boolean hasTieStart = directChildWithAttribute(child, "tie", "type", "start") != null;
                Element notations = directChild(child, "notations");
                boolean hasSlurStart = directChildWithAttribute(notations, "slur", "type", "start") != null;
                boolean hasSlurStop = directChildWithAttribute(notations, "slur", "type", "stop") != null;
                boolean hasGraceSlash = "yes".equals(trimToEmpty(directChild(child, "grace") == null ? ""
                        : directChild(child, "grace").getAttribute("slash")).toLowerCase());
                boolean hasTupletStart = directChildWithAttribute(notations, "tuplet", "type", "start") != null;
                Element timeModification = directChild(child, "time-modification");
                int timeModificationActual = parseInt(directChildText(timeModification, "actual-notes"), 0);
                int timeModificationNormal = parseInt(directChildText(timeModification, "normal-notes"), 0);
                boolean hasTimeModification = timeModificationActual > 0 && timeModificationNormal > 0;
                Fraction wholeFraction = reduceFraction(timing.getDuration(),
                        Math.max(1, (int) Math.round(currentDivisions * 4)));
                if (hasTimeModification) {
                    wholeFraction = multiplyFractions(wholeFraction,
                            new Fraction(timeModificationActual, timeModificationNormal));
                }
                Fraction lengthRatio = divideFractions(wholeFraction, DEFAULT_UNIT);
                AbcMusicXmlPitchToken pitch = resolveMusicXmlNotePitchToken(child, state.getKeyAlterByStep(),
                        state.getMeasureAccidentalByStepOctave());
                String lengthToken = abcLengthTokenFromFraction(lengthRatio);
                AbcMusicXmlNoteOrnaments ornaments = collectMusicXmlNoteOrnaments(child);
                if (timing.isGrace()) {
                    String graceToken = (hasGraceSlash ? "/" : "") + pitch.getToken() + lengthToken
                            + (hasTieStart ? "-" : "");
                    if (timing.isChord() && !pendingGraceTokens.isEmpty()) {
                        String last = pendingGraceTokens.remove(pendingGraceTokens.size() - 1);
                        pendingGraceTokens.add(mergeMusicXmlToAbcGraceChordToken(last, graceToken));
                    } else {
                        pendingGraceTokens.add(graceToken);
                    }
                    continue;
                }
                if (!timing.isChord() && hasTimeModification
                        && (hasTupletStart || activeTupletRemaining <= 0)) {
                    activeTupletActual = timeModificationActual;
                    activeTupletNormal = timeModificationNormal;
                    activeTupletRemaining = timeModificationActual;
                }
                String tupletPrefix = !timing.isChord() && activeTupletRemaining > 0
                        && activeTupletRemaining == activeTupletActual
                                ? "(" + activeTupletActual + ":" + activeTupletNormal + ":" + activeTupletActual
                                : "";
                String eventPrefix = "";
                if (!timing.isChord()) {
                    for (String symbol : pendingHarmonySymbols) {
                        eventPrefix += "\"" + abcQuotedTextEscape(symbol) + "\"";
                    }
                    for (String word : pendingDirectionWords) {
                        eventPrefix += "\"" + abcQuotedTextEscape(word) + "\"";
                    }
                    eventPrefix += joinStrings(pendingDirectionDecorations);
                    eventPrefix += tupletPrefix;
                    if (hasSlurStart) {
                        eventPrefix += "(";
                    }
                    if (!pendingGraceTokens.isEmpty()) {
                        eventPrefix += "{" + joinStrings(pendingGraceTokens) + "}";
                        pendingGraceTokens.clear();
                    }
                    eventPrefix += buildMusicXmlNoteOrnamentPrefix(ornaments);
                    eventPrefix += buildMusicXmlNoteArticulationPrefix(collectMusicXmlNoteArticulations(child));
                    eventPrefix += buildMusicXmlNoteTechnicalPrefix(collectMusicXmlNoteTechnical(child));
                    eventPrefix += buildMusicXmlNoteFermataPrefix(child);
                    pendingHarmonySymbols.clear();
                    pendingDirectionWords.clear();
                    pendingDirectionDecorations.clear();
                }
                if (timing.isChord()) {
                    if (pendingChordPitches.isEmpty()) {
                        pendingChordPitches.add(pitch.getToken());
                        pendingChordLength = lengthToken;
                        pendingChordPrefix = eventPrefix;
                        pendingChordTie = hasTieStart;
                        pendingChordSlurStop = hasSlurStop;
                    } else {
                        pendingChordPitches.add(pitch.getToken());
                        pendingChordTie = pendingChordTie || hasTieStart;
                        pendingChordSlurStop = pendingChordSlurStop || hasSlurStop;
                    }
                    continue;
                }
                eventNo++;
                appendMusicXmlTrillAccidentalMetaLine(metaLines, lane.getNormalizedVoiceId(), measure,
                        safeMeasureNumber, eventNo, ornaments);
                flushMusicXmlToAbcPendingChordToken(tokens, pendingChordPitches, pendingChordLength,
                        pendingChordPrefix, pendingChordTie, pendingChordSlurStop);
                pendingChordPitches.clear();
                pendingChordPitches.add(pitch.getToken());
                pendingChordLength = lengthToken;
                pendingChordPrefix = eventPrefix;
                pendingChordTie = hasTieStart;
                pendingChordSlurStop = hasSlurStop;
                if (directChild(child, "rest") == null) {
                    Element lyric = directChild(child, "lyric");
                    String lyricText = directChildText(lyric, "text").trim();
                    String lyricSyllabic = firstNonEmpty(directChildText(lyric, "syllabic"), "", "single");
                    boolean lyricExtend = directChild(lyric, "extend") != null;
                    if (lyricText.length() > 0) {
                        lyricTokens.add(abcLyricTokenFromMusicXml(lyricText, lyricSyllabic));
                        pendingLyricExtension = lyricExtend;
                    } else if (pendingLyricExtension) {
                        lyricTokens.add("_");
                    } else {
                        lyricTokens.add("*");
                    }
                    if (lyricText.length() > 0 && !lyricExtend) {
                        pendingLyricExtension = false;
                    }
                }
                if (!timing.isChord() && activeTupletRemaining > 0) {
                    activeTupletRemaining--;
                    if (activeTupletRemaining <= 0) {
                        activeTupletActual = 0;
                        activeTupletNormal = 0;
                    }
                }
            }
            if (!pendingGraceTokens.isEmpty()) {
                tokens.add("{" + joinStrings(pendingGraceTokens) + "}");
                pendingGraceTokens.clear();
            }
            flushMusicXmlToAbcPendingChordToken(tokens, pendingChordPitches, pendingChordLength, pendingChordPrefix,
                    pendingChordTie, pendingChordSlurStop);
            if (tokens.isEmpty()) {
                int measureDuration = Math.max(1, (int) Math.round(currentDivisions * currentBeats
                        * (4.0 / Math.max(1.0, currentBeatType))));
                Fraction wholeFraction = reduceFraction(measureDuration, Math.max(1, (int) Math.round(currentDivisions
                        * 4)));
                tokens.add("z" + abcLengthTokenFromFraction(divideFractions(wholeFraction, DEFAULT_UNIT)));
            }
            String keyPrefix = state.isNeedsInlineKeyChange()
                    ? "[K:" + keyFromFifthsMode(Math.max(-7, Math.min(7, currentFifths)), "major") + "] "
                    : "";
            AbcMusicXmlMeasureBarlineTokens barlineTokens = buildMusicXmlMeasureBarlineTokens(measure);
            String leftPrefix = barlineTokens.getLeftPrefix();
            String rightSuffix = barlineTokens.getRightSuffix();
            measureTexts.add((leftPrefix + (leftPrefix.length() > 0 ? " " : "") + keyPrefix
                    + joinStringsWithSeparator(tokens, " ") + " " + rightSuffix).trim());
            lastEmittedKeyFifths = Integer.valueOf(currentFifths);
        }
        String body = joinStringsWithSeparator(measureTexts, " ");
        if (hasMeaningfulMusicXmlLyricTokens(lyricTokens)) {
            body += "\nw: " + joinStringsWithSeparator(lyricTokens, " ");
        }
        return body;
    }

    private static void appendMusicXmlTrillAccidentalMetaLine(List<String> metaLines, String normalizedVoiceId,
            Element measure, int safeMeasureNumber, int eventNo, AbcMusicXmlNoteOrnaments ornaments) {
        if (metaLines == null || ornaments == null || !ornaments.isTrill()
                || trimToEmpty(ornaments.getTrillAccidentalText()).length() == 0 || eventNo <= 0) {
            return;
        }
        String rawMeasureNumber = trimToEmpty(measure == null ? "" : measure.getAttribute("number"));
        String measureNumber = rawMeasureNumber.length() > 0 ? rawMeasureNumber : String.valueOf(safeMeasureNumber);
        metaLines.add("%@mks trill voice=" + trimToEmpty(normalizedVoiceId) + " measure=" + measureNumber
                + " event=" + eventNo + " upper=" + trimToEmpty(ornaments.getTrillAccidentalText()));
    }

    private static void flushMusicXmlToAbcPendingChordToken(List<String> tokens, List<String> pendingChordPitches,
            String pendingChordLength) {
        flushMusicXmlToAbcPendingChordToken(tokens, pendingChordPitches, pendingChordLength, "");
    }

    private static void flushMusicXmlToAbcPendingChordToken(List<String> tokens, List<String> pendingChordPitches,
            String pendingChordLength, String pendingChordPrefix) {
        flushMusicXmlToAbcPendingChordToken(tokens, pendingChordPitches, pendingChordLength, pendingChordPrefix, false,
                false);
    }

    private static void flushMusicXmlToAbcPendingChordToken(List<String> tokens, List<String> pendingChordPitches,
            String pendingChordLength, String pendingChordPrefix, boolean pendingChordTie, boolean pendingChordSlurStop) {
        if (tokens == null || pendingChordPitches == null || pendingChordPitches.isEmpty()) {
            return;
        }
        String prefix = trimToEmpty(pendingChordPrefix);
        String suffix = (pendingChordTie ? "-" : "") + (pendingChordSlurStop ? ")" : "");
        if (pendingChordPitches.size() == 1) {
            tokens.add(prefix + pendingChordPitches.get(0) + trimToEmpty(pendingChordLength) + suffix);
        } else {
            tokens.add(prefix + "[" + joinStrings(pendingChordPitches) + "]" + trimToEmpty(pendingChordLength)
                    + suffix);
        }
    }

    private static String mergeMusicXmlToAbcGraceChordToken(String left, String right) {
        String safeLeft = trimToEmpty(left);
        String safeRight = trimToEmpty(right);
        if (safeLeft.startsWith("[") && safeLeft.indexOf(']') >= 0) {
            return safeLeft.replaceFirst("\\]", Matcher.quoteReplacement(safeRight + "]"));
        }
        return "[" + safeLeft + safeRight + "]";
    }

    private static AbcMusicXmlMeasureBarlineTokens buildMusicXmlMeasureBarlineTokens(Element measure) {
        Element leftBarline = directChildWithAttribute(measure, "barline", "location", "left");
        Element rightBarline = directChildWithAttribute(measure, "barline", "location", "right");
        Element leftRepeat = directChild(leftBarline, "repeat");
        Element rightRepeat = directChild(rightBarline, "repeat");
        Element leftEnding = directChild(leftBarline, "ending");
        Element rightEnding = directChild(rightBarline, "ending");
        boolean hasLeftRepeat = "forward".equals(trimToEmpty(leftRepeat == null ? ""
                : leftRepeat.getAttribute("direction")).toLowerCase());
        boolean hasRightRepeat = "backward".equals(trimToEmpty(rightRepeat == null ? ""
                : rightRepeat.getAttribute("direction")).toLowerCase());
        String leftEndingNumber = trimToEmpty(leftEnding == null ? "" : leftEnding.getAttribute("number"));
        String rightEndingNumber = trimToEmpty(rightEnding == null ? "" : rightEnding.getAttribute("number"));
        String leftPrefix = (hasLeftRepeat ? "|:" : "") + (leftEndingNumber.length() > 0 ? "[" + leftEndingNumber : "");
        String rightSuffix;
        if (hasRightRepeat && rightEndingNumber.length() > 0) {
            rightSuffix = ":|]";
        } else if (hasRightRepeat) {
            rightSuffix = ":|";
        } else if (rightEndingNumber.length() > 0) {
            rightSuffix = "]|";
        } else {
            rightSuffix = "|";
        }
        return new AbcMusicXmlMeasureBarlineTokens(leftPrefix, rightSuffix);
    }

    private static String buildMusicXmlNoteFermataPrefix(Element note) {
        Element fermata = directChild(directChild(note, "notations"), "fermata");
        if (fermata == null) {
            return "";
        }
        String type = trimToEmpty(fermata.getAttribute("type")).toLowerCase();
        String shape = elementText(fermata).trim().toLowerCase();
        return "inverted".equals(type) || "inverted".equals(shape) ? "!invertedfermata!" : "!fermata!";
    }

    private static boolean hasMeaningfulMusicXmlLyricTokens(List<String> lyricTokens) {
        if (lyricTokens == null) {
            return false;
        }
        for (String token : lyricTokens) {
            if (!"*".equals(trimToEmpty(token))) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> collectMusicXmlPartNames(Element root) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        Element partList = directChild(root, "part-list");
        for (Element scorePart : directChildren(partList, "score-part")) {
            String id = trimToEmpty(scorePart.getAttribute("id"));
            if (id.length() > 0) {
                result.put(id, firstNonEmpty(directChildText(scorePart, "part-name"), "", id));
            }
        }
        return result;
    }

    private static String findMusicXmlCreator(Element root, String type) {
        Element identification = directChild(root, "identification");
        String normalizedType = trimToEmpty(type).toLowerCase();
        for (Element creator : directChildren(identification, "creator")) {
            if (normalizedType.equals(trimToEmpty(creator.getAttribute("type")).toLowerCase())) {
                return elementText(creator);
            }
        }
        return "";
    }

    private static AbcTempoHeader findMusicXmlTempoHeader(Element root) {
        AbcTempoHeader leading = findLeadingMusicXmlTempoHeader(root);
        if (leading != null) {
            return leading;
        }
        for (Element part : directChildren(root, "part")) {
            for (Element measure : directChildren(part, "measure")) {
                for (Element direction : directChildren(measure, "direction")) {
                    AbcTempoHeader tempo = musicXmlTempoHeaderFromDirection(direction);
                    if (tempo != null) {
                        return tempo;
                    }
                }
            }
        }
        return null;
    }

    private static AbcTempoHeader findLeadingMusicXmlTempoHeader(Element root) {
        for (Element part : directChildren(root, "part")) {
            for (Element measure : directChildren(part, "measure")) {
                AbcTempoHeader tempo = null;
                for (Element child : directChildren(measure)) {
                    if ("note".equals(child.getTagName())) {
                        return tempo;
                    }
                    if ("direction".equals(child.getTagName())) {
                        AbcTempoHeader directionTempo = musicXmlTempoHeaderFromDirection(child);
                        if (directionTempo != null) {
                            tempo = directionTempo;
                        }
                    }
                }
                if (tempo != null) {
                    return tempo;
                }
            }
        }
        return null;
    }

    private static AbcTempoHeader musicXmlTempoHeaderFromDirection(Element direction) {
        for (Element directionType : directChildren(direction, "direction-type")) {
            Element metronome = directChild(directionType, "metronome");
            Double tempo = parseOptionalNumber(directChildText(metronome, "per-minute"));
            if (tempo != null && tempo.doubleValue() > 0) {
                String unit = musicXmlMetronomeBeatUnitToAbcUnit(directChildText(metronome, "beat-unit"));
                return new AbcTempoHeader(unit, clampRoundedTempo((int) Math.round(tempo.doubleValue())));
            }
        }
        for (Element sound : directChildren(direction, "sound")) {
            Double tempo = parseOptionalNumber(trimToEmpty(sound.getAttribute("tempo")));
            if (tempo != null && tempo.doubleValue() > 0) {
                return new AbcTempoHeader("1/4", clampRoundedTempo((int) Math.round(tempo.doubleValue())));
            }
        }
        return null;
    }

    private static String musicXmlMetronomeBeatUnitToAbcUnit(String beatUnit) {
        String normalized = trimToEmpty(beatUnit).toLowerCase();
        if ("whole".equals(normalized)) {
            return "1/1";
        }
        if ("half".equals(normalized)) {
            return "1/2";
        }
        if ("eighth".equals(normalized)) {
            return "1/8";
        }
        if ("16th".equals(normalized)) {
            return "1/16";
        }
        return "1/4";
    }

    private static int importAbcBodyEntryToVoiceStores(AbcImportBodyEntry entry, AbcVoiceStores voiceStores,
            Fraction unitLength, AbcMeter meter, AbcKeyInfo keyInfo, Integer tempoBpm,
            Map<String, Integer> keyHintFifthsByKey, Map<String, String> trillWidthHintByKey,
            List<String> warnings) {
        String voiceId = entry == null ? "1" : entry.getVoiceId();
        List<List<AbcMeasureNote>> measures = ensureAbcVoiceMeasures(voiceStores, voiceId);
        List<AbcMeasureNote> currentMeasure = measures.get(measures.size() - 1);
        Map<String, Integer> measureAccidentals = new LinkedHashMap<String, Integer>();
        Fraction activeUnitLength = unitLength == null ? DEFAULT_UNIT : unitLength;
        AbcMeter activeMeter = meter == null ? new AbcMeter(4, 4) : meter;
        Integer activeTempoBpm = tempoBpm;
        int activeKeyFifths = keyInfo == null ? 0 : keyInfo.getFifths();
        Map<String, Integer> activeKeySignatureAccidentals = keySignatureAlterByStep(activeKeyFifths);
        String text = entry == null ? "" : entry.getText();
        int lineNo = entry == null ? 1 : entry.getLineNo();
        int idx = 0;
        int noteCount = 0;
        Fraction tupletScale = null;
        int tupletRemaining = 0;
        int tupletActual = 0;
        int tupletNormal = 0;
        int tupletSpecRemaining = 0;
        AbcPendingBodyDecorationState pendingDecoration = new AbcPendingBodyDecorationState();
        String activeEndingMarker = trimToEmpty(voiceStores.getActiveEndingByVoice().get(voiceId));
        boolean pendingTieToNext = false;
        int pendingSlurStart = 0;
        int eventNo = 0;
        AbcParser.AbcRatio pendingRhythmScale = null;
        List<String> pendingChordSymbols = new ArrayList<String>();
        List<String> pendingAnnotations = new ArrayList<String>();
        List<Integer> lastEventNoteIndices = new ArrayList<Integer>();
        String pendingBeamMode = "";
        int lastPlayableEndIdx = -1;
        boolean beamRunHasAdjacentNotes = false;
        while (idx < text.length()) {
            char ch = text.charAt(idx);
            if (ch == '\\') {
                warnings.add("line " + lineNo + ": Skipped stray body continuation marker");
                idx++;
                continue;
            }
            if (ch == ' ' || ch == '\t' || ch == ',' || ch == '\'') {
                if (beamRunHasAdjacentNotes && (ch == ' ' || ch == '\t')) {
                    pendingBeamMode = "begin";
                    beamRunHasAdjacentNotes = false;
                }
                idx++;
                continue;
            }
            if (ch == '{') {
                AbcParser.AbcParsedGraceGroup graceGroup = AbcParser.parseAbcGraceGroupAt(text, idx, lineNo,
                        warnings);
                if (graceGroup == null) {
                    warnings.add("line " + lineNo + ": Failed to parse grace group; skipped.");
                    idx++;
                    continue;
                }
                List<AbcMeasureNote> graceNotes = buildAbcGraceNotes(graceGroup, voiceId, activeUnitLength, lineNo,
                        activeKeySignatureAccidentals, measureAccidentals, warnings);
                currentMeasure.addAll(graceNotes);
                noteCount += graceNotes.size();
                idx = graceGroup.getNextIdx();
                continue;
            }
            AbcParser.AbcParsedBodyEntry bodyEntry = AbcParser.parseAbcBodyEntryAt(text, idx);
            if (bodyEntry == null) {
                if (isUnsupportedBodyPunctuation(ch)) {
                    warnings.add("line " + lineNo + ": Skipped unsupported body punctuation: " + ch);
                    idx++;
                    continue;
                }
                throw new IllegalArgumentException("line " + lineNo + ": Failed to parse note/rest: "
                        + text.substring(idx, Math.min(text.length(), idx + 12)));
            }
            if ("barline".equals(bodyEntry.getKind())) {
                AbcParser.AbcParsedBarlineToken barlineToken = bodyEntry.getBarlineToken();
                AbcParser.AbcParsedRepeatEndingMarker bareRepeatEndingMarker = barlineToken.isEndsMeasure()
                        ? AbcParser.parseAbcBareRepeatEndingMarkerAt(text, barlineToken.getNextIdx())
                        : null;
                int currentMeasureNo = Math.max(1, measures.size());
                if (barlineToken.isRepeatEnd()) {
                    putAbcNotationMeasureMeta(voiceStores, voiceId, currentMeasureNo, false, true, "", "", "");
                }
                if (barlineToken.isRepeatStart()) {
                    putAbcNotationMeasureMeta(voiceStores, voiceId, currentMeasureNo, true, false, "", "", "");
                }
                if ((barlineToken.isEndingStop() || bareRepeatEndingMarker != null)
                        && trimToEmpty(activeEndingMarker).length() > 0) {
                    putAbcNotationMeasureMeta(voiceStores, voiceId, currentMeasureNo, false, false, "",
                            activeEndingMarker, "stop");
                    activeEndingMarker = "";
                }
                if (barlineToken.isEndsMeasure() && (currentMeasure.size() > 0 || measures.size() == 0)) {
                    currentMeasure = new ArrayList<AbcMeasureNote>();
                    measures.add(currentMeasure);
                    measureAccidentals.clear();
                    eventNo = 0;
                    pendingBeamMode = "";
                    lastPlayableEndIdx = -1;
                    beamRunHasAdjacentNotes = false;
                }
                if (bareRepeatEndingMarker != null) {
                    int nextMeasureNo = Math.max(1, measures.size());
                    putAbcNotationMeasureMeta(voiceStores, voiceId, nextMeasureNo, false, false,
                            bareRepeatEndingMarker.getMarker(), "", "");
                    activeEndingMarker = bareRepeatEndingMarker.getMarker();
                    idx = bareRepeatEndingMarker.getNextIdx();
                    continue;
                }
                idx = barlineToken.getNextIdx();
                continue;
            }
            if ("standalone-body-field".equals(bodyEntry.getKind())) {
                AbcParser.AbcParsedStandaloneBodyField field = bodyEntry.getStandaloneBodyField();
                AbcBodyFieldResult fieldResult = applyAbcBodyField(field.getFieldName(), field.getFieldValue(),
                        new AbcBodyFieldContext(activeKeyFifths, activeUnitLength, activeMeter, activeTempoBpm,
                                measureAccidentals, voiceStores, voiceId, measures.size(), keyHintFifthsByKey,
                                warnings));
                activeKeyFifths = fieldResult.getActiveKeyFifths();
                activeKeySignatureAccidentals = fieldResult.getActiveKeySignatureAccidentals();
                activeUnitLength = fieldResult.getActiveUnitLength();
                activeMeter = fieldResult.getActiveMeter();
                activeTempoBpm = fieldResult.getActiveTempoBpm();
                measureAccidentals = fieldResult.getMeasureAccidentals();
                if (!fieldResult.isHandled()) {
                    warnings.add("line " + lineNo + ": Skipped unsupported standalone body field token: "
                            + field.getToken());
                }
                idx = field.getNextIdx();
                continue;
            }
            if ("unsupported-body-token".equals(bodyEntry.getKind())) {
                warnings.add("line " + lineNo + ": Skipped unsupported body token: "
                        + bodyEntry.getUnsupportedBodyToken().getToken());
                idx = bodyEntry.getUnsupportedBodyToken().getNextIdx();
                continue;
            }
            if ("unsupported-body-number".equals(bodyEntry.getKind())) {
                warnings.add("line " + lineNo + ": Skipped unsupported body number token: "
                        + bodyEntry.getUnsupportedBodyNumber().getToken());
                idx = bodyEntry.getUnsupportedBodyNumber().getNextIdx();
                continue;
            }
            if ("body-token".equals(bodyEntry.getKind())) {
                AbcParser.AbcParsedBodyToken bodyToken = bodyEntry.getBodyToken();
                if ("single-char-shorthand".equals(bodyToken.getKind())) {
                    applyBasicAbcSingleCharShorthand(bodyToken.getShorthand().getKind(), pendingDecoration);
                    idx = bodyToken.getShorthand().getNextIdx();
                    continue;
                }
                if ("broken-rhythm".equals(bodyToken.getKind())) {
                    AbcParser.AbcParsedBrokenRhythm brokenRhythm = bodyToken.getBrokenRhythm();
                    if (lastEventNoteIndices.isEmpty()
                            || !scaleAbcLastEventDuration(currentMeasure, lastEventNoteIndices,
                                    brokenRhythm.getLeftScale())) {
                        warnings.add("line " + lineNo + ": broken rhythm(" + brokenRhythm.getSymbol()
                                + ")  has no preceding note; skipped.");
                    } else {
                        pendingRhythmScale = brokenRhythm.getRightScale();
                    }
                    idx = brokenRhythm.getNextIdx();
                    continue;
                }
                if ("decoration".equals(bodyToken.getKind())) {
                    AbcParser.AbcParsedDecoration decoration = bodyToken.getDecoration();
                    if (!decoration.isTerminated()) {
                        warnings.add("line " + lineNo + ": Unterminated decoration marker: "
                                + decoration.getDelimiter());
                    } else if (!applyBasicAbcBodyDecoration(decoration.getRawDecoration(),
                            decoration.getDecoration(), pendingDecoration)) {
                        warnings.add("line " + lineNo + ": Skipped decoration: " + decoration.getDelimiter()
                                + decoration.getDecoration() + decoration.getDelimiter());
                    }
                    idx = decoration.getNextIdx();
                    continue;
                }
                if ("tie".equals(bodyToken.getKind())) {
                    boolean marked = markAbcTieStartOnLastEvent(currentMeasure, lastEventNoteIndices);
                    if (!marked) {
                        warnings.add("line " + lineNo + ": tie(-)  has no preceding note; skipped.");
                    } else {
                        pendingTieToNext = true;
                    }
                    idx = bodyToken.getTie().getNextIdx();
                    continue;
                }
                if ("quoted-string".equals(bodyToken.getKind())) {
                    AbcParser.AbcParsedQuotedString quotedString = bodyToken.getQuotedString();
                    if (!quotedString.isTerminated()) {
                        warnings.add("line " + lineNo + ": Unterminated quoted string marker; value kept.");
                    }
                    String quotedText = quotedString.getNormalizedText();
                    if (isLikelyAbcChordSymbol(quotedText)
                            && buildHarmonyXmlFromChordSymbol(quotedText).length() > 0) {
                        pendingChordSymbols.add(quotedText);
                    } else if (trimToEmpty(quotedText).length() > 0) {
                        pendingAnnotations.add(quotedText);
                    }
                    idx = quotedString.getNextIdx();
                    continue;
                }
                if ("slur-stop".equals(bodyToken.getKind())) {
                    if (!markAbcSlurStopOnLastNote(currentMeasure, lastEventNoteIndices)) {
                        warnings.add("line " + lineNo + ": slur stop()) has no preceding note; skipped.");
                    }
                    idx = bodyToken.getSlurStop().getNextIdx();
                    continue;
                }
                if ("paren".equals(bodyToken.getKind()) && "tuplet".equals(bodyToken.getParenToken().getKind())) {
                    AbcParser.AbcParsedTuplet tuplet = bodyToken.getParenToken().getTuplet();
                    if (tuplet.getActual() > 0 && tuplet.getNormal() > 0 && tuplet.getCount() > 0) {
                        tupletScale = new Fraction(tuplet.getNormal(), tuplet.getActual());
                        tupletRemaining = tuplet.getCount();
                        tupletActual = tuplet.getActual();
                        tupletNormal = tuplet.getNormal();
                        tupletSpecRemaining = tuplet.getCount();
                    } else {
                        warnings.add("line " + lineNo + ": Failed to parse tuplet notation: " + tuplet.getRaw());
                    }
                    idx = bodyToken.getParenToken().getNextIdx();
                    continue;
                }
                if ("paren".equals(bodyToken.getKind())) {
                    pendingSlurStart++;
                    idx = bodyToken.getParenToken().getNextIdx();
                    continue;
                }
                if ("bracket".equals(bodyToken.getKind())
                        && "inline-field".equals(bodyToken.getBracketToken().getKind())) {
                    AbcParser.AbcParsedInlineField field = bodyToken.getBracketToken().getInlineField();
                    AbcBodyFieldResult fieldResult = applyAbcBodyField(field.getFieldName(), field.getFieldValue(),
                            new AbcBodyFieldContext(activeKeyFifths, activeUnitLength, activeMeter, activeTempoBpm,
                                    measureAccidentals, voiceStores, voiceId, measures.size(), keyHintFifthsByKey,
                                    warnings));
                    activeKeyFifths = fieldResult.getActiveKeyFifths();
                    activeKeySignatureAccidentals = fieldResult.getActiveKeySignatureAccidentals();
                    activeUnitLength = fieldResult.getActiveUnitLength();
                    activeMeter = fieldResult.getActiveMeter();
                    activeTempoBpm = fieldResult.getActiveTempoBpm();
                    measureAccidentals = fieldResult.getMeasureAccidentals();
                    if (!fieldResult.isHandled()) {
                        warnings.add("line " + lineNo + ": Skipped unsupported inline field: ["
                                + field.getFieldName() + ":" + field.getFieldValue() + "]");
                    }
                    idx = field.getNextIdx();
                    continue;
                }
                if ("bracket".equals(bodyToken.getKind())
                        && "repeat-ending".equals(bodyToken.getBracketToken().getKind())) {
                    AbcParser.AbcParsedRepeatEndingMarker marker = bodyToken.getBracketToken().getRepeatEndingMarker();
                    putAbcNotationMeasureMeta(voiceStores, voiceId, Math.max(1, measures.size()), false, false,
                            marker.getMarker(), "", "");
                    activeEndingMarker = marker.getMarker();
                    idx = marker.getNextIdx();
                    continue;
                }
                if ("bracket".equals(bodyToken.getKind())) {
                    AbcParser.AbcParsedPlayableEvent playableEvent = AbcParser.parseAbcPlayableEventAt(text, idx);
                    if (playableEvent != null && "playable".equals(playableEvent.getKind())) {
                        Fraction length = parseAbcLengthToken(playableEvent.getRawLengthToken(), lineNo);
                        Fraction absoluteLength = multiplyFractions(activeUnitLength, length);
                        if (pendingRhythmScale != null) {
                            absoluteLength = multiplyFractions(absoluteLength,
                                    new Fraction(pendingRhythmScale.getNum(), pendingRhythmScale.getDen()));
                            pendingRhythmScale = null;
                        }
                        AbcTupletEvent tupletEvent = null;
                        if (tupletRemaining > 0 && tupletScale != null) {
                            tupletEvent = new AbcTupletEvent(tupletActual, tupletNormal, tupletSpecRemaining);
                            absoluteLength = multiplyFractions(absoluteLength, tupletScale);
                            tupletRemaining--;
                            tupletSpecRemaining--;
                            if (tupletRemaining <= 0) {
                                tupletScale = null;
                                tupletActual = 0;
                                tupletNormal = 0;
                                tupletSpecRemaining = 0;
                            }
                        }
                        int duration = durationInDivisions(absoluteLength, 960);
                        if (duration <= 0) {
                            warnings.add("line " + lineNo + ": Skipped "
                                    + invalidPlayableLengthKind(playableEvent) + " with invalid length.");
                            idx = playableEvent.getNextIdx();
                            continue;
                        }
                        boolean adjacentToPreviousPlayable = idx == lastPlayableEndIdx;
                        List<AbcMeasureNote> notes = buildAbcPlayableNotes(playableEvent, voiceId, absoluteLength,
                                duration, lineNo, activeKeySignatureAccidentals, measureAccidentals, tupletEvent,
                                pendingDecoration, pendingBeamMode, warnings);
                        pendingBeamMode = "";
                        if (notes.isEmpty()) {
                            idx = playableEvent.getNextIdx();
                            continue;
                        }
                        applyAbcPendingQuotedStringsToEvent(notes, pendingChordSymbols, pendingAnnotations);
                        eventNo++;
                        applyAbcTrillHintToEvent(notes, trillWidthHintByKey, voiceId, measures.size(), eventNo);
                        if (pendingSlurStart > 0 && applyAbcSlurStartToEvent(notes)) {
                            pendingSlurStart = 0;
                        }
                        if (pendingTieToNext) {
                            if (!applyAbcTieStopToEvent(notes)) {
                                warnings.add("line " + lineNo + ": tie(-) was followed by a rest; tie removed.");
                            }
                            pendingTieToNext = false;
                        }
                        int eventStartIndex = currentMeasure.size();
                        currentMeasure.addAll(notes);
                        lastEventNoteIndices = eventNoteIndices(eventStartIndex, notes.size());
                        noteCount += notes.size();
                        if (adjacentToPreviousPlayable) {
                            beamRunHasAdjacentNotes = true;
                        }
                        lastPlayableEndIdx = playableEvent.getNextIdx();
                        idx = playableEvent.getNextIdx();
                        continue;
                    }
                }
                idx++;
                continue;
            }
            if (isUnsupportedBodyPunctuation(ch)) {
                warnings.add("line " + lineNo + ": Skipped unsupported body punctuation: " + ch);
                idx++;
                continue;
            }
            if ("playable-event".equals(bodyEntry.getKind())) {
                AbcParser.AbcParsedPlayableEvent playableEvent = bodyEntry.getPlayableEvent();
                if (!"playable".equals(playableEvent.getKind())) {
                    if ("malformed-accidental".equals(playableEvent.getKind())) {
                        warnings.add("line " + lineNo + ": Skipped malformed accidental token: "
                                + playableEvent.getAccidentalText());
                        idx = Math.max(idx + 1, playableEvent.getNextIdx());
                        continue;
                    }
                    warnings.add("line " + lineNo + ": Skipped malformed playable token.");
                    idx = Math.max(idx + 1, playableEvent.getNextIdx());
                    continue;
                }
                Fraction length = parseAbcLengthToken(playableEvent.getRawLengthToken(), lineNo);
                Fraction absoluteLength = multiplyFractions(activeUnitLength, length);
                if (pendingRhythmScale != null) {
                    absoluteLength = multiplyFractions(absoluteLength,
                            new Fraction(pendingRhythmScale.getNum(), pendingRhythmScale.getDen()));
                    pendingRhythmScale = null;
                }
                AbcTupletEvent tupletEvent = null;
                if (tupletRemaining > 0 && tupletScale != null) {
                    tupletEvent = new AbcTupletEvent(tupletActual, tupletNormal, tupletSpecRemaining);
                    absoluteLength = multiplyFractions(absoluteLength, tupletScale);
                    tupletRemaining--;
                    tupletSpecRemaining--;
                    if (tupletRemaining <= 0) {
                        tupletScale = null;
                        tupletActual = 0;
                        tupletNormal = 0;
                        tupletSpecRemaining = 0;
                    }
                }
                int duration = durationInDivisions(absoluteLength, 960);
                if (duration <= 0) {
                    warnings.add("line " + lineNo + ": Skipped "
                            + invalidPlayableLengthKind(playableEvent) + " with invalid length.");
                    idx = playableEvent.getNextIdx();
                    continue;
                }
                boolean adjacentToPreviousPlayable = idx == lastPlayableEndIdx;
                List<AbcMeasureNote> notes = buildAbcPlayableNotes(playableEvent, voiceId, absoluteLength,
                        duration, lineNo, activeKeySignatureAccidentals, measureAccidentals, tupletEvent,
                        pendingDecoration, pendingBeamMode, warnings);
                pendingBeamMode = "";
                if (notes.isEmpty()) {
                    idx = playableEvent.getNextIdx();
                    continue;
                }
                applyAbcPendingQuotedStringsToEvent(notes, pendingChordSymbols, pendingAnnotations);
                eventNo++;
                applyAbcTrillHintToEvent(notes, trillWidthHintByKey, voiceId, measures.size(), eventNo);
                if (pendingSlurStart > 0 && applyAbcSlurStartToEvent(notes)) {
                    pendingSlurStart = 0;
                }
                if (pendingTieToNext) {
                    if (!applyAbcTieStopToEvent(notes)) {
                        warnings.add("line " + lineNo + ": tie(-) was followed by a rest; tie removed.");
                    }
                    pendingTieToNext = false;
                }
                int eventStartIndex = currentMeasure.size();
                currentMeasure.addAll(notes);
                lastEventNoteIndices = eventNoteIndices(eventStartIndex, notes.size());
                noteCount += notes.size();
                if (adjacentToPreviousPlayable) {
                    beamRunHasAdjacentNotes = true;
                }
                lastPlayableEndIdx = playableEvent.getNextIdx();
                idx = playableEvent.getNextIdx();
                continue;
            }
            idx++;
        }
        voiceStores.getActiveEndingByVoice().put(voiceId, activeEndingMarker);
        voiceStores.getCurrentKeyFifthsByVoice().put(voiceId, Integer.valueOf(activeKeyFifths));
        return noteCount;
    }

    private static void applyAbcTrillHintToEvent(List<AbcMeasureNote> notes,
            Map<String, String> trillWidthHintByKey, String voiceId, int measureNo, int eventNo) {
        if (notes == null || trillWidthHintByKey == null || eventNo <= 0) {
            return;
        }
        String hint = trimToEmpty(trillWidthHintByKey.get(trimToEmpty(voiceId) + "#"
                + Math.max(1, measureNo) + "#" + eventNo));
        if (hint.length() == 0) {
            return;
        }
        for (AbcMeasureNote note : notes) {
            if (note != null && (note.isTrill() || note.isTrillLineStart())) {
                note.setTrillAccidentalText(hint);
                return;
            }
        }
    }

    private static void putAbcNotationMeasureMeta(AbcVoiceStores voiceStores, String voiceId, int measureNo,
            boolean repeatStart, boolean repeatEnd, String endingStart, String endingStop, String endingStopType) {
        Map<Integer, AbcMeasureMeta> byMeasure = voiceStores.getNotationMeasureMetaByVoice().get(voiceId);
        if (byMeasure == null) {
            byMeasure = new LinkedHashMap<Integer, AbcMeasureMeta>();
            voiceStores.getNotationMeasureMetaByVoice().put(voiceId, byMeasure);
        }
        Integer key = Integer.valueOf(Math.max(1, measureNo));
        AbcMeasureMeta existing = byMeasure.get(key);
        String number = existing == null ? String.valueOf(key.intValue()) : existing.getNumber();
        boolean implicit = existing != null && existing.isImplicit();
        Integer repeatTimes = existing == null ? null : existing.getRepeatTimes();
        String mergedEndingStart = firstNonEmpty(endingStart, existing == null ? "" : existing.getEndingStart(), "");
        String mergedEndingStop = firstNonEmpty(endingStop, existing == null ? "" : existing.getEndingStop(), "");
        String mergedEndingStopType = firstNonEmpty(endingStopType,
                existing == null ? "" : existing.getEndingStopType(), "");
        byMeasure.put(key, new AbcMeasureMeta(number, implicit, repeatStart || (existing != null
                && existing.isRepeatStart()), repeatEnd || (existing != null && existing.isRepeatEnd()), repeatTimes,
                mergedEndingStart, mergedEndingStop, mergedEndingStopType));
    }

    private static List<Integer> eventNoteIndices(int startIndex, int count) {
        List<Integer> indices = new ArrayList<Integer>();
        for (int index = 0; index < count; index++) {
            indices.add(Integer.valueOf(startIndex + index));
        }
        return indices;
    }

    private static boolean applyAbcTieStopToEvent(List<AbcMeasureNote> notes) {
        boolean applied = false;
        if (notes == null) {
            return false;
        }
        for (AbcMeasureNote note : notes) {
            if (note != null && !note.isRest()) {
                note.setTieStop(true);
                applied = true;
            }
        }
        return applied;
    }

    private static void applyAbcPendingQuotedStringsToEvent(List<AbcMeasureNote> notes, List<String> pendingChordSymbols,
            List<String> pendingAnnotations) {
        if (notes == null || notes.isEmpty()) {
            return;
        }
        AbcMeasureNote target = notes.get(0);
        if (target == null) {
            return;
        }
        if (pendingChordSymbols != null && !pendingChordSymbols.isEmpty()) {
            target.getChordSymbols().addAll(pendingChordSymbols);
            pendingChordSymbols.clear();
        }
        if (pendingAnnotations != null && !pendingAnnotations.isEmpty()) {
            target.getAnnotations().addAll(pendingAnnotations);
            pendingAnnotations.clear();
        }
    }

    private static boolean applyAbcSlurStartToEvent(List<AbcMeasureNote> notes) {
        if (notes == null || notes.isEmpty()) {
            return false;
        }
        for (int index = 0; index < notes.size(); index++) {
            AbcMeasureNote note = notes.get(index);
            if (note != null && !note.isRest()) {
                notes.set(index, copyAbcMeasureNote(note, note.getDuration(), note.isTieStart(), true,
                        note.isSlurStop()));
                return true;
            }
        }
        return false;
    }

    private static boolean markAbcTieStartOnLastEvent(List<AbcMeasureNote> currentMeasure,
            List<Integer> lastEventNoteIndices) {
        if (currentMeasure == null || lastEventNoteIndices == null || lastEventNoteIndices.isEmpty()) {
            return false;
        }
        boolean marked = false;
        for (Integer noteIndex : lastEventNoteIndices) {
            int index = noteIndex == null ? -1 : noteIndex.intValue();
            if (index < 0 || index >= currentMeasure.size()) {
                continue;
            }
            AbcMeasureNote note = currentMeasure.get(index);
            if (note != null && !note.isRest()) {
                currentMeasure.set(index, copyAbcMeasureNoteWithTieStart(note));
                marked = true;
            }
        }
        return marked;
    }

    private static boolean markAbcSlurStopOnLastNote(List<AbcMeasureNote> currentMeasure,
            List<Integer> lastEventNoteIndices) {
        if (currentMeasure == null || lastEventNoteIndices == null || lastEventNoteIndices.isEmpty()) {
            return false;
        }
        for (int reverseIndex = lastEventNoteIndices.size() - 1; reverseIndex >= 0; reverseIndex--) {
            Integer noteIndex = lastEventNoteIndices.get(reverseIndex);
            int index = noteIndex == null ? -1 : noteIndex.intValue();
            if (index < 0 || index >= currentMeasure.size()) {
                continue;
            }
            AbcMeasureNote note = currentMeasure.get(index);
            if (note != null && !note.isRest()) {
                currentMeasure.set(index, copyAbcMeasureNote(note, note.getDuration(), note.isTieStart(),
                        note.isSlurStart(), true));
                return true;
            }
        }
        return false;
    }

    private static boolean scaleAbcLastEventDuration(List<AbcMeasureNote> currentMeasure,
            List<Integer> lastEventNoteIndices, AbcParser.AbcRatio scale) {
        if (currentMeasure == null || lastEventNoteIndices == null || lastEventNoteIndices.isEmpty()
                || scale == null) {
            return false;
        }
        boolean scaled = false;
        for (Integer noteIndex : lastEventNoteIndices) {
            int index = noteIndex == null ? -1 : noteIndex.intValue();
            if (index < 0 || index >= currentMeasure.size()) {
                continue;
            }
            AbcMeasureNote note = currentMeasure.get(index);
            if (note == null || note.isRest()) {
                continue;
            }
            int duration = Math.max(1,
                    (int) Math.round(note.getDuration() * (((double) scale.getNum()) / Math.max(1, scale.getDen()))));
            currentMeasure.set(index, copyAbcMeasureNote(note, duration, note.isTieStart(), note.isSlurStart(),
                    note.isSlurStop()));
            scaled = true;
        }
        return scaled;
    }

    private static AbcMeasureNote copyAbcMeasureNoteWithTieStart(AbcMeasureNote note) {
        return copyAbcMeasureNote(note, note.getDuration(), true, note.isSlurStart(), note.isSlurStop());
    }

    private static AbcMeasureNote copyAbcMeasureNote(AbcMeasureNote note, int duration, boolean tieStart,
            boolean slurStart, boolean slurStop) {
        String type = duration == note.getDuration() ? note.getType() : typeFromDuration(duration, 960);
        return copyAbcMeasureNoteWithValues(note, duration, note.isChord(), type, tieStart, slurStart, slurStop);
    }

    private static AbcMeasureNote copyAbcMeasureNoteWithChordFlag(AbcMeasureNote note, boolean chord) {
        return copyAbcMeasureNoteWithValues(note, note.getDuration(), chord, note.getType(), note.isTieStart(),
                note.isSlurStart(), note.isSlurStop());
    }

    private static AbcMeasureNote copyAbcMeasureNoteWithValues(AbcMeasureNote note, int duration, boolean chord,
            String type, boolean tieStart, boolean slurStart, boolean slurStop) {
        return new AbcMeasureNote(note.getVoice(), duration, chord, note.isGrace(), note.isRest(),
                note.getStep(), note.getOctave(), note.getAlter(), type, note.getStaff(),
                note.getAccidentalText(), note.isAccidentalEditorial(), note.isAccidentalCautionary(), tieStart,
                note.isTieStop(), note.isGraceSlash(), note.getBeamMode(), note.getLyricText(),
                note.getLyricSyllabic(), note.isLyricExtend(), note.getTimeModificationActual(),
                note.getTimeModificationNormal(), new ArrayList<String>(note.getAnnotations()), note.isSegno(),
                note.isCoda(), note.getRehearsalMark(), note.isFine(), note.isDaCapo(), note.isDalSegno(),
                note.isToCoda(), note.isCrescendoStart(), note.isCrescendoStop(), note.isDiminuendoStart(),
                note.isDiminuendoStop(), note.getDynamicMark(), note.isSfz(), slurStart, slurStop,
                note.isTupletStart(), note.isTupletStop(), note.isTrill(), note.isTrillLineStart(),
                note.isTrillLineStop(), note.getTrillAccidentalText(), note.getTurnType(), note.isTurnSlash(),
                note.isDelayedTurn(), note.getMordentType(), note.getTremoloType(), note.getTremoloMarks(),
                note.isGlissandoStart(), note.isGlissandoStop(), note.isSlideStart(), note.isSlideStop(),
                note.isSchleifer(), note.isShake(), note.isArpeggiate(), note.isStaccato(),
                note.isStaccatissimo(), note.isAccent(), note.isTenuto(), note.isStress(), note.isUnstress(),
                note.getFermataType(), note.isStrongAccent(), note.isBreathMark(), note.isCaesura(),
                note.getPhraseMark(), note.isUpBow(), note.isDownBow(), note.isDoubleTongue(),
                note.isTripleTongue(), note.isHeel(), note.isToe(), new ArrayList<String>(note.getFingerings()),
                new ArrayList<String>(note.getStrings()), new ArrayList<String>(note.getPlucks()),
                note.isOpenString(), note.isSnapPizzicato(), note.isHarmonic(), note.isStopped(),
                note.isThumbPosition());
    }

    private static int measureCapacityDiv(AbcMeter meter) {
        AbcMeter safeMeter = meter == null ? new AbcMeter(4, 4) : meter;
        return Math.max(1, (int) Math.round((960.0 * 4.0 * Math.max(1, safeMeter.getBeats()))
                / Math.max(1, safeMeter.getBeatType())));
    }

    private static void normalizeAbcVoiceStoresToMeasureCapacity(AbcVoiceStores voiceStores, int measureCapacity,
            List<AbcImportDiagnostic> diagnostics) {
        if (voiceStores == null || voiceStores.getMeasuresByVoice() == null) {
            return;
        }
        for (Map.Entry<String, List<List<AbcMeasureNote>>> entry : voiceStores.getMeasuresByVoice().entrySet()) {
            AbcNormalizedMeasures normalized = normalizeAbcMeasuresToCapacity(entry.getValue(), measureCapacity);
            entry.setValue(normalized.getMeasures());
            for (AbcMeasureReflowDiagnostic diagnostic : normalized.getDiagnostics()) {
                diagnostics.add(new AbcImportDiagnostic("warn", "OVERFULL_REFLOWED", "abc", "",
                        entry.getKey(), Integer.valueOf(diagnostic.getSourceMeasure()), "reflowed",
                        Integer.valueOf(diagnostic.getMovedEvents())));
            }
        }
    }

    private static AbcNormalizedMeasures normalizeAbcMeasuresToCapacity(List<List<AbcMeasureNote>> measures,
            int capacity) {
        if (measures == null || measures.isEmpty()) {
            return new AbcNormalizedMeasures(java.util.Arrays.asList(new ArrayList<AbcMeasureNote>()),
                    new ArrayList<AbcMeasureReflowDiagnostic>());
        }
        if (capacity <= 0) {
            return new AbcNormalizedMeasures(measures, new ArrayList<AbcMeasureReflowDiagnostic>());
        }

        List<List<AbcMeasureNote>> normalized = new ArrayList<List<AbcMeasureNote>>();
        List<AbcMeasureNote> carry = new ArrayList<AbcMeasureNote>();
        List<AbcMeasureReflowDiagnostic> diagnostics = new ArrayList<AbcMeasureReflowDiagnostic>();
        int measureIdx = 0;

        while (measureIdx < measures.size() || !carry.isEmpty()) {
            List<AbcMeasureNote> source = measureIdx < measures.size() ? measures.get(measureIdx)
                    : new ArrayList<AbcMeasureNote>();
            measureIdx++;
            List<AbcMeasureNote> events = new ArrayList<AbcMeasureNote>(carry);
            if (source != null) {
                events.addAll(source);
            }
            carry = new ArrayList<AbcMeasureNote>();

            List<AbcMeasureNote> out = new ArrayList<AbcMeasureNote>();
            int occupied = 0;

            for (int index = 0; index < events.size(); index++) {
                AbcMeasureNote note = events.get(index);
                if (note == null) {
                    continue;
                }
                if (note.isChord()) {
                    out.add(out.isEmpty() ? copyAbcMeasureNoteWithChordFlag(note, false) : note);
                    continue;
                }
                int duration = note.isGrace() ? 0 : Math.max(1, note.getDuration());
                if (occupied + duration <= capacity || out.isEmpty()) {
                    out.add(note);
                    occupied += duration;
                    continue;
                }
                carry = new ArrayList<AbcMeasureNote>(events.subList(index, events.size()));
                diagnostics.add(new AbcMeasureReflowDiagnostic(normalized.size() + 1, Math.max(1, carry.size())));
                break;
            }

            normalized.add(out);
        }

        while (normalized.size() > 1 && normalized.get(normalized.size() - 1).isEmpty()) {
            normalized.remove(normalized.size() - 1);
        }
        if (normalized.isEmpty()) {
            normalized.add(new ArrayList<AbcMeasureNote>());
        }
        return new AbcNormalizedMeasures(normalized, diagnostics);
    }

    private static List<AbcMeasureNote> buildAbcGraceNotes(AbcParser.AbcParsedGraceGroup graceGroup, String voiceId,
            Fraction unitLength, int lineNo, Map<String, Integer> keySignatureAccidentals,
            Map<String, Integer> measureAccidentals, List<String> warnings) {
        List<AbcMeasureNote> notes = new ArrayList<AbcMeasureNote>();
        Map<String, Integer> graceAccidentals = new LinkedHashMap<String, Integer>(
                measureAccidentals == null ? new LinkedHashMap<String, Integer>() : measureAccidentals);
        for (AbcParser.AbcParsedGraceNote parsedNote : graceGroup.getNotes()) {
            Fraction length = parseAbcLengthToken(parsedNote.getLengthToken(), lineNo);
            Fraction absoluteLength = multiplyFractions(unitLength == null ? DEFAULT_UNIT : unitLength, length);
            int duration = durationInDivisions(absoluteLength, 960);
            if (duration <= 0) {
                warnings.add("line " + lineNo + ": Skipped grace note with invalid length.");
                continue;
            }
            try {
                notes.add(buildAbcGraceNoteData(voiceId, parsedNote.getPitchChar(), parsedNote.getAccidentalText(),
                        parsedNote.getOctaveShift(), absoluteLength, duration, lineNo, keySignatureAccidentals,
                        graceAccidentals, parsedNote.isGraceSlash()));
            } catch (IllegalArgumentException ex) {
                if (ex.getMessage() != null && ex.getMessage().matches("(?i).*Octave out of range.*")) {
                    warnings.add("line " + lineNo + ": Skipped grace note with unsupported octave range.");
                    continue;
                }
                throw ex;
            }
        }
        return notes;
    }

    private static List<AbcMeasureNote> buildAbcPlayableNotes(AbcParser.AbcParsedPlayableEvent playableEvent,
            String voiceId, Fraction absoluteLength, int duration, int lineNo,
            Map<String, Integer> keySignatureAccidentals, Map<String, Integer> measureAccidentals,
            AbcTupletEvent tupletEvent, AbcPendingBodyDecorationState pendingDecoration, String beamMode,
            List<String> warnings) {
        List<AbcMeasureNote> notes = new ArrayList<AbcMeasureNote>();
        List<AbcParser.AbcParsedPitchSource> pitchSources = playableEvent.getPitchSources();
        for (int index = 0; index < pitchSources.size(); index++) {
            AbcParser.AbcParsedPitchSource pitchSource = pitchSources.get(index);
            try {
                notes.add(buildAbcNoteData(voiceId, pitchSource.getPitchChar(), pitchSource.getAccidentalText(),
                        pitchSource.getOctaveShift(), absoluteLength, duration, lineNo, keySignatureAccidentals,
                        measureAccidentals, index > 0, index == 0 ? tupletEvent : null,
                        index == 0 ? pendingDecoration : null, index == 0 ? beamMode : ""));
            } catch (IllegalArgumentException ex) {
                if (ex.getMessage() != null && ex.getMessage().matches("(?i).*Octave out of range.*")) {
                    String kind = pitchSources.size() > 1 ? "chord note" : "note";
                    warnings.add("line " + lineNo + ": Skipped " + kind + " with unsupported octave range.");
                    return new ArrayList<AbcMeasureNote>();
                }
                throw ex;
            }
        }
        return notes;
    }

    private static String invalidPlayableLengthKind(AbcParser.AbcParsedPlayableEvent playableEvent) {
        return playableEvent != null && playableEvent.getPitchSources().size() > 1 ? "chord" : "note";
    }

    private static AbcMeasureNote buildAbcGraceNoteData(String voiceId, String pitchChar, String accidental,
            String octaveShift, Fraction absoluteLength, int duration, int lineNo,
            Map<String, Integer> keySignatureAccidentals, Map<String, Integer> measureAccidentals,
            boolean graceSlash) {
        AbcMeasureNote note = buildAbcNoteData(voiceId, pitchChar, accidental, octaveShift, absoluteLength, duration,
                lineNo, keySignatureAccidentals, measureAccidentals, false, null, null, "");
        return new AbcMeasureNote(note.getVoice(), note.getDuration(), false, true, note.isRest(), note.getStep(),
                note.getOctave(), note.getAlter(), note.getType(), note.getStaff(), note.getAccidentalText(),
                note.isAccidentalEditorial(), note.isAccidentalCautionary(), note.isTieStart(), note.isTieStop(),
                graceSlash);
    }

    private static AbcMeasureNote buildAbcNoteData(String voiceId, String pitchChar, String accidental,
            String octaveShift, Fraction absoluteLength, int duration, int lineNo,
            Map<String, Integer> keySignatureAccidentals, Map<String, Integer> measureAccidentals, boolean chord,
            AbcTupletEvent tupletEvent, AbcPendingBodyDecorationState pendingDecoration, String beamMode) {
        String pitch = trimToEmpty(pitchChar);
        boolean rest = pitch.matches("[zZxX]");
        String type = typeFromFraction(absoluteLength);
        AbcPendingBodyDecorationState appliedDecoration = rest || pendingDecoration == null
                ? new AbcPendingBodyDecorationState()
                : pendingDecoration.copyAndClear();
        if (rest) {
            if (tupletEvent != null) {
                return new AbcMeasureNote(voiceId, duration, chord, false, true, "C", Integer.valueOf(4),
                        Integer.valueOf(0), type, null, "", false, false, false, false, false, "", "", "single",
                        false, Integer.valueOf(tupletEvent.getActual()), Integer.valueOf(tupletEvent.getNormal()),
                        new ArrayList<String>(), false, false, "", false, false, false, false, false, false, false,
                        false, "", false, false, false, tupletEvent.getRemaining() == tupletEvent.getActual(),
                        tupletEvent.getRemaining() == 1, false, false, false, "", "", false, false, "", "", null,
                        false, false, false, false, false, false, false, false, false, false, false, false, false,
                        "", false, false, false, "", false, false, false, false, false, false,
                        new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), false, false, false,
                        false, false);
            }
            return new AbcMeasureNote(voiceId, duration, chord, false, true, "C", Integer.valueOf(4),
                    Integer.valueOf(0), type);
        }
        String step = pitch.toUpperCase();
        int octave = pitch.matches("[a-g]") ? 5 : 4;
        String shifts = octaveShift == null ? "" : octaveShift;
        for (int index = 0; index < shifts.length(); index++) {
            char ch = shifts.charAt(index);
            if (ch == '\'') {
                octave++;
            } else if (ch == ',') {
                octave--;
            }
        }
        if (octave < 0 || octave > 9) {
            throw new IllegalArgumentException("line " + lineNo + ": Octave out of range");
        }
        Integer alter = null;
        String accidentalText = "";
        Integer explicitAlter = accidentalToAlter(accidental);
        if (explicitAlter != null) {
            alter = explicitAlter;
            if (explicitAlter.intValue() == 0) {
                accidentalText = "natural";
            } else if (explicitAlter.intValue() > 0) {
                accidentalText = explicitAlter.intValue() >= 2 ? "double-sharp" : "sharp";
            } else {
                accidentalText = explicitAlter.intValue() <= -2 ? "flat-flat" : "flat";
            }
            measureAccidentals.put(step, explicitAlter);
        } else {
            int resolvedAlter = 0;
            if (measureAccidentals.containsKey(step)) {
                resolvedAlter = measureAccidentals.get(step).intValue();
            } else if (keySignatureAccidentals.containsKey(step)) {
                resolvedAlter = keySignatureAccidentals.get(step).intValue();
            }
            alter = resolvedAlter == 0 ? null : Integer.valueOf(resolvedAlter);
        }
        Integer timeModificationActual = tupletEvent == null ? null : Integer.valueOf(tupletEvent.getActual());
        Integer timeModificationNormal = tupletEvent == null ? null : Integer.valueOf(tupletEvent.getNormal());
        boolean tupletStart = tupletEvent != null && tupletEvent.getRemaining() == tupletEvent.getActual();
        boolean tupletStop = tupletEvent != null && tupletEvent.getRemaining() == 1;
        return new AbcMeasureNote(voiceId, duration, chord, false, false, step, Integer.valueOf(octave), alter,
                type, null, accidentalText,
                appliedDecoration.isEditorialAccidental() && accidentalText.length() > 0,
                appliedDecoration.isCourtesyAccidental() && accidentalText.length() > 0, false, false, false,
                trimToEmpty(beamMode), "", "single", false,
                timeModificationActual, timeModificationNormal, new ArrayList<String>(), appliedDecoration.isSegno(),
                appliedDecoration.isCoda(), appliedDecoration.getRehearsalMark(), appliedDecoration.isFine(),
                appliedDecoration.isDaCapo(), appliedDecoration.isDalSegno(), appliedDecoration.isToCoda(),
                appliedDecoration.isCrescendoStart(), appliedDecoration.isCrescendoStop(),
                appliedDecoration.isDiminuendoStart(), appliedDecoration.isDiminuendoStop(),
                appliedDecoration.getDynamicMark(), appliedDecoration.isSfz(), false, false, tupletStart, tupletStop,
                appliedDecoration.isTrill(), appliedDecoration.isTrillLineStart(),
                appliedDecoration.isTrillLineStop(), "", appliedDecoration.getTurnType(),
                appliedDecoration.isTurnSlash(), appliedDecoration.isDelayedTurn(), appliedDecoration.getMordentType(),
                appliedDecoration.getTremoloType(), appliedDecoration.getTremoloMarks(),
                appliedDecoration.isGlissandoStart(), appliedDecoration.isGlissandoStop(),
                appliedDecoration.isSlideStart(), appliedDecoration.isSlideStop(), appliedDecoration.isSchleifer(),
                appliedDecoration.isShake(), appliedDecoration.isArpeggiate(), appliedDecoration.isStaccato(),
                appliedDecoration.isStaccatissimo(), appliedDecoration.isAccent(), appliedDecoration.isTenuto(),
                appliedDecoration.isStress(), appliedDecoration.isUnstress(), appliedDecoration.getFermataType(),
                appliedDecoration.isStrongAccent(), appliedDecoration.isBreathMark(), appliedDecoration.isCaesura(),
                appliedDecoration.getPhraseMark(), appliedDecoration.isUpBow(), appliedDecoration.isDownBow(),
                appliedDecoration.isDoubleTongue(), appliedDecoration.isTripleTongue(), appliedDecoration.isHeel(),
                appliedDecoration.isToe(),
                new ArrayList<String>(appliedDecoration.getFingerings()),
                new ArrayList<String>(appliedDecoration.getStrings()),
                new ArrayList<String>(appliedDecoration.getPlucks()), appliedDecoration.isOpenString(),
                appliedDecoration.isSnapPizzicato(), appliedDecoration.isHarmonic(), appliedDecoration.isStopped(),
                appliedDecoration.isThumbPosition());
    }

    private static boolean applyBasicAbcBodyDecoration(String rawDecoration, String decoration,
            AbcPendingBodyDecorationState state) {
        String normalized = trimToEmpty(decoration).toLowerCase();
        if ("trill".equals(normalized) || "tr".equals(normalized) || "triller".equals(normalized)) {
            state.setTrill(true);
            return true;
        }
        if ("staccato".equals(normalized) || "stacc".equals(normalized) || "stac".equals(normalized)) {
            state.setStaccato(true);
            return true;
        }
        if ("accent".equals(normalized) || ">".equals(normalized) || "emphasis".equals(normalized)) {
            state.setAccent(true);
            return true;
        }
        if ("fermata".equals(normalized)) {
            state.setFermataType("normal");
            return true;
        }
        if ("trill(".equals(normalized)) {
            state.setTrill(true);
            state.setTrillLineStart(true);
            return true;
        }
        if ("trill)".equals(normalized)) {
            state.setTrillLineStop(true);
            return true;
        }
        if ("turn".equals(normalized)) {
            state.setTurnType("turn");
            return true;
        }
        if ("turnx".equals(normalized)) {
            state.setTurnType("turn");
            state.setTurnSlash(true);
            return true;
        }
        if ("invertedturn".equals(normalized) || "inverted-turn".equals(normalized)
                || "lowerturn".equals(normalized)) {
            state.setTurnType("inverted-turn");
            return true;
        }
        if ("invertedturnx".equals(normalized) || "inverted-turnx".equals(normalized)) {
            state.setTurnType("inverted-turn");
            state.setTurnSlash(true);
            return true;
        }
        if ("delayedturn".equals(normalized) || "delayed-turn".equals(normalized)) {
            if (state.getTurnType().length() == 0) {
                state.setTurnType("turn");
            }
            state.setDelayedTurn(true);
            return true;
        }
        if ("delayedinvertedturn".equals(normalized) || "delayed-inverted-turn".equals(normalized)) {
            state.setTurnType("inverted-turn");
            state.setDelayedTurn(true);
            return true;
        }
        if (applyAbcTremoloDecoration(normalized, state)) {
            return true;
        }
        if ("gliss-start".equals(normalized) || "glissando-start".equals(normalized)) {
            state.setGlissandoStart(true);
            return true;
        }
        if ("gliss-stop".equals(normalized) || "glissando-stop".equals(normalized)) {
            state.setGlissandoStop(true);
            return true;
        }
        if ("slide".equals(normalized) || "slide-start".equals(normalized)) {
            state.setSlideStart(true);
            return true;
        }
        if ("slide-stop".equals(normalized)) {
            state.setSlideStop(true);
            return true;
        }
        if ("schleifer".equals(normalized)) {
            state.setSchleifer(true);
            return true;
        }
        if ("shake".equals(normalized)) {
            state.setShake(true);
            return true;
        }
        if ("staccatissimo".equals(normalized) || "wedge".equals(normalized) || "spiccato".equals(normalized)) {
            state.setStaccatissimo(true);
            return true;
        }
        if ("tenuto".equals(normalized)) {
            state.setTenuto(true);
            return true;
        }
        if ("stress".equals(normalized)) {
            state.setStress(true);
            return true;
        }
        if ("unstress".equals(normalized)) {
            state.setUnstress(true);
            return true;
        }
        if ("marcato".equals(normalized) || "strongaccent".equals(normalized)
                || "strong-accent".equals(normalized) || "strong accent".equals(normalized)) {
            state.setStrongAccent(true);
            return true;
        }
        if ("breath".equals(normalized) || "breath-mark".equals(normalized) || "breathmark".equals(normalized)
                || "breath mark".equals(normalized)) {
            state.setBreathMark(true);
            return true;
        }
        if ("caesura".equals(normalized)) {
            state.setCaesura(true);
            return true;
        }
        if ("shortphrase".equals(normalized) || "mediumphrase".equals(normalized)
                || "longphrase".equals(normalized)) {
            state.setPhraseMark(normalized);
            return true;
        }
        if ("fine".equals(normalized)) {
            state.setFine(true);
            return true;
        }
        if ("sfz".equals(normalized)) {
            state.setSfz(true);
            return true;
        }
        if ("dacoda".equals(normalized)) {
            state.setDaCapo(true);
            state.setToCoda(true);
            return true;
        }
        if ("dacapo".equals(normalized) || "da-capo".equals(normalized) || "da capo".equals(normalized)
                || "d.c.".equals(normalized)) {
            state.setDaCapo(true);
            return true;
        }
        if ("dalsegno".equals(normalized) || "dal-segno".equals(normalized) || "dal segno".equals(normalized)
                || "d.s.".equals(normalized)) {
            state.setDalSegno(true);
            return true;
        }
        if ("tocoda".equals(normalized) || "to-coda".equals(normalized) || "to coda".equals(normalized)) {
            state.setToCoda(true);
            return true;
        }
        if ("crescendo(".equals(normalized) || "cresc(".equals(normalized) || "<(".equals(normalized)) {
            state.setCrescendoStart(true);
            return true;
        }
        if ("crescendo)".equals(normalized) || "cresc)".equals(normalized) || "<)".equals(normalized)) {
            state.setCrescendoStop(true);
            return true;
        }
        if ("diminuendo(".equals(normalized) || "decrescendo(".equals(normalized) || "dim(".equals(normalized)
                || "decresc(".equals(normalized) || ">(".equals(normalized)) {
            state.setDiminuendoStart(true);
            return true;
        }
        if ("diminuendo)".equals(normalized) || "decrescendo)".equals(normalized) || "dim)".equals(normalized)
                || "decresc)".equals(normalized) || ">)".equals(normalized)) {
            state.setDiminuendoStop(true);
            return true;
        }
        if (isAbcDynamicDecoration(normalized)) {
            state.setDynamicMark(normalized);
            return true;
        }
        if ("editorial".equals(normalized)) {
            state.setEditorialAccidental(true);
            return true;
        }
        if ("courtesy".equals(normalized)) {
            state.setCourtesyAccidental(true);
            return true;
        }
        if (normalized.startsWith("rehearsal:")) {
            String text = trimToEmpty(rawDecoration).substring(Math.min(trimToEmpty(rawDecoration).length(),
                    "rehearsal:".length())).trim();
            if (text.length() > 0) {
                state.setRehearsalMark(text);
            }
            return true;
        }
        if (normalized.startsWith("fingering:")) {
            String text = trimToEmpty(rawDecoration).substring(Math.min(trimToEmpty(rawDecoration).length(),
                    "fingering:".length())).trim();
            if (text.length() > 0) {
                state.getFingerings().add(text);
            }
            return true;
        }
        if (normalized.startsWith("string:")) {
            String text = trimToEmpty(rawDecoration).substring(Math.min(trimToEmpty(rawDecoration).length(),
                    "string:".length())).trim();
            if (text.length() > 0) {
                state.getStrings().add(text);
            }
            return true;
        }
        if (normalized.startsWith("pluck:")) {
            String text = trimToEmpty(rawDecoration).substring(Math.min(trimToEmpty(rawDecoration).length(),
                    "pluck:".length())).trim();
            if (text.length() > 0) {
                state.getPlucks().add(text);
            }
            return true;
        }
        if (normalized.matches("^[0-5]$")) {
            state.getFingerings().add(normalized);
            return true;
        }
        if ("mordent".equals(normalized) || "lowermordent".equals(normalized)) {
            state.setMordentType("mordent");
            return true;
        }
        if ("inverted-mordent".equals(normalized) || "invertedmordent".equals(normalized)
                || "uppermordent".equals(normalized) || "pralltriller".equals(normalized)
                || "pralltrill".equals(normalized) || "prall".equals(normalized)) {
            state.setMordentType("inverted-mordent");
            return true;
        }
        if ("roll".equals(normalized) || "arpeggio".equals(normalized) || "arpeggiate".equals(normalized)) {
            state.setArpeggiate(true);
            return true;
        }
        if ("segno".equals(normalized)) {
            state.setSegno(true);
            return true;
        }
        if ("coda".equals(normalized)) {
            state.setCoda(true);
            return true;
        }
        if ("upbow".equals(normalized) || "up-bow".equals(normalized) || "up bow".equals(normalized)) {
            state.setUpBow(true);
            return true;
        }
        if ("downbow".equals(normalized) || "down-bow".equals(normalized) || "down bow".equals(normalized)) {
            state.setDownBow(true);
            return true;
        }
        if ("doubletongue".equals(normalized) || "double-tongue".equals(normalized)
                || "double tongue".equals(normalized)) {
            state.setDoubleTongue(true);
            return true;
        }
        if ("tripletongue".equals(normalized) || "triple-tongue".equals(normalized)
                || "triple tongue".equals(normalized)) {
            state.setTripleTongue(true);
            return true;
        }
        if ("heel".equals(normalized) || "heel mark".equals(normalized)) {
            state.setHeel(true);
            return true;
        }
        if ("toe".equals(normalized) || "toe mark".equals(normalized)) {
            state.setToe(true);
            return true;
        }
        if ("open".equals(normalized) || "open-string".equals(normalized) || "openstring".equals(normalized)
                || "open string".equals(normalized)) {
            state.setOpenString(true);
            return true;
        }
        if ("snap".equals(normalized) || "snap-pizzicato".equals(normalized)
                || "snappizzicato".equals(normalized) || "snap pizzicato".equals(normalized)) {
            state.setSnapPizzicato(true);
            return true;
        }
        if ("harmonic".equals(normalized)) {
            state.setHarmonic(true);
            return true;
        }
        if ("stopped".equals(normalized) || "+".equals(normalized) || "plus".equals(normalized)
                || "stopped horn".equals(normalized) || "stopped-horn".equals(normalized)) {
            state.setStopped(true);
            return true;
        }
        if ("thumb".equals(normalized) || "thumbposition".equals(normalized)
                || "thumb-position".equals(normalized) || "thumbpos".equals(normalized)
                || "thumb pos".equals(normalized) || "thumb position".equals(normalized)) {
            state.setThumbPosition(true);
            return true;
        }
        if ("invertedfermata".equals(normalized) || "inverted-fermata".equals(normalized)
                || "inverted fermata".equals(normalized)) {
            state.setFermataType("inverted");
            return true;
        }
        return false;
    }

    private static boolean isAbcDynamicDecoration(String normalized) {
        String value = trimToEmpty(normalized);
        return "pppp".equals(value) || "ppp".equals(value) || "pp".equals(value) || "p".equals(value)
                || "mp".equals(value) || "mf".equals(value) || "f".equals(value) || "ff".equals(value)
                || "fff".equals(value) || "ffff".equals(value) || "fp".equals(value) || "fz".equals(value)
                || "rfz".equals(value) || "sf".equals(value) || "sfp".equals(value);
    }

    private static boolean applyAbcTremoloDecoration(String normalized, AbcPendingBodyDecorationState state) {
        Matcher matcher = Pattern.compile("^tremolo-(single|start|stop)-([1-9]\\d*)$").matcher(trimToEmpty(normalized));
        if (!matcher.find()) {
            return false;
        }
        state.setTremoloType(matcher.group(1));
        state.setTremoloMarks(Integer.valueOf(Math.max(1, Math.min(8, parseInt(matcher.group(2), 1)))));
        return true;
    }

    private static boolean applyBasicAbcSingleCharShorthand(String kind, AbcPendingBodyDecorationState state) {
        String normalized = trimToEmpty(kind);
        if ("trill".equals(normalized)) {
            state.setTrill(true);
            return true;
        }
        if ("staccato".equals(normalized)) {
            state.setStaccato(true);
            return true;
        }
        if ("accent".equals(normalized)) {
            state.setAccent(true);
            return true;
        }
        if ("fermata".equals(normalized)) {
            state.setFermataType("normal");
            return true;
        }
        if ("mordent".equals(normalized)) {
            state.setMordentType("mordent");
            return true;
        }
        if ("inverted-mordent".equals(normalized)) {
            state.setMordentType("inverted-mordent");
            return true;
        }
        if ("arpeggiate".equals(normalized)) {
            state.setArpeggiate(true);
            return true;
        }
        if ("segno".equals(normalized)) {
            state.setSegno(true);
            return true;
        }
        if ("coda".equals(normalized)) {
            state.setCoda(true);
            return true;
        }
        if ("upbow".equals(normalized)) {
            state.setUpBow(true);
            return true;
        }
        if ("downbow".equals(normalized)) {
            state.setDownBow(true);
            return true;
        }
        return false;
    }

    private static final class AbcTempoHeader {
        private final String unit;
        private final int bpm;

        AbcTempoHeader(String unit, int bpm) {
            this.unit = unit;
            this.bpm = bpm;
        }

        String getUnit() {
            return unit;
        }

        int getBpm() {
            return bpm;
        }
    }

    private static final class AbcTupletEvent {
        private final int actual;
        private final int normal;
        private final int remaining;

        AbcTupletEvent(int actual, int normal, int remaining) {
            this.actual = actual;
            this.normal = normal;
            this.remaining = remaining;
        }

        int getActual() {
            return actual;
        }

        int getNormal() {
            return normal;
        }

        int getRemaining() {
            return remaining;
        }
    }

    private static final class AbcPendingBodyDecorationState {
        private boolean trill;
        private boolean staccato;
        private boolean accent;
        private String fermataType = "";
        private String mordentType = "";
        private boolean trillLineStart;
        private boolean trillLineStop;
        private String turnType = "";
        private boolean turnSlash;
        private boolean delayedTurn;
        private String tremoloType = "";
        private Integer tremoloMarks;
        private boolean glissandoStart;
        private boolean glissandoStop;
        private boolean slideStart;
        private boolean slideStop;
        private boolean schleifer;
        private boolean shake;
        private boolean arpeggiate;
        private boolean staccatissimo;
        private boolean tenuto;
        private boolean stress;
        private boolean unstress;
        private boolean strongAccent;
        private boolean breathMark;
        private boolean caesura;
        private String phraseMark = "";
        private boolean segno;
        private boolean coda;
        private boolean upBow;
        private boolean downBow;
        private boolean doubleTongue;
        private boolean tripleTongue;
        private boolean heel;
        private boolean toe;
        private boolean editorialAccidental;
        private boolean courtesyAccidental;
        private boolean fine;
        private boolean daCapo;
        private boolean dalSegno;
        private boolean toCoda;
        private boolean crescendoStart;
        private boolean crescendoStop;
        private boolean diminuendoStart;
        private boolean diminuendoStop;
        private String dynamicMark = "";
        private boolean sfz;
        private String rehearsalMark = "";
        private List<String> fingerings = new ArrayList<String>();
        private List<String> strings = new ArrayList<String>();
        private List<String> plucks = new ArrayList<String>();
        private boolean openString;
        private boolean snapPizzicato;
        private boolean harmonic;
        private boolean stopped;
        private boolean thumbPosition;

        AbcPendingBodyDecorationState copyAndClear() {
            AbcPendingBodyDecorationState copy = new AbcPendingBodyDecorationState();
            copy.trill = trill;
            copy.staccato = staccato;
            copy.accent = accent;
            copy.fermataType = fermataType;
            copy.mordentType = mordentType;
            copy.trillLineStart = trillLineStart;
            copy.trillLineStop = trillLineStop;
            copy.turnType = turnType;
            copy.turnSlash = turnSlash;
            copy.delayedTurn = delayedTurn;
            copy.tremoloType = tremoloType;
            copy.tremoloMarks = tremoloMarks;
            copy.glissandoStart = glissandoStart;
            copy.glissandoStop = glissandoStop;
            copy.slideStart = slideStart;
            copy.slideStop = slideStop;
            copy.schleifer = schleifer;
            copy.shake = shake;
            copy.arpeggiate = arpeggiate;
            copy.staccatissimo = staccatissimo;
            copy.tenuto = tenuto;
            copy.stress = stress;
            copy.unstress = unstress;
            copy.strongAccent = strongAccent;
            copy.breathMark = breathMark;
            copy.caesura = caesura;
            copy.phraseMark = phraseMark;
            copy.segno = segno;
            copy.coda = coda;
            copy.upBow = upBow;
            copy.downBow = downBow;
            copy.doubleTongue = doubleTongue;
            copy.tripleTongue = tripleTongue;
            copy.heel = heel;
            copy.toe = toe;
            copy.editorialAccidental = editorialAccidental;
            copy.courtesyAccidental = courtesyAccidental;
            copy.fine = fine;
            copy.daCapo = daCapo;
            copy.dalSegno = dalSegno;
            copy.toCoda = toCoda;
            copy.crescendoStart = crescendoStart;
            copy.crescendoStop = crescendoStop;
            copy.diminuendoStart = diminuendoStart;
            copy.diminuendoStop = diminuendoStop;
            copy.dynamicMark = dynamicMark;
            copy.sfz = sfz;
            copy.rehearsalMark = rehearsalMark;
            copy.fingerings = new ArrayList<String>(fingerings);
            copy.strings = new ArrayList<String>(strings);
            copy.plucks = new ArrayList<String>(plucks);
            copy.openString = openString;
            copy.snapPizzicato = snapPizzicato;
            copy.harmonic = harmonic;
            copy.stopped = stopped;
            copy.thumbPosition = thumbPosition;
            trill = false;
            staccato = false;
            accent = false;
            fermataType = "";
            mordentType = "";
            trillLineStart = false;
            trillLineStop = false;
            turnType = "";
            turnSlash = false;
            delayedTurn = false;
            tremoloType = "";
            tremoloMarks = null;
            glissandoStart = false;
            glissandoStop = false;
            slideStart = false;
            slideStop = false;
            schleifer = false;
            shake = false;
            arpeggiate = false;
            staccatissimo = false;
            tenuto = false;
            stress = false;
            unstress = false;
            strongAccent = false;
            breathMark = false;
            caesura = false;
            phraseMark = "";
            segno = false;
            coda = false;
            upBow = false;
            downBow = false;
            doubleTongue = false;
            tripleTongue = false;
            heel = false;
            toe = false;
            editorialAccidental = false;
            courtesyAccidental = false;
            fine = false;
            daCapo = false;
            dalSegno = false;
            toCoda = false;
            crescendoStart = false;
            crescendoStop = false;
            diminuendoStart = false;
            diminuendoStop = false;
            dynamicMark = "";
            sfz = false;
            rehearsalMark = "";
            fingerings.clear();
            strings.clear();
            plucks.clear();
            openString = false;
            snapPizzicato = false;
            harmonic = false;
            stopped = false;
            thumbPosition = false;
            return copy;
        }

        boolean isTrill() {
            return trill;
        }

        void setTrill(boolean trill) {
            this.trill = trill;
        }

        boolean isStaccato() {
            return staccato;
        }

        void setStaccato(boolean staccato) {
            this.staccato = staccato;
        }

        boolean isAccent() {
            return accent;
        }

        void setAccent(boolean accent) {
            this.accent = accent;
        }

        String getFermataType() {
            return fermataType;
        }

        void setFermataType(String fermataType) {
            this.fermataType = trimToEmpty(fermataType);
        }

        String getMordentType() {
            return mordentType;
        }

        void setMordentType(String mordentType) {
            this.mordentType = trimToEmpty(mordentType);
        }

        boolean isTrillLineStart() {
            return trillLineStart;
        }

        void setTrillLineStart(boolean trillLineStart) {
            this.trillLineStart = trillLineStart;
        }

        boolean isTrillLineStop() {
            return trillLineStop;
        }

        void setTrillLineStop(boolean trillLineStop) {
            this.trillLineStop = trillLineStop;
        }

        String getTurnType() {
            return turnType;
        }

        void setTurnType(String turnType) {
            this.turnType = trimToEmpty(turnType);
        }

        boolean isTurnSlash() {
            return turnSlash;
        }

        void setTurnSlash(boolean turnSlash) {
            this.turnSlash = turnSlash;
        }

        boolean isDelayedTurn() {
            return delayedTurn;
        }

        void setDelayedTurn(boolean delayedTurn) {
            this.delayedTurn = delayedTurn;
        }

        String getTremoloType() {
            return tremoloType;
        }

        void setTremoloType(String tremoloType) {
            this.tremoloType = trimToEmpty(tremoloType);
        }

        Integer getTremoloMarks() {
            return tremoloMarks;
        }

        void setTremoloMarks(Integer tremoloMarks) {
            this.tremoloMarks = tremoloMarks;
        }

        boolean isGlissandoStart() {
            return glissandoStart;
        }

        void setGlissandoStart(boolean glissandoStart) {
            this.glissandoStart = glissandoStart;
        }

        boolean isGlissandoStop() {
            return glissandoStop;
        }

        void setGlissandoStop(boolean glissandoStop) {
            this.glissandoStop = glissandoStop;
        }

        boolean isSlideStart() {
            return slideStart;
        }

        void setSlideStart(boolean slideStart) {
            this.slideStart = slideStart;
        }

        boolean isSlideStop() {
            return slideStop;
        }

        void setSlideStop(boolean slideStop) {
            this.slideStop = slideStop;
        }

        boolean isSchleifer() {
            return schleifer;
        }

        void setSchleifer(boolean schleifer) {
            this.schleifer = schleifer;
        }

        boolean isShake() {
            return shake;
        }

        void setShake(boolean shake) {
            this.shake = shake;
        }

        boolean isArpeggiate() {
            return arpeggiate;
        }

        void setArpeggiate(boolean arpeggiate) {
            this.arpeggiate = arpeggiate;
        }

        boolean isStaccatissimo() {
            return staccatissimo;
        }

        void setStaccatissimo(boolean staccatissimo) {
            this.staccatissimo = staccatissimo;
        }

        boolean isTenuto() {
            return tenuto;
        }

        void setTenuto(boolean tenuto) {
            this.tenuto = tenuto;
        }

        boolean isStress() {
            return stress;
        }

        void setStress(boolean stress) {
            this.stress = stress;
        }

        boolean isUnstress() {
            return unstress;
        }

        void setUnstress(boolean unstress) {
            this.unstress = unstress;
        }

        boolean isStrongAccent() {
            return strongAccent;
        }

        void setStrongAccent(boolean strongAccent) {
            this.strongAccent = strongAccent;
        }

        boolean isBreathMark() {
            return breathMark;
        }

        void setBreathMark(boolean breathMark) {
            this.breathMark = breathMark;
        }

        boolean isCaesura() {
            return caesura;
        }

        void setCaesura(boolean caesura) {
            this.caesura = caesura;
        }

        String getPhraseMark() {
            return phraseMark;
        }

        void setPhraseMark(String phraseMark) {
            this.phraseMark = trimToEmpty(phraseMark);
        }

        boolean isSegno() {
            return segno;
        }

        void setSegno(boolean segno) {
            this.segno = segno;
        }

        boolean isCoda() {
            return coda;
        }

        void setCoda(boolean coda) {
            this.coda = coda;
        }

        boolean isUpBow() {
            return upBow;
        }

        void setUpBow(boolean upBow) {
            this.upBow = upBow;
        }

        boolean isDownBow() {
            return downBow;
        }

        void setDownBow(boolean downBow) {
            this.downBow = downBow;
        }

        boolean isDoubleTongue() {
            return doubleTongue;
        }

        void setDoubleTongue(boolean doubleTongue) {
            this.doubleTongue = doubleTongue;
        }

        boolean isTripleTongue() {
            return tripleTongue;
        }

        void setTripleTongue(boolean tripleTongue) {
            this.tripleTongue = tripleTongue;
        }

        boolean isHeel() {
            return heel;
        }

        void setHeel(boolean heel) {
            this.heel = heel;
        }

        boolean isToe() {
            return toe;
        }

        void setToe(boolean toe) {
            this.toe = toe;
        }

        boolean isEditorialAccidental() {
            return editorialAccidental;
        }

        void setEditorialAccidental(boolean editorialAccidental) {
            this.editorialAccidental = editorialAccidental;
        }

        boolean isCourtesyAccidental() {
            return courtesyAccidental;
        }

        void setCourtesyAccidental(boolean courtesyAccidental) {
            this.courtesyAccidental = courtesyAccidental;
        }

        String getRehearsalMark() {
            return rehearsalMark;
        }

        void setRehearsalMark(String rehearsalMark) {
            this.rehearsalMark = trimToEmpty(rehearsalMark);
        }

        List<String> getFingerings() {
            return fingerings;
        }

        List<String> getStrings() {
            return strings;
        }

        List<String> getPlucks() {
            return plucks;
        }

        boolean isOpenString() {
            return openString;
        }

        void setOpenString(boolean openString) {
            this.openString = openString;
        }

        boolean isSnapPizzicato() {
            return snapPizzicato;
        }

        void setSnapPizzicato(boolean snapPizzicato) {
            this.snapPizzicato = snapPizzicato;
        }

        boolean isHarmonic() {
            return harmonic;
        }

        void setHarmonic(boolean harmonic) {
            this.harmonic = harmonic;
        }

        boolean isStopped() {
            return stopped;
        }

        void setStopped(boolean stopped) {
            this.stopped = stopped;
        }

        boolean isThumbPosition() {
            return thumbPosition;
        }

        void setThumbPosition(boolean thumbPosition) {
            this.thumbPosition = thumbPosition;
        }

        boolean isFine() {
            return fine;
        }

        void setFine(boolean fine) {
            this.fine = fine;
        }

        boolean isDaCapo() {
            return daCapo;
        }

        void setDaCapo(boolean daCapo) {
            this.daCapo = daCapo;
        }

        boolean isDalSegno() {
            return dalSegno;
        }

        void setDalSegno(boolean dalSegno) {
            this.dalSegno = dalSegno;
        }

        boolean isToCoda() {
            return toCoda;
        }

        void setToCoda(boolean toCoda) {
            this.toCoda = toCoda;
        }

        boolean isCrescendoStart() {
            return crescendoStart;
        }

        void setCrescendoStart(boolean crescendoStart) {
            this.crescendoStart = crescendoStart;
        }

        boolean isCrescendoStop() {
            return crescendoStop;
        }

        void setCrescendoStop(boolean crescendoStop) {
            this.crescendoStop = crescendoStop;
        }

        boolean isDiminuendoStart() {
            return diminuendoStart;
        }

        void setDiminuendoStart(boolean diminuendoStart) {
            this.diminuendoStart = diminuendoStart;
        }

        boolean isDiminuendoStop() {
            return diminuendoStop;
        }

        void setDiminuendoStop(boolean diminuendoStop) {
            this.diminuendoStop = diminuendoStop;
        }

        String getDynamicMark() {
            return dynamicMark;
        }

        void setDynamicMark(String dynamicMark) {
            this.dynamicMark = trimToEmpty(dynamicMark);
        }

        boolean isSfz() {
            return sfz;
        }

        void setSfz(boolean sfz) {
            this.sfz = sfz;
        }
    }

    private static List<AbcParsedPart> buildAbcParsedParts(AbcImportVoiceRegistry voiceRegistry,
            AbcVoiceStores voiceStores, Map<String, Integer> keyHintFifthsByKey,
            Map<String, AbcMeasureMeta> measureMetaByKey, Map<String, AbcTransposeMeta> transposeHintByVoiceId,
            String scoreDirective) {
        List<AbcParsedPart> simpleParts = buildSimpleAbcParsedParts(voiceRegistry, voiceStores, keyHintFifthsByKey,
                measureMetaByKey, transposeHintByVoiceId);
        List<List<String>> scoreGroups = parseAbcScoreLayoutGroups(scoreDirective, simpleParts);
        if (scoreGroups.size() == 0) {
            return simpleParts;
        }
        List<AbcParsedPart> result = new ArrayList<AbcParsedPart>();
        for (List<String> groupVoiceIds : scoreGroups) {
            if (groupVoiceIds == null || groupVoiceIds.size() == 0) {
                continue;
            }
            AbcParsedPart base = findAbcParsedPartByVoiceId(simpleParts, groupVoiceIds.get(0));
            if (base == null) {
                return simpleParts;
            }
            if (groupVoiceIds.size() == 1) {
                result.add(new AbcParsedPart("P" + (result.size() + 1), base.getPartName(), base.getVoiceId(),
                        base.getClef(), base.getTranspose(), base.getStaffVoices(), base.getMeasures(),
                        base.getKeyByMeasure(), base.getMeterByMeasure(), base.getTempoByMeasure(),
                        base.getMeasureMetaByIndex()));
                continue;
            }
            List<AbcParsedStaffVoice> staffVoices = new ArrayList<AbcParsedStaffVoice>();
            List<String> partNames = new ArrayList<String>();
            for (String voiceId : groupVoiceIds) {
                AbcParsedPart part = findAbcParsedPartByVoiceId(simpleParts, voiceId);
                if (part == null) {
                    return simpleParts;
                }
                int staff = staffVoices.size() + 1;
                staffVoices.add(new AbcParsedStaffVoice(voiceId, staff, part.getClef(), part.getMeasures()));
                String partName = trimToEmpty(part.getPartName()).length() == 0 ? "Voice " + voiceId
                        : part.getPartName();
                if (!partNames.contains(partName)) {
                    partNames.add(partName);
                }
            }
            result.add(new AbcParsedPart("P" + (result.size() + 1), joinStringsWithSeparator(partNames, " / "),
                    base.getVoiceId(), base.getClef(), base.getTranspose(), staffVoices, base.getMeasures(),
                    base.getKeyByMeasure(), base.getMeterByMeasure(), base.getTempoByMeasure(),
                    base.getMeasureMetaByIndex()));
        }
        return result.size() == 0 ? simpleParts : result;
    }

    private static List<List<String>> parseAbcScoreLayoutGroups(String scoreDirective,
            List<AbcParsedPart> simpleParts) {
        List<List<String>> result = new ArrayList<List<String>>();
        List<String> seen = new ArrayList<String>();
        String normalized = trimToEmpty(scoreDirective);
        if (normalized.length() > 0) {
            Matcher chunkMatcher = Pattern.compile("\\(([^)]*)\\)|([^\\s()]+)").matcher(normalized);
            while (chunkMatcher.find()) {
                String chunk = chunkMatcher.group(1) != null ? chunkMatcher.group(1) : chunkMatcher.group(2);
                appendAbcScoreLayoutGroup(result, seen, chunk == null ? new String[0] : chunk.split("\\s+"));
            }
        }

        if (simpleParts != null) {
            for (AbcParsedPart part : simpleParts) {
                if (part != null && !seen.contains(part.getVoiceId())) {
                    appendAbcScoreLayoutGroup(result, seen, new String[] { part.getVoiceId() });
                }
            }
        }
        return result;
    }

    private static void appendAbcScoreLayoutGroup(List<List<String>> groups, List<String> seen, String[] rawIds) {
        List<String> group = new ArrayList<String>();
        if (rawIds != null) {
            for (String token : rawIds) {
                String voiceId = trimToEmpty(token);
                if (voiceId.matches("^[A-Za-z0-9_.-]+$") && !seen.contains(voiceId)) {
                    seen.add(voiceId);
                    group.add(voiceId);
                }
            }
        }
        if (group.size() > 0) {
            groups.add(group);
        }
    }

    private static AbcParsedPart findAbcParsedPartByVoiceId(List<AbcParsedPart> parts, String voiceId) {
        String normalized = trimToEmpty(voiceId);
        if (parts == null || normalized.length() == 0) {
            return null;
        }
        for (AbcParsedPart part : parts) {
            if (part != null && normalized.equals(part.getVoiceId())) {
                return part;
            }
        }
        return null;
    }

    private static List<AbcParsedPart> buildSimpleAbcParsedParts(AbcImportVoiceRegistry voiceRegistry,
            AbcVoiceStores voiceStores, Map<String, Integer> keyHintFifthsByKey,
            Map<String, AbcMeasureMeta> measureMetaByKey, Map<String, AbcTransposeMeta> transposeHintByVoiceId) {
        List<String> voiceIds = new ArrayList<String>(voiceRegistry.getDeclaredVoiceIds());
        for (String voiceId : voiceStores.getMeasuresByVoice().keySet()) {
            if (!voiceIds.contains(voiceId)) {
                voiceIds.add(voiceId);
            }
        }
        List<AbcParsedPart> parts = new ArrayList<AbcParsedPart>();
        for (int index = 0; index < voiceIds.size(); index++) {
            String voiceId = voiceIds.get(index);
            String partId = "P" + (index + 1);
            String partName = firstNonEmpty(voiceRegistry.getVoiceNameById().get(voiceId), "", "Voice " + voiceId);
            String clef = trimToEmpty(voiceRegistry.getVoiceClefById().get(voiceId));
            AbcTransposeMeta transpose = voiceRegistry.getVoiceTransposeById().get(voiceId);
            if (transpose == null) {
                transpose = transposeHintByVoiceId.get(voiceId);
            }
            parts.add(new AbcParsedPart(partId, partName, voiceId, clef, transpose,
                    new ArrayList<AbcParsedStaffVoice>(), voiceStores.getMeasuresByVoice().get(voiceId),
                    mapKeyHintsForVoice(voiceId, keyHintFifthsByKey),
                    getNestedMapOrEmpty(voiceStores.getMeterByMeasureByVoice(), voiceId),
                    getNestedMapOrEmpty(voiceStores.getTempoByMeasureByVoice(), voiceId),
                    mapMeasureMetaHintsForVoice(voiceId, measureMetaByKey, voiceStores)));
        }
        return parts;
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
                        isTruthy(rightRepeatRaw) || "backward".equals(repeatRaw) || repeatTimesRaw > 1,
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

    private static Map<Integer, Integer> mapKeyHintsForVoice(String voiceId, Map<String, Integer> keyHintFifthsByKey) {
        Map<Integer, Integer> result = new LinkedHashMap<Integer, Integer>();
        if (keyHintFifthsByKey == null) {
            return result;
        }
        String prefix = trimToEmpty(voiceId) + "#";
        for (String key : keyHintFifthsByKey.keySet()) {
            if (!key.startsWith(prefix)) {
                continue;
            }
            Integer measureNo = parseIntegerOrNull(key.substring(prefix.length()));
            if (measureNo != null) {
                result.put(measureNo, keyHintFifthsByKey.get(key));
            }
        }
        return result;
    }

    private static Map<Integer, AbcMeasureMeta> mapMeasureMetaHintsForVoice(String voiceId,
            Map<String, AbcMeasureMeta> measureMetaByKey, AbcVoiceStores voiceStores) {
        Map<Integer, AbcMeasureMeta> result = new LinkedHashMap<Integer, AbcMeasureMeta>();
        Map<Integer, AbcMeasureMeta> notationMeta = voiceStores == null ? null
                : voiceStores.getNotationMeasureMetaByVoice().get(voiceId);
        if (notationMeta != null) {
            result.putAll(notationMeta);
        }
        if (measureMetaByKey == null) {
            return result;
        }
        String prefix = trimToEmpty(voiceId) + "#";
        for (String key : measureMetaByKey.keySet()) {
            if (!key.startsWith(prefix)) {
                continue;
            }
            Integer measureNo = parseIntegerOrNull(key.substring(prefix.length()));
            if (measureNo != null) {
                AbcMeasureMeta existing = result.get(measureNo);
                AbcMeasureMeta hinted = measureMetaByKey.get(key);
                if (existing == null || hinted == null) {
                    result.put(measureNo, hinted);
                } else {
                    result.put(measureNo, new AbcMeasureMeta(
                            firstNonEmpty(hinted.getNumber(), existing.getNumber(), String.valueOf(measureNo)),
                            hinted.isImplicit(),
                            existing.isRepeatStart() || hinted.isRepeatStart(),
                            existing.isRepeatEnd() || hinted.isRepeatEnd(),
                            hinted.getRepeatTimes() != null ? hinted.getRepeatTimes() : existing.getRepeatTimes(),
                            firstNonEmpty(existing.getEndingStart(), hinted.getEndingStart(), ""),
                            firstNonEmpty(existing.getEndingStop(), hinted.getEndingStop(), ""),
                            firstNonEmpty(hinted.getEndingStopType(), existing.getEndingStopType(), "")));
                }
            }
        }
        return result;
    }

    private static <T> Map<Integer, T> getNestedMapOrEmpty(Map<String, Map<Integer, T>> source, String voiceId) {
        Map<Integer, T> result = source == null ? null : source.get(voiceId);
        return result == null ? new LinkedHashMap<Integer, T>() : result;
    }

    private static Integer accidentalToAlter(String accidental) {
        String raw = trimToEmpty(accidental);
        if (raw.length() == 0) {
            return null;
        }
        if ("=".equals(raw)) {
            return Integer.valueOf(0);
        }
        if (raw.matches("^\\^+$")) {
            return Integer.valueOf(raw.length());
        }
        if (raw.matches("^_+$")) {
            return Integer.valueOf(-raw.length());
        }
        return null;
    }

    private static String typeFromFraction(Fraction fraction) {
        Fraction safe = fraction == null ? DEFAULT_RATIO : fraction;
        double value = ((double) safe.getNum()) / Math.max(1, safe.getDen());
        if (value >= 1.0) {
            return "whole";
        }
        if (value >= 0.5) {
            return "half";
        }
        if (value >= 0.25) {
            return "quarter";
        }
        if (value >= 0.125) {
            return "eighth";
        }
        if (value >= 0.0625) {
            return "16th";
        }
        return "32nd";
    }

    private static int durationInDivisions(Fraction wholeFraction, int divisionsPerQuarter) {
        Fraction safe = wholeFraction == null ? DEFAULT_RATIO : wholeFraction;
        return (int) Math.round((((double) safe.getNum()) / Math.max(1, safe.getDen())) * 4.0
                * Math.max(1, divisionsPerQuarter));
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
        return "F".equals(StaffClefPolicy.chooseSingleClefByKeys(keys)) ? "bass" : "treble";
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

    private static AbcHarmonyPitch xmlHarmonyRootFromChordToken(String token) {
        Matcher matcher = Pattern.compile("^([A-G])(#|b)?$").matcher(trimToEmpty(token));
        if (!matcher.find()) {
            return null;
        }
        String accidental = matcher.group(2) == null ? "" : matcher.group(2);
        int alter = "#".equals(accidental) ? 1 : ("b".equals(accidental) ? -1 : 0);
        return new AbcHarmonyPitch(matcher.group(1), alter);
    }

    private static Element directChild(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                return (Element) child;
            }
            child = child.getNextSibling();
        }
        return null;
    }

    private static List<Element> directChildren(Element parent, String tagName) {
        List<Element> children = new ArrayList<Element>();
        if (parent == null) {
            return children;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                children.add((Element) child);
            }
            child = child.getNextSibling();
        }
        return children;
    }

    private static List<Element> directChildren(Element parent) {
        List<Element> children = new ArrayList<Element>();
        if (parent == null) {
            return children;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element) {
                children.add((Element) child);
            }
            child = child.getNextSibling();
        }
        return children;
    }

    private static Element directChildWithAttribute(Element parent, String tagName, String attrName, String attrValue) {
        for (Element child : directChildren(parent, tagName)) {
            if (trimToEmpty(child.getAttribute(attrName)).equals(attrValue)) {
                return child;
            }
        }
        return null;
    }

    private static boolean hasMusicXmlTurnSlash(Element ornaments) {
        for (Element turn : directChildren(ornaments, "turn")) {
            if ("yes".equals(trimToEmpty(turn.getAttribute("slash")).toLowerCase())) {
                return true;
            }
        }
        for (Element turn : directChildren(ornaments, "inverted-turn")) {
            if ("yes".equals(trimToEmpty(turn.getAttribute("slash")).toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static String directChildText(Element parent, String tagName) {
        return elementText(directChild(parent, tagName));
    }

    private static List<String> directChildTexts(Element parent, String tagName) {
        List<String> values = new ArrayList<String>();
        for (Element child : directChildren(parent, tagName)) {
            String text = elementText(child);
            if (text.length() > 0) {
                values.add(text);
            }
        }
        return values;
    }

    private static String elementText(Element element) {
        return element == null ? "" : trimToEmpty(element.getTextContent());
    }

    private static Element firstDescendantByPath(Element root, String[] path) {
        if (root == null || path == null || path.length == 0) {
            return root;
        }
        return firstDescendantByPath(root, path, 0);
    }

    private static Element firstDescendantByPath(Element current, String[] path, int index) {
        if (current == null) {
            return null;
        }
        if (index >= path.length) {
            return current;
        }
        Node child = current.getFirstChild();
        while (child != null) {
            if (child instanceof Element && path[index].equals(((Element) child).getTagName())) {
                Element found = firstDescendantByPath((Element) child, path, index + 1);
                if (found != null) {
                    return found;
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    private static boolean isTruthy(String value) {
        String normalized = trimToEmpty(value).toLowerCase();
        return "1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized);
    }

    private static String encodeUriComponent(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8")
                    .replace("+", "%20")
                    .replace("%21", "!")
                    .replace("%27", "'")
                    .replace("%28", "(")
                    .replace("%29", ")")
                    .replace("%7E", "~");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static int compareMusicXmlLaneToken(String a, String b, boolean nullLast) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return nullLast ? 1 : -1;
        }
        if (b == null) {
            return nullLast ? -1 : 1;
        }
        Double aNumber = parseOptionalNumber(a);
        Double bNumber = parseOptionalNumber(b);
        if (aNumber != null && bNumber != null) {
            int numeric = Double.compare(aNumber.doubleValue(), bNumber.doubleValue());
            if (numeric != 0) {
                return numeric;
            }
        }
        return a.compareTo(b);
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

    private static final class AbcMeasureReflowDiagnostic {
        private final int sourceMeasure;
        private final int movedEvents;

        private AbcMeasureReflowDiagnostic(int sourceMeasure, int movedEvents) {
            this.sourceMeasure = Math.max(1, sourceMeasure);
            this.movedEvents = Math.max(1, movedEvents);
        }

        private int getSourceMeasure() {
            return sourceMeasure;
        }

        private int getMovedEvents() {
            return movedEvents;
        }
    }

    private static final class AbcNormalizedMeasures {
        private final List<List<AbcMeasureNote>> measures;
        private final List<AbcMeasureReflowDiagnostic> diagnostics;

        private AbcNormalizedMeasures(List<List<AbcMeasureNote>> measures,
                List<AbcMeasureReflowDiagnostic> diagnostics) {
            this.measures = measures == null ? new ArrayList<List<AbcMeasureNote>>() : measures;
            this.diagnostics = diagnostics == null ? new ArrayList<AbcMeasureReflowDiagnostic>() : diagnostics;
        }

        private List<List<AbcMeasureNote>> getMeasures() {
            return measures;
        }

        private List<AbcMeasureReflowDiagnostic> getDiagnostics() {
            return diagnostics;
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

    private static final class AbcHarmonyPitch {
        private final String step;
        private final int alter;

        private AbcHarmonyPitch(String step, int alter) {
            this.step = trimToEmpty(step);
            this.alter = alter;
        }

        private String getStep() {
            return step;
        }

        private int getAlter() {
            return alter;
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
        private boolean tieStop;
        private final boolean graceSlash;
        private final String beamMode;
        private String lyricText;
        private String lyricSyllabic;
        private boolean lyricExtend;
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
        private final boolean slurStart;
        private final boolean slurStop;
        private final boolean tupletStart;
        private final boolean tupletStop;
        private final boolean trill;
        private final boolean trillLineStart;
        private final boolean trillLineStop;
        private String trillAccidentalText;
        private final String turnType;
        private final boolean turnSlash;
        private final boolean delayedTurn;
        private final String mordentType;
        private final String tremoloType;
        private final Integer tremoloMarks;
        private final boolean glissandoStart;
        private final boolean glissandoStop;
        private final boolean slideStart;
        private final boolean slideStop;
        private final boolean schleifer;
        private final boolean shake;
        private final boolean arpeggiate;
        private final boolean staccato;
        private final boolean staccatissimo;
        private final boolean accent;
        private final boolean tenuto;
        private final boolean stress;
        private final boolean unstress;
        private final String fermataType;
        private final boolean strongAccent;
        private final boolean breathMark;
        private final boolean caesura;
        private final String phraseMark;
        private final boolean upBow;
        private final boolean downBow;
        private final boolean doubleTongue;
        private final boolean tripleTongue;
        private final boolean heel;
        private final boolean toe;
        private final List<String> fingerings;
        private final List<String> strings;
        private final List<String> plucks;
        private final List<String> chordSymbols;
        private final boolean openString;
        private final boolean snapPizzicato;
        private final boolean harmonic;
        private final boolean stopped;
        private final boolean thumbPosition;

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
            this(voice, duration, chord, grace, rest, step, octave, alter, type, staff, accidentalText,
                    accidentalEditorial, accidentalCautionary, tieStart, tieStop, graceSlash, beamMode, lyricText,
                    lyricSyllabic, lyricExtend, timeModificationActual, timeModificationNormal, annotations, segno,
                    coda, rehearsalMark, fine, daCapo, dalSegno, toCoda, crescendoStart, crescendoStop,
                    diminuendoStart, diminuendoStop, dynamicMark, sfz, false, false, false, false, false, false,
                    false, "", "", false, false, "", "", null, false, false, false, false, false, false, false,
                    false, false, false, false, false, false, "", false, false, false, "", false, false, false,
                    false, false, false, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(),
                    false, false, false, false, false);
        }

        public AbcMeasureNote(String voice, int duration, boolean chord, boolean grace, boolean rest, String step,
                Integer octave, Integer alter, String type, Integer staff, String accidentalText,
                boolean accidentalEditorial, boolean accidentalCautionary, boolean tieStart, boolean tieStop,
                boolean graceSlash, String beamMode, String lyricText, String lyricSyllabic, boolean lyricExtend,
                Integer timeModificationActual, Integer timeModificationNormal, List<String> annotations,
                boolean segno, boolean coda, String rehearsalMark, boolean fine, boolean daCapo, boolean dalSegno,
                boolean toCoda, boolean crescendoStart, boolean crescendoStop, boolean diminuendoStart,
                boolean diminuendoStop, String dynamicMark, boolean sfz, boolean slurStart, boolean slurStop,
                boolean tupletStart, boolean tupletStop, boolean trill, boolean trillLineStart,
                boolean trillLineStop, String trillAccidentalText, String turnType, boolean turnSlash,
                boolean delayedTurn, String mordentType, String tremoloType, Integer tremoloMarks,
                boolean glissandoStart, boolean glissandoStop, boolean slideStart, boolean slideStop,
                boolean schleifer, boolean shake, boolean arpeggiate, boolean staccato, boolean staccatissimo,
                boolean accent, boolean tenuto, boolean stress, boolean unstress, String fermataType,
                boolean strongAccent, boolean breathMark, boolean caesura, String phraseMark, boolean upBow,
                boolean downBow, boolean doubleTongue, boolean tripleTongue, boolean heel, boolean toe,
                List<String> fingerings, List<String> strings, List<String> plucks, boolean openString,
                boolean snapPizzicato, boolean harmonic, boolean stopped, boolean thumbPosition) {
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
            this.slurStart = slurStart;
            this.slurStop = slurStop;
            this.tupletStart = tupletStart;
            this.tupletStop = tupletStop;
            this.trill = trill;
            this.trillLineStart = trillLineStart;
            this.trillLineStop = trillLineStop;
            this.trillAccidentalText = trimToEmpty(trillAccidentalText);
            this.turnType = trimToEmpty(turnType);
            this.turnSlash = turnSlash;
            this.delayedTurn = delayedTurn;
            this.mordentType = trimToEmpty(mordentType);
            this.tremoloType = trimToEmpty(tremoloType);
            this.tremoloMarks = tremoloMarks;
            this.glissandoStart = glissandoStart;
            this.glissandoStop = glissandoStop;
            this.slideStart = slideStart;
            this.slideStop = slideStop;
            this.schleifer = schleifer;
            this.shake = shake;
            this.arpeggiate = arpeggiate;
            this.staccato = staccato;
            this.staccatissimo = staccatissimo;
            this.accent = accent;
            this.tenuto = tenuto;
            this.stress = stress;
            this.unstress = unstress;
            this.fermataType = trimToEmpty(fermataType);
            this.strongAccent = strongAccent;
            this.breathMark = breathMark;
            this.caesura = caesura;
            this.phraseMark = trimToEmpty(phraseMark);
            this.upBow = upBow;
            this.downBow = downBow;
            this.doubleTongue = doubleTongue;
            this.tripleTongue = tripleTongue;
            this.heel = heel;
            this.toe = toe;
            this.fingerings = fingerings == null ? new ArrayList<String>() : fingerings;
            this.strings = strings == null ? new ArrayList<String>() : strings;
            this.plucks = plucks == null ? new ArrayList<String>() : plucks;
            this.chordSymbols = new ArrayList<String>();
            this.openString = openString;
            this.snapPizzicato = snapPizzicato;
            this.harmonic = harmonic;
            this.stopped = stopped;
            this.thumbPosition = thumbPosition;
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

        public void setTieStop(boolean tieStop) {
            this.tieStop = tieStop;
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

        public void setLyricText(String lyricText) {
            this.lyricText = trimToEmpty(lyricText);
        }

        public void setLyricSyllabic(String lyricSyllabic) {
            this.lyricSyllabic = trimToEmpty(lyricSyllabic);
        }

        public void setLyricExtend(boolean lyricExtend) {
            this.lyricExtend = lyricExtend;
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

        public boolean isSlurStart() {
            return slurStart;
        }

        public boolean isSlurStop() {
            return slurStop;
        }

        public boolean isTupletStart() {
            return tupletStart;
        }

        public boolean isTupletStop() {
            return tupletStop;
        }

        public boolean isTrill() {
            return trill;
        }

        public boolean isTrillLineStart() {
            return trillLineStart;
        }

        public boolean isTrillLineStop() {
            return trillLineStop;
        }

        public String getTrillAccidentalText() {
            return trillAccidentalText;
        }

        public void setTrillAccidentalText(String trillAccidentalText) {
            this.trillAccidentalText = trimToEmpty(trillAccidentalText);
        }

        public String getTurnType() {
            return turnType;
        }

        public boolean isTurnSlash() {
            return turnSlash;
        }

        public boolean isDelayedTurn() {
            return delayedTurn;
        }

        public String getMordentType() {
            return mordentType;
        }

        public String getTremoloType() {
            return tremoloType;
        }

        public Integer getTremoloMarks() {
            return tremoloMarks;
        }

        public boolean isGlissandoStart() {
            return glissandoStart;
        }

        public boolean isGlissandoStop() {
            return glissandoStop;
        }

        public boolean isSlideStart() {
            return slideStart;
        }

        public boolean isSlideStop() {
            return slideStop;
        }

        public boolean isSchleifer() {
            return schleifer;
        }

        public boolean isShake() {
            return shake;
        }

        public boolean isArpeggiate() {
            return arpeggiate;
        }

        public boolean isStaccato() {
            return staccato;
        }

        public boolean isStaccatissimo() {
            return staccatissimo;
        }

        public boolean isAccent() {
            return accent;
        }

        public boolean isTenuto() {
            return tenuto;
        }

        public boolean isStress() {
            return stress;
        }

        public boolean isUnstress() {
            return unstress;
        }

        public String getFermataType() {
            return fermataType;
        }

        public boolean isStrongAccent() {
            return strongAccent;
        }

        public boolean isBreathMark() {
            return breathMark;
        }

        public boolean isCaesura() {
            return caesura;
        }

        public String getPhraseMark() {
            return phraseMark;
        }

        public boolean isUpBow() {
            return upBow;
        }

        public boolean isDownBow() {
            return downBow;
        }

        public boolean isDoubleTongue() {
            return doubleTongue;
        }

        public boolean isTripleTongue() {
            return tripleTongue;
        }

        public boolean isHeel() {
            return heel;
        }

        public boolean isToe() {
            return toe;
        }

        public List<String> getFingerings() {
            return fingerings;
        }

        public List<String> getStrings() {
            return strings;
        }

        public List<String> getPlucks() {
            return plucks;
        }

        public List<String> getChordSymbols() {
            return chordSymbols;
        }

        public boolean isOpenString() {
            return openString;
        }

        public boolean isSnapPizzicato() {
            return snapPizzicato;
        }

        public boolean isHarmonic() {
            return harmonic;
        }

        public boolean isStopped() {
            return stopped;
        }

        public boolean isThumbPosition() {
            return thumbPosition;
        }
    }

    public static final class AbcMeasureMeta {
        private final String number;
        private final boolean implicit;
        private final boolean repeatStart;
        private final boolean repeatEnd;
        private final Integer repeatTimes;
        private final String endingStart;
        private String endingStop;
        private String endingStopType;

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

        public void setEndingStop(String endingStop) {
            this.endingStop = trimToEmpty(endingStop);
        }

        public void setEndingStopType(String endingStopType) {
            this.endingStopType = trimToEmpty(endingStopType);
        }
    }

    public static final class AbcVoiceStores {
        private final Map<String, List<List<AbcMeasureNote>>> measuresByVoice;
        private final Map<String, Map<Integer, AbcMeasureMeta>> notationMeasureMetaByVoice;
        private final Map<String, String> activeEndingByVoice;
        private final Map<String, Integer> currentKeyFifthsByVoice;
        private final Map<String, Map<Integer, AbcMeter>> meterByMeasureByVoice;
        private final Map<String, Map<Integer, Integer>> tempoByMeasureByVoice;

        public AbcVoiceStores() {
            this.measuresByVoice = new LinkedHashMap<String, List<List<AbcMeasureNote>>>();
            this.notationMeasureMetaByVoice = new LinkedHashMap<String, Map<Integer, AbcMeasureMeta>>();
            this.activeEndingByVoice = new LinkedHashMap<String, String>();
            this.currentKeyFifthsByVoice = new LinkedHashMap<String, Integer>();
            this.meterByMeasureByVoice = new LinkedHashMap<String, Map<Integer, AbcMeter>>();
            this.tempoByMeasureByVoice = new LinkedHashMap<String, Map<Integer, Integer>>();
        }

        public Map<String, List<List<AbcMeasureNote>>> getMeasuresByVoice() {
            return measuresByVoice;
        }

        public Map<String, Map<Integer, AbcMeasureMeta>> getNotationMeasureMetaByVoice() {
            return notationMeasureMetaByVoice;
        }

        public Map<String, String> getActiveEndingByVoice() {
            return activeEndingByVoice;
        }

        public Map<String, Integer> getCurrentKeyFifthsByVoice() {
            return currentKeyFifthsByVoice;
        }

        public Map<String, Map<Integer, AbcMeter>> getMeterByMeasureByVoice() {
            return meterByMeasureByVoice;
        }

        public Map<String, Map<Integer, Integer>> getTempoByMeasureByVoice() {
            return tempoByMeasureByVoice;
        }
    }

    public static final class AbcLyricToken {
        private final String type;
        private final String text;
        private final String syllabic;

        public AbcLyricToken(String type, String text, String syllabic) {
            this.type = trimToEmpty(type);
            this.text = trimToEmpty(text);
            this.syllabic = trimToEmpty(syllabic);
        }

        public String getType() {
            return type;
        }

        public String getText() {
            return text;
        }

        public String getSyllabic() {
            return syllabic;
        }
    }

    public static final class AbcBodyFieldContext {
        private final int activeKeyFifths;
        private final Fraction activeUnitLength;
        private final AbcMeter activeMeter;
        private final Integer activeTempoBpm;
        private final Map<String, Integer> measureAccidentals;
        private final AbcVoiceStores voiceStores;
        private final String entryVoiceId;
        private final int currentMeasureNo;
        private final Map<String, Integer> keyHintFifthsByKey;
        private final List<String> warnings;

        public AbcBodyFieldContext() {
            this(0, DEFAULT_UNIT, new AbcMeter(4, 4), null, new LinkedHashMap<String, Integer>(),
                    createAbcVoiceStores(), "1", 1, new LinkedHashMap<String, Integer>(), new ArrayList<String>());
        }

        public AbcBodyFieldContext(int activeKeyFifths, Fraction activeUnitLength, AbcMeter activeMeter,
                Integer activeTempoBpm, Map<String, Integer> measureAccidentals, AbcVoiceStores voiceStores,
                String entryVoiceId, int currentMeasureNo, Map<String, Integer> keyHintFifthsByKey,
                List<String> warnings) {
            this.activeKeyFifths = activeKeyFifths;
            this.activeUnitLength = activeUnitLength;
            this.activeMeter = activeMeter;
            this.activeTempoBpm = activeTempoBpm;
            this.measureAccidentals = measureAccidentals;
            this.voiceStores = voiceStores;
            this.entryVoiceId = trimToEmpty(entryVoiceId).length() == 0 ? "1" : trimToEmpty(entryVoiceId);
            this.currentMeasureNo = currentMeasureNo;
            this.keyHintFifthsByKey = keyHintFifthsByKey;
            this.warnings = warnings;
        }

        public int getActiveKeyFifths() {
            return activeKeyFifths;
        }

        public Fraction getActiveUnitLength() {
            return activeUnitLength;
        }

        public AbcMeter getActiveMeter() {
            return activeMeter;
        }

        public Integer getActiveTempoBpm() {
            return activeTempoBpm;
        }

        public Map<String, Integer> getMeasureAccidentals() {
            return measureAccidentals;
        }

        public AbcVoiceStores getVoiceStores() {
            return voiceStores;
        }

        public String getEntryVoiceId() {
            return entryVoiceId;
        }

        public int getCurrentMeasureNo() {
            return currentMeasureNo;
        }

        public Map<String, Integer> getKeyHintFifthsByKey() {
            return keyHintFifthsByKey;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }

    public static final class AbcBodyFieldResult {
        private final boolean handled;
        private final int activeKeyFifths;
        private final Map<String, Integer> activeKeySignatureAccidentals;
        private final Fraction activeUnitLength;
        private final AbcMeter activeMeter;
        private final Integer activeTempoBpm;
        private final Map<String, Integer> measureAccidentals;

        public AbcBodyFieldResult(boolean handled, int activeKeyFifths,
                Map<String, Integer> activeKeySignatureAccidentals, Fraction activeUnitLength, AbcMeter activeMeter,
                Integer activeTempoBpm, Map<String, Integer> measureAccidentals) {
            this.handled = handled;
            this.activeKeyFifths = activeKeyFifths;
            this.activeKeySignatureAccidentals = activeKeySignatureAccidentals == null
                    ? new LinkedHashMap<String, Integer>()
                    : activeKeySignatureAccidentals;
            this.activeUnitLength = activeUnitLength;
            this.activeMeter = activeMeter;
            this.activeTempoBpm = activeTempoBpm;
            this.measureAccidentals = measureAccidentals == null ? new LinkedHashMap<String, Integer>()
                    : measureAccidentals;
        }

        public boolean isHandled() {
            return handled;
        }

        public int getActiveKeyFifths() {
            return activeKeyFifths;
        }

        public Map<String, Integer> getActiveKeySignatureAccidentals() {
            return activeKeySignatureAccidentals;
        }

        public Fraction getActiveUnitLength() {
            return activeUnitLength;
        }

        public AbcMeter getActiveMeter() {
            return activeMeter;
        }

        public Integer getActiveTempoBpm() {
            return activeTempoBpm;
        }

        public Map<String, Integer> getMeasureAccidentals() {
            return measureAccidentals;
        }
    }

    public static final class AbcBarlineEntryContext {
        private final String text;
        private int idx;
        private String activeEndingMarker;
        private int currentMeasureNo;
        private int currentMeasureLength;
        private int measuresLength;
        private final Map<String, Integer> measureAccidentals;
        private boolean repeatEndMarked;
        private boolean repeatStartMarked;
        private boolean activeEndingStopped;
        private int stoppedEndingMeasureNo;
        private boolean advancedToNextMeasure;
        private boolean measureAccidentalsCleared;
        private boolean lastNoteCleared;
        private boolean beamContextReset;
        private String startedEndingMarker;
        private int startedEndingNextIdx;

        public AbcBarlineEntryContext(String text, int idx, String activeEndingMarker, int currentMeasureNo,
                int currentMeasureLength, int measuresLength, Map<String, Integer> measureAccidentals) {
            this.text = text == null ? "" : text;
            this.idx = idx;
            this.activeEndingMarker = trimToEmpty(activeEndingMarker);
            this.currentMeasureNo = Math.max(1, currentMeasureNo);
            this.currentMeasureLength = currentMeasureLength;
            this.measuresLength = measuresLength;
            this.measureAccidentals = measureAccidentals == null ? new LinkedHashMap<String, Integer>()
                    : measureAccidentals;
            this.startedEndingMarker = "";
            this.startedEndingNextIdx = -1;
        }

        public void markRepeatEnd() {
            this.repeatEndMarked = true;
        }

        public void markRepeatStart() {
            this.repeatStartMarked = true;
        }

        public void stopActiveEndingAtMeasure(int measureNo) {
            this.activeEndingStopped = true;
            this.stoppedEndingMeasureNo = measureNo;
            this.activeEndingMarker = "";
        }

        public void advanceToNextMeasure() {
            this.advancedToNextMeasure = true;
            this.currentMeasureNo += 1;
            this.measuresLength += 1;
            this.currentMeasureLength = 0;
        }

        public void clearMeasureAccidentals() {
            this.measureAccidentals.clear();
            this.measureAccidentalsCleared = true;
        }

        public void clearLastNote() {
            this.lastNoteCleared = true;
        }

        public boolean startEndingAtCurrentMeasure(String marker, int nextIdx) {
            this.startedEndingMarker = trimToEmpty(marker);
            this.startedEndingNextIdx = nextIdx;
            this.activeEndingMarker = this.startedEndingMarker;
            this.idx = nextIdx;
            resetBeamContext();
            return true;
        }

        public void resetBeamContext() {
            this.beamContextReset = true;
        }

        public String getText() {
            return text;
        }

        public int getIdx() {
            return idx;
        }

        public void setIdx(int idx) {
            this.idx = idx;
        }

        public String getActiveEndingMarker() {
            return activeEndingMarker;
        }

        public int getCurrentMeasureNo() {
            return currentMeasureNo;
        }

        public int getCurrentMeasureLength() {
            return currentMeasureLength;
        }

        public int getMeasuresLength() {
            return measuresLength;
        }

        public Map<String, Integer> getMeasureAccidentals() {
            return measureAccidentals;
        }

        public boolean isRepeatEndMarked() {
            return repeatEndMarked;
        }

        public boolean isRepeatStartMarked() {
            return repeatStartMarked;
        }

        public boolean isActiveEndingStopped() {
            return activeEndingStopped;
        }

        public int getStoppedEndingMeasureNo() {
            return stoppedEndingMeasureNo;
        }

        public boolean isAdvancedToNextMeasure() {
            return advancedToNextMeasure;
        }

        public boolean isMeasureAccidentalsCleared() {
            return measureAccidentalsCleared;
        }

        public boolean isLastNoteCleared() {
            return lastNoteCleared;
        }

        public boolean isBeamContextReset() {
            return beamContextReset;
        }

        public String getStartedEndingMarker() {
            return startedEndingMarker;
        }

        public int getStartedEndingNextIdx() {
            return startedEndingNextIdx;
        }
    }

    public interface AbcBarlineTokenHandler {
        boolean handle(AbcParser.AbcParsedBarlineToken barlineToken);
    }

    public interface AbcBodyFieldApplier {
        boolean apply(String fieldName, String fieldValue);
    }

    public static final class AbcNonPlayableBodyEntryContext {
        private int idx;
        private final List<String> warnings;
        private final AbcBarlineTokenHandler barlineTokenHandler;
        private final AbcBodyFieldApplier bodyFieldApplier;

        public AbcNonPlayableBodyEntryContext(int idx, List<String> warnings,
                AbcBarlineTokenHandler barlineTokenHandler, AbcBodyFieldApplier bodyFieldApplier) {
            this.idx = idx;
            this.warnings = warnings == null ? new ArrayList<String>() : warnings;
            this.barlineTokenHandler = barlineTokenHandler;
            this.bodyFieldApplier = bodyFieldApplier;
        }

        public boolean handleBarlineToken(AbcParser.AbcParsedBarlineToken barlineToken) {
            return barlineTokenHandler != null && barlineTokenHandler.handle(barlineToken);
        }

        public boolean applyBodyField(String fieldName, String fieldValue) {
            return bodyFieldApplier != null && bodyFieldApplier.apply(fieldName, fieldValue);
        }

        public void warnBody(String warning) {
            warnings.add(warning);
        }

        public int getIdx() {
            return idx;
        }

        public void setIdx(int idx) {
            this.idx = idx;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }

    public interface AbcSimpleBodyTokenHandler {
        boolean handle(AbcParser.AbcParsedBodyToken bodyToken);
    }

    public interface AbcSimpleBodyTokenCharHandler {
        boolean handle(AbcParser.AbcParsedBodyToken bodyToken, String ch);
    }

    public static final class AbcSimpleBodyTokenHandlerContext {
        private final String ch;
        private final AbcSimpleBodyTokenHandler brokenRhythmBodyTokenHandler;
        private final AbcSimpleBodyTokenCharHandler decorationBodyTokenHandler;
        private final AbcSimpleBodyTokenHandler parenBodyTokenHandler;
        private final AbcSimpleBodyTokenHandler quotedStringBodyTokenHandler;
        private final AbcSimpleBodyTokenCharHandler singleCharShorthandBodyTokenHandler;
        private final AbcSimpleBodyTokenHandler slurStopBodyTokenHandler;
        private final AbcSimpleBodyTokenHandler tieBodyTokenHandler;

        public AbcSimpleBodyTokenHandlerContext(String ch, AbcSimpleBodyTokenHandler brokenRhythmBodyTokenHandler,
                AbcSimpleBodyTokenCharHandler decorationBodyTokenHandler,
                AbcSimpleBodyTokenHandler parenBodyTokenHandler,
                AbcSimpleBodyTokenHandler quotedStringBodyTokenHandler,
                AbcSimpleBodyTokenCharHandler singleCharShorthandBodyTokenHandler,
                AbcSimpleBodyTokenHandler slurStopBodyTokenHandler,
                AbcSimpleBodyTokenHandler tieBodyTokenHandler) {
            this.ch = trimToEmpty(ch);
            this.brokenRhythmBodyTokenHandler = brokenRhythmBodyTokenHandler;
            this.decorationBodyTokenHandler = decorationBodyTokenHandler;
            this.parenBodyTokenHandler = parenBodyTokenHandler;
            this.quotedStringBodyTokenHandler = quotedStringBodyTokenHandler;
            this.singleCharShorthandBodyTokenHandler = singleCharShorthandBodyTokenHandler;
            this.slurStopBodyTokenHandler = slurStopBodyTokenHandler;
            this.tieBodyTokenHandler = tieBodyTokenHandler;
        }

        public String getChar() {
            return ch;
        }

        public boolean handleBrokenRhythmBodyToken(AbcParser.AbcParsedBodyToken bodyToken) {
            return brokenRhythmBodyTokenHandler != null && brokenRhythmBodyTokenHandler.handle(bodyToken);
        }

        public boolean handleDecorationBodyToken(AbcParser.AbcParsedBodyToken bodyToken, String ch) {
            return decorationBodyTokenHandler != null && decorationBodyTokenHandler.handle(bodyToken, ch);
        }

        public boolean handleParenBodyToken(AbcParser.AbcParsedBodyToken bodyToken) {
            return parenBodyTokenHandler != null && parenBodyTokenHandler.handle(bodyToken);
        }

        public boolean handleQuotedStringBodyToken(AbcParser.AbcParsedBodyToken bodyToken) {
            return quotedStringBodyTokenHandler != null && quotedStringBodyTokenHandler.handle(bodyToken);
        }

        public boolean handleSingleCharShorthandBodyToken(AbcParser.AbcParsedBodyToken bodyToken, String ch) {
            return singleCharShorthandBodyTokenHandler != null
                    && singleCharShorthandBodyTokenHandler.handle(bodyToken, ch);
        }

        public boolean handleSlurStopBodyToken(AbcParser.AbcParsedBodyToken bodyToken) {
            return slurStopBodyTokenHandler != null && slurStopBodyTokenHandler.handle(bodyToken);
        }

        public boolean handleTieBodyToken(AbcParser.AbcParsedBodyToken bodyToken) {
            return tieBodyTokenHandler != null && tieBodyTokenHandler.handle(bodyToken);
        }
    }

    public interface AbcBracketTokenHandler {
        boolean handle(AbcParser.AbcParsedBracketToken bracketToken);
    }

    public interface AbcPlayableEventFallbackHandler {
        boolean handle(AbcParser.AbcParsedPlayableEvent playableEvent, boolean fallbackToNextChar);
    }

    public static final class AbcBracketBodyTokenContext {
        private final String text;
        private final int idx;
        private final AbcBracketTokenHandler inlineFieldBracketTokenHandler;
        private final AbcBracketTokenHandler repeatEndingBracketTokenHandler;
        private final AbcPlayableEventFallbackHandler playableEventHandler;

        public AbcBracketBodyTokenContext(String text, int idx,
                AbcBracketTokenHandler inlineFieldBracketTokenHandler,
                AbcBracketTokenHandler repeatEndingBracketTokenHandler,
                AbcPlayableEventFallbackHandler playableEventHandler) {
            this.text = text == null ? "" : text;
            this.idx = idx;
            this.inlineFieldBracketTokenHandler = inlineFieldBracketTokenHandler;
            this.repeatEndingBracketTokenHandler = repeatEndingBracketTokenHandler;
            this.playableEventHandler = playableEventHandler;
        }

        public String getText() {
            return text;
        }

        public int getIdx() {
            return idx;
        }

        public boolean handleInlineFieldBracketToken(AbcParser.AbcParsedBracketToken bracketToken) {
            return inlineFieldBracketTokenHandler != null && inlineFieldBracketTokenHandler.handle(bracketToken);
        }

        public boolean handleRepeatEndingBracketToken(AbcParser.AbcParsedBracketToken bracketToken) {
            return repeatEndingBracketTokenHandler != null && repeatEndingBracketTokenHandler.handle(bracketToken);
        }

        public boolean handlePlayableEvent(AbcParser.AbcParsedPlayableEvent playableEvent, boolean fallbackToNextChar) {
            return playableEventHandler != null && playableEventHandler.handle(playableEvent, fallbackToNextChar);
        }
    }

    public interface AbcGraceNotesAppender {
        void append(List<AbcParser.AbcParsedGraceNote> notes);
    }

    public static final class AbcGraceGroupContext {
        private final String text;
        private final int idx;
        private final String ch;
        private final int lineNo;
        private final List<String> warnings;
        private final AbcGraceNotesAppender graceNotesAppender;

        public AbcGraceGroupContext(String text, int idx, String ch, int lineNo, List<String> warnings,
                AbcGraceNotesAppender graceNotesAppender) {
            this.text = text == null ? "" : text;
            this.idx = idx;
            this.ch = trimToEmpty(ch);
            this.lineNo = lineNo;
            this.warnings = warnings == null ? new ArrayList<String>() : warnings;
            this.graceNotesAppender = graceNotesAppender;
        }

        public String getText() {
            return text;
        }

        public int getIdx() {
            return idx;
        }

        public String getChar() {
            return ch;
        }

        public int getLineNo() {
            return lineNo;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void warnBody(String warning) {
            warnings.add(warning);
        }

        public void appendGraceNotes(List<AbcParser.AbcParsedGraceNote> notes) {
            if (graceNotesAppender != null) {
                graceNotesAppender.append(notes);
            }
        }
    }

    public static final class AbcGraceGroupProcessResult {
        private final boolean handled;
        private final int nextIdx;

        public AbcGraceGroupProcessResult(boolean handled, int nextIdx) {
            this.handled = handled;
            this.nextIdx = nextIdx;
        }

        public boolean isHandled() {
            return handled;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public interface AbcCharHandler {
        boolean handle(String ch);
    }

    public interface AbcBodyParseErrorThrower {
        void throwError();
    }

    public static final class AbcBodyFallbackContext {
        private final String ch;
        private final AbcParser.AbcParsedBodyEntry bodyEntry;
        private final AbcCharHandler closingNotationHandler;
        private final AbcCharHandler unsupportedPunctuationHandler;
        private final AbcBodyParseErrorThrower bodyParseErrorThrower;

        public AbcBodyFallbackContext(String ch, AbcParser.AbcParsedBodyEntry bodyEntry,
                AbcCharHandler closingNotationHandler, AbcCharHandler unsupportedPunctuationHandler,
                AbcBodyParseErrorThrower bodyParseErrorThrower) {
            this.ch = trimToEmpty(ch);
            this.bodyEntry = bodyEntry;
            this.closingNotationHandler = closingNotationHandler;
            this.unsupportedPunctuationHandler = unsupportedPunctuationHandler;
            this.bodyParseErrorThrower = bodyParseErrorThrower;
        }

        public String getChar() {
            return ch;
        }

        public AbcParser.AbcParsedBodyEntry getBodyEntry() {
            return bodyEntry;
        }

        public boolean handleClosingNotation(String ch) {
            return closingNotationHandler != null && closingNotationHandler.handle(ch);
        }

        public boolean handleUnsupportedPunctuation(String ch) {
            return unsupportedPunctuationHandler != null && unsupportedPunctuationHandler.handle(ch);
        }

        public void throwBodyParseError() {
            if (bodyParseErrorThrower != null) {
                bodyParseErrorThrower.throwError();
                return;
            }
            throw new IllegalArgumentException("Failed to parse ABC body token.");
        }
    }

    public static final class AbcPendingPlayableNoteOptions {
        private final Boolean applySlurStart;
        private final Boolean applyTieStop;
        private final String trillHint;

        public AbcPendingPlayableNoteOptions() {
            this(null, null, "");
        }

        public AbcPendingPlayableNoteOptions(Boolean applySlurStart, Boolean applyTieStop, String trillHint) {
            this.applySlurStart = applySlurStart;
            this.applyTieStop = applyTieStop;
            this.trillHint = trimToEmpty(trillHint);
        }

        public Boolean getApplySlurStart() {
            return applySlurStart;
        }

        public Boolean getApplyTieStop() {
            return applyTieStop;
        }

        public String getTrillHint() {
            return trillHint;
        }
    }

    public interface AbcPendingOrnamentStateApplier {
        void apply(AbcMeasureNote note, boolean applySlurStart, String trillHint);
    }

    public interface AbcPendingNoteApplier {
        void apply(AbcMeasureNote note);
    }

    public interface AbcPendingTieChecker {
        boolean hasPending();
    }

    public interface AbcPendingClearer {
        void clear();
    }

    public interface AbcBodyWarner {
        void warn(String message);
    }

    public static final class AbcPendingPlayableNoteContext {
        private final AbcMeasureNote note;
        private final AbcPendingPlayableNoteOptions options;
        private final AbcPendingOrnamentStateApplier pendingOrnamentStateApplier;
        private final AbcPendingNoteApplier pendingArticulationStateApplier;
        private final AbcPendingNoteApplier pendingDirectionStateApplier;
        private final AbcPendingNoteApplier pendingTechnicalStateApplier;
        private final AbcPendingTieChecker pendingTieChecker;
        private final AbcPendingClearer pendingTieClearer;
        private final AbcBodyWarner bodyWarner;

        public AbcPendingPlayableNoteContext(AbcMeasureNote note, AbcPendingPlayableNoteOptions options,
                AbcPendingOrnamentStateApplier pendingOrnamentStateApplier,
                AbcPendingNoteApplier pendingArticulationStateApplier,
                AbcPendingNoteApplier pendingDirectionStateApplier,
                AbcPendingNoteApplier pendingTechnicalStateApplier, AbcPendingTieChecker pendingTieChecker,
                AbcPendingClearer pendingTieClearer, AbcBodyWarner bodyWarner) {
            this.note = note;
            this.options = options;
            this.pendingOrnamentStateApplier = pendingOrnamentStateApplier;
            this.pendingArticulationStateApplier = pendingArticulationStateApplier;
            this.pendingDirectionStateApplier = pendingDirectionStateApplier;
            this.pendingTechnicalStateApplier = pendingTechnicalStateApplier;
            this.pendingTieChecker = pendingTieChecker;
            this.pendingTieClearer = pendingTieClearer;
            this.bodyWarner = bodyWarner;
        }

        public AbcMeasureNote getNote() {
            return note;
        }

        public AbcPendingPlayableNoteOptions getOptions() {
            return options;
        }

        public void applyPendingOrnamentState(AbcMeasureNote note, boolean applySlurStart, String trillHint) {
            if (pendingOrnamentStateApplier != null) {
                pendingOrnamentStateApplier.apply(note, applySlurStart, trillHint);
            }
        }

        public void applyPendingArticulationState(AbcMeasureNote note) {
            if (pendingArticulationStateApplier != null) {
                pendingArticulationStateApplier.apply(note);
            }
        }

        public void applyPendingDirectionState(AbcMeasureNote note) {
            if (pendingDirectionStateApplier != null) {
                pendingDirectionStateApplier.apply(note);
            }
        }

        public void applyPendingTechnicalState(AbcMeasureNote note) {
            if (pendingTechnicalStateApplier != null) {
                pendingTechnicalStateApplier.apply(note);
            }
        }

        public boolean hasPendingTieToNext() {
            return pendingTieChecker != null && pendingTieChecker.hasPending();
        }

        public void clearPendingTieToNext() {
            if (pendingTieClearer != null) {
                pendingTieClearer.clear();
            }
        }

        public void warnBody(String message) {
            if (bodyWarner != null) {
                bodyWarner.warn(message);
            }
        }
    }

    public interface AbcPendingValueApplier {
        void apply();
    }

    public static final class AbcPendingNoteValueContext {
        private final AbcMeasureNote note;
        private final boolean pending;
        private final AbcPendingValueApplier applier;
        private final AbcPendingClearer clearer;

        public AbcPendingNoteValueContext(AbcMeasureNote note, boolean pending, AbcPendingValueApplier applier,
                AbcPendingClearer clearer) {
            this.note = note;
            this.pending = pending;
            this.applier = applier;
            this.clearer = clearer;
        }

        public AbcMeasureNote getNote() {
            return note;
        }

        public boolean isPending() {
            return pending;
        }

        public void apply() {
            if (applier != null) {
                applier.apply();
            }
        }

        public void clear() {
            if (clearer != null) {
                clearer.clear();
            }
        }
    }

    public interface AbcPendingValueEmptyChecker {
        boolean isEmpty(Object value);
    }

    public interface AbcPendingOptionalValueApplier {
        void apply(Object value);
    }

    public static final class AbcPendingNoteOptionalValueContext {
        private final AbcMeasureNote note;
        private final Object value;
        private final AbcPendingValueEmptyChecker emptyChecker;
        private final AbcPendingOptionalValueApplier applier;
        private final AbcPendingClearer clearer;

        public AbcPendingNoteOptionalValueContext(AbcMeasureNote note, Object value,
                AbcPendingValueEmptyChecker emptyChecker, AbcPendingOptionalValueApplier applier,
                AbcPendingClearer clearer) {
            this.note = note;
            this.value = value;
            this.emptyChecker = emptyChecker;
            this.applier = applier;
            this.clearer = clearer;
        }

        public AbcMeasureNote getNote() {
            return note;
        }

        public Object getValue() {
            return value;
        }

        public boolean isEmpty(Object value) {
            return emptyChecker != null ? emptyChecker.isEmpty(value) : value == null;
        }

        public void apply(Object value) {
            if (applier != null) {
                applier.apply(value);
            }
        }

        public void clear() {
            if (clearer != null) {
                clearer.clear();
            }
        }
    }

    public interface AbcPendingArrayValueApplier {
        void apply(List<?> values);
    }

    public static final class AbcPendingNoteArrayContext {
        private final AbcMeasureNote note;
        private final List<?> values;
        private final AbcPendingArrayValueApplier applier;
        private final AbcPendingClearer clearer;

        public AbcPendingNoteArrayContext(AbcMeasureNote note, List<?> values, AbcPendingArrayValueApplier applier,
                AbcPendingClearer clearer) {
            this.note = note;
            this.values = values == null ? new ArrayList<Object>() : values;
            this.applier = applier;
            this.clearer = clearer;
        }

        public AbcMeasureNote getNote() {
            return note;
        }

        public List<?> getValues() {
            return values;
        }

        public void apply(List<?> values) {
            if (applier != null) {
                applier.apply(values);
            }
        }

        public void clear() {
            if (clearer != null) {
                clearer.clear();
            }
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

    public static final class AbcMusicXmlLaneDef {
        private final String staff;
        private final String voice;
        private final String voiceId;
        private final String normalizedVoiceId;
        private final String laneName;
        private final String clef;

        public AbcMusicXmlLaneDef(String staff, String voice, String voiceId, String normalizedVoiceId, String laneName,
                String clef) {
            this.staff = staff == null ? null : trimToEmpty(staff);
            this.voice = voice == null ? null : trimToEmpty(voice);
            this.voiceId = trimToEmpty(voiceId);
            this.normalizedVoiceId = trimToEmpty(normalizedVoiceId);
            this.laneName = trimToEmpty(laneName);
            this.clef = trimToEmpty(clef);
        }

        public String getStaff() {
            return staff;
        }

        public String getVoice() {
            return voice;
        }

        public String getVoiceId() {
            return voiceId;
        }

        public String getNormalizedVoiceId() {
            return normalizedVoiceId;
        }

        public String getLaneName() {
            return laneName;
        }

        public String getClef() {
            return clef;
        }
    }

    public static final class AbcMusicXmlMeasureState {
        private final double divisions;
        private final int fifths;
        private final double beats;
        private final double beatType;
        private final boolean needsInlineKeyChange;
        private final Map<String, Integer> keyAlterByStep;
        private final Map<String, Integer> measureAccidentalByStepOctave;

        public AbcMusicXmlMeasureState(double divisions, int fifths, double beats, double beatType,
                boolean needsInlineKeyChange, Map<String, Integer> keyAlterByStep,
                Map<String, Integer> measureAccidentalByStepOctave) {
            this.divisions = divisions;
            this.fifths = fifths;
            this.beats = beats;
            this.beatType = beatType;
            this.needsInlineKeyChange = needsInlineKeyChange;
            this.keyAlterByStep = keyAlterByStep == null ? new LinkedHashMap<String, Integer>() : keyAlterByStep;
            this.measureAccidentalByStepOctave = measureAccidentalByStepOctave == null
                    ? new LinkedHashMap<String, Integer>()
                    : measureAccidentalByStepOctave;
        }

        public double getDivisions() {
            return divisions;
        }

        public int getFifths() {
            return fifths;
        }

        public double getBeats() {
            return beats;
        }

        public double getBeatType() {
            return beatType;
        }

        public boolean isNeedsInlineKeyChange() {
            return needsInlineKeyChange;
        }

        public Map<String, Integer> getKeyAlterByStep() {
            return keyAlterByStep;
        }

        public Map<String, Integer> getMeasureAccidentalByStepOctave() {
            return measureAccidentalByStepOctave;
        }
    }

    public static final class AbcMusicXmlDirectionTokens {
        private final List<String> words;
        private final List<String> decorations;
        private final String activeWedgeType;

        public AbcMusicXmlDirectionTokens(List<String> words, List<String> decorations, String activeWedgeType) {
            this.words = words == null ? new ArrayList<String>() : words;
            this.decorations = decorations == null ? new ArrayList<String>() : decorations;
            this.activeWedgeType = trimToEmpty(activeWedgeType);
        }

        public List<String> getWords() {
            return words;
        }

        public List<String> getDecorations() {
            return decorations;
        }

        public String getActiveWedgeType() {
            return activeWedgeType;
        }
    }

    public static final class AbcMusicXmlMeasureBarlineTokens {
        private final String leftPrefix;
        private final String rightSuffix;

        public AbcMusicXmlMeasureBarlineTokens(String leftPrefix, String rightSuffix) {
            this.leftPrefix = trimToEmpty(leftPrefix);
            this.rightSuffix = trimToEmpty(rightSuffix).length() == 0 ? "|" : trimToEmpty(rightSuffix);
        }

        public String getLeftPrefix() {
            return leftPrefix;
        }

        public String getRightSuffix() {
            return rightSuffix;
        }
    }

    public static final class AbcMusicXmlNoteTiming {
        private final boolean chord;
        private final boolean grace;
        private final int duration;
        private final boolean playable;

        public AbcMusicXmlNoteTiming(boolean chord, boolean grace, int duration, boolean playable) {
            this.chord = chord;
            this.grace = grace;
            this.duration = duration;
            this.playable = playable;
        }

        public boolean isChord() {
            return chord;
        }

        public boolean isGrace() {
            return grace;
        }

        public int getDuration() {
            return duration;
        }

        public boolean isPlayable() {
            return playable;
        }
    }

    public static final class AbcMusicXmlNoteOrnaments {
        private final boolean trill;
        private final boolean trillMark;
        private final boolean wavyLineStart;
        private final boolean wavyLineStop;
        private final String trillAccidentalText;
        private final String turnType;
        private final boolean turnSlash;
        private final boolean delayedTurn;
        private final String mordentType;
        private final String tremoloType;
        private final Integer tremoloMarks;
        private final boolean glissandoStart;
        private final boolean glissandoStop;
        private final boolean slideStart;
        private final boolean slideStop;
        private final boolean schleifer;
        private final boolean shake;
        private final boolean arpeggiate;

        public AbcMusicXmlNoteOrnaments(boolean trill, boolean trillMark, boolean wavyLineStart, boolean wavyLineStop,
                String trillAccidentalText, String turnType, boolean turnSlash, boolean delayedTurn,
                String mordentType, String tremoloType, Integer tremoloMarks, boolean glissandoStart,
                boolean glissandoStop, boolean slideStart, boolean slideStop, boolean schleifer, boolean shake,
                boolean arpeggiate) {
            this.trill = trill;
            this.trillMark = trillMark;
            this.wavyLineStart = wavyLineStart;
            this.wavyLineStop = wavyLineStop;
            this.trillAccidentalText = trimToEmpty(trillAccidentalText);
            this.turnType = trimToEmpty(turnType);
            this.turnSlash = turnSlash;
            this.delayedTurn = delayedTurn;
            this.mordentType = trimToEmpty(mordentType);
            this.tremoloType = trimToEmpty(tremoloType);
            this.tremoloMarks = tremoloMarks;
            this.glissandoStart = glissandoStart;
            this.glissandoStop = glissandoStop;
            this.slideStart = slideStart;
            this.slideStop = slideStop;
            this.schleifer = schleifer;
            this.shake = shake;
            this.arpeggiate = arpeggiate;
        }

        public boolean isTrill() {
            return trill;
        }

        public boolean hasTrillMark() {
            return trillMark;
        }

        public boolean isWavyLineStart() {
            return wavyLineStart;
        }

        public boolean isWavyLineStop() {
            return wavyLineStop;
        }

        public String getTrillAccidentalText() {
            return trillAccidentalText;
        }

        public String getTurnType() {
            return turnType;
        }

        public boolean isTurnSlash() {
            return turnSlash;
        }

        public boolean isDelayedTurn() {
            return delayedTurn;
        }

        public String getMordentType() {
            return mordentType;
        }

        public String getTremoloType() {
            return tremoloType;
        }

        public Integer getTremoloMarks() {
            return tremoloMarks;
        }

        public boolean isGlissandoStart() {
            return glissandoStart;
        }

        public boolean isGlissandoStop() {
            return glissandoStop;
        }

        public boolean isSlideStart() {
            return slideStart;
        }

        public boolean isSlideStop() {
            return slideStop;
        }

        public boolean isSchleifer() {
            return schleifer;
        }

        public boolean isShake() {
            return shake;
        }

        public boolean isArpeggiate() {
            return arpeggiate;
        }
    }

    public static final class AbcMusicXmlPitchToken {
        private final String token;
        private final Map<String, Integer> measureAccidentalByStepOctave;
        private final boolean accidentalEditorial;
        private final boolean accidentalCautionary;
        private final String stepOctaveKey;
        private final int targetAlter;

        public AbcMusicXmlPitchToken(String token, Map<String, Integer> measureAccidentalByStepOctave,
                boolean accidentalEditorial, boolean accidentalCautionary, String stepOctaveKey, int targetAlter) {
            this.token = trimToEmpty(token);
            this.measureAccidentalByStepOctave = measureAccidentalByStepOctave == null
                    ? new LinkedHashMap<String, Integer>()
                    : measureAccidentalByStepOctave;
            this.accidentalEditorial = accidentalEditorial;
            this.accidentalCautionary = accidentalCautionary;
            this.stepOctaveKey = trimToEmpty(stepOctaveKey);
            this.targetAlter = targetAlter;
        }

        public String getToken() {
            return token;
        }

        public Map<String, Integer> getMeasureAccidentalByStepOctave() {
            return measureAccidentalByStepOctave;
        }

        public boolean isAccidentalEditorial() {
            return accidentalEditorial;
        }

        public boolean isAccidentalCautionary() {
            return accidentalCautionary;
        }

        public String getStepOctaveKey() {
            return stepOctaveKey;
        }

        public int getTargetAlter() {
            return targetAlter;
        }
    }

    public static final class AbcMusicXmlNoteArticulations {
        private final boolean staccato;
        private final boolean staccatissimo;
        private final boolean accent;
        private final boolean tenuto;
        private final boolean stress;
        private final boolean unstress;
        private final boolean strongAccent;
        private final boolean breathMark;
        private final boolean caesura;
        private final String phraseMarkText;

        public AbcMusicXmlNoteArticulations(boolean staccato, boolean staccatissimo, boolean accent, boolean tenuto,
                boolean stress, boolean unstress, boolean strongAccent, boolean breathMark, boolean caesura,
                String phraseMarkText) {
            this.staccato = staccato;
            this.staccatissimo = staccatissimo;
            this.accent = accent;
            this.tenuto = tenuto;
            this.stress = stress;
            this.unstress = unstress;
            this.strongAccent = strongAccent;
            this.breathMark = breathMark;
            this.caesura = caesura;
            this.phraseMarkText = trimToEmpty(phraseMarkText);
        }

        public boolean isStaccato() {
            return staccato;
        }

        public boolean isStaccatissimo() {
            return staccatissimo;
        }

        public boolean isAccent() {
            return accent;
        }

        public boolean isTenuto() {
            return tenuto;
        }

        public boolean isStress() {
            return stress;
        }

        public boolean isUnstress() {
            return unstress;
        }

        public boolean isStrongAccent() {
            return strongAccent;
        }

        public boolean isBreathMark() {
            return breathMark;
        }

        public boolean isCaesura() {
            return caesura;
        }

        public String getPhraseMarkText() {
            return phraseMarkText;
        }
    }

    public static final class AbcMusicXmlNoteTechnical {
        private final boolean upBow;
        private final boolean downBow;
        private final boolean doubleTongue;
        private final boolean tripleTongue;
        private final boolean heel;
        private final boolean toe;
        private final List<String> fingerings;
        private final List<String> strings;
        private final List<String> plucks;
        private final boolean openString;
        private final boolean snapPizzicato;
        private final boolean harmonic;
        private final boolean stopped;
        private final boolean thumbPosition;

        public AbcMusicXmlNoteTechnical(boolean upBow, boolean downBow, boolean doubleTongue, boolean tripleTongue,
                boolean heel, boolean toe, List<String> fingerings, List<String> strings, List<String> plucks,
                boolean openString, boolean snapPizzicato, boolean harmonic, boolean stopped, boolean thumbPosition) {
            this.upBow = upBow;
            this.downBow = downBow;
            this.doubleTongue = doubleTongue;
            this.tripleTongue = tripleTongue;
            this.heel = heel;
            this.toe = toe;
            this.fingerings = fingerings == null ? new ArrayList<String>() : fingerings;
            this.strings = strings == null ? new ArrayList<String>() : strings;
            this.plucks = plucks == null ? new ArrayList<String>() : plucks;
            this.openString = openString;
            this.snapPizzicato = snapPizzicato;
            this.harmonic = harmonic;
            this.stopped = stopped;
            this.thumbPosition = thumbPosition;
        }

        public boolean isUpBow() {
            return upBow;
        }

        public boolean isDownBow() {
            return downBow;
        }

        public boolean isDoubleTongue() {
            return doubleTongue;
        }

        public boolean isTripleTongue() {
            return tripleTongue;
        }

        public boolean isHeel() {
            return heel;
        }

        public boolean isToe() {
            return toe;
        }

        public List<String> getFingerings() {
            return fingerings;
        }

        public List<String> getStrings() {
            return strings;
        }

        public List<String> getPlucks() {
            return plucks;
        }

        public boolean isOpenString() {
            return openString;
        }

        public boolean isSnapPizzicato() {
            return snapPizzicato;
        }

        public boolean isHarmonic() {
            return harmonic;
        }

        public boolean isStopped() {
            return stopped;
        }

        public boolean isThumbPosition() {
            return thumbPosition;
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
