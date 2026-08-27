package net.fliver.trio.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServerVersionTest {
  @Test
  void parsesClassicAndYearDrop() {
    assertEquals(0, ServerVersion.parse("1.20.4").compareTo(ServerVersion.of(1, 20, 4)));
    assertEquals(0, ServerVersion.parse("26.2").compareTo(ServerVersion.of(26, 2)));
  }

  @Test
  void comparesAcrossRanges() {
    ServerVersion v120 = ServerVersion.parse("1.20.1");
    ServerVersion v121 = ServerVersion.parse("1.21.4");
    ServerVersion v262 = ServerVersion.parse("26.2");
    assertTrue(v121.atLeast(v120));
    assertTrue(v262.atLeast(v121));
    assertTrue(v120.between(ServerVersion.of(1, 20), ServerVersion.of(1, 21)));
    assertFalse(v262.between(ServerVersion.of(1, 20), ServerVersion.of(1, 21)));
  }

  @Test
  void stripsBukkitSuffix() {
    assertEquals(0, ServerVersion.parse("1.20.1-R0.1-SNAPSHOT").compareTo(ServerVersion.of(1, 20, 1)));
  }
}
