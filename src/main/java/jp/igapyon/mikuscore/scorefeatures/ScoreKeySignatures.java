/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ScoreKeySignatures {
    private ScoreKeySignatures() {
    }

    public static KeySignatureFeature normalizeKeySignatureFeature(KeySignatureFeature feature) {
        if (feature == null) {
            return null;
        }
        Integer fifths = roundedFinite(feature.getFifths());
        if (fifths == null) {
            return null;
        }
        String mode = feature.getMode() == null ? "" : feature.getMode().trim().toLowerCase(Locale.ROOT);
        return new KeySignatureFeature(fifths, mode.length() == 0 ? null : mode);
    }

    public static String buildMusicXmlKeySignatureXml(KeySignatureFeature feature) {
        KeySignatureFeature normalized = normalizeKeySignatureFeature(feature);
        if (normalized == null) {
            return "";
        }
        String modeXml = normalized.getMode() == null ? "" : "<mode>" + normalized.getMode() + "</mode>";
        return "<key><fifths>" + normalized.getFifths() + "</fifths>" + modeXml + "</key>";
    }

    public static KeySignatureFeature extractMusicXmlKeySignatureFeature(Element key) {
        if (key == null) {
            return null;
        }
        return normalizeKeySignatureFeature(
                new KeySignatureFeature(directChildText(key, "fifths"), directChildText(key, "mode")));
    }

    private static Integer roundedFinite(Object value) {
        double n = toNumber(value);
        if (Double.isNaN(n) || Double.isInfinite(n)) {
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

    public static final class KeySignatureFeature {
        private final Object fifths;
        private final String mode;

        public KeySignatureFeature(Object fifths) {
            this(fifths, null);
        }

        public KeySignatureFeature(Object fifths, Object mode) {
            this.fifths = fifths;
            this.mode = mode == null ? null : String.valueOf(mode);
        }

        public Object getFifths() {
            return fifths;
        }

        public String getMode() {
            return mode;
        }
    }
}
