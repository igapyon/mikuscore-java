/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ScoreTimeSignatures {
    private ScoreTimeSignatures() {
    }

    public static TimeSignatureFeature normalizeTimeSignatureFeature(TimeSignatureFeature feature) {
        if (feature == null) {
            return null;
        }
        Integer beats = positiveRounded(feature.getBeats());
        Integer beatType = positiveRounded(feature.getBeatType());
        if (beats == null || beatType == null) {
            return null;
        }
        String symbol = normalizeSymbol(feature.getSymbol());
        return new TimeSignatureFeature(beats, beatType, symbol);
    }

    public static String buildMusicXmlTimeSignatureXml(TimeSignatureFeature feature) {
        TimeSignatureFeature normalized = normalizeTimeSignatureFeature(feature);
        if (normalized == null) {
            return "";
        }
        String symbolAttr = normalized.getSymbol() == null ? "" : " symbol=\"" + normalized.getSymbol() + "\"";
        return "<time" + symbolAttr + "><beats>" + normalized.getBeats() + "</beats><beat-type>"
                + normalized.getBeatType() + "</beat-type></time>";
    }

    public static TimeSignatureFeature extractMusicXmlTimeSignatureFeature(Element time) {
        if (time == null) {
            return null;
        }
        return normalizeTimeSignatureFeature(new TimeSignatureFeature(directChildText(time, "beats"),
                directChildText(time, "beat-type"), time.getAttribute("symbol")));
    }

    private static String normalizeSymbol(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return "common".equals(normalized) || "cut".equals(normalized) ? normalized : null;
    }

    private static Integer positiveRounded(Object value) {
        double n = toNumber(value);
        if (Double.isNaN(n) || Double.isInfinite(n) || n <= 0) {
            return null;
        }
        return Integer.valueOf((int) Math.round(n));
    }

    private static double toNumber(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? 1 : 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return 0;
        }
        if (text.startsWith("0x") || text.startsWith("0X")) {
            return parseRadixNumber(text.substring(2), 16);
        }
        if (text.startsWith("0b") || text.startsWith("0B")) {
            return parseRadixNumber(text.substring(2), 2);
        }
        if (text.startsWith("0o") || text.startsWith("0O")) {
            return parseRadixNumber(text.substring(2), 8);
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    private static double parseRadixNumber(String digits, int radix) {
        if (digits.isEmpty()) {
            return Double.NaN;
        }
        try {
            return Long.parseLong(digits, radix);
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    private static String directChildText(Element parent, String tagName) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                return child.getTextContent() == null ? "" : child.getTextContent();
            }
        }
        return "";
    }

    public static final class TimeSignatureFeature {
        private final Object beats;
        private final Object beatType;
        private final String symbol;

        public TimeSignatureFeature(Object beats, Object beatType) {
            this(beats, beatType, null);
        }

        public TimeSignatureFeature(Object beats, Object beatType, Object symbol) {
            this.beats = beats;
            this.beatType = beatType;
            this.symbol = symbol == null ? null : String.valueOf(symbol);
        }

        public Object getBeats() {
            return beats;
        }

        public Object getBeatType() {
            return beatType;
        }

        public String getSymbol() {
            return symbol;
        }
    }
}
