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

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isTruthy(String value) {
        String normalized = trimToEmpty(value).toLowerCase();
        return "1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized);
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

    public static final class AbcMeasureNote {
        private final String voice;
        private final int duration;
        private final boolean chord;
        private final boolean grace;

        public AbcMeasureNote(String voice, int duration, boolean chord, boolean grace) {
            this.voice = voice;
            this.duration = duration;
            this.chord = chord;
            this.grace = grace;
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
