/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ScoreClefs {
    private ScoreClefs() {
    }

    public static ClefFeature normalizeClefFeature(ClefFeature feature) {
        if (feature == null) {
            return null;
        }
        String sign = feature.getSign() == null ? "" : feature.getSign().trim();
        Integer line = positiveRounded(feature.getLine());
        if (sign.length() == 0 || line == null) {
            return null;
        }
        String number = feature.getNumber() == null || feature.getNumber().trim().length() == 0 ? null
                : feature.getNumber().trim();
        return new ClefFeature(sign, line, number);
    }

    public static String buildMusicXmlClefXml(ClefFeature feature) {
        ClefFeature normalized = normalizeClefFeature(feature);
        if (normalized == null) {
            return "";
        }
        String numberAttr = normalized.getNumber() == null ? ""
                : " number=\"" + xmlEscape(normalized.getNumber()) + "\"";
        return "<clef" + numberAttr + "><sign>" + xmlEscape(normalized.getSign()) + "</sign><line>"
                + normalized.getLine() + "</line></clef>";
    }

    public static ClefFeature extractMusicXmlClefFeature(Element clef) {
        if (clef == null) {
            return null;
        }
        return normalizeClefFeature(
                new ClefFeature(directChildText(clef, "sign"), directChildText(clef, "line"), clef.getAttribute("number")));
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
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(text);
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

    private static String xmlEscape(String value) {
        return String.valueOf(value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    public static final class ClefFeature {
        private final String sign;
        private final Object line;
        private final String number;

        public ClefFeature(String sign, Object line) {
            this(sign, line, null);
        }

        public ClefFeature(String sign, Object line, Object number) {
            this.sign = sign;
            this.line = line;
            this.number = number == null ? null : String.valueOf(number);
        }

        public String getSign() {
            return sign;
        }

        public Object getLine() {
            return line;
        }

        public String getNumber() {
            return number;
        }
    }
}
