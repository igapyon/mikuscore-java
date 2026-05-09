/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.abc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AbcParser {
    private static final Pattern REPEAT_ENDING_PATTERN = Pattern.compile("^(\\d+(?:[,-]\\d+)*)");
    private static final Pattern INLINE_FIELD_PATTERN = Pattern.compile("^\\[([A-Za-z]):([^\\]]*)\\]");
    private static final Pattern STANDALONE_BODY_FIELD_PATTERN = Pattern.compile("^([A-Za-z]):([^\\s\\]|]+)");
    private static final Pattern UNSUPPORTED_BODY_TOKEN_PATTERN = Pattern
            .compile("^([IJNQRWY][A-Za-z0-9_-]*|[h-jl-pr-twy][a-z][A-Za-z0-9_-]*)");
    private static final Pattern UNSUPPORTED_BODY_NUMBER_PATTERN = Pattern.compile("^(\\d+)");
    private static final Pattern TUPLET_PATTERN = Pattern.compile("^\\((\\d)(?::(\\d))?(?::(\\d))?");
    private static final AbcBarlineCandidate[] ABC_BARLINE_CANDIDATES = new AbcBarlineCandidate[] {
            new AbcBarlineCandidate(":|]", true, true, false, true),
            new AbcBarlineCandidate(":|:", true, true, true, false),
            new AbcBarlineCandidate("|:", true, false, true, false),
            new AbcBarlineCandidate(":|", true, true, false, false),
            new AbcBarlineCandidate("::", true, true, true, false),
            new AbcBarlineCandidate("[|", true, false, false, false),
            new AbcBarlineCandidate("]|", true, false, false, true),
            new AbcBarlineCandidate("|]", true, false, false, false),
            new AbcBarlineCandidate("||", true, false, false, false),
            new AbcBarlineCandidate("|", true, false, false, false),
            new AbcBarlineCandidate(":", false, false, false, false) };

    private AbcParser() {
    }

    public static AbcNoteParseResult parseAbcNoteAt(String text, int startIdx) {
        AbcLexer.AbcNoteLex note = AbcLexer.lexAbcNote(text, startIdx);
        if (note != null) {
            return AbcNoteParseResult.note(new AbcParsedNote(note.getAccidentalText(), note.getPitchChar(),
                    note.getOctaveShift(), note.getLengthToken(), note.getNextIdx()));
        }
        AbcLexer.AbcAccidentalLex accidental = AbcLexer.lexAbcAccidental(text, startIdx);
        if (accidental != null) {
            return AbcNoteParseResult.malformedAccidental(accidental.getAccidentalText(), accidental.getNextIdx());
        }
        return null;
    }

    public static AbcParsedChord parseAbcChordAt(String text, int startIdx) {
        String source = rawText(text);
        if (charAt(source, startIdx) != '[') {
            return null;
        }
        int closeIdx = source.indexOf(']', startIdx + 1);
        if (closeIdx < 0) {
            return null;
        }
        String inner = source.substring(startIdx + 1, closeIdx);
        List<AbcParsedPitchSource> notes = new ArrayList<AbcParsedPitchSource>();
        List<String> lengthTokens = new ArrayList<String>();
        int idx = 0;
        while (idx < inner.length()) {
            char ch = inner.charAt(idx);
            if (ch == ' ' || ch == '\t') {
                idx++;
                continue;
            }
            AbcNoteParseResult noteResult = parseAbcNoteAt(inner, idx);
            if (noteResult != null && noteResult.isNote()) {
                AbcParsedNote note = noteResult.getNote();
                notes.add(new AbcParsedPitchSource(note.getAccidentalText(), note.getPitchChar(),
                        note.getOctaveShift()));
                lengthTokens.add(note.getLengthToken());
                idx = note.getNextIdx();
                continue;
            }
            idx = noteResult != null && noteResult.isMalformedAccidental() ? noteResult.getNextIdx() : idx + 1;
        }
        if (notes.isEmpty()) {
            return null;
        }
        AbcLexer.AbcLengthTokenLex length = AbcLexer.lexAbcLengthToken(source, closeIdx + 1);
        String lengthToken = length != null ? length.getToken() : "";
        int nextIdx = length != null ? length.getNextIdx() : closeIdx + 1;
        return new AbcParsedChord(notes, lengthTokens, lengthToken, nextIdx);
    }

    public static AbcParsedPlayableEvent parseAbcPlayableEventAt(String text, int startIdx) {
        String source = rawText(text);
        if (charAt(source, startIdx) == '[') {
            AbcParsedChord chord = parseAbcChordAt(source, startIdx);
            if (chord == null) {
                return AbcParsedPlayableEvent.invalidChord(startIdx + 1);
            }
            String rawLengthToken = chord.getLengthToken();
            if (rawLengthToken.length() == 0 && !chord.getNoteLengthTokens().isEmpty()) {
                rawLengthToken = chord.getNoteLengthTokens().get(0);
            }
            return AbcParsedPlayableEvent.playable("chord", chord.getPitchSources(), rawLengthToken,
                    chord.getNextIdx());
        }

        AbcNoteParseResult noteResult = parseAbcNoteAt(source, startIdx);
        if (noteResult == null) {
            return null;
        }
        if (noteResult.isMalformedAccidental()) {
            return AbcParsedPlayableEvent.malformedAccidental(noteResult.getAccidentalText(), noteResult.getNextIdx());
        }
        AbcParsedNote note = noteResult.getNote();
        List<AbcParsedPitchSource> pitchSources = Collections.singletonList(new AbcParsedPitchSource(
                note.getAccidentalText(), note.getPitchChar(), note.getOctaveShift()));
        return AbcParsedPlayableEvent.playable("note", pitchSources, note.getLengthToken(), note.getNextIdx());
    }

    public static AbcParsedTuplet parseAbcTupletAt(String text, int startIdx) {
        if (charAt(rawText(text), startIdx) != '(') {
            return null;
        }
        Matcher match = matchFrom(text, startIdx, TUPLET_PATTERN);
        if (match == null) {
            return null;
        }
        int actual = parseInt(match.group(1), 0);
        int normalRaw = parseInt(match.group(2), Integer.MIN_VALUE);
        int countRaw = parseInt(match.group(3), Integer.MIN_VALUE);
        int normal = normalRaw > 0 ? normalRaw : (actual == 3 ? 2 : actual);
        int count = countRaw > 0 ? countRaw : actual;
        return new AbcParsedTuplet(actual, normal, count, startIdx + match.group(0).length(), match.group(0));
    }

    public static AbcParsedRepeatEndingMarker parseAbcRepeatEndingMarkerAt(String text, int startIdx) {
        if (charAt(rawText(text), startIdx) != '[') {
            return null;
        }
        Matcher match = matchFrom(text, startIdx + 1, REPEAT_ENDING_PATTERN);
        if (match == null) {
            return null;
        }
        return new AbcParsedRepeatEndingMarker(match.group(1), startIdx + 1 + match.group(0).length());
    }

    public static AbcParsedRepeatEndingMarker parseAbcBareRepeatEndingMarkerAt(String text, int startIdx) {
        Matcher match = matchFrom(text, startIdx, REPEAT_ENDING_PATTERN);
        if (match == null) {
            return null;
        }
        return new AbcParsedRepeatEndingMarker(match.group(1), startIdx + match.group(0).length());
    }

    public static AbcParsedInlineField parseAbcInlineFieldAt(String text, int startIdx) {
        Matcher match = matchFrom(text, startIdx, INLINE_FIELD_PATTERN);
        if (match == null) {
            return null;
        }
        return new AbcParsedInlineField(match.group(1).toUpperCase(Locale.ROOT), match.group(2).trim(),
                startIdx + match.group(0).length());
    }

    public static AbcParsedBarlineToken parseAbcBarlineTokenAt(String text, int startIdx) {
        String slice = sliceFrom(text, startIdx);
        for (AbcBarlineCandidate candidate : ABC_BARLINE_CANDIDATES) {
            if (slice.startsWith(candidate.token)) {
                return new AbcParsedBarlineToken(startIdx + candidate.token.length(), candidate.endsMeasure,
                        candidate.repeatEnd, candidate.repeatStart, candidate.endingStop);
            }
        }
        return null;
    }

    public static AbcParsedStandaloneBodyField parseAbcStandaloneBodyFieldAt(String text, int startIdx) {
        Matcher match = matchFrom(text, startIdx, STANDALONE_BODY_FIELD_PATTERN);
        if (match == null) {
            return null;
        }
        return new AbcParsedStandaloneBodyField(match.group(1).toUpperCase(Locale.ROOT), match.group(2).trim(),
                match.group(0), startIdx + match.group(0).length());
    }

    public static AbcParsedUnsupportedBodyToken parseAbcUnsupportedBodyTokenAt(String text, int startIdx) {
        Matcher match = matchFrom(text, startIdx, UNSUPPORTED_BODY_TOKEN_PATTERN);
        if (match == null) {
            return null;
        }
        return new AbcParsedUnsupportedBodyToken(match.group(1), startIdx + match.group(1).length());
    }

    public static AbcParsedUnsupportedBodyToken parseAbcUnsupportedBodyNumberAt(String text, int startIdx) {
        Matcher match = matchFrom(text, startIdx, UNSUPPORTED_BODY_NUMBER_PATTERN);
        if (match == null) {
            return null;
        }
        return new AbcParsedUnsupportedBodyToken(match.group(1), startIdx + match.group(1).length());
    }

    public static AbcParsedDelimitedSpan parseAbcDelimitedSpanAt(String text, int startIdx, char delimiter) {
        String source = rawText(text);
        if (delimiter == 0 || charAt(source, startIdx) != delimiter) {
            return null;
        }
        int endIdx = startIdx + 1;
        while (endIdx < source.length() && source.charAt(endIdx) != delimiter) {
            endIdx++;
        }
        int nextIdx = Math.min(source.length(), endIdx + 1);
        return new AbcParsedDelimitedSpan(String.valueOf(delimiter), source.substring(startIdx, nextIdx), nextIdx);
    }

    public static AbcParsedQuotedString parseAbcQuotedStringAt(String text, int startIdx) {
        AbcParsedDelimitedSpan span = parseAbcDelimitedSpanAt(text, startIdx, '"');
        if (span == null) {
            return null;
        }
        boolean terminated = span.getText().endsWith("\"") && span.getText().length() >= 2;
        String raw = terminated ? span.getText().substring(1, span.getText().length() - 1) : span.getText().substring(1);
        return new AbcParsedQuotedString(raw, raw.replaceFirst("^[\\^_<>@]", "").trim(), span.getNextIdx(),
                terminated);
    }

    public static AbcParsedDecoration parseAbcDecorationAt(String text, int startIdx) {
        char first = charAt(rawText(text), startIdx);
        if (first != '!' && first != '+') {
            return null;
        }
        AbcParsedDelimitedSpan span = parseAbcDelimitedSpanAt(text, startIdx, first);
        if (span == null) {
            return null;
        }
        boolean terminated = span.getText().endsWith(String.valueOf(first)) && span.getText().length() >= 2;
        String rawDecoration = terminated ? span.getText().substring(1, span.getText().length() - 1).trim()
                : span.getText().substring(1).trim();
        return new AbcParsedDecoration(rawDecoration, rawDecoration.toLowerCase(Locale.ROOT), String.valueOf(first),
                span.getNextIdx(), terminated);
    }

    public static AbcParsedBrokenRhythm parseAbcBrokenRhythmAt(String text, int startIdx) {
        char symbol = charAt(rawText(text), startIdx);
        if (symbol != '>' && symbol != '<') {
            return null;
        }
        if (symbol == '>') {
            return new AbcParsedBrokenRhythm(">", new AbcRatio(3, 2), new AbcRatio(1, 2), startIdx + 1);
        }
        return new AbcParsedBrokenRhythm("<", new AbcRatio(1, 2), new AbcRatio(3, 2), startIdx + 1);
    }

    public static AbcParsedSingleCharShorthand parseAbcSingleCharShorthandAt(String text, int startIdx) {
        char symbol = charAt(rawText(text), startIdx);
        String kind = singleCharShorthandKind(symbol);
        if (kind == null) {
            return null;
        }
        return new AbcParsedSingleCharShorthand(kind, startIdx + 1);
    }

    public static AbcParsedNextIndex parseAbcTieAt(String text, int startIdx) {
        return charAt(rawText(text), startIdx) == '-' ? new AbcParsedNextIndex(startIdx + 1) : null;
    }

    public static AbcParsedNextIndex parseAbcSlurStopAt(String text, int startIdx) {
        return charAt(rawText(text), startIdx) == ')' ? new AbcParsedNextIndex(startIdx + 1) : null;
    }

    public static AbcParsedParenToken parseAbcParenTokenAt(String text, int startIdx) {
        if (charAt(rawText(text), startIdx) != '(') {
            return null;
        }
        AbcParsedTuplet tuplet = parseAbcTupletAt(text, startIdx);
        if (tuplet != null) {
            return AbcParsedParenToken.tuplet(tuplet);
        }
        return AbcParsedParenToken.slurStart(startIdx + 1);
    }

    public static AbcParsedBracketToken parseAbcBracketTokenAt(String text, int startIdx) {
        if (charAt(rawText(text), startIdx) != '[') {
            return null;
        }
        AbcParsedInlineField inlineField = parseAbcInlineFieldAt(text, startIdx);
        if (inlineField != null) {
            return AbcParsedBracketToken.inlineField(inlineField);
        }
        AbcParsedRepeatEndingMarker repeatEndingMarker = parseAbcRepeatEndingMarkerAt(text, startIdx);
        if (repeatEndingMarker != null) {
            return AbcParsedBracketToken.repeatEnding(repeatEndingMarker);
        }
        return AbcParsedBracketToken.chordStart(startIdx + 1);
    }

    public static AbcParsedBodyToken parseAbcBodyTokenAt(String text, int startIdx) {
        AbcParsedBrokenRhythm brokenRhythm = parseAbcBrokenRhythmAt(text, startIdx);
        if (brokenRhythm != null) {
            return AbcParsedBodyToken.brokenRhythm(brokenRhythm);
        }
        AbcParsedParenToken parenToken = parseAbcParenTokenAt(text, startIdx);
        if (parenToken != null) {
            return AbcParsedBodyToken.paren(parenToken);
        }
        AbcParsedSingleCharShorthand shorthand = parseAbcSingleCharShorthandAt(text, startIdx);
        if (shorthand != null) {
            return AbcParsedBodyToken.singleCharShorthand(shorthand);
        }
        AbcParsedNextIndex tie = parseAbcTieAt(text, startIdx);
        if (tie != null) {
            return AbcParsedBodyToken.tie(tie);
        }
        AbcParsedQuotedString quotedString = parseAbcQuotedStringAt(text, startIdx);
        if (quotedString != null) {
            return AbcParsedBodyToken.quotedString(quotedString);
        }
        AbcParsedDecoration decoration = parseAbcDecorationAt(text, startIdx);
        if (decoration != null) {
            return AbcParsedBodyToken.decoration(decoration);
        }
        AbcParsedBracketToken bracketToken = parseAbcBracketTokenAt(text, startIdx);
        if (bracketToken != null) {
            return AbcParsedBodyToken.bracket(bracketToken);
        }
        AbcParsedNextIndex slurStop = parseAbcSlurStopAt(text, startIdx);
        if (slurStop != null) {
            return AbcParsedBodyToken.slurStop(slurStop);
        }
        return null;
    }

    public static AbcParsedBodyEntry parseAbcBodyEntryAt(String text, int startIdx) {
        AbcParsedBarlineToken barlineToken = parseAbcBarlineTokenAt(text, startIdx);
        if (barlineToken != null) {
            return AbcParsedBodyEntry.barline(barlineToken);
        }
        AbcParsedStandaloneBodyField standaloneBodyField = parseAbcStandaloneBodyFieldAt(text, startIdx);
        if (standaloneBodyField != null) {
            return AbcParsedBodyEntry.standaloneBodyField(standaloneBodyField);
        }
        AbcParsedUnsupportedBodyToken unsupportedBodyToken = parseAbcUnsupportedBodyTokenAt(text, startIdx);
        if (unsupportedBodyToken != null) {
            return AbcParsedBodyEntry.unsupportedBodyToken(unsupportedBodyToken);
        }
        AbcParsedUnsupportedBodyToken unsupportedBodyNumber = parseAbcUnsupportedBodyNumberAt(text, startIdx);
        if (unsupportedBodyNumber != null) {
            return AbcParsedBodyEntry.unsupportedBodyNumber(unsupportedBodyNumber);
        }
        AbcParsedBodyToken bodyToken = parseAbcBodyTokenAt(text, startIdx);
        if (bodyToken != null) {
            return AbcParsedBodyEntry.bodyToken(bodyToken);
        }
        AbcParsedPlayableEvent playableEvent = parseAbcPlayableEventAt(text, startIdx);
        if (playableEvent != null) {
            return AbcParsedBodyEntry.playableEvent(playableEvent);
        }
        return null;
    }

    public static AbcParsedGraceGroup parseAbcGraceGroupAt(String text, int startIdx, int lineNo,
            List<String> warnings) {
        String source = rawText(text);
        if (charAt(source, startIdx) != '{') {
            return null;
        }
        int closeIdx = source.indexOf('}', startIdx + 1);
        if (closeIdx < 0) {
            return null;
        }
        String inner = source.substring(startIdx + 1, closeIdx);
        List<AbcParsedGraceNote> notes = new ArrayList<AbcParsedGraceNote>();
        int idx = 0;
        boolean graceSlashPending = false;
        while (idx < inner.length()) {
            char ch = inner.charAt(idx);
            if (ch == ' ' || ch == '\t') {
                idx++;
                continue;
            }
            if (ch == '/') {
                graceSlashPending = true;
                idx++;
                continue;
            }
            AbcNoteParseResult noteResult = parseAbcNoteAt(inner, idx);
            if (noteResult != null && noteResult.isNote()) {
                AbcParsedNote note = noteResult.getNote();
                notes.add(new AbcParsedGraceNote(note.getAccidentalText(), note.getPitchChar(), note.getOctaveShift(),
                        note.getLengthToken(), graceSlashPending));
                graceSlashPending = false;
                idx = note.getNextIdx();
                continue;
            }
            if (noteResult != null && noteResult.isMalformedAccidental()) {
                if (warnings != null) {
                    warnings.add("line " + lineNo + ": Skipped malformed grace accidental token: "
                            + noteResult.getAccidentalText());
                }
                idx = noteResult.getNextIdx();
                continue;
            }
            idx++;
        }
        return new AbcParsedGraceGroup(notes, closeIdx + 1);
    }

    private static String rawText(String text) {
        return text == null ? "" : text;
    }

    private static String sliceFrom(String text, int startIdx) {
        String source = rawText(text);
        if (startIdx < 0) {
            startIdx = 0;
        }
        if (startIdx >= source.length()) {
            return "";
        }
        return source.substring(startIdx);
    }

    private static Matcher matchFrom(String text, int startIdx, Pattern pattern) {
        Matcher matcher = pattern.matcher(sliceFrom(text, startIdx));
        return matcher.find() ? matcher : null;
    }

    private static char charAt(String text, int idx) {
        if (idx < 0 || idx >= text.length()) {
            return 0;
        }
        return text.charAt(idx);
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String singleCharShorthandKind(char symbol) {
        switch (symbol) {
        case '~':
            return "arpeggiate";
        case 'H':
            return "fermata";
        case 'L':
            return "accent";
        case 'M':
            return "mordent";
        case 'O':
            return "coda";
        case 'P':
            return "inverted-mordent";
        case 'S':
            return "segno";
        case 'T':
            return "trill";
        case 'u':
            return "upbow";
        case 'v':
            return "downbow";
        case '.':
            return "staccato";
        default:
            return null;
        }
    }

    private static final class AbcBarlineCandidate {
        private final String token;
        private final boolean endsMeasure;
        private final boolean repeatEnd;
        private final boolean repeatStart;
        private final boolean endingStop;

        private AbcBarlineCandidate(String token, boolean endsMeasure, boolean repeatEnd, boolean repeatStart,
                boolean endingStop) {
            this.token = token;
            this.endsMeasure = endsMeasure;
            this.repeatEnd = repeatEnd;
            this.repeatStart = repeatStart;
            this.endingStop = endingStop;
        }
    }

    public static class AbcParsedPitchSource {
        private final String accidentalText;
        private final String pitchChar;
        private final String octaveShift;

        public AbcParsedPitchSource(String accidentalText, String pitchChar, String octaveShift) {
            this.accidentalText = accidentalText;
            this.pitchChar = pitchChar;
            this.octaveShift = octaveShift;
        }

        public String getAccidentalText() {
            return accidentalText;
        }

        public String getPitchChar() {
            return pitchChar;
        }

        public String getOctaveShift() {
            return octaveShift;
        }
    }

    public static final class AbcParsedNote extends AbcParsedPitchSource {
        private final String lengthToken;
        private final int nextIdx;

        public AbcParsedNote(String accidentalText, String pitchChar, String octaveShift, String lengthToken,
                int nextIdx) {
            super(accidentalText, pitchChar, octaveShift);
            this.lengthToken = lengthToken;
            this.nextIdx = nextIdx;
        }

        public String getLengthToken() {
            return lengthToken;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public static final class AbcNoteParseResult {
        private final String kind;
        private final AbcParsedNote note;
        private final String accidentalText;
        private final int nextIdx;

        private AbcNoteParseResult(String kind, AbcParsedNote note, String accidentalText, int nextIdx) {
            this.kind = kind;
            this.note = note;
            this.accidentalText = accidentalText;
            this.nextIdx = nextIdx;
        }

        public static AbcNoteParseResult note(AbcParsedNote note) {
            return new AbcNoteParseResult("note", note, "", note.getNextIdx());
        }

        public static AbcNoteParseResult malformedAccidental(String accidentalText, int nextIdx) {
            return new AbcNoteParseResult("malformed-accidental", null, accidentalText, nextIdx);
        }

        public String getKind() {
            return kind;
        }

        public boolean isNote() {
            return "note".equals(kind);
        }

        public boolean isMalformedAccidental() {
            return "malformed-accidental".equals(kind);
        }

        public AbcParsedNote getNote() {
            return note;
        }

        public String getAccidentalText() {
            return accidentalText;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public static final class AbcParsedChord {
        private final List<AbcParsedPitchSource> pitchSources;
        private final List<String> noteLengthTokens;
        private final String lengthToken;
        private final int nextIdx;

        public AbcParsedChord(List<AbcParsedPitchSource> pitchSources, List<String> noteLengthTokens,
                String lengthToken, int nextIdx) {
            this.pitchSources = Collections.unmodifiableList(new ArrayList<AbcParsedPitchSource>(pitchSources));
            this.noteLengthTokens = Collections.unmodifiableList(new ArrayList<String>(noteLengthTokens));
            this.lengthToken = lengthToken;
            this.nextIdx = nextIdx;
        }

        public List<AbcParsedPitchSource> getPitchSources() {
            return pitchSources;
        }

        public List<String> getNoteLengthTokens() {
            return noteLengthTokens;
        }

        public String getLengthToken() {
            return lengthToken;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public static final class AbcParsedPlayableEvent {
        private final String kind;
        private final List<AbcParsedPitchSource> pitchSources;
        private final String rawLengthToken;
        private final int nextIdx;
        private final String source;
        private final String accidentalText;

        private AbcParsedPlayableEvent(String kind, List<AbcParsedPitchSource> pitchSources, String rawLengthToken,
                int nextIdx, String source, String accidentalText) {
            this.kind = kind;
            this.pitchSources = Collections.unmodifiableList(new ArrayList<AbcParsedPitchSource>(pitchSources));
            this.rawLengthToken = rawLengthToken;
            this.nextIdx = nextIdx;
            this.source = source;
            this.accidentalText = accidentalText;
        }

        public static AbcParsedPlayableEvent playable(String source, List<AbcParsedPitchSource> pitchSources,
                String rawLengthToken, int nextIdx) {
            return new AbcParsedPlayableEvent("playable", pitchSources, rawLengthToken, nextIdx, source, "");
        }

        public static AbcParsedPlayableEvent malformedAccidental(String accidentalText, int nextIdx) {
            return new AbcParsedPlayableEvent("malformed-accidental", Collections.<AbcParsedPitchSource>emptyList(), "",
                    nextIdx, "", accidentalText);
        }

        public static AbcParsedPlayableEvent invalidChord(int nextIdx) {
            return new AbcParsedPlayableEvent("invalid-chord", Collections.<AbcParsedPitchSource>emptyList(), "",
                    nextIdx, "", "");
        }

        public String getKind() {
            return kind;
        }

        public List<AbcParsedPitchSource> getPitchSources() {
            return pitchSources;
        }

        public String getRawLengthToken() {
            return rawLengthToken;
        }

        public int getNextIdx() {
            return nextIdx;
        }

        public String getSource() {
            return source;
        }

        public String getAccidentalText() {
            return accidentalText;
        }
    }

    public static final class AbcParsedTuplet {
        private final int actual;
        private final int normal;
        private final int count;
        private final int nextIdx;
        private final String raw;

        public AbcParsedTuplet(int actual, int normal, int count, int nextIdx, String raw) {
            this.actual = actual;
            this.normal = normal;
            this.count = count;
            this.nextIdx = nextIdx;
            this.raw = raw;
        }

        public int getActual() {
            return actual;
        }

        public int getNormal() {
            return normal;
        }

        public int getCount() {
            return count;
        }

        public int getNextIdx() {
            return nextIdx;
        }

        public String getRaw() {
            return raw;
        }
    }

    public static final class AbcParsedRepeatEndingMarker {
        private final String marker;
        private final int nextIdx;

        public AbcParsedRepeatEndingMarker(String marker, int nextIdx) {
            this.marker = marker;
            this.nextIdx = nextIdx;
        }

        public String getMarker() {
            return marker;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public static final class AbcParsedInlineField {
        private final String fieldName;
        private final String fieldValue;
        private final int nextIdx;

        public AbcParsedInlineField(String fieldName, String fieldValue, int nextIdx) {
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
            this.nextIdx = nextIdx;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getFieldValue() {
            return fieldValue;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public static final class AbcParsedBarlineToken {
        private final int nextIdx;
        private final boolean endsMeasure;
        private final boolean repeatEnd;
        private final boolean repeatStart;
        private final boolean endingStop;

        public AbcParsedBarlineToken(int nextIdx, boolean endsMeasure, boolean repeatEnd, boolean repeatStart,
                boolean endingStop) {
            this.nextIdx = nextIdx;
            this.endsMeasure = endsMeasure;
            this.repeatEnd = repeatEnd;
            this.repeatStart = repeatStart;
            this.endingStop = endingStop;
        }

        public int getNextIdx() {
            return nextIdx;
        }

        public boolean isEndsMeasure() {
            return endsMeasure;
        }

        public boolean isRepeatEnd() {
            return repeatEnd;
        }

        public boolean isRepeatStart() {
            return repeatStart;
        }

        public boolean isEndingStop() {
            return endingStop;
        }
    }

    public static final class AbcParsedStandaloneBodyField {
        private final String fieldName;
        private final String fieldValue;
        private final String token;
        private final int nextIdx;

        public AbcParsedStandaloneBodyField(String fieldName, String fieldValue, String token, int nextIdx) {
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
            this.token = token;
            this.nextIdx = nextIdx;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getFieldValue() {
            return fieldValue;
        }

        public String getToken() {
            return token;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public static final class AbcParsedUnsupportedBodyToken {
        private final String token;
        private final int nextIdx;

        public AbcParsedUnsupportedBodyToken(String token, int nextIdx) {
            this.token = token;
            this.nextIdx = nextIdx;
        }

        public String getToken() {
            return token;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public static final class AbcParsedDelimitedSpan {
        private final String delimiter;
        private final String text;
        private final int nextIdx;

        public AbcParsedDelimitedSpan(String delimiter, String text, int nextIdx) {
            this.delimiter = delimiter;
            this.text = text;
            this.nextIdx = nextIdx;
        }

        public String getDelimiter() {
            return delimiter;
        }

        public String getText() {
            return text;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public static final class AbcParsedQuotedString {
        private final String rawText;
        private final String normalizedText;
        private final int nextIdx;
        private final boolean terminated;

        public AbcParsedQuotedString(String rawText, String normalizedText, int nextIdx, boolean terminated) {
            this.rawText = rawText;
            this.normalizedText = normalizedText;
            this.nextIdx = nextIdx;
            this.terminated = terminated;
        }

        public String getRawText() {
            return rawText;
        }

        public String getNormalizedText() {
            return normalizedText;
        }

        public int getNextIdx() {
            return nextIdx;
        }

        public boolean isTerminated() {
            return terminated;
        }
    }

    public static final class AbcParsedDecoration {
        private final String rawDecoration;
        private final String decoration;
        private final String delimiter;
        private final int nextIdx;
        private final boolean terminated;

        public AbcParsedDecoration(String rawDecoration, String decoration, String delimiter, int nextIdx,
                boolean terminated) {
            this.rawDecoration = rawDecoration;
            this.decoration = decoration;
            this.delimiter = delimiter;
            this.nextIdx = nextIdx;
            this.terminated = terminated;
        }

        public String getRawDecoration() {
            return rawDecoration;
        }

        public String getDecoration() {
            return decoration;
        }

        public String getDelimiter() {
            return delimiter;
        }

        public int getNextIdx() {
            return nextIdx;
        }

        public boolean isTerminated() {
            return terminated;
        }
    }

    public static final class AbcParsedBrokenRhythm {
        private final String symbol;
        private final AbcRatio leftScale;
        private final AbcRatio rightScale;
        private final int nextIdx;

        public AbcParsedBrokenRhythm(String symbol, AbcRatio leftScale, AbcRatio rightScale, int nextIdx) {
            this.symbol = symbol;
            this.leftScale = leftScale;
            this.rightScale = rightScale;
            this.nextIdx = nextIdx;
        }

        public String getSymbol() {
            return symbol;
        }

        public AbcRatio getLeftScale() {
            return leftScale;
        }

        public AbcRatio getRightScale() {
            return rightScale;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public static final class AbcRatio {
        private final int num;
        private final int den;

        public AbcRatio(int num, int den) {
            this.num = num;
            this.den = den;
        }

        public int getNum() {
            return num;
        }

        public int getDen() {
            return den;
        }
    }

    public static final class AbcParsedSingleCharShorthand {
        private final String kind;
        private final int nextIdx;

        public AbcParsedSingleCharShorthand(String kind, int nextIdx) {
            this.kind = kind;
            this.nextIdx = nextIdx;
        }

        public String getKind() {
            return kind;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public static final class AbcParsedNextIndex {
        private final int nextIdx;

        public AbcParsedNextIndex(int nextIdx) {
            this.nextIdx = nextIdx;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public static final class AbcParsedParenToken {
        private final String kind;
        private final AbcParsedTuplet tuplet;
        private final int nextIdx;

        private AbcParsedParenToken(String kind, AbcParsedTuplet tuplet, int nextIdx) {
            this.kind = kind;
            this.tuplet = tuplet;
            this.nextIdx = nextIdx;
        }

        public static AbcParsedParenToken tuplet(AbcParsedTuplet tuplet) {
            return new AbcParsedParenToken("tuplet", tuplet, tuplet.getNextIdx());
        }

        public static AbcParsedParenToken slurStart(int nextIdx) {
            return new AbcParsedParenToken("slur-start", null, nextIdx);
        }

        public String getKind() {
            return kind;
        }

        public AbcParsedTuplet getTuplet() {
            return tuplet;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public static final class AbcParsedBracketToken {
        private final String kind;
        private final AbcParsedInlineField inlineField;
        private final AbcParsedRepeatEndingMarker repeatEndingMarker;
        private final int nextIdx;

        private AbcParsedBracketToken(String kind, AbcParsedInlineField inlineField,
                AbcParsedRepeatEndingMarker repeatEndingMarker, int nextIdx) {
            this.kind = kind;
            this.inlineField = inlineField;
            this.repeatEndingMarker = repeatEndingMarker;
            this.nextIdx = nextIdx;
        }

        public static AbcParsedBracketToken inlineField(AbcParsedInlineField inlineField) {
            return new AbcParsedBracketToken("inline-field", inlineField, null, inlineField.getNextIdx());
        }

        public static AbcParsedBracketToken repeatEnding(AbcParsedRepeatEndingMarker repeatEndingMarker) {
            return new AbcParsedBracketToken("repeat-ending", null, repeatEndingMarker,
                    repeatEndingMarker.getNextIdx());
        }

        public static AbcParsedBracketToken chordStart(int nextIdx) {
            return new AbcParsedBracketToken("chord-start", null, null, nextIdx);
        }

        public String getKind() {
            return kind;
        }

        public AbcParsedInlineField getInlineField() {
            return inlineField;
        }

        public AbcParsedRepeatEndingMarker getRepeatEndingMarker() {
            return repeatEndingMarker;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public static final class AbcParsedBodyToken {
        private final String kind;
        private final Object value;

        private AbcParsedBodyToken(String kind, Object value) {
            this.kind = kind;
            this.value = value;
        }

        public static AbcParsedBodyToken brokenRhythm(AbcParsedBrokenRhythm value) {
            return new AbcParsedBodyToken("broken-rhythm", value);
        }

        public static AbcParsedBodyToken paren(AbcParsedParenToken value) {
            return new AbcParsedBodyToken("paren", value);
        }

        public static AbcParsedBodyToken singleCharShorthand(AbcParsedSingleCharShorthand value) {
            return new AbcParsedBodyToken("single-char-shorthand", value);
        }

        public static AbcParsedBodyToken tie(AbcParsedNextIndex value) {
            return new AbcParsedBodyToken("tie", value);
        }

        public static AbcParsedBodyToken quotedString(AbcParsedQuotedString value) {
            return new AbcParsedBodyToken("quoted-string", value);
        }

        public static AbcParsedBodyToken decoration(AbcParsedDecoration value) {
            return new AbcParsedBodyToken("decoration", value);
        }

        public static AbcParsedBodyToken bracket(AbcParsedBracketToken value) {
            return new AbcParsedBodyToken("bracket", value);
        }

        public static AbcParsedBodyToken slurStop(AbcParsedNextIndex value) {
            return new AbcParsedBodyToken("slur-stop", value);
        }

        public String getKind() {
            return kind;
        }

        public AbcParsedBrokenRhythm getBrokenRhythm() {
            return (AbcParsedBrokenRhythm) value;
        }

        public AbcParsedParenToken getParenToken() {
            return (AbcParsedParenToken) value;
        }

        public AbcParsedSingleCharShorthand getShorthand() {
            return (AbcParsedSingleCharShorthand) value;
        }

        public AbcParsedNextIndex getTie() {
            return (AbcParsedNextIndex) value;
        }

        public AbcParsedQuotedString getQuotedString() {
            return (AbcParsedQuotedString) value;
        }

        public AbcParsedDecoration getDecoration() {
            return (AbcParsedDecoration) value;
        }

        public AbcParsedBracketToken getBracketToken() {
            return (AbcParsedBracketToken) value;
        }

        public AbcParsedNextIndex getSlurStop() {
            return (AbcParsedNextIndex) value;
        }
    }

    public static final class AbcParsedBodyEntry {
        private final String kind;
        private final Object value;

        private AbcParsedBodyEntry(String kind, Object value) {
            this.kind = kind;
            this.value = value;
        }

        public static AbcParsedBodyEntry barline(AbcParsedBarlineToken value) {
            return new AbcParsedBodyEntry("barline", value);
        }

        public static AbcParsedBodyEntry standaloneBodyField(AbcParsedStandaloneBodyField value) {
            return new AbcParsedBodyEntry("standalone-body-field", value);
        }

        public static AbcParsedBodyEntry unsupportedBodyToken(AbcParsedUnsupportedBodyToken value) {
            return new AbcParsedBodyEntry("unsupported-body-token", value);
        }

        public static AbcParsedBodyEntry unsupportedBodyNumber(AbcParsedUnsupportedBodyToken value) {
            return new AbcParsedBodyEntry("unsupported-body-number", value);
        }

        public static AbcParsedBodyEntry bodyToken(AbcParsedBodyToken value) {
            return new AbcParsedBodyEntry("body-token", value);
        }

        public static AbcParsedBodyEntry playableEvent(AbcParsedPlayableEvent value) {
            return new AbcParsedBodyEntry("playable-event", value);
        }

        public String getKind() {
            return kind;
        }

        public AbcParsedBarlineToken getBarlineToken() {
            return (AbcParsedBarlineToken) value;
        }

        public AbcParsedStandaloneBodyField getStandaloneBodyField() {
            return (AbcParsedStandaloneBodyField) value;
        }

        public AbcParsedUnsupportedBodyToken getUnsupportedBodyToken() {
            return (AbcParsedUnsupportedBodyToken) value;
        }

        public AbcParsedUnsupportedBodyToken getUnsupportedBodyNumber() {
            return (AbcParsedUnsupportedBodyToken) value;
        }

        public AbcParsedBodyToken getBodyToken() {
            return (AbcParsedBodyToken) value;
        }

        public AbcParsedPlayableEvent getPlayableEvent() {
            return (AbcParsedPlayableEvent) value;
        }
    }

    public static final class AbcParsedGraceNote extends AbcParsedPitchSource {
        private final String lengthToken;
        private final boolean graceSlash;

        public AbcParsedGraceNote(String accidentalText, String pitchChar, String octaveShift, String lengthToken,
                boolean graceSlash) {
            super(accidentalText, pitchChar, octaveShift);
            this.lengthToken = lengthToken;
            this.graceSlash = graceSlash;
        }

        public String getLengthToken() {
            return lengthToken;
        }

        public boolean isGraceSlash() {
            return graceSlash;
        }
    }

    public static final class AbcParsedGraceGroup {
        private final List<AbcParsedGraceNote> notes;
        private final int nextIdx;

        public AbcParsedGraceGroup(List<AbcParsedGraceNote> notes, int nextIdx) {
            this.notes = Collections.unmodifiableList(new ArrayList<AbcParsedGraceNote>(notes));
            this.nextIdx = nextIdx;
        }

        public List<AbcParsedGraceNote> getNotes() {
            return notes;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }
}
