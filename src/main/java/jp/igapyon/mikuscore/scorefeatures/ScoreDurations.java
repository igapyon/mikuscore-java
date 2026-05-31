/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ScoreDurations {
    private ScoreDurations() {
    }

    public static int normalizeDotCount(Object value) {
        double n = toNumber(value);
        if (Double.isNaN(n) || Double.isInfinite(n) || n <= 0) {
            return 0;
        }
        return (int) Math.round(n);
    }

    public static String buildMusicXmlDotsXml(Object count) {
        int normalized = normalizeDotCount(count);
        StringBuilder xml = new StringBuilder();
        for (int index = 0; index < normalized; index++) {
            xml.append("<dot/>");
        }
        return xml.toString();
    }

    public static int countMusicXmlDots(Element note) {
        if (note == null) {
            return 0;
        }
        int count = 0;
        for (Node child = note.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && "dot".equals(((Element) child).getTagName())) {
                count++;
            }
        }
        return count;
    }

    private static double toNumber(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? 1 : 0;
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
}
