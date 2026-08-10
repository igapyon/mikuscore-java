/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ScorePitches {
    private ScorePitches() {
    }

    public static PitchFeature normalizePitchFeature(PitchFeature feature) {
        PitchFeature safe = feature == null ? new PitchFeature(null, null, null) : feature;
        Integer octave = roundedFinite(safe.getOctave());
        Integer alter = roundedFinite(safe.getAlter());
        Integer normalizedOctave = octave == null ? Integer.valueOf(4)
                : Integer.valueOf(Math.max(0, Math.min(9, octave.intValue())));
        return new PitchFeature(normalizeStep(safe.getStep()),
                alter != null && alter.intValue() != 0 ? alter : null, normalizedOctave);
    }

    public static String buildMusicXmlPitchXml(PitchFeature feature) {
        PitchFeature normalized = normalizePitchFeature(feature);
        String alterXml = normalized.getAlter() == null ? "" : "<alter>" + normalized.getAlter() + "</alter>";
        return "<pitch><step>" + normalized.getStep() + "</step>" + alterXml + "<octave>" + normalized.getOctave()
                + "</octave></pitch>";
    }

    public static PitchFeature extractMusicXmlPitchFeature(Element pitch) {
        if (pitch == null) {
            return normalizePitchFeature(null);
        }
        return normalizePitchFeature(new PitchFeature(directChildText(pitch, "step"), directChildText(pitch, "alter"),
                directChildText(pitch, "octave")));
    }

    private static String normalizeStep(Object value) {
        String step = value == null ? "" : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        return step.matches("[A-G]") ? step : "C";
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

    public static final class PitchFeature {
        private final Object step;
        private final Object alter;
        private final Object octave;

        public PitchFeature(Object step, Object alter, Object octave) {
            this.step = step;
            this.alter = alter;
            this.octave = octave;
        }

        public Object getStep() {
            return step;
        }

        public Object getAlter() {
            return alter;
        }

        public Object getOctave() {
            return octave;
        }
    }
}
