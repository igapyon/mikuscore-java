/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

public final class ScoreMeasureFlow {
    private ScoreMeasureFlow() {
    }

    public static String buildMusicXmlBackupXml(FlowInput input) {
        Integer duration = positiveRounded(input == null ? null : input.getDuration());
        return duration == null ? "" : "<backup><duration>" + duration + "</duration></backup>";
    }

    public static String buildMusicXmlForwardXml(FlowInput input) {
        Integer duration = positiveRounded(input == null ? null : input.getDuration());
        if (duration == null) {
            return "";
        }
        String voiceXml = input.getVoice() == null || String.valueOf(input.getVoice()).trim().length() == 0 ? ""
                : "<voice>" + xmlEscape(String.valueOf(input.getVoice())) + "</voice>";
        String staffXml = input.getStaff() == null || String.valueOf(input.getStaff()).trim().length() == 0 ? ""
                : "<staff>" + xmlEscape(String.valueOf(input.getStaff())) + "</staff>";
        return "<forward><duration>" + duration + "</duration>" + voiceXml + staffXml + "</forward>";
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

    private static String xmlEscape(String value) {
        return String.valueOf(value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    public static final class FlowInput {
        private final Object duration;
        private final Object voice;
        private final Object staff;

        public FlowInput(Object duration) {
            this(duration, null, null);
        }

        public FlowInput(Object duration, Object voice, Object staff) {
            this.duration = duration;
            this.voice = voice;
            this.staff = staff;
        }

        public Object getDuration() {
            return duration;
        }

        public Object getVoice() {
            return voice;
        }

        public Object getStaff() {
            return staff;
        }
    }
}
