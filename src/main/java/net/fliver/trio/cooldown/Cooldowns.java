package net.fliver.trio.cooldown;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Cooldowns {
  private final ConcurrentHashMap<String, Long> until = new ConcurrentHashMap<>();

  public boolean ready(String key, UUID player, long durationMs) {
    if (key == null || player == null) {
      throw new IllegalArgumentException("key/player");
    }
    if (durationMs < 0) {
      throw new IllegalArgumentException("durationMs");
    }
    String id = id(key, player);
    long now = System.currentTimeMillis();
    Long end = until.get(id);
    if (end != null && end > now) {
      return false;
    }
    until.put(id, now + durationMs);
    return true;
  }

  public long remainingMs(String key, UUID player) {
    if (key == null || player == null) {
      throw new IllegalArgumentException("key/player");
    }
    Long end = until.get(id(key, player));
    if (end == null) {
      return 0L;
    }
    long left = end - System.currentTimeMillis();
    return Math.max(0L, left);
  }

  public void clear(String key, UUID player) {
    if (key == null || player == null) {
      throw new IllegalArgumentException("key/player");
    }
    until.remove(id(key, player));
  }

  public void clearAll(UUID player) {
    if (player == null) {
      throw new IllegalArgumentException("player");
    }
    String suffix = ":" + player;
    Iterator<Map.Entry<String, Long>> it = until.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Long> entry = it.next();
      if (entry.getKey().endsWith(suffix)) {
        it.remove();
      }
    }
  }

  private static String id(String key, UUID player) {
    return key + ":" + player;
  }
}
