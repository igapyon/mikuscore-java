/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ScoreDynamics {
    private static final List<String> DYNAMIC_MARKS = Arrays.asList("pppp", "ppp", "pp", "p", "mp", "mf", "f",
            "ff", "fff", "ffff", "fp", "fz", "sffz", "rf", "sf", "sfp", "sfz", "rfz");
    private static final Set<String> DYNAMIC_MARK_SET = new LinkedHashSet<String>(DYNAMIC_MARKS);

    private ScoreDynamics() {
    }

    public static String normalizeDynamicMark(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return DYNAMIC_MARK_SET.contains(normalized) ? normalized : null;
    }

    public static String velocityToDynamicMark(Object velocity) {
        double numeric = velocity instanceof Number ? ((Number) velocity).doubleValue() : Double.NaN;
        int v = (int) Math.round(Double.isNaN(numeric) || Double.isInfinite(numeric) ? 64 : numeric);
        v = Math.max(1, Math.min(127, v));
        if (v <= 15) {
            return "ppp";
        }
        if (v <= 31) {
            return "pp";
        }
        if (v <= 47) {
            return "p";
        }
        if (v <= 63) {
            return "mp";
        }
        if (v <= 79) {
            return "mf";
        }
        if (v <= 95) {
            return "f";
        }
        if (v <= 111) {
            return "ff";
        }
        return "fff";
    }

    public static String buildMusicXmlDirectionFeatureXml(DynamicFeature feature) {
        if (feature == null) {
            return "";
        }
        String placementAttr = isJavaScriptTruthyText(feature.getPlacement())
                ? " placement=\"" + feature.getPlacement() + "\""
                : "";
        Integer offset = positiveRounded(feature.getOffsetDiv());
        String offsetXml = offset == null ? "" : "<offset>" + offset + "</offset>";
        String voiceXml = isJavaScriptTruthyText(feature.getVoice())
                ? "<voice>" + xmlEscape(feature.getVoice()) + "</voice>"
                : "";
        String staffXml = feature.getStaff() == null || String.valueOf(feature.getStaff()).trim().length() == 0 ? ""
                : "<staff>" + xmlEscape(String.valueOf(feature.getStaff())) + "</staff>";

        String directionType;
        if ("dynamic".equals(feature.getKind())) {
            directionType = "<dynamics><" + String.valueOf(feature.getMark()) + "/></dynamics>";
        } else {
            String numberAttr = isJavaScriptTruthyText(feature.getNumber())
                    ? " number=\"" + xmlEscape(feature.getNumber()) + "\""
                    : "";
            directionType = "<wedge type=\"" + String.valueOf(feature.getWedgeType()) + "\"" + numberAttr + "/>";
        }

        return "<direction" + placementAttr + "><direction-type>" + directionType + "</direction-type>" + offsetXml
                + voiceXml + staffXml + "</direction>";
    }

    public static List<DynamicFeature> extractMusicXmlDirectionFeatures(Element direction) {
        List<DynamicFeature> features = new ArrayList<DynamicFeature>();
        if (direction == null) {
            return features;
        }
        Element offsetNode = directChild(direction, "offset");
        Element voiceNode = directChild(direction, "voice");
        Element staffNode = directChild(direction, "staff");
        Integer offset = offsetNode == null ? null : positiveRounded(offsetNode.getTextContent());
        String voice = voiceNode == null ? null : trimToNull(voiceNode.getTextContent());
        String staff = staffNode == null ? null : trimToNull(staffNode.getTextContent());
        String placement = normalizePlacement(direction.getAttribute("placement"));

        List<DynamicFeature> dynamics = new ArrayList<DynamicFeature>();
        List<DynamicFeature> wedges = new ArrayList<DynamicFeature>();
        for (Element directionType : directChildElements(direction, "direction-type")) {
            for (Element child : directChildElements(directionType)) {
                if ("dynamics".equals(child.getTagName())) {
                    for (Element dynamicNode : directChildElements(child)) {
                        String mark = normalizeDynamicMark(dynamicNode.getTagName());
                        if (mark != null) {
                            dynamics.add(DynamicFeature.dynamic(mark, offset, voice, staff, placement));
                        }
                    }
                } else if ("wedge".equals(child.getTagName())) {
                    String wedgeType = normalizeWedgeType(child.getAttribute("type"));
                    if (wedgeType != null) {
                        DynamicFeature feature = DynamicFeature.wedge(wedgeType, trimToNull(child.getAttribute("number")),
                                offset, voice, staff, placement);
                        wedges.add(feature);
                    }
                }
            }
        }
        features.addAll(dynamics);
        features.addAll(wedges);
        return features;
    }

    private static String normalizePlacement(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return "above".equals(normalized) || "below".equals(normalized) ? normalized : null;
    }

    private static String normalizeWedgeType(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return "crescendo".equals(normalized) || "diminuendo".equals(normalized) || "stop".equals(normalized)
                ? normalized
                : null;
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
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? 1 : 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String text = String.valueOf(value).trim();
        if (text.length() == 0) {
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

    private static List<Element> directChildElements(Element parent) {
        return directChildElements(parent, null);
    }

    private static List<Element> directChildElements(Element parent, String tagName) {
        List<Element> out = new ArrayList<Element>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && (tagName == null || tagName.equals(((Element) child).getTagName()))) {
                out.add((Element) child);
            }
        }
        return out;
    }

    private static Element directChild(Element parent, String tagName) {
        for (Element child : directChildElements(parent, tagName)) {
            return child;
        }
        return null;
    }

    private static String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private static boolean isJavaScriptTruthyText(String value) {
        return value != null && value.length() > 0;
    }

    private static String xmlEscape(String value) {
        return String.valueOf(value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    public static final class DynamicFeature {
        private final String kind;
        private final String mark;
        private final String wedgeType;
        private final String number;
        private final Object offsetDiv;
        private final String voice;
        private final Object staff;
        private final String placement;

        public static DynamicFeature dynamic(String mark) {
            return dynamic(mark, null, null, null, null);
        }

        public static DynamicFeature dynamic(String mark, Object offsetDiv, String voice, Object staff, String placement) {
            return new DynamicFeature("dynamic", mark, null, null, offsetDiv, voice, staff, placement);
        }

        public static DynamicFeature wedge(String wedgeType) {
            return wedge(wedgeType, null, null, null, null, null);
        }

        public static DynamicFeature wedge(String wedgeType, String number, Object offsetDiv, String voice, Object staff,
                String placement) {
            return new DynamicFeature("wedge", null, wedgeType, number, offsetDiv, voice, staff, placement);
        }

        private DynamicFeature(String kind, String mark, String wedgeType, String number, Object offsetDiv, String voice,
                Object staff, String placement) {
            this.kind = kind;
            this.mark = mark;
            this.wedgeType = wedgeType;
            this.number = number;
            this.offsetDiv = offsetDiv;
            this.voice = voice;
            this.staff = staff;
            this.placement = placement;
        }

        public String getKind() {
            return kind;
        }

        public String getMark() {
            return mark;
        }

        public String getWedgeType() {
            return wedgeType;
        }

        public String getNumber() {
            return number;
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

        public String getPlacement() {
            return placement;
        }
    }
}
