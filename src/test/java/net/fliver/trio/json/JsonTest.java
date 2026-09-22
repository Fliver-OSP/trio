package net.fliver.trio.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {
  @Test
  void parsesFlatObject() {
    Object parsed = Json.parse("{\"version\":\"1.2.3\",\"downloadUrl\":\"https://x/y\"}");
    Map<String, Object> obj = Json.asObject(parsed);
    assertEquals("1.2.3", Json.asString(obj.get("version"), ""));
    assertEquals("https://x/y", Json.asString(obj.get("downloadUrl"), ""));
  }

  @Test
  void parsesArrayAndLiterals() {
    Object parsed = Json.parse("[1, true, null, \"hi\"]");
    List<Object> list = Json.asList(parsed);
    assertEquals(4, list.size());
    assertEquals("hi", Json.asString(list.get(3), ""));
  }

  @Test
  void roundTripKeepsValues() {
    Map<String, Object> values = new java.util.LinkedHashMap<String, Object>();
    values.put("name", "hello");
    values.put("count", Long.valueOf(3));
    values.put("on", Boolean.TRUE);
    String text = Json.stringify(values);
    Map<String, Object> back = Json.asObject(Json.parse(text));
    assertEquals("hello", Json.asString(back.get("name"), ""));
    assertTrue(text.indexOf("count") >= 0);
  }
}
