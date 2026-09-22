package net.fliver.trio.cooldown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CooldownsStringTest {
  @Test
  void stringIdWorksLikeUuid() {
    Cooldowns cooldowns = new Cooldowns();
    assertTrue(cooldowns.ready("shop", "console", 5_000L));
    assertFalse(cooldowns.ready("shop", "console", 5_000L));
    assertTrue(cooldowns.remainingMs("shop", "console") > 0L);
    assertTrue(cooldowns.remainingSec("shop", "console") >= 1L);
    cooldowns.clear("shop", "console");
    assertEquals(0L, cooldowns.remainingMs("shop", "console"));
  }

  @Test
  void expiredEntriesGetPurged() throws Exception {
    Cooldowns cooldowns = new Cooldowns();
    assertTrue(cooldowns.ready("temp", "one", 1L));
    Thread.sleep(15L);
    assertEquals(1, cooldowns.purgeExpired());
    assertEquals(0L, cooldowns.remainingMs("temp", "one"));
    assertEquals(0, cooldowns.size());
  }

  @Test
  void clearAllDropsEveryKeyForId() {
    Cooldowns cooldowns = new Cooldowns();
    assertTrue(cooldowns.ready("a", "same", 5_000L));
    assertTrue(cooldowns.ready("b", "same", 5_000L));
    cooldowns.clearAll("same");
    assertEquals(0L, cooldowns.remainingMs("a", "same"));
    assertEquals(0L, cooldowns.remainingMs("b", "same"));
  }
}
