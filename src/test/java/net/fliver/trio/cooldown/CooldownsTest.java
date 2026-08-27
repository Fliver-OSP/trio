package net.fliver.trio.cooldown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CooldownsTest {
  @Test
  void readyThenBlocksUntilClear() {
    Cooldowns cooldowns = new Cooldowns();
    UUID player = UUID.randomUUID();
    assertTrue(cooldowns.ready("hello", player, 5_000L));
    assertFalse(cooldowns.ready("hello", player, 5_000L));
    assertTrue(cooldowns.remainingMs("hello", player) > 0L);
    cooldowns.clear("hello", player);
    assertEquals(0L, cooldowns.remainingMs("hello", player));
    assertTrue(cooldowns.ready("hello", player, 1L));
  }
}
