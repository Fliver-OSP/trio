package net.fliver.trio.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServerVersion implements Comparable<ServerVersion> {
  private static final Pattern TOKEN = Pattern.compile("(\\d+)");

  private final int[] parts;
  private final String raw;

  private ServerVersion(String raw, int[] parts) {
    this.raw = raw;
    this.parts = parts;
  }

  public static ServerVersion parse(String input) {
    if (input == null || input.isBlank()) {
      throw new IllegalArgumentException("version");
    }
    String trimmed = input.trim();
    Matcher matcher = TOKEN.matcher(trimmed);
    List<Integer> numbers = new ArrayList<>();
    while (matcher.find()) {
      numbers.add(Integer.parseInt(matcher.group(1)));
      if (numbers.size() >= 4) {
        break;
      }
    }
    if (numbers.isEmpty()) {
      throw new IllegalArgumentException("unparseable version: " + input);
    }
    int[] parts = new int[numbers.size()];
    for (int i = 0; i < numbers.size(); i++) {
      parts[i] = numbers.get(i);
    }
    return new ServerVersion(trimmed, parts);
  }

  public static ServerVersion of(int major, int minor) {
    return new ServerVersion(major + "." + minor, new int[] {major, minor});
  }

  public static ServerVersion of(int major, int minor, int patch) {
    return new ServerVersion(major + "." + minor + "." + patch, new int[] {major, minor, patch});
  }

  public boolean atLeast(ServerVersion other) {
    return compareTo(other) >= 0;
  }

  public boolean atMost(ServerVersion other) {
    return compareTo(other) <= 0;
  }

  public boolean between(ServerVersion minInclusive, ServerVersion maxInclusive) {
    return atLeast(minInclusive) && atMost(maxInclusive);
  }

  public String raw() {
    return raw;
  }

  public int part(int index) {
    if (index < 0 || index >= parts.length) {
      return 0;
    }
    return parts[index];
  }

  @Override
  public int compareTo(ServerVersion other) {
    Objects.requireNonNull(other, "other");
    int len = Math.max(parts.length, other.parts.length);
    for (int i = 0; i < len; i++) {
      int a = i < parts.length ? parts[i] : 0;
      int b = i < other.parts.length ? other.parts[i] : 0;
      if (a != b) {
        return Integer.compare(a, b);
      }
    }
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ServerVersion other)) {
      return false;
    }
    return compareTo(other) == 0;
  }

  @Override
  public int hashCode() {
    int hash = 1;
    for (int part : parts) {
      hash = 31 * hash + part;
    }
    return hash;
  }

  @Override
  public String toString() {
    return raw;
  }
}
