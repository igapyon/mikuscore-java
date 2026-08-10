/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import java.util.List;
import java.util.Locale;

public final class ScoreNoteElements {
    private ScoreNoteElements() {
    }

    public static AccidentalFeature normalizeAccidentalFeature(AccidentalFeature feature) {
        if (feature == null) {
            return null;
        }
        String text = feature.getText() == null ? "" : feature.getText().trim();
        if (text.length() == 0) {
            return null;
        }
        return new AccidentalFeature(text, feature.isEditorial(), feature.isCautionary());
    }

    public static String buildMusicXmlAccidentalXml(AccidentalFeature feature) {
        AccidentalFeature normalized = normalizeAccidentalFeature(feature);
        if (normalized == null) {
            return "";
        }
        StringBuilder attrs = new StringBuilder();
        if (normalized.isEditorial()) {
            attrs.append(" editorial=\"yes\"");
        }
        if (normalized.isCautionary()) {
            attrs.append(" cautionary=\"yes\"");
        }
        return "<accidental" + attrs + ">" + xmlEscape(normalized.getText()) + "</accidental>";
    }

    public static String buildMusicXmlGraceXml(GraceFeature feature) {
        return feature != null && feature.isSlash() ? "<grace slash=\"yes\"/>" : "<grace/>";
    }

    public static String buildMusicXmlGraceXml() {
        return buildMusicXmlGraceXml(null);
    }

    public static String buildMusicXmlStemXml(Object value) {
        String normalized = value == null ? "" : String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return "up".equals(normalized) || "down".equals(normalized) ? "<stem>" + normalized + "</stem>" : "";
    }

    public static LyricFeature normalizeLyricFeature(LyricFeature feature) {
        if (feature == null) {
            return null;
        }
        String text = feature.getText() == null ? "" : feature.getText().trim();
        if (text.length() == 0) {
            return null;
        }
        String syllabic = feature.getSyllabic() == null ? "" : feature.getSyllabic().trim();
        return new LyricFeature(text, syllabic.length() == 0 ? null : syllabic, feature.isExtend());
    }

    public static String buildMusicXmlLyricXml(LyricFeature feature) {
        LyricFeature normalized = normalizeLyricFeature(feature);
        if (normalized == null) {
            return "";
        }
        String syllabicXml = normalized.getSyllabic() == null ? ""
                : "<syllabic>" + xmlEscape(normalized.getSyllabic()) + "</syllabic>";
        String extendXml = normalized.isExtend() ? "<extend/>" : "";
        return "<lyric>" + syllabicXml + "<text>" + xmlEscape(normalized.getText()) + "</text>" + extendXml
                + "</lyric>";
    }

    public static String buildMusicXmlFingeringXml(Object value) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return text.length() == 0 ? "" : "<fingering>" + xmlEscape(text) + "</fingering>";
    }

    public static String buildMusicXmlStringNumberXml(Object value) {
        return buildMusicXmlStringNumberXml(value, false);
    }

    public static String buildMusicXmlStringNumberXml(Object value, boolean roundNumeric) {
        String text = value == null ? "" : String.valueOf(value).trim();
        if (text.length() == 0) {
            return "";
        }
        Double n = parseFiniteNumber(value);
        String normalized = roundNumeric && n != null && n.doubleValue() > 0 ? String.valueOf(Math.round(n.doubleValue()))
                : text;
        return "<string>" + xmlEscape(normalized) + "</string>";
    }

    public static String buildMusicXmlTechnicalXml(List<String> items) {
        StringBuilder body = new StringBuilder();
        if (items != null) {
            for (String item : items) {
                if (item != null && item.length() > 0) {
                    body.append(item);
                }
            }
        }
        return body.length() == 0 ? "" : "<technical>" + body + "</technical>";
    }

    private static Double parseFiniteNumber(Object value) {
        if (value instanceof Boolean) {
            return Double.valueOf(((Boolean) value).booleanValue() ? 1 : 0);
        }
        try {
            double n = value instanceof Number ? ((Number) value).doubleValue() : parseJavaScriptNumber(String.valueOf(value).trim());
            return Double.isNaN(n) || Double.isInfinite(n) ? null : Double.valueOf(n);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static double parseJavaScriptNumber(String text) {
        if (text.startsWith("0x") || text.startsWith("0X")) {
            return parseRadixNumber(text.substring(2), 16);
        }
        if (text.startsWith("0b") || text.startsWith("0B")) {
            return parseRadixNumber(text.substring(2), 2);
        }
        if (text.startsWith("0o") || text.startsWith("0O")) {
            return parseRadixNumber(text.substring(2), 8);
        }
        return Double.parseDouble(text);
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

    private static String xmlEscape(String value) {
        return String.valueOf(value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    public static final class AccidentalFeature {
        private final String text;
        private final boolean editorial;
        private final boolean cautionary;

        public AccidentalFeature(String text) {
            this(text, false, false);
        }

        public AccidentalFeature(String text, boolean editorial, boolean cautionary) {
            this.text = text;
            this.editorial = editorial;
            this.cautionary = cautionary;
        }

        public String getText() {
            return text;
        }

        public boolean isEditorial() {
            return editorial;
        }

        public boolean isCautionary() {
            return cautionary;
        }
    }

    public static final class GraceFeature {
        private final boolean slash;

        public GraceFeature(boolean slash) {
            this.slash = slash;
        }

        public boolean isSlash() {
            return slash;
        }
    }

    public static final class LyricFeature {
        private final String text;
        private final String syllabic;
        private final boolean extend;

        public LyricFeature(String text) {
            this(text, null, false);
        }

        public LyricFeature(String text, String syllabic, boolean extend) {
            this.text = text;
            this.syllabic = syllabic;
            this.extend = extend;
        }

        public String getText() {
            return text;
        }

        public String getSyllabic() {
            return syllabic;
        }

        public boolean isExtend() {
            return extend;
        }
    }
}
