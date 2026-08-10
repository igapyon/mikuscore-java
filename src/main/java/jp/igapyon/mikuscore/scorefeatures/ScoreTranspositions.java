/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ScoreTranspositions {
    private ScoreTranspositions() {
    }

    public static TranspositionFeature normalizeTranspositionFeature(TranspositionFeature feature) {
        if (feature == null) {
            return null;
        }
        Integer diatonic = roundedFinite(feature.getDiatonic());
        Integer chromatic = roundedFinite(feature.getChromatic());
        if (diatonic == null && chromatic == null) {
            return null;
        }
        return new TranspositionFeature(diatonic, chromatic);
    }

    public static String buildMusicXmlTransposeXml(TranspositionFeature feature) {
        TranspositionFeature normalized = normalizeTranspositionFeature(feature);
        if (normalized == null) {
            return "";
        }
        String diatonicXml = normalized.getDiatonic() == null ? ""
                : "<diatonic>" + normalized.getDiatonic() + "</diatonic>";
        String chromaticXml = normalized.getChromatic() == null ? ""
                : "<chromatic>" + normalized.getChromatic() + "</chromatic>";
        return "<transpose>" + diatonicXml + chromaticXml + "</transpose>";
    }

    public static TranspositionFeature extractMusicXmlTranspositionFeature(Element transpose) {
        if (transpose == null) {
            return null;
        }
        return normalizeTranspositionFeature(new TranspositionFeature(directChildText(transpose, "diatonic"),
                directChildText(transpose, "chromatic")));
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
            return Double.NaN;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return 0;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? 1 : 0;
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

    public static final class TranspositionFeature {
        private final Object diatonic;
        private final Object chromatic;

        public TranspositionFeature(Object diatonic, Object chromatic) {
            this.diatonic = diatonic;
            this.chromatic = chromatic;
        }

        public Object getDiatonic() {
            return diatonic;
        }

        public Object getChromatic() {
            return chromatic;
        }
    }
}
