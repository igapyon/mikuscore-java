/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ScoreDirectionText {
    private ScoreDirectionText() {
    }

    public static Double normalizeTempoBpm(Object raw) {
        double n = toNumber(raw);
        if (Double.isNaN(n) || Double.isInfinite(n) || n <= 0) {
            return null;
        }
        return Double.valueOf(n);
    }

    public static String formatTempoBpm(Object bpm) {
        Double normalized = normalizeTempoBpm(bpm);
        if (normalized == null) {
            return "";
        }
        double value = normalized.doubleValue();
        if (value == Math.rint(value)) {
            return String.valueOf((long) Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("\\.00$", "").replaceAll("(\\.\\d)0$", "$1");
    }

    public static String buildMusicXmlWordsDirectionXml(DirectionWordsFeature feature) {
        if (feature == null) {
            return "";
        }
        String text = trimToNull(feature.getText());
        if (text == null) {
            return "";
        }
        String placement = normalizePlacement(feature.getPlacement());
        String placementAttr = placement == null ? "" : " placement=\"" + placement + "\"";
        String fontStyle = normalizeFontStyle(feature.getFontStyle());
        String fontStyleAttr = fontStyle == null ? "" : " font-style=\"" + fontStyle + "\"";
        Double tempo = normalizeTempoBpm(feature.getTempoBpm());
        String soundXml = tempo == null ? "" : "<sound tempo=\"" + formatTempoBpm(tempo) + "\"/>";
        return "<direction" + placementAttr + "><direction-type><words" + fontStyleAttr + ">" + xmlEscape(text)
                + "</words></direction-type>" + directionTailXml(feature.getOffsetDiv(), feature.getVoice(),
                        feature.getStaff())
                + soundXml + "</direction>";
    }

    public static String buildMusicXmlTempoDirectionXml(DirectionTempoFeature feature) {
        if (feature == null) {
            return "";
        }
        Double tempo = normalizeTempoBpm(feature.getBpm());
        if (tempo == null) {
            return "";
        }
        String placement = normalizePlacement(feature.getPlacement());
        String placementAttr = placement == null ? "" : " placement=\"" + placement + "\"";
        StringBuilder directionTypes = new StringBuilder();
        String text = trimToNull(feature.getText());
        if (text != null) {
            directionTypes.append("<direction-type><words>").append(xmlEscape(text)).append("</words></direction-type>");
        }
        if (feature.isIncludeQuarterMetronome()) {
            directionTypes.append("<direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>")
                    .append(formatTempoBpm(tempo)).append("</per-minute></metronome></direction-type>");
        }
        return "<direction" + placementAttr + ">" + directionTypes.toString()
                + directionTailXml(feature.getOffsetDiv(), feature.getVoice(), feature.getStaff()) + "<sound tempo=\""
                + formatTempoBpm(tempo) + "\"/></direction>";
    }

    public static List<DirectionWord> extractMusicXmlDirectionWords(Element direction) {
        List<DirectionWord> out = new ArrayList<DirectionWord>();
        if (direction == null) {
            return out;
        }
        for (Element directionType : directChildElements(direction, "direction-type")) {
            for (Element words : directChildElements(directionType, "words")) {
                String text = trimToNull(words.getTextContent());
                if (text == null) {
                    continue;
                }
                out.add(new DirectionWord(text, normalizeFontStyle(words.getAttribute("font-style"))));
            }
        }
        return out;
    }

    public static Double extractMusicXmlSoundTempoBpm(Element directionOrMeasure) {
        if (directionOrMeasure == null) {
            return null;
        }
        if ("sound".equals(directionOrMeasure.getTagName())) {
            return normalizeTempoBpm(directionOrMeasure.getAttribute("tempo"));
        }
        for (Element child : directChildElements(directionOrMeasure, "sound")) {
            return normalizeTempoBpm(child.getAttribute("tempo"));
        }
        return null;
    }

    public static String extractMusicXmlDirectionPlacement(Element direction) {
        return direction == null ? null : normalizePlacement(direction.getAttribute("placement"));
    }

    private static String directionTailXml(Object offsetDiv, String voice, Object staff) {
        Integer offset = positiveRounded(offsetDiv);
        String offsetXml = offset == null ? "" : "<offset>" + offset + "</offset>";
        String voiceXml = trimToNull(voice) == null ? "" : "<voice>" + xmlEscape(voice) + "</voice>";
        String staffXml = staff == null || String.valueOf(staff).trim().length() == 0 ? ""
                : "<staff>" + xmlEscape(String.valueOf(staff)) + "</staff>";
        return offsetXml + voiceXml + staffXml;
    }

    private static String normalizePlacement(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return "above".equals(normalized) || "below".equals(normalized) ? normalized : null;
    }

    private static String normalizeFontStyle(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return "italic".equals(normalized) || "normal".equals(normalized) ? normalized : null;
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
            return Double.NaN;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String text = String.valueOf(value).trim();
        if (text.length() == 0) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    private static List<Element> directChildElements(Element parent, String tagName) {
        List<Element> out = new ArrayList<Element>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                out.add((Element) child);
            }
        }
        return out;
    }

    private static String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private static String xmlEscape(String value) {
        return String.valueOf(value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    public static final class DirectionWordsFeature {
        private final String text;
        private final String placement;
        private final String fontStyle;
        private final Object tempoBpm;
        private final Object offsetDiv;
        private final String voice;
        private final Object staff;

        public DirectionWordsFeature(String text) {
            this(text, null, null, null, null, null, null);
        }

        public DirectionWordsFeature(String text, String placement, String fontStyle, Object tempoBpm, Object offsetDiv,
                String voice, Object staff) {
            this.text = text;
            this.placement = placement;
            this.fontStyle = fontStyle;
            this.tempoBpm = tempoBpm;
            this.offsetDiv = offsetDiv;
            this.voice = voice;
            this.staff = staff;
        }

        public String getText() {
            return text;
        }

        public String getPlacement() {
            return placement;
        }

        public String getFontStyle() {
            return fontStyle;
        }

        public Object getTempoBpm() {
            return tempoBpm;
        }

        public Object getOffsetDiv() {
            return offsetDiv;
        }

        public String getVoice() {
            return voice;
        }

        public Object getStaff() {
            return staff;
        }
    }

    public static final class DirectionTempoFeature {
        private final Object bpm;
        private final String text;
        private final String placement;
        private final Object offsetDiv;
        private final String voice;
        private final Object staff;
        private final boolean includeQuarterMetronome;

        public DirectionTempoFeature(Object bpm) {
            this(bpm, null, null, null, null, null, false);
        }

        public DirectionTempoFeature(Object bpm, String text, String placement, Object offsetDiv, String voice,
                Object staff, boolean includeQuarterMetronome) {
            this.bpm = bpm;
            this.text = text;
            this.placement = placement;
            this.offsetDiv = offsetDiv;
            this.voice = voice;
            this.staff = staff;
            this.includeQuarterMetronome = includeQuarterMetronome;
        }

        public Object getBpm() {
            return bpm;
        }

        public String getText() {
            return text;
        }

        public String getPlacement() {
            return placement;
        }

        public Object getOffsetDiv() {
            return offsetDiv;
        }

        public String getVoice() {
            return voice;
        }

        public Object getStaff() {
            return staff;
        }

        public boolean isIncludeQuarterMetronome() {
            return includeQuarterMetronome;
        }
    }

    public static final class DirectionWord {
        private final String text;
        private final String fontStyle;

        private DirectionWord(String text, String fontStyle) {
            this.text = text;
            this.fontStyle = fontStyle;
        }

        public String getText() {
            return text;
        }

        public String getFontStyle() {
            return fontStyle;
        }
    }
}
