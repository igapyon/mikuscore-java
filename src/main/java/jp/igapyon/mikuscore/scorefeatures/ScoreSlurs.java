/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ScoreSlurs {
    private ScoreSlurs() {
    }

    public static String buildMusicXmlSlurXml(SlurFeature feature) {
        if (feature == null) {
            return "";
        }
        String type = normalizeSlurType(feature.getType());
        if (type == null) {
            return "";
        }
        Integer number = positiveRounded(feature.getNumber());
        String placement = normalizeSlurPlacement(feature.getPlacement());
        String numberAttr = number == null ? "" : " number=\"" + number + "\"";
        String placementAttr = "start".equals(type) && placement != null ? " placement=\"" + placement + "\"" : "";
        return "<slur type=\"" + type + "\"" + numberAttr + placementAttr + "/>";
    }

    public static String buildMusicXmlSlursXml(Iterable<SlurFeature> features) {
        StringBuilder xml = new StringBuilder();
        if (features != null) {
            for (SlurFeature feature : features) {
                xml.append(buildMusicXmlSlurXml(feature));
            }
        }
        return xml.toString();
    }

    public static List<SlurFeature> extractMusicXmlSlurFeatures(Element note) {
        List<SlurFeature> out = new ArrayList<SlurFeature>();
        if (note == null) {
            return out;
        }
        for (Node child = note.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && "notations".equals(((Element) child).getTagName())) {
                collectDirectSlurFeatures((Element) child, out);
            }
        }
        return out;
    }

    private static void collectDirectSlurFeatures(Element notations, List<SlurFeature> out) {
        for (Node child = notations.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element) || !"slur".equals(((Element) child).getTagName())) {
                continue;
            }
            Element slur = (Element) child;
            String type = normalizeSlurType(slur.getAttribute("type"));
            if (type == null) {
                continue;
            }
            Integer number = positiveRounded(slur.getAttribute("number"));
            String placement = normalizeSlurPlacement(slur.getAttribute("placement"));
            out.add(new SlurFeature(type, number, "start".equals(type) ? placement : null));
        }
    }

    private static String normalizeSlurType(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase();
        return "start".equals(normalized) || "stop".equals(normalized) ? normalized : null;
    }

    private static String normalizeSlurPlacement(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase();
        return "above".equals(normalized) || "below".equals(normalized) ? normalized : null;
    }

    private static Integer positiveRounded(Object raw) {
        double parsed = toNumber(raw);
        if (Double.isNaN(parsed) || Double.isInfinite(parsed) || parsed <= 0) {
            return null;
        }
        return Integer.valueOf((int) Math.round(parsed));
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

    public static final class SlurFeature {
        private final String type;
        private final Object number;
        private final String placement;

        public SlurFeature(String type) {
            this(type, null, null);
        }

        public SlurFeature(String type, Object number) {
            this(type, number, null);
        }

        public SlurFeature(String type, Object number, String placement) {
            this.type = type;
            this.number = number;
            this.placement = placement;
        }

        public String getType() {
            return type;
        }

        public Object getNumber() {
            return number;
        }

        public String getPlacement() {
            return placement;
        }
    }
}
