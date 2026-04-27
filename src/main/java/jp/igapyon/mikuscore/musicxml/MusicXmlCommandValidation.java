package jp.igapyon.mikuscore.musicxml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MusicXmlCommandValidation {
    private final boolean ok;
    private final boolean dirtyChanged;
    private final List<String> changedNodeIds;
    private final List<String> affectedMeasureNumbers;
    private final List<Diagnostic> diagnostics;

    public MusicXmlCommandValidation(boolean ok, boolean dirtyChanged, List<String> changedNodeIds,
            List<String> affectedMeasureNumbers, List<Diagnostic> diagnostics) {
        this.ok = ok;
        this.dirtyChanged = dirtyChanged;
        this.changedNodeIds = Collections.unmodifiableList(new ArrayList<String>(changedNodeIds));
        this.affectedMeasureNumbers = Collections.unmodifiableList(new ArrayList<String>(affectedMeasureNumbers));
        this.diagnostics = Collections.unmodifiableList(new ArrayList<Diagnostic>(diagnostics));
    }

    public boolean isOk() {
        return ok;
    }

    public List<String> getChangedNodeIds() {
        return changedNodeIds;
    }

    public List<String> getAffectedMeasureNumbers() {
        return affectedMeasureNumbers;
    }

    public List<Diagnostic> getDiagnostics() {
        return diagnostics;
    }

    public String toJson() {
        return toJsonWithKind("musicxml_command_validation", true);
    }

    public String toApplyJson() {
        return toJsonWithKind("musicxml_command_apply", false);
    }

    private String toJsonWithKind(String kind, boolean includeDirtyChanged) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"kind\": \"").append(kind).append("\",\n");
        builder.append("  \"ok\": ").append(ok).append(",\n");
        if (includeDirtyChanged) {
            builder.append("  \"dirty_changed\": ").append(dirtyChanged).append(",\n");
        }
        builder.append("  \"changed_node_ids\": ");
        MusicXmlJson.appendStringArray(builder, changedNodeIds);
        builder.append(",\n");
        builder.append("  \"affected_measure_numbers\": ");
        MusicXmlJson.appendStringArray(builder, affectedMeasureNumbers);
        builder.append(",\n");
        builder.append("  \"warnings\": [],\n");
        builder.append("  \"diagnostics\": [");
        for (int index = 0; index < diagnostics.size(); index++) {
            if (index > 0) {
                builder.append(",");
            }
            builder.append("\n");
            diagnostics.get(index).appendJson(builder, "    ");
        }
        if (!diagnostics.isEmpty()) {
            builder.append("\n  ");
        }
        builder.append("]\n");
        builder.append("}\n");
        return builder.toString();
    }

    public static final class Diagnostic {
        private final String code;
        private final String message;

        public Diagnostic(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        private void appendJson(StringBuilder builder, String indent) {
            builder.append(indent).append("{\n");
            builder.append(indent).append("  \"code\": ");
            MusicXmlJson.appendString(builder, code);
            builder.append(",\n");
            builder.append(indent).append("  \"message\": ");
            MusicXmlJson.appendString(builder, message);
            builder.append("\n");
            builder.append(indent).append("}");
        }
    }
}
