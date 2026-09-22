package net.fliver.trio.update;

import java.util.Map;
import java.util.function.Consumer;
import net.fliver.trio.http.Http;
import net.fliver.trio.json.Json;

public final class Updates {
  private Updates() {}

  public static boolean isNewer(String remote, String installed) {
    return compareVersions(normalize(remote), normalize(installed)) > 0;
  }

  public static String normalize(String version) {
    if (version == null) {
      return "";
    }
    String clean = version.trim();
    if (clean.startsWith("v") || clean.startsWith("V")) {
      clean = clean.substring(1);
    }
    return clean;
  }

  public static int compareVersions(String a, String b) {
    if (a == null) {
      a = "";
    }
    if (b == null) {
      b = "";
    }
    if (a.equalsIgnoreCase(b)) {
      return 0;
    }
    String[] first = splitCoreQualifier(a);
    String[] second = splitCoreQualifier(b);
    int core = compareNumericCore(first[0], second[0]);
    if (core != 0) {
      return core;
    }
    boolean firstQualifier = !first[1].isEmpty();
    boolean secondQualifier = !second[1].isEmpty();
    if (!firstQualifier && secondQualifier) {
      return 1;
    }
    if (firstQualifier && !secondQualifier) {
      return -1;
    }
    return first[1].compareToIgnoreCase(second[1]);
  }

  public static void check(
      Http http,
      String url,
      final String currentVersion,
      final Consumer<Release> onNewVersion,
      final Consumer<Throwable> onError) {
    if (http == null || url == null) {
      throw new IllegalArgumentException("http/url");
    }
    java.util.Map<String, String> headers = new java.util.HashMap<String, String>();
    headers.put("Accept", "application/json");
    http.getWithHeaders(
        url,
        headers,
        new Consumer<Http.Response>() {
          @Override
          public void accept(Http.Response response) {
            try {
              if (response == null || !response.ok()) {
                if (onError != null && response != null && response.status() != 404) {
                  onError.accept(
                      new java.io.IOException("Update feed HTTP " + response.status()));
                }
                return;
              }
              Release release = parseRelease(response.body());
              if (release == null) {
                return;
              }
              if (onNewVersion != null && isNewer(release.version, currentVersion)) {
                onNewVersion.accept(release);
              }
            } catch (Throwable e) {
              if (onError != null) {
                onError.accept(e);
              }
            }
          }
        },
        onError);
  }

  static Release parseRelease(String body) {
    if (body == null || body.trim().isEmpty()) {
      return null;
    }
    Map<String, Object> json = Json.asObject(Json.parse(body));
    String version = Json.asString(json.get("version"), "").trim();
    if (version.isEmpty()) {
      return null;
    }
    String downloadUrl = Json.asString(json.get("downloadUrl"), "").trim();
    String notes = Json.asString(json.get("notes"), "");
    return new Release(version, downloadUrl, notes);
  }

  private static String[] splitCoreQualifier(String version) {
    int dash = version.indexOf('-');
    if (dash < 0) {
      return new String[] {version, ""};
    }
    return new String[] {version.substring(0, dash), version.substring(dash + 1)};
  }

  private static int compareNumericCore(String a, String b) {
    String[] first = a.split("\\.");
    String[] second = b.split("\\.");
    int count = Math.max(first.length, second.length);
    for (int i = 0; i < count; i++) {
      int left = i < first.length ? parseSegment(first[i]) : 0;
      int right = i < second.length ? parseSegment(second[i]) : 0;
      if (left != right) {
        return Integer.compare(left, right);
      }
    }
    return 0;
  }

  private static int parseSegment(String part) {
    int end = 0;
    while (end < part.length() && part.charAt(end) >= '0' && part.charAt(end) <= '9') {
      end++;
    }
    if (end == 0) {
      return 0;
    }
    try {
      return Integer.parseInt(part.substring(0, end));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public static final class Release {
    public final String version;
    public final String downloadUrl;
    public final String notes;

    public Release(String version, String downloadUrl, String notes) {
      this.version = version;
      this.downloadUrl = downloadUrl == null ? "" : downloadUrl;
      this.notes = notes == null ? "" : notes;
    }
  }
}
