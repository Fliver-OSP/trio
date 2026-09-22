package net.fliver.trio.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
  private Json() {}

  public static Object parse(String input) {
    if (input == null) {
      throw new IllegalArgumentException("input");
    }
    Parser parser = new Parser(input);
    Object value = parser.readValue();
    parser.skipEmpty();
    if (!parser.end()) {
      throw new IllegalArgumentException("Trailing content after JSON value");
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> asObject(Object value) {
    if (value instanceof Map) {
      return (Map<String, Object>) value;
    }
    return new LinkedHashMap<String, Object>();
  }

  @SuppressWarnings("unchecked")
  public static List<Object> asList(Object value) {
    if (value instanceof List) {
      return (List<Object>) value;
    }
    return new ArrayList<Object>();
  }

  public static String asString(Object value, String def) {
    if (value instanceof String) {
      return (String) value;
    }
    if (value instanceof Number || value instanceof Boolean) {
      return String.valueOf(value);
    }
    return def;
  }

  public static String stringify(Object value) {
    StringBuilder out = new StringBuilder();
    write(value, out);
    return out.toString();
  }

  public static String stringify(Map<String, Object> values) {
    StringBuilder out = new StringBuilder();
    out.append('{');
    boolean first = true;
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      if (!first) {
        out.append(',');
      }
      first = false;
      writeString(entry.getKey() == null ? "" : entry.getKey(), out);
      out.append(':');
      write(entry.getValue(), out);
    }
    out.append('}');
    return out.toString();
  }

  private static void write(Object value, StringBuilder out) {
    if (value == null) {
      out.append("null");
    } else if (value instanceof String) {
      writeString((String) value, out);
    } else if (value instanceof Number || value instanceof Boolean) {
      out.append(String.valueOf(value));
    } else if (value instanceof Map) {
      out.append('{');
      boolean first = true;
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
        if (!first) {
          out.append(',');
        }
        first = false;
        writeString(String.valueOf(entry.getKey()), out);
        out.append(':');
        write(entry.getValue(), out);
      }
      out.append('}');
    } else if (value instanceof List) {
      out.append('[');
      boolean first = true;
      for (Object item : (List<?>) value) {
        if (!first) {
          out.append(',');
        }
        first = false;
        write(item, out);
      }
      out.append(']');
    } else {
      writeString(String.valueOf(value), out);
    }
  }

  private static void writeString(String value, StringBuilder out) {
    out.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '"' || c == '\\') {
        out.append('\\').append(c);
      } else if (c == '\n') {
        out.append("\\n");
      } else if (c == '\r') {
        out.append("\\r");
      } else if (c == '\t') {
        out.append("\\t");
      } else if (c < 0x20) {
        out.append(String.format("\\u%04x", (int) c));
      } else {
        out.append(c);
      }
    }
    out.append('"');
  }

  private static final class Parser {
    private final String text;
    private int pos;

    Parser(String text) {
      this.text = text;
    }

    boolean end() {
      return pos >= text.length();
    }

    void skipEmpty() {
      while (!end() && Character.isWhitespace(text.charAt(pos))) {
        pos++;
      }
    }

    Object readValue() {
      skipEmpty();
      if (end()) {
        throw new IllegalArgumentException("Empty JSON value");
      }
      char c = text.charAt(pos);
      if (c == '{') {
        return readObject();
      }
      if (c == '[') {
        return readArray();
      }
      if (c == '"') {
        return readString();
      }
      if (c == 't' || c == 'f' || c == 'n') {
        return readLiteral();
      }
      return readNumber();
    }

    Map<String, Object> readObject() {
      Map<String, Object> map = new LinkedHashMap<String, Object>();
      pos++;
      skipEmpty();
      if (!end() && text.charAt(pos) == '}') {
        pos++;
        return map;
      }
      while (true) {
        skipEmpty();
        if (end() || text.charAt(pos) != '"') {
          throw new IllegalArgumentException("Expected string key at " + pos);
        }
        String key = readString();
        skipEmpty();
        if (end() || text.charAt(pos) != ':') {
          throw new IllegalArgumentException("Expected : at " + pos);
        }
        pos++;
        map.put(key, readValue());
        skipEmpty();
        if (end()) {
          throw new IllegalArgumentException("Unclosed object");
        }
        char next = text.charAt(pos);
        if (next == '}') {
          pos++;
          return map;
        }
        if (next == ',') {
          pos++;
          continue;
        }
        throw new IllegalArgumentException("Expected , or } at " + pos);
      }
    }

    List<Object> readArray() {
      List<Object> list = new ArrayList<Object>();
      pos++;
      skipEmpty();
      if (!end() && text.charAt(pos) == ']') {
        pos++;
        return list;
      }
      while (true) {
        list.add(readValue());
        skipEmpty();
        if (end()) {
          throw new IllegalArgumentException("Unclosed array");
        }
        char next = text.charAt(pos);
        if (next == ']') {
          pos++;
          return list;
        }
        if (next == ',') {
          pos++;
          continue;
        }
        throw new IllegalArgumentException("Expected , or ] at " + pos);
      }
    }

    String readString() {
      pos++;
      StringBuilder out = new StringBuilder();
      while (!end()) {
        char c = text.charAt(pos++);
        if (c == '"') {
          return out.toString();
        }
        if (c == '\\' && !end()) {
          char esc = text.charAt(pos++);
          if (esc == 'n') {
            out.append('\n');
          } else if (esc == 'r') {
            out.append('\r');
          } else if (esc == 't') {
            out.append('\t');
          } else if (esc == 'u' && pos + 4 <= text.length()) {
            out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
            pos += 4;
          } else {
            out.append(esc);
          }
        } else {
          out.append(c);
        }
      }
      throw new IllegalArgumentException("Unclosed string");
    }

    Object readLiteral() {
      if (text.startsWith("true", pos)) {
        pos += 4;
        return Boolean.TRUE;
      }
      if (text.startsWith("false", pos)) {
        pos += 5;
        return Boolean.FALSE;
      }
      if (text.startsWith("null", pos)) {
        pos += 4;
        return null;
      }
      throw new IllegalArgumentException("Bad literal at " + pos);
    }

    Number readNumber() {
      int start = pos;
      while (!end()) {
        char c = text.charAt(pos);
        if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
          pos++;
        } else {
          break;
        }
      }
      String raw = text.substring(start, pos);
      try {
        if (raw.indexOf('.') >= 0 || raw.indexOf('e') >= 0 || raw.indexOf('E') >= 0) {
          return Double.valueOf(raw);
        }
        return Long.valueOf(raw);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Bad number: " + raw);
      }
    }
  }
}
