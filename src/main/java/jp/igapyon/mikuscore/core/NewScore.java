/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.core;

import java.util.Collections;
import java.util.List;

import jp.igapyon.mikuscore.scorefeatures.ScoreClefs;

/** Runtime-independent MusicXML generator corresponding to {@code new-score.ts}. */
public final class NewScore {
    private static final int DEFAULT_DIVISIONS = 480;
    private static final int DEFAULT_MEASURE_COUNT = 8;
    private static final int MAX_PARTS = 16;

    private NewScore() {
    }

    public static String createNewScoreMusicXml() {
        return createNewScoreMusicXml(new Options());
    }

    public static String createNewScoreMusicXml(Options options) {
        Options safe = options == null ? new Options() : options;
        boolean pianoGrandStaff = safe.isUsePianoGrandStaffTemplate();
        int partCount = pianoGrandStaff ? 1 : boundedInteger(safe.getPartCount(), 1, 1, MAX_PARTS);
        int fifths = boundedInteger(safe.getFifths(), 0, -7, 7);
        int beats = boundedInteger(safe.getBeats(), 4, 1, 16);
        int beatType = normalizeBeatType(safe.getBeatType());
        int measureDuration = Math.max(1, Math.round(DEFAULT_DIVISIONS * beats * (4.0f / beatType)));

        StringBuilder partList = new StringBuilder();
        StringBuilder parts = new StringBuilder();
        for (int index = 0; index < partCount; index++) {
            int partNumber = index + 1;
            String partId = "P" + partNumber;
            int channelCandidate = (index % 16) + 1;
            int midiChannel = channelCandidate == 10 ? 11 : channelCandidate;
            int midiProgram = pianoGrandStaff ? 1 : 6;
            String partName = pianoGrandStaff ? "Piano" : "Part " + partNumber;
            partList.append("<score-part id=\"").append(partId).append("\"><part-name>")
                    .append(partName).append("</part-name><midi-instrument id=\"").append(partId)
                    .append("-I1\"><midi-channel>").append(midiChannel).append("</midi-channel><midi-program>")
                    .append(midiProgram).append("</midi-program></midi-instrument></score-part>");

            parts.append("<part id=\"").append(partId).append("\">");
            for (int measureIndex = 0; measureIndex < DEFAULT_MEASURE_COUNT; measureIndex++) {
                parts.append("<measure number=\"").append(measureIndex + 1).append("\">");
                if (measureIndex == 0) {
                    parts.append("<attributes><divisions>").append(DEFAULT_DIVISIONS)
                            .append("</divisions><key><fifths>").append(fifths)
                            .append("</fifths><mode>major</mode></key><time><beats>").append(beats)
                            .append("</beats><beat-type>").append(beatType).append("</beat-type></time>");
                    if (pianoGrandStaff) {
                        parts.append("<staves>2</staves><clef number=\"1\"><sign>G</sign><line>2</line></clef>")
                                .append("<clef number=\"2\"><sign>F</sign><line>4</line></clef>");
                    } else {
                        parts.append(buildClefXml(clefAt(safe.getClefs(), index)));
                    }
                    parts.append("</attributes>");
                }
                if (pianoGrandStaff) {
                    parts.append(buildMeasureRestNoteXml(measureDuration, Integer.valueOf(1)))
                            .append("<backup><duration>").append(measureDuration).append("</duration></backup>")
                            .append(buildMeasureRestNoteXml(measureDuration, Integer.valueOf(2)));
                } else {
                    parts.append(buildMeasureRestNoteXml(measureDuration, null));
                }
                parts.append("</measure>");
            }
            parts.append("</part>");
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<score-partwise version=\"3.1\">\n  <work>\n    <work-title>Untitled</work-title>\n  </work>\n"
                + "  <identification>\n    <creator type=\"composer\">Unknown</creator>\n  </identification>\n"
                + "  <part-list>" + partList + "</part-list>\n  " + parts + "\n</score-partwise>";
    }

    private static Object clefAt(List<?> clefs, int index) {
        return clefs == null || index < 0 || index >= clefs.size() ? null : clefs.get(index);
    }

    private static String buildClefXml(Object clef) {
        String normalized = String.valueOf(clef == null ? "" : clef).trim().toLowerCase();
        if ("alto".equals(normalized)) {
            return ScoreClefs.buildMusicXmlClefXml(new ScoreClefs.ClefFeature("C", Integer.valueOf(3)));
        }
        if ("bass".equals(normalized)) {
            return ScoreClefs.buildMusicXmlClefXml(new ScoreClefs.ClefFeature("F", Integer.valueOf(4)));
        }
        return ScoreClefs.buildMusicXmlClefXml(new ScoreClefs.ClefFeature("G", Integer.valueOf(2)));
    }

    private static String buildMeasureRestNoteXml(int duration, Integer staff) {
        return "<note><rest measure=\"yes\"/><duration>" + duration + "</duration><voice>1</voice>"
                + (staff == null ? "" : "<staff>" + staff + "</staff>") + "</note>";
    }

    private static int boundedInteger(Object value, int fallback, int min, int max) {
        Double parsed = toFiniteNumber(value);
        if (parsed == null) {
            return fallback;
        }
        return Math.max(min, Math.min(max, (int) Math.round(parsed.doubleValue())));
    }

    private static int normalizeBeatType(Object value) {
        Double parsed = toFiniteNumber(value);
        if (parsed == null) {
            return 4;
        }
        int candidate = (int) parsed.doubleValue();
        return parsed.doubleValue() == candidate && (candidate == 2 || candidate == 4 || candidate == 8 || candidate == 16)
                ? candidate
                : 4;
    }

    private static Double toFiniteNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? Double.valueOf(1) : Double.valueOf(0);
        }
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            return Double.isNaN(number) || Double.isInfinite(number) ? null : Double.valueOf(number);
        }
        String text = String.valueOf(value).trim();
        if (text.length() == 0) {
            return Double.valueOf(0);
        }
        try {
            if (text.startsWith("0x") || text.startsWith("0X")) {
                return Double.valueOf(Long.parseLong(text.substring(2), 16));
            }
            if (text.startsWith("0b") || text.startsWith("0B")) {
                return Double.valueOf(Long.parseLong(text.substring(2), 2));
            }
            if (text.startsWith("0o") || text.startsWith("0O")) {
                return Double.valueOf(Long.parseLong(text.substring(2), 8));
            }
            double number = Double.parseDouble(text);
            return Double.isNaN(number) || Double.isInfinite(number) ? null : Double.valueOf(number);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Immutable public options corresponding to {@code CreateNewScoreOptions}. */
    public static final class Options {
        private final boolean usePianoGrandStaffTemplate;
        private final Object partCount;
        private final Object fifths;
        private final Object beats;
        private final Object beatType;
        private final List<?> clefs;

        public Options() {
            this(false, null, null, null, null, Collections.emptyList());
        }

        public Options(boolean usePianoGrandStaffTemplate, Object partCount, Object fifths, Object beats,
                Object beatType, List<?> clefs) {
            this.usePianoGrandStaffTemplate = usePianoGrandStaffTemplate;
            this.partCount = partCount;
            this.fifths = fifths;
            this.beats = beats;
            this.beatType = beatType;
            this.clefs = clefs == null ? Collections.emptyList() : Collections.unmodifiableList(clefs);
        }

        public boolean isUsePianoGrandStaffTemplate() {
            return usePianoGrandStaffTemplate;
        }

        public Object getPartCount() {
            return partCount;
        }

        public Object getFifths() {
            return fifths;
        }

        public Object getBeats() {
            return beats;
        }

        public Object getBeatType() {
            return beatType;
        }

        public List<?> getClefs() {
            return clefs;
        }
    }
}
