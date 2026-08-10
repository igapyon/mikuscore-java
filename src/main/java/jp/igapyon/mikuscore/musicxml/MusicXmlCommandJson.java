package jp.igapyon.mikuscore.musicxml;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MusicXmlCommandJson {
    private MusicXmlCommandJson() {
    }

    static Map<String, Object> parseObject(String text) {
        Object value = new Parser(text).parseDocument();
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("command JSON must be an object.");
        }
        return castMap(value);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> castMap(Object value) {
        if (!(value instanceof Map)) {
            return null;
        }
        return (Map<String, Object>) value;
    }

    static String stringValue(Map<String, Object> object, String key) {
        Object value = object.get(key);
        return value instanceof String ? (String) value : null;
    }

    static Integer intValue(Map<String, Object> object, String key) {
        Object value = object.get(key);
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            if (!Double.isNaN(number) && !Double.isInfinite(number) && Math.rint(number) == number
                    && number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) {
                return Integer.valueOf((int) number);
            }
        }
        return null;
    }

    /**
     * Mirrors {@code Number.isFinite(value) && Number.isInteger(value)} for a
     * parsed JSON number without imposing Java's 32-bit {@code int} range.
     */
    static Double finiteIntegerValue(Map<String, Object> object, String key) {
        Object value = object == null ? null : object.get(key);
        if (!(value instanceof Number)) {
            return null;
        }
        double number = ((Number) value).doubleValue();
        if (!Double.isFinite(number) || number != Math.rint(number)) {
            return null;
        }
        return Double.valueOf(number);
    }

    /**
     * Mirrors JavaScript {@code String(value)} for JSON values used at the
     * unchecked command boundary.  Validation keeps the original value type;
     * this conversion is only for diagnostics and DOM text assignment, where
     * the Node implementation invokes Web/JavaScript string conversion.
     */
    static String javascriptStringValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue() ? "true" : "false";
        }
        if (value instanceof Number) {
            return javascriptNumberText(((Number) value).doubleValue());
        }
        if (value instanceof Map) {
            return "[object Object]";
        }
        if (value instanceof List) {
            StringBuilder result = new StringBuilder();
            for (Object item : (List<?>) value) {
                if (result.length() > 0) {
                    result.append(',');
                }
                // Array#toString joins null/undefined as empty fields.
                if (item != null) {
                    result.append(javascriptStringValue(item));
                }
            }
            return result.toString();
        }
        return String.valueOf(value);
    }

    /** Serializes the JSON values accepted by the command boundary. */
    static String toJson(Object value) {
        StringBuilder result = new StringBuilder();
        appendJsonValue(result, value);
        return result.toString();
    }

    private static void appendJsonValue(StringBuilder result, Object value) {
        if (value == null) {
            result.append("null");
            return;
        }
        if (value instanceof String) {
            MusicXmlJson.appendString(result, (String) value);
            return;
        }
        if (value instanceof Boolean || value instanceof Number) {
            result.append(value);
            return;
        }
        if (value instanceof Map) {
            result.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) {
                    result.append(',');
                }
                first = false;
                MusicXmlJson.appendString(result, String.valueOf(entry.getKey()));
                result.append(':');
                appendJsonValue(result, entry.getValue());
            }
            result.append('}');
            return;
        }
        if (value instanceof List) {
            result.append('[');
            boolean first = true;
            for (Object item : (List<?>) value) {
                if (!first) {
                    result.append(',');
                }
                first = false;
                appendJsonValue(result, item);
            }
            result.append(']');
            return;
        }
        MusicXmlJson.appendString(result, String.valueOf(value));
    }

    private static String javascriptNumberText(double value) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (Double.isInfinite(value)) {
            return value < 0 ? "-Infinity" : "Infinity";
        }
        if (value == 0d) {
            return "0";
        }
        double magnitude = Math.abs(value);
        if (magnitude >= 0.000001d && magnitude < 1.0e21d) {
            return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        }
        String text = Double.toString(value);
        int exponentIndex = Math.max(text.indexOf('E'), text.indexOf('e'));
        if (exponentIndex < 0) {
            return text;
        }
        String mantissa = text.substring(0, exponentIndex);
        if (mantissa.endsWith(".0")) {
            mantissa = mantissa.substring(0, mantissa.length() - 2);
        }
        int exponent = Integer.parseInt(text.substring(exponentIndex + 1));
        return mantissa + "e" + (exponent >= 0 ? "+" : "") + exponent;
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text == null ? "" : text;
        }

        private Object parseDocument() {
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) {
                throw new IllegalArgumentException("Unexpected trailing JSON content at index " + index + ".");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= text.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON.");
            }
            char ch = text.charAt(index);
            if (ch == '{') {
                return parseObject();
            }
            if (ch == '[') {
                return parseArray();
            }
            if (ch == '"') {
                return parseString();
            }
            if (ch == '-' || Character.isDigit(ch)) {
                return parseNumber();
            }
            if (text.startsWith("null", index)) {
                index += 4;
                return null;
            }
            if (text.startsWith("true", index)) {
                index += 4;
                return Boolean.TRUE;
            }
            if (text.startsWith("false", index)) {
                index += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Unexpected JSON token at index " + index + ".");
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<String, Object>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                result.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return result;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> result = new ArrayList<Object>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return result;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch == '\\') {
                    if (index >= text.length()) {
                        throw new IllegalArgumentException("Invalid JSON escape.");
                    }
                    char escaped = text.charAt(index++);
                    switch (escaped) {
                    case '"':
                    case '\\':
                    case '/':
                        builder.append(escaped);
                        break;
                    case 'b':
                        builder.append('\b');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case 'u':
                        if (index + 4 > text.length()) {
                            throw new IllegalArgumentException("Invalid JSON unicode escape.");
                        }
                        builder.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                        index += 4;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid JSON escape.");
                    }
                } else {
                    if (ch < 0x20) {
                        throw new IllegalArgumentException("Invalid JSON control character.");
                    }
                    builder.append(ch);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string.");
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            if (index >= text.length()) {
                throw new IllegalArgumentException("Invalid JSON number at index " + start + ".");
            }
            if (peek('0')) {
                index++;
                if (index < text.length() && Character.isDigit(text.charAt(index))) {
                    throw new IllegalArgumentException("Invalid JSON number at index " + start + ".");
                }
            } else if (Character.isDigit(text.charAt(index))) {
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            } else {
                throw new IllegalArgumentException("Invalid JSON number at index " + start + ".");
            }
            if (peek('.')) {
                index++;
                int fractionStart = index;
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
                if (fractionStart == index) {
                    throw new IllegalArgumentException("Invalid JSON number at index " + start + ".");
                }
            }
            if (peek('e') || peek('E')) {
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                int exponentStart = index;
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
                if (exponentStart == index) {
                    throw new IllegalArgumentException("Invalid JSON number at index " + start + ".");
                }
            }
            try {
                return Double.valueOf(text.substring(start, index));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid JSON number at index " + start + ".", ex);
            }
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= text.length() || text.charAt(index) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at index " + index + ".");
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < text.length() && text.charAt(index) == expected;
        }

        private void skipWhitespace() {
            // JSON whitespace is deliberately narrower than Java's
            // Character.isWhitespace: JSON.parse accepts only SP, TAB, CR,
            // and LF outside strings.
            while (index < text.length() && isJsonWhitespace(text.charAt(index))) {
                index++;
            }
        }

        private static boolean isJsonWhitespace(char ch) {
            return ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n';
        }
    }
}
