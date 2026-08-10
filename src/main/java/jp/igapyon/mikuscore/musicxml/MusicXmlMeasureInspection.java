package jp.igapyon.mikuscore.musicxml;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MusicXmlMeasureInspection {
    private final String measureNumber;
    private final List<Measure> measures;

    public MusicXmlMeasureInspection(String measureNumber, List<Measure> measures) {
        this.measureNumber = measureNumber;
        this.measures = Collections.unmodifiableList(new ArrayList<Measure>(measures));
    }

    public String getMeasureNumber() {
        return measureNumber;
    }

    public List<Measure> getMeasures() {
        return measures;
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"kind\": \"musicxml_measure_inspection\",\n");
        builder.append("  \"measure_number\": ");
        MusicXmlJson.appendNullableString(builder, measureNumber);
        builder.append(",\n");
        builder.append("  \"measures\": [");
        for (int index = 0; index < measures.size(); index++) {
            if (index > 0) {
                builder.append(",");
            }
            builder.append("\n");
            measures.get(index).appendJson(builder, "    ");
        }
        if (!measures.isEmpty()) {
            builder.append("\n  ");
        }
        builder.append("]\n");
        builder.append("}\n");
        return builder.toString();
    }

    public static final class Measure {
        private final String partId;
        private final List<Note> notes;

        public Measure(String partId, List<Note> notes) {
            this.partId = partId;
            this.notes = Collections.unmodifiableList(new ArrayList<Note>(notes));
        }

        public String getPartId() {
            return partId;
        }

        public int getNoteCount() {
            return notes.size();
        }

        public List<Note> getNotes() {
            return notes;
        }

        private void appendJson(StringBuilder builder, String indent) {
            builder.append(indent).append("{\n");
            builder.append(indent).append("  \"part_id\": ");
            MusicXmlJson.appendNullableString(builder, partId);
            builder.append(",\n");
            builder.append(indent).append("  \"note_count\": ").append(notes.size()).append(",\n");
            builder.append(indent).append("  \"notes\": [");
            for (int index = 0; index < notes.size(); index++) {
                if (index > 0) {
                    builder.append(",");
                }
                builder.append("\n");
                notes.get(index).appendJson(builder, indent + "    ");
            }
            if (!notes.isEmpty()) {
                builder.append("\n").append(indent).append("  ");
            }
            builder.append("]\n");
            builder.append(indent).append("}");
        }
    }

    public static final class Note {
        private final String nodeId;
        private final Selector selector;
        private final String voice;
        private final Double duration;
        private final boolean rest;
        private final Pitch pitch;

        public Note(String nodeId, Selector selector, String voice, Double duration, boolean rest, Pitch pitch) {
            this.nodeId = nodeId;
            this.selector = selector;
            this.voice = voice;
            this.duration = duration;
            this.rest = rest;
            this.pitch = pitch;
        }

        public String getNodeId() {
            return nodeId;
        }

        public Selector getSelector() {
            return selector;
        }

        public String getVoice() {
            return voice;
        }

        public Double getDuration() {
            return duration;
        }

        public boolean isRest() {
            return rest;
        }

        public Pitch getPitch() {
            return pitch;
        }

        private void appendJson(StringBuilder builder, String indent) {
            builder.append(indent).append("{\n");
            builder.append(indent).append("  \"node_id\": ");
            MusicXmlJson.appendNullableString(builder, nodeId);
            builder.append(",\n");
            builder.append(indent).append("  \"selector\": ");
            selector.appendJson(builder, indent + "  ");
            builder.append(",\n");
            builder.append(indent).append("  \"voice\": ");
            MusicXmlJson.appendNullableString(builder, voice);
            builder.append(",\n");
            builder.append(indent).append("  \"duration\": ");
            appendNullableNumber(builder, duration);
            builder.append(",\n");
            builder.append(indent).append("  \"is_rest\": ").append(rest).append(",\n");
            builder.append(indent).append("  \"pitch\": ");
            if (pitch == null) {
                builder.append("null");
            } else {
                pitch.appendJson(builder, indent + "  ");
            }
            builder.append("\n");
            builder.append(indent).append("}");
        }
    }

    public static final class Selector {
        private final String partId;
        private final String measureNumber;
        private final int measureNoteIndex;
        private final String voice;
        private final Integer voiceNoteIndex;

        public Selector(String partId, String measureNumber, int measureNoteIndex, String voice, Integer voiceNoteIndex) {
            this.partId = partId;
            this.measureNumber = measureNumber;
            this.measureNoteIndex = measureNoteIndex;
            this.voice = voice;
            this.voiceNoteIndex = voiceNoteIndex;
        }

        public String getPartId() {
            return partId;
        }

        public String getMeasureNumber() {
            return measureNumber;
        }

        public int getMeasureNoteIndex() {
            return measureNoteIndex;
        }

        public String getVoice() {
            return voice;
        }

        public Integer getVoiceNoteIndex() {
            return voiceNoteIndex;
        }

        private void appendJson(StringBuilder builder, String indent) {
            builder.append("{\n");
            builder.append(indent).append("  \"part_id\": ");
            MusicXmlJson.appendNullableString(builder, partId);
            builder.append(",\n");
            builder.append(indent).append("  \"measure_number\": ");
            MusicXmlJson.appendNullableString(builder, measureNumber);
            builder.append(",\n");
            builder.append(indent).append("  \"measure_note_index\": ").append(measureNoteIndex).append(",\n");
            builder.append(indent).append("  \"voice\": ");
            MusicXmlJson.appendNullableString(builder, voice);
            builder.append(",\n");
            builder.append(indent).append("  \"voice_note_index\": ");
            appendNullableInteger(builder, voiceNoteIndex);
            builder.append("\n");
            builder.append(indent).append("}");
        }
    }

    public static final class Pitch {
        private final String step;
        private final Double alter;
        private final Double octave;

        public Pitch(String step, Double alter, Double octave) {
            this.step = step;
            this.alter = alter;
            this.octave = octave;
        }

        public String getStep() {
            return step;
        }

        public Integer getAlter() {
            return integralValueOrNull(alter);
        }

        public Integer getOctave() {
            return integralValueOrNull(octave);
        }

        /** Source-shaped numeric alter value for callers that need non-integer inspection data. */
        public Double getAlterNumber() {
            return alter;
        }

        /** Source-shaped numeric octave value for callers that need non-integer inspection data. */
        public Double getOctaveNumber() {
            return octave;
        }

        private void appendJson(StringBuilder builder, String indent) {
            builder.append("{\n");
            builder.append(indent).append("  \"step\": ");
            MusicXmlJson.appendNullableString(builder, step);
            builder.append(",\n");
            builder.append(indent).append("  \"alter\": ");
            appendNullableNumber(builder, alter);
            builder.append(",\n");
            builder.append(indent).append("  \"octave\": ");
            appendNullableNumber(builder, octave);
            builder.append("\n");
            builder.append(indent).append("}");
        }
    }

    private static void appendNullableInteger(StringBuilder builder, Integer value) {
        if (value == null) {
            builder.append("null");
        } else {
            builder.append(value.intValue());
        }
    }

    private static void appendNullableNumber(StringBuilder builder, Double value) {
        if (value == null || !Double.isFinite(value.doubleValue())) {
            builder.append("null");
            return;
        }
        double number = value.doubleValue();
        if (number == 0d) {
            builder.append('0');
            return;
        }
        double magnitude = Math.abs(number);
        if (magnitude >= 0.000001d && magnitude < 1.0e21d) {
            builder.append(BigDecimal.valueOf(number).stripTrailingZeros().toPlainString());
            return;
        }
        String text = Double.toString(number);
        int exponentIndex = Math.max(text.indexOf('E'), text.indexOf('e'));
        if (exponentIndex < 0) {
            builder.append(text);
            return;
        }
        String mantissa = text.substring(0, exponentIndex);
        if (mantissa.endsWith(".0")) {
            mantissa = mantissa.substring(0, mantissa.length() - 2);
        }
        int exponent = Integer.parseInt(text.substring(exponentIndex + 1));
        builder.append(mantissa).append('e').append(exponent >= 0 ? "+" : "").append(exponent);
    }

    private static Integer integralValueOrNull(Double value) {
        if (value == null || !Double.isFinite(value.doubleValue()) || value.doubleValue() != Math.rint(value.doubleValue())
                || value.doubleValue() < Integer.MIN_VALUE || value.doubleValue() > Integer.MAX_VALUE) {
            return null;
        }
        return Integer.valueOf((int) value.doubleValue());
    }
}
