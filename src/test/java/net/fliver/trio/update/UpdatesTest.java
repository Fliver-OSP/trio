package net.fliver.trio.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpdatesTest {
  @Test
  void newerRemoteWins() {
    assertTrue(Updates.isNewer("1.2.1", "1.2.0"));
    assertFalse(Updates.isNewer("1.2.0", "1.2.0"));
    assertFalse(Updates.isNewer("1.1.9", "1.2.0"));
  }

  @Test
  void releaseBeatsBeta() {
    assertTrue(Updates.isNewer("1.2.0", "1.2.0-beta"));
    assertFalse(Updates.isNewer("1.2.0-beta", "1.2.0"));
  }

  @Test
  void leadingVIsIgnored() {
    assertEquals(0, Updates.compareVersions(Updates.normalize("v1.2.0"), Updates.normalize("1.2.0")));
    assertTrue(Updates.isNewer("v1.2.1", "1.2.0"));
  }

  @Test
  void parsesReleaseFeed() {
    Updates.Release release =
        Updates.parseRelease("{\"version\":\"2.0.0\",\"downloadUrl\":\"https://x/y.jar\"}");
    assertTrue(release != null);
    assertEquals("2.0.0", release.version);
    assertEquals("https://x/y.jar", release.downloadUrl);
  }
}
