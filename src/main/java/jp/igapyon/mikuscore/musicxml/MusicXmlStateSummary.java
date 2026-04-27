package jp.igapyon.mikuscore.musicxml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MusicXmlStateSummary {
    private final String title;
    private final int partCount;
    private final int measureCount;
    private final List<String> measureNumbers;
    private final List<String> voices;

    public MusicXmlStateSummary(String title, int partCount, int measureCount, List<String> measureNumbers,
            List<String> voices) {
        this.title = title;
        this.partCount = partCount;
        this.measureCount = measureCount;
        this.measureNumbers = Collections.unmodifiableList(new ArrayList<String>(measureNumbers));
        this.voices = Collections.unmodifiableList(new ArrayList<String>(voices));
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

    public List<String> getMeasureNumbers() {
        return measureNumbers;
    }

    public List<String> getVoices() {
        return voices;
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"kind\": \"musicxml_state_summary\",\n");
        builder.append("  \"title\": ");
        MusicXmlJson.appendNullableString(builder, title);
        builder.append(",\n");
        builder.append("  \"part_count\": ").append(partCount).append(",\n");
        builder.append("  \"measure_count\": ").append(measureCount).append(",\n");
        builder.append("  \"measure_numbers\": ");
        MusicXmlJson.appendStringArray(builder, measureNumbers);
        builder.append(",\n");
        builder.append("  \"voices\": ");
        MusicXmlJson.appendStringArray(builder, voices);
        builder.append("\n");
        builder.append("}\n");
        return builder.toString();
    }
}
