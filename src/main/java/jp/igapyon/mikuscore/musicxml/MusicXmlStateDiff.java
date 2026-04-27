package jp.igapyon.mikuscore.musicxml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MusicXmlStateDiff {
    private final boolean changed;
    private final List<String> changedFields;
    private final List<String> changedMeasureNumbers;
    private final List<ChangedMeasure> changedMeasures;
    private final Summary before;
    private final Summary after;

    public MusicXmlStateDiff(boolean changed, List<String> changedFields, List<String> changedMeasureNumbers,
            List<ChangedMeasure> changedMeasures, Summary before, Summary after) {
        this.changed = changed;
        this.changedFields = Collections.unmodifiableList(new ArrayList<String>(changedFields));
        this.changedMeasureNumbers = Collections.unmodifiableList(new ArrayList<String>(changedMeasureNumbers));
        this.changedMeasures = Collections.unmodifiableList(new ArrayList<ChangedMeasure>(changedMeasures));
        this.before = before;
        this.after = after;
    }

    public boolean isChanged() {
        return changed;
    }

    public List<String> getChangedFields() {
        return changedFields;
    }

    public List<String> getChangedMeasureNumbers() {
        return changedMeasureNumbers;
    }

    public List<ChangedMeasure> getChangedMeasures() {
        return changedMeasures;
    }

    public Summary getBefore() {
        return before;
    }

    public Summary getAfter() {
        return after;
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"kind\": \"musicxml_state_diff\",\n");
        builder.append("  \"changed\": ").append(changed).append(",\n");
        builder.append("  \"changed_fields\": ");
        MusicXmlJson.appendStringArray(builder, changedFields);
        builder.append(",\n");
        builder.append("  \"changed_measure_numbers\": ");
        MusicXmlJson.appendStringArray(builder, changedMeasureNumbers);
        builder.append(",\n");
        builder.append("  \"changed_measures\": [");
        for (int index = 0; index < changedMeasures.size(); index++) {
            if (index > 0) {
                builder.append(",");
            }
            builder.append("\n");
            changedMeasures.get(index).appendJson(builder, "    ");
        }
        if (!changedMeasures.isEmpty()) {
            builder.append("\n  ");
        }
        builder.append("],\n");
        builder.append("  \"before\": ");
        before.appendJson(builder, "  ");
        builder.append(",\n");
        builder.append("  \"after\": ");
        after.appendJson(builder, "  ");
        builder.append("\n");
        builder.append("}\n");
        return builder.toString();
    }

    public static final class Summary {
        private final String title;
        private final int partCount;
        private final int measureCount;
        private final int noteCount;
        private final List<String> measureNumbers;

        public Summary(String title, int partCount, int measureCount, int noteCount, List<String> measureNumbers) {
            this.title = title;
            this.partCount = partCount;
            this.measureCount = measureCount;
            this.noteCount = noteCount;
            this.measureNumbers = Collections.unmodifiableList(new ArrayList<String>(measureNumbers));
        }

        public String getTitle() {
            return title;
        }

        public int getPartCount() {
            return partCount;
        }

        public int getMeasureCount() {
            return measureCount;
        }

        public int getNoteCount() {
            return noteCount;
        }

        public List<String> getMeasureNumbers() {
            return measureNumbers;
        }

        private void appendJson(StringBuilder builder, String indent) {
            builder.append("{\n");
            builder.append(indent).append("  \"title\": ");
            MusicXmlJson.appendNullableString(builder, title);
            builder.append(",\n");
            builder.append(indent).append("  \"part_count\": ").append(partCount).append(",\n");
            builder.append(indent).append("  \"measure_count\": ").append(measureCount).append(",\n");
            builder.append(indent).append("  \"note_count\": ").append(noteCount).append(",\n");
            builder.append(indent).append("  \"measure_numbers\": ");
            MusicXmlJson.appendStringArray(builder, measureNumbers);
            builder.append("\n");
            builder.append(indent).append("}");
        }

        boolean fieldEquals(Summary other, String fieldName) {
            if ("title".equals(fieldName)) {
                return title == null ? other.title == null : title.equals(other.title);
            }
            if ("part_count".equals(fieldName)) {
                return partCount == other.partCount;
            }
            if ("measure_count".equals(fieldName)) {
                return measureCount == other.measureCount;
            }
            if ("note_count".equals(fieldName)) {
                return noteCount == other.noteCount;
            }
            if ("measure_numbers".equals(fieldName)) {
                return measureNumbers.equals(other.measureNumbers);
            }
            return true;
        }
    }

    public static final class ChangedMeasure {
        private final String partId;
        private final String measureNumber;
        private final int beforeNoteCount;
        private final int afterNoteCount;

        public ChangedMeasure(String partId, String measureNumber, int beforeNoteCount, int afterNoteCount) {
            this.partId = partId;
            this.measureNumber = measureNumber;
            this.beforeNoteCount = beforeNoteCount;
            this.afterNoteCount = afterNoteCount;
        }

        public String getPartId() {
            return partId;
        }

        public String getMeasureNumber() {
            return measureNumber;
        }

        public int getBeforeNoteCount() {
            return beforeNoteCount;
        }

        public int getAfterNoteCount() {
            return afterNoteCount;
        }

        private void appendJson(StringBuilder builder, String indent) {
            builder.append(indent).append("{\n");
            builder.append(indent).append("  \"part_id\": ");
            MusicXmlJson.appendNullableString(builder, partId);
            builder.append(",\n");
            builder.append(indent).append("  \"measure_number\": ");
            MusicXmlJson.appendNullableString(builder, measureNumber);
            builder.append(",\n");
            builder.append(indent).append("  \"before_note_count\": ").append(beforeNoteCount).append(",\n");
            builder.append(indent).append("  \"after_note_count\": ").append(afterNoteCount).append("\n");
            builder.append(indent).append("}");
        }
    }
}
