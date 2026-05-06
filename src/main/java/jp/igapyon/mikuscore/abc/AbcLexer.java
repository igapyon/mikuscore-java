/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.abc;

public final class AbcLexer {
    private AbcLexer() {
    }

    public static AbcLengthTokenLex lexAbcLengthToken(String text, int startIdx) {
        String source = rawText(text);
        int start = normalizeStartIndex(startIdx);
        char first = charAt(source, start);
        if (first == 0) {
            return null;
        }

        if (first == '/') {
            int idx = start;
            while (charAt(source, idx) == '/') {
                idx++;
            }
            if (idx > start + 1) {
                return new AbcLengthTokenLex(source.substring(start, idx), idx);
            }
            while (isDigit(charAt(source, idx))) {
                idx++;
            }
            return new AbcLengthTokenLex(source.substring(start, idx), idx);
        }

        if (!isDigit(first)) {
            return null;
        }

        int idx = start;
        while (isDigit(charAt(source, idx))) {
            idx++;
        }
        if (charAt(source, idx) == '/') {
            idx++;
            while (isDigit(charAt(source, idx))) {
                idx++;
            }
        }
        return new AbcLengthTokenLex(source.substring(start, idx), idx);
    }

    public static AbcAccidentalLex lexAbcAccidental(String text, int startIdx) {
        String source = rawText(text);
        int idx = normalizeStartIndex(startIdx);
        StringBuilder accidentalText = new StringBuilder();
        while (idx < source.length()) {
            char current = source.charAt(idx);
            if (current != '^' && current != '_' && current != '=') {
                break;
            }
            accidentalText.append(current);
            idx++;
            String accidental = accidentalText.toString();
            if ("=".equals(accidental) || accidental.startsWith("^") || accidental.startsWith("_")) {
                if (accidental.length() >= 2 && accidental.charAt(0) != accidental.charAt(1)) {
                    break;
                }
                if (accidental.length() >= 2 && accidental.charAt(0) == '=') {
                    accidentalText.setLength(0);
                    accidentalText.append('=');
                    break;
                }
            }
        }
        if (accidentalText.length() == 0) {
            return null;
        }
        return new AbcAccidentalLex(accidentalText.toString(), idx);
    }

    public static AbcNoteLex lexAbcNote(String text, int startIdx) {
        String source = rawText(text);
        int idx = normalizeStartIndex(startIdx);
        AbcAccidentalLex accidental = lexAbcAccidental(source, idx);
        String accidentalText = "";
        if (accidental != null) {
            accidentalText = accidental.getAccidentalText();
            idx = accidental.getNextIdx();
        }

        char pitchChar = charAt(source, idx);
        if (!isAbcPitchChar(pitchChar)) {
            return null;
        }
        idx++;

        int octaveStart = idx;
        while (charAt(source, idx) == '\'' || charAt(source, idx) == ',') {
            idx++;
        }
        String octaveShift = source.substring(octaveStart, idx);

        AbcLengthTokenLex length = lexAbcLengthToken(source, idx);
        String lengthToken = "";
        if (length != null) {
            lengthToken = length.getToken();
            idx = length.getNextIdx();
        }

        return new AbcNoteLex(accidentalText, String.valueOf(pitchChar), octaveShift, lengthToken, idx);
    }

    private static int normalizeStartIndex(int startIdx) {
        return Math.max(0, startIdx);
    }

    private static String rawText(String text) {
        return text == null ? "" : text;
    }

    private static char charAt(String text, int idx) {
        if (idx < 0 || idx >= text.length()) {
            return 0;
        }
        return text.charAt(idx);
    }

    private static boolean isDigit(char value) {
        return value >= '0' && value <= '9';
    }

    private static boolean isAbcPitchChar(char value) {
        return (value >= 'A' && value <= 'G')
                || (value >= 'a' && value <= 'g')
                || value == 'z'
                || value == 'Z'
                || value == 'x'
                || value == 'X';
    }

    public static final class AbcLengthTokenLex {
        private final String token;
        private final int nextIdx;

        public AbcLengthTokenLex(String token, int nextIdx) {
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

    public static final class AbcAccidentalLex {
        private final String accidentalText;
        private final int nextIdx;

        public AbcAccidentalLex(String accidentalText, int nextIdx) {
            this.accidentalText = accidentalText;
            this.nextIdx = nextIdx;
        }

        public String getAccidentalText() {
            return accidentalText;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }

    public static final class AbcNoteLex {
        private final String accidentalText;
        private final String pitchChar;
        private final String octaveShift;
        private final String lengthToken;
        private final int nextIdx;

        public AbcNoteLex(String accidentalText, String pitchChar, String octaveShift, String lengthToken, int nextIdx) {
            this.accidentalText = accidentalText;
            this.pitchChar = pitchChar;
            this.octaveShift = octaveShift;
            this.lengthToken = lengthToken;
            this.nextIdx = nextIdx;
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

        public String getLengthToken() {
            return lengthToken;
        }

        public int getNextIdx() {
            return nextIdx;
        }
    }
}
