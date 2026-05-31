/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ScoreBarlines {
    private ScoreBarlines() {
    }

    public static String buildMusicXmlBarlineXml(BarlineFeature feature) {
        BarlineFeature safe = feature == null ? new BarlineFeature() : feature;
        String location = normalizeLocation(safe.getLocation());
        StringBuilder xml = new StringBuilder();
        StringBuilder parts = new StringBuilder();
        if (safe.getBarStyle() != null && safe.getBarStyle().length() > 0) {
            parts.append("<bar-style>").append(xmlEscape(safe.getBarStyle())).append("</bar-style>");
        }
        for (String repeat : safe.getRepeats()) {
            String direction = normalizeRepeatDirection(repeat);
            if (direction != null) {
                parts.append("<repeat direction=\"").append(direction).append("\"/>");
            }
        }
        EndingFeature ending = safe.getEnding();
        if (ending != null) {
            String endingType = normalizeEndingType(ending.getType());
            String number = ending.getNumber();
            if (endingType != null && number != null && number.trim().length() > 0) {
                parts.append("<ending number=\"").append(xmlEscape(number.trim())).append("\" type=\"")
                        .append(endingType).append("\"/>");
            }
        }
        if (parts.length() == 0) {
            return "";
        }
        xml.append("<barline");
        if (location != null) {
            xml.append(" location=\"").append(location).append("\"");
        }
        xml.append(">").append(parts).append("</barline>");
        return xml.toString();
    }

    public static BarlineFeature extractMusicXmlBarlineFeature(Element barline) {
        BarlineFeature feature = new BarlineFeature();
        if (barline == null) {
            return feature;
        }
        feature.setLocation(normalizeLocation(barline.getAttribute("location")));
        for (Node child = barline.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element)) {
                continue;
            }
            Element element = (Element) child;
            if ("bar-style".equals(element.getTagName())) {
                String text = element.getTextContent() == null ? "" : element.getTextContent().trim();
                if (text.length() > 0) {
                    feature.setBarStyle(text);
                }
            } else if ("repeat".equals(element.getTagName())) {
                String direction = normalizeRepeatDirection(element.getAttribute("direction"));
                if (direction != null) {
                    feature.addRepeat(direction);
                }
            } else if ("ending".equals(element.getTagName())) {
                String type = normalizeEndingType(element.getAttribute("type"));
                String number = element.getAttribute("number") == null ? "" : element.getAttribute("number").trim();
                if (type != null && number.length() > 0) {
                    feature.setEnding(new EndingFeature(number, type));
                }
            }
        }
        return feature;
    }

    private static String normalizeLocation(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase();
        return "left".equals(normalized) || "right".equals(normalized) || "middle".equals(normalized) ? normalized
                : null;
    }

    private static String normalizeRepeatDirection(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase();
        return "forward".equals(normalized) || "backward".equals(normalized) ? normalized : null;
    }

    private static String normalizeEndingType(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase();
        return "start".equals(normalized) || "stop".equals(normalized) || "discontinue".equals(normalized)
                ? normalized
                : null;
    }

    private static String xmlEscape(String value) {
        return String.valueOf(value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    public static final class BarlineFeature {
        private String location;
        private String barStyle;
        private final List<String> repeats = new ArrayList<String>();
        private EndingFeature ending;

        public String getLocation() {
            return location;
        }

        public BarlineFeature setLocation(String location) {
            this.location = location;
            return this;
        }

        public String getBarStyle() {
            return barStyle;
        }

        public BarlineFeature setBarStyle(String barStyle) {
            this.barStyle = barStyle;
            return this;
        }

        public List<String> getRepeats() {
            return Collections.unmodifiableList(repeats);
        }

        public BarlineFeature addRepeat(String repeat) {
            if (repeat != null) {
                repeats.add(repeat);
            }
            return this;
        }

        public EndingFeature getEnding() {
            return ending;
        }

        public BarlineFeature setEnding(EndingFeature ending) {
            this.ending = ending;
            return this;
        }
    }

    public static final class EndingFeature {
        private final String number;
        private final String type;

        public EndingFeature(Object number, String type) {
            this.number = number == null ? "" : String.valueOf(number);
            this.type = type;
        }

        public String getNumber() {
            return number;
        }

        public String getType() {
            return type;
        }
    }
}
