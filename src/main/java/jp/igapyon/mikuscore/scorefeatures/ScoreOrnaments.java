/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ScoreOrnaments {
    private static final List<String> ORNAMENT_KINDS = Arrays.asList("trill-mark", "turn", "inverted-turn",
            "delayed-turn", "mordent", "inverted-mordent", "shake", "schleifer");
    private static final Set<String> ORNAMENT_KIND_SET = new LinkedHashSet<String>(ORNAMENT_KINDS);

    private ScoreOrnaments() {
    }

    public static String normalizeOrnamentKind(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase();
        return ORNAMENT_KIND_SET.contains(normalized) ? normalized : null;
    }

    public static String buildMusicXmlOrnamentItemsXml(Iterable<OrnamentFeature> features) {
        StringBuilder xml = new StringBuilder();
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        if (features != null) {
            for (OrnamentFeature feature : features) {
                appendOrnamentFeatureXml(feature, seen, xml);
            }
        }
        return xml.toString();
    }

    public static String buildMusicXmlOrnamentsXml(Iterable<OrnamentFeature> features) {
        String itemsXml = buildMusicXmlOrnamentItemsXml(features);
        return itemsXml.length() == 0 ? "" : "<ornaments>" + itemsXml + "</ornaments>";
    }

    public static List<OrnamentFeature> extractMusicXmlOrnamentFeatures(Element note) {
        List<OrnamentFeature> out = new ArrayList<OrnamentFeature>();
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        if (note == null) {
            return out;
        }
        for (Node notationNode = note.getFirstChild(); notationNode != null; notationNode = notationNode.getNextSibling()) {
            if (notationNode instanceof Element && "notations".equals(((Element) notationNode).getTagName())) {
                collectDirectOrnamentFeatures((Element) notationNode, seen, out);
            }
        }
        return out;
    }

    private static void appendOrnamentFeatureXml(OrnamentFeature feature, LinkedHashSet<String> seen, StringBuilder xml) {
        if (feature == null || feature.getKind() == null) {
            return;
        }
        if ("tremolo".equals(feature.getKind())) {
            String type = normalizeTremoloType(feature.getTremoloType());
            int marks = normalizeTremoloMarks(feature.getMarks(), 1);
            String key = "tremolo:" + (type == null ? "" : type) + ":" + marks;
            if (!seen.add(key)) {
                return;
            }
            String typeAttr = type == null ? "" : " type=\"" + type + "\"";
            xml.append("<tremolo").append(typeAttr).append(">").append(marks).append("</tremolo>");
            return;
        }
        String kind = normalizeOrnamentKind(feature.getKind());
        if (kind == null) {
            return;
        }
        boolean slash = ("turn".equals(kind) || "inverted-turn".equals(kind)) && feature.isSlash();
        String slashAttr = slash ? " slash=\"yes\"" : "";
        String key = kind + ":" + slash;
        if (!seen.add(key)) {
            return;
        }
        xml.append("<").append(kind).append(slashAttr).append("/>");
    }

    private static void collectDirectOrnamentFeatures(Element notations, LinkedHashSet<String> seen,
            List<OrnamentFeature> out) {
        for (Node ornamentsNode = notations.getFirstChild(); ornamentsNode != null; ornamentsNode = ornamentsNode
                .getNextSibling()) {
            if (!(ornamentsNode instanceof Element) || !"ornaments".equals(((Element) ornamentsNode).getTagName())) {
                continue;
            }
            collectOrnamentItems((Element) ornamentsNode, seen, out);
        }
    }

    private static void collectOrnamentItems(Element ornaments, LinkedHashSet<String> seen, List<OrnamentFeature> out) {
        for (Node child = ornaments.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element)) {
                continue;
            }
            Element element = (Element) child;
            String tag = element.getTagName().toLowerCase();
            if ("tremolo".equals(tag)) {
                String type = normalizeTremoloType(element.getAttribute("type"));
                Integer marks = normalizeTremoloMarksOrNull(element.getTextContent());
                String key = "tremolo:" + (type == null ? "" : type) + ":" + (marks == null ? "" : marks);
                if (seen.add(key)) {
                    out.add(new OrnamentFeature("tremolo", false, type, marks));
                }
                continue;
            }
            String kind = normalizeOrnamentKind(tag);
            if (kind == null) {
                continue;
            }
            boolean slash = ("turn".equals(kind) || "inverted-turn".equals(kind))
                    && "yes".equals(element.getAttribute("slash").trim().toLowerCase());
            String key = kind + ":" + slash;
            if (seen.add(key)) {
                out.add(new OrnamentFeature(kind, slash, null, null));
            }
        }
    }

    private static String normalizeTremoloType(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase();
        return "single".equals(normalized) || "start".equals(normalized) || "stop".equals(normalized) ? normalized
                : null;
    }

    private static int normalizeTremoloMarks(Object raw, int fallback) {
        Integer marks = normalizeTremoloMarksOrNull(raw);
        return marks == null ? fallback : marks.intValue();
    }

    private static Integer normalizeTremoloMarksOrNull(Object raw) {
        double parsed = toNumber(raw);
        if (Double.isNaN(parsed) || Double.isInfinite(parsed) || parsed <= 0) {
            return null;
        }
        return Integer.valueOf(Math.max(1, Math.min(8, (int) Math.round(parsed))));
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

    public static final class OrnamentFeature {
        private final String kind;
        private final boolean slash;
        private final String tremoloType;
        private final Object marks;

        public OrnamentFeature(String kind) {
            this(kind, false, null, null);
        }

        public OrnamentFeature(String kind, boolean slash) {
            this(kind, slash, null, null);
        }

        public static OrnamentFeature tremolo(String tremoloType, Object marks) {
            return new OrnamentFeature("tremolo", false, tremoloType, marks);
        }

        private OrnamentFeature(String kind, boolean slash, String tremoloType, Object marks) {
            this.kind = kind;
            this.slash = slash;
            this.tremoloType = tremoloType;
            this.marks = marks;
        }

        public String getKind() {
            return kind;
        }

        public boolean isSlash() {
            return slash;
        }

        public String getTremoloType() {
            return tremoloType;
        }

        public Object getMarks() {
            return marks;
        }
    }
}
