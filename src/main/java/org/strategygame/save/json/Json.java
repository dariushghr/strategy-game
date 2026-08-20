package org.strategygame.save.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON بدون وابستگی خارجی. فقط همان نوعی که SaveCodec می‌نویسد را می‌خواند:
 * شیء، آرایه، رشته، عدد، بولین و null.
 */
public final class Json {

    private Json() { }

    public static final class Obj {
        private final Map<String, Object> map = new LinkedHashMap<>();

        public Obj put(String key, Object value) {
            map.put(key, value);
            return this;
        }

        public boolean has(String key) { return map.containsKey(key) && map.get(key) != null; }

        public Object raw(String key) { return map.get(key); }

        public String str(String key) {
            Object v = map.get(key);
            return v == null ? null : String.valueOf(v);
        }

        public String str(String key, String fallback) {
            String v = str(key);
            return v == null ? fallback : v;
        }

        public int i(String key) { return (int) lng(key); }

        public int i(String key, int fallback) {
            Object v = map.get(key);
            return v instanceof Number n ? n.intValue() : fallback;
        }

        public long lng(String key) {
            Object v = map.get(key);
            if (v instanceof Number n) return n.longValue();
            throw new IllegalArgumentException("عدد نیست: " + key);
        }

        public long lng(String key, long fallback) {
            Object v = map.get(key);
            return v instanceof Number n ? n.longValue() : fallback;
        }

        public double dbl(String key, double fallback) {
            Object v = map.get(key);
            return v instanceof Number n ? n.doubleValue() : fallback;
        }

        public boolean bool(String key) { return bool(key, false); }

        public boolean bool(String key, boolean fallback) {
            Object v = map.get(key);
            return v instanceof Boolean b ? b : fallback;
        }

        public Obj obj(String key) {
            Object v = map.get(key);
            return v instanceof Obj o ? o : null;
        }

        public Arr arr(String key) {
            Object v = map.get(key);
            return v instanceof Arr a ? a : new Arr();
        }

        public Map<String, Object> map() { return map; }
    }

    public static final class Arr {
        private final List<Object> items = new ArrayList<>();

        public Arr add(Object value) {
            items.add(value);
            return this;
        }

        public int size() { return items.size(); }

        public Object raw(int i) { return items.get(i); }

        public Obj obj(int i) {
            Object v = items.get(i);
            return v instanceof Obj o ? o : null;
        }

        public String str(int i) {
            Object v = items.get(i);
            return v == null ? null : String.valueOf(v);
        }

        public int i(int i) {
            Object v = items.get(i);
            return v instanceof Number n ? n.intValue() : 0;
        }

        public long lng(int i) {
            Object v = items.get(i);
            return v instanceof Number n ? n.longValue() : 0L;
        }
    }

    public static String stringify(Obj root) {
        StringBuilder sb = new StringBuilder();
        write(sb, root);
        return sb.toString();
    }

    public static Obj parseObject(String text) {
        Object v = new Parser(text).parseValue();
        if (!(v instanceof Obj obj)) {
            throw new IllegalArgumentException("ریشه باید شیء JSON باشد");
        }
        return obj;
    }

    private static void write(StringBuilder sb, Object value) {
        switch (value) {
            case null -> sb.append("null");
            case Obj obj -> {
                sb.append('{');
                boolean first = true;
                for (var e : obj.map().entrySet()) {
                    if (!first) sb.append(',');
                    first = false;
                    writeString(sb, e.getKey());
                    sb.append(':');
                    write(sb, e.getValue());
                }
                sb.append('}');
            }
            case Arr arr -> {
                sb.append('[');
                for (int i = 0; i < arr.size(); i++) {
                    if (i > 0) sb.append(',');
                    write(sb, arr.raw(i));
                }
                sb.append(']');
            }
            case String s -> writeString(sb, s);
            case Boolean b -> sb.append(b);
            case Integer n -> sb.append(n.intValue());
            case Long n -> sb.append(n.longValue());
            case Double n -> sb.append(n.doubleValue());
            case Float n -> sb.append(n.floatValue());
            default -> writeString(sb, String.valueOf(value));
        }
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            this.s = s == null ? "" : s;
        }

        Object parseValue() {
            skip();
            if (i >= s.length()) throw error("JSON خالی است");
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default  -> parseNumber();
            };
        }

        private Obj parseObject() {
            expect('{');
            Obj obj = new Obj();
            skip();
            if (peek('}')) { i++; return obj; }
            while (true) {
                skip();
                String key = parseString();
                skip();
                expect(':');
                obj.put(key, parseValue());
                skip();
                if (peek('}')) { i++; return obj; }
                expect(',');
            }
        }

        private Arr parseArray() {
            expect('[');
            Arr arr = new Arr();
            skip();
            if (peek(']')) { i++; return arr; }
            while (true) {
                arr.add(parseValue());
                skip();
                if (peek(']')) { i++; return arr; }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c != '\\') { sb.append(c); continue; }
                if (i >= s.length()) throw error("رشته ناقص است");
                char e = s.charAt(i++);
                switch (e) {
                    case '"' , '\\' , '/' -> sb.append(e);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (i + 4 > s.length()) throw error("unicode ناقص است");
                        int cp = Integer.parseInt(s.substring(i, i + 4), 16);
                        sb.append((char) cp);
                        i += 4;
                    }
                    default -> throw error("escape نامعتبر");
                }
            }
            throw error("رشته بسته نشده");
        }

        private Object parseNumber() {
            int start = i;
            if (peek('-')) i++;
            while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            boolean frac = false;
            if (peek('.')) {
                frac = true;
                i++;
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            }
            if (i == start) throw error("عدد نامعتبر");
            String n = s.substring(start, i);
            try {
                return frac ? Double.parseDouble(n) : Long.parseLong(n);
            } catch (NumberFormatException e) {
                throw error("عدد نامعتبر");
            }
        }

        private Object parseLiteral(String lit, Object value) {
            if (!s.startsWith(lit, i)) throw error("مقدار نامعتبر");
            i += lit.length();
            return value;
        }

        private void skip() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') i++;
                else break;
            }
        }

        private boolean peek(char c) { return i < s.length() && s.charAt(i) == c; }

        private void expect(char c) {
            skip();
            if (!peek(c)) throw error("انتظار '" + c + "'");
            i++;
        }

        private IllegalArgumentException error(String msg) {
            return new IllegalArgumentException(msg + " (موقعیت " + i + ")");
        }
    }
}
