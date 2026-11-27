package com.chessmaster.data;

import java.util.*;

/**
 * Lightweight, standards-compliant RFC 8259 JSON parser and serializer with zero external dependencies.
 */
public final class JsonParser {
    private final String src;
    private int pos;

    private JsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    public static Object parse(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Empty JSON input");
        }
        JsonParser parser = new JsonParser(json);
        Object val = parser.parseValue();
        parser.skipWs();
        return val;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String json) {
        Object o = parse(json);
        if (o instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        throw new IllegalArgumentException("Expected JSON object");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object o) {
        return (o instanceof Map<?, ?> m) ? (Map<String, Object>) m : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object o) {
        return (o instanceof List<?> l) ? (List<Object>) l : null;
    }

    public static String asString(Object o, String def) {
        return (o instanceof String s) ? s : def;
    }

    public static int asInt(Object o, int def) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private Object parseValue() {
        skipWs();
        if (pos >= src.length()) throw new IllegalArgumentException("Unexpected end of JSON");
        char ch = src.charAt(pos);
        return switch (ch) {
            case '{' -> parseObj();
            case '[' -> parseArr();
            case '"' -> parseString();
            case 't' -> parseLiteral("true", Boolean.TRUE);
            case 'f' -> parseLiteral("false", Boolean.FALSE);
            case 'n' -> parseLiteral("null", null);
            default -> {
                if (ch == '-' || (ch >= '0' && ch <= '9')) yield parseNumber();
                throw new IllegalArgumentException("Unexpected char '" + ch + "' at pos " + pos);
            }
        };
    }

    private Map<String, Object> parseObj() {
        pos++; // '{'
        Map<String, Object> map = new LinkedHashMap<>();
        skipWs();
        if (peek('}')) { pos++; return map; }
        while (true) {
            skipWs();
            String key = parseString();
            skipWs();
            expect(':');
            Object val = parseValue();
            map.put(key, val);
            skipWs();
            if (peek(',')) { pos++; continue; }
            if (peek('}')) { pos++; break; }
            throw new IllegalArgumentException("Expected ',' or '}' at pos " + pos);
        }
        return map;
    }

    private List<Object> parseArr() {
        pos++; // '['
        List<Object> list = new ArrayList<>();
        skipWs();
        if (peek(']')) { pos++; return list; }
        while (true) {
            list.add(parseValue());
            skipWs();
            if (peek(',')) { pos++; continue; }
            if (peek(']')) { pos++; break; }
            throw new IllegalArgumentException("Expected ',' or ']' at pos " + pos);
        }
        return list;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char ch = src.charAt(pos++);
            if (ch == '"') return sb.toString();
            if (ch == '\\') {
                if (pos >= src.length()) throw new IllegalArgumentException("Unterminated escape");
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"', '\\', '/' -> sb.append(esc);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        String hex = src.substring(pos, pos + 4);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                    }
                    default -> sb.append(esc);
                }
            } else {
                sb.append(ch);
            }
        }
        throw new IllegalArgumentException("Unterminated string");
    }

    private Number parseNumber() {
        int start = pos;
        if (peek('-')) pos++;
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        boolean isFloat = false;
        if (peek('.')) {
            isFloat = true;
            pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        }
        if (peek('e') || peek('E')) {
            isFloat = true;
            pos++;
            if (peek('+') || peek('-')) pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        }
        String numStr = src.substring(start, pos);
        if (isFloat) return Double.parseDouble(numStr);
        long v = Long.parseLong(numStr);
        if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) return (int) v;
        return v;
    }

    private Object parseLiteral(String lit, Object value) {
        if (!src.startsWith(lit, pos)) {
            throw new IllegalArgumentException("Expected " + lit + " at pos " + pos);
        }
        pos += lit.length();
        return value;
    }

    private void skipWs() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
            else break;
        }
    }

    private boolean peek(char c) {
        return pos < src.length() && src.charAt(pos) == c;
    }

    private void expect(char c) {
        if (pos >= src.length() || src.charAt(pos) != c) {
            throw new IllegalArgumentException("Expected '" + c + "' at pos " + pos);
        }
        pos++;
    }

    public static String stringify(Object obj) {
        StringBuilder sb = new StringBuilder();
        writeJson(obj, sb);
        return sb.toString();
    }

    private static void writeJson(Object obj, StringBuilder sb) {
        if (obj == null) {
            sb.append("null");
        } else if (obj instanceof String s) {
            sb.append('"');
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"' -> sb.append("\\\"");
                    case '\\' -> sb.append("\\\\");
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> sb.append(c);
                }
            }
            sb.append('"');
        } else if (obj instanceof Number || obj instanceof Boolean) {
            sb.append(obj);
        } else if (obj instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeJson(String.valueOf(e.getKey()), sb);
                sb.append(':');
                writeJson(e.getValue(), sb);
            }
            sb.append('}');
        } else if (obj instanceof Collection<?> col) {
            sb.append('[');
            boolean first = true;
            for (Object item : col) {
                if (!first) sb.append(',');
                first = false;
                writeJson(item, sb);
            }
            sb.append(']');
        } else {
            writeJson(String.valueOf(obj), sb);
        }
    }
}
