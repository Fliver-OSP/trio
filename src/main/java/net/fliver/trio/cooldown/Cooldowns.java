package net.fliver.trio.cooldown;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Cooldowns {
  private final ConcurrentHashMap<String, Long> until = new ConcurrentHashMap<>();

  public boolean ready(String key, UUID player, long durationMs) {
    if (player == null) {
      throw new IllegalArgumentException("player");
    }
    return ready(key, player.toString(), durationMs);
  }

  public boolean ready(String key, String id, long durationMs) {
    if (key == null || id == null) {
      throw new IllegalArgumentException("key/id");
    }
    if (durationMs < 0) {
      throw new IllegalArgumentException("durationMs");
    }
    String full = id(key, id);
    long now = System.currentTimeMillis();
    Long end = until.get(full);
    if (end != null && end > now) {
      return false;
    }
    until.put(full, now + durationMs);
    return true;
  }

  public long remainingMs(String key, UUID player) {
    if (player == null) {
      throw new IllegalArgumentException("player");
    }
    return remainingMs(key, player.toString());
  }

  public long remainingMs(String key, String id) {
    if (key == null || id == null) {
      throw new IllegalArgumentException("key/id");
    }
    Long end = until.get(id(key, id));
    if (end == null) {
      return 0L;
    }
    long left = end - System.currentTimeMillis();
    if (left <= 0L) {
      until.remove(id(key, id), end);
      return 0L;
    }
    return left;
  }

  public long remainingSec(String key, UUID player) {
    return (remainingMs(key, player) + 999L) / 1000L;
  }

  public long remainingSec(String key, String id) {
    return (remainingMs(key, id) + 999L) / 1000L;
  }

  public void clear(String key, UUID player) {
    if (player == null) {
      throw new IllegalArgumentException("player");
    }
    clear(key, player.toString());
  }

  public void clear(String key, String id) {
    if (key == null || id == null) {
      throw new IllegalArgumentException("key/id");
    }
    until.remove(id(key, id));
  }

  public void clearAll(UUID player) {
    if (player == null) {
      throw new IllegalArgumentException("player");
    }
    clearAll(player.toString());
  }

  public void clearAll(String id) {
    if (id == null) {
      throw new IllegalArgumentException("id");
    }
    String suffix = ":" + id;
    Iterator<Map.Entry<String, Long>> it = until.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Long> entry = it.next();
      if (entry.getKey().endsWith(suffix)) {
        it.remove();
      }
    }
  }

  public int purgeExpired() {
    long now = System.currentTimeMillis();
    int removed = 0;
    Iterator<Map.Entry<String, Long>> it = until.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, Long> entry = it.next();
      if (entry.getValue() <= now) {
        it.remove();
        removed++;
      }
    }
    return removed;
  }

  public int size() {
    return until.size();
  }

  private static String id(String key, String id) {
    return key + ":" + id;
  }

  private static String id(String key, UUID player) {
    return key + ":" + player.toString();
  }
}
