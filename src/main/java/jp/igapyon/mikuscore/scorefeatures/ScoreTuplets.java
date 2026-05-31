/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ScoreTuplets {
    private ScoreTuplets() {
    }

    public static TimeModificationFeature normalizeTimeModificationFeature(TimeModificationFeature feature) {
        if (feature == null) {
            return null;
        }
        Integer actualNotes = positiveRounded(feature.getActualNotes());
        Integer normalNotes = positiveRounded(feature.getNormalNotes());
        if (actualNotes == null || normalNotes == null) {
            return null;
        }
        return new TimeModificationFeature(actualNotes, normalNotes);
    }

    public static String buildMusicXmlTimeModificationXml(TimeModificationFeature feature) {
        TimeModificationFeature normalized = normalizeTimeModificationFeature(feature);
        if (normalized == null) {
            return "";
        }
        return "<time-modification><actual-notes>" + normalized.getActualNotes() + "</actual-notes><normal-notes>"
                + normalized.getNormalNotes() + "</normal-notes></time-modification>";
    }

    public static TimeModificationFeature extractMusicXmlTimeModificationFeature(Element note) {
        Element timeModification = directChild(note, "time-modification");
        if (timeModification == null) {
            return null;
        }
        return normalizeTimeModificationFeature(new TimeModificationFeature(directChildText(timeModification, "actual-notes"),
                directChildText(timeModification, "normal-notes")));
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

    private static Element directChild(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && tagName.equals(((Element) child).getTagName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private static String directChildText(Element parent, String tagName) {
        Element child = directChild(parent, tagName);
        return child == null || child.getTextContent() == null ? "" : child.getTextContent();
    }

    public static final class TimeModificationFeature {
        private final Object actualNotes;
        private final Object normalNotes;

        public TimeModificationFeature(Object actualNotes, Object normalNotes) {
            this.actualNotes = actualNotes;
            this.normalNotes = normalNotes;
        }

        public Object getActualNotes() {
            return actualNotes;
        }

        public Object getNormalNotes() {
            return normalNotes;
        }
    }
}
