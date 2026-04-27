package jp.igapyon.mikuscore.musicxml;

import java.util.List;

final class MusicXmlJson {
    private MusicXmlJson() {
    }

    static void appendStringArray(StringBuilder builder, List<String> values) {
        builder.append("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            appendString(builder, values.get(index));
        }
        builder.append("]");
    }

    static void appendNullableString(StringBuilder builder, String value) {
        if (value == null) {
            builder.append("null");
        } else {
            appendString(builder, value);
        }
    }

    static void appendString(StringBuilder builder, String value) {
        builder.append('"');
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            switch (ch) {
            case '"':
                builder.append("\\\"");
                break;
            case '\\':
                builder.append("\\\\");
                break;
            case '\b':
                builder.append("\\b");
                break;
            case '\f':
                builder.append("\\f");
                break;
            case '\n':
                builder.append("\\n");
                break;
            case '\r':
                builder.append("\\r");
                break;
            case '\t':
                builder.append("\\t");
                break;
            default:
                if (ch < 0x20) {
                    String hex = Integer.toHexString(ch);
                    builder.append("\\u");
                    for (int pad = hex.length(); pad < 4; pad++) {
                        builder.append('0');
                    }
                    builder.append(hex);
                } else {
                    builder.append(ch);
                }
                break;
            }
        }
        builder.append('"');
    }
}
