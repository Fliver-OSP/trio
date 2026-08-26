package net.fliver.trio.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Http {
  private static final String USER_AGENT = "Trio/0.3.0-beta";

  private final JavaPlugin plugin;
  private final HttpClient client;

  private Http(JavaPlugin plugin) {
    this.plugin = plugin;
    this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  }

  public static Http of(JavaPlugin plugin) {
    return new Http(plugin);
  }

  public void get(String url, Consumer<Response> onMain, Consumer<Throwable> onError) {
    request(Request.get(url), onMain, onError);
  }

  public void request(Request req, Consumer<Response> onMain, Consumer<Throwable> onError) {
    if (req == null) {
      throw new IllegalArgumentException("req");
    }
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create(req.url()))
            .timeout(req.timeout())
            .header("User-Agent", USER_AGENT);

    for (Map.Entry<String, String> header : req.headers().entrySet()) {
      builder.header(header.getKey(), header.getValue());
    }

    String method = req.method().toUpperCase();
    String body = req.body();
    if (body == null || body.isEmpty() || method.equals("GET") || method.equals("DELETE")) {
      builder.method(method, HttpRequest.BodyPublishers.noBody());
    } else {
      builder.method(method, HttpRequest.BodyPublishers.ofString(body));
    }

    client
        .sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
        .whenComplete(
            (httpResponse, error) -> {
              if (error != null) {
                runMain(() -> {
                  if (onError != null) {
                    onError.accept(error);
                  }
                });
                return;
              }
              Response response = Response.from(httpResponse);
              runMain(() -> {
                if (onMain != null) {
                  onMain.accept(response);
                }
              });
            });
  }

  private void runMain(Runnable task) {
    if (Bukkit.isPrimaryThread()) {
      task.run();
    } else {
      Bukkit.getScheduler().runTask(plugin, task);
    }
  }

  public static final class Request {
    private final String url;
    private final String method;
    private final Map<String, String> headers;
    private final String body;
    private final Duration timeout;

    private Request(
        String url, String method, Map<String, String> headers, String body, Duration timeout) {
      this.url = url;
      this.method = method;
      this.headers = headers;
      this.body = body;
      this.timeout = timeout;
    }

    public static Request get(String url) {
      return builder(url).method("GET").build();
    }

    public static Builder builder(String url) {
      return new Builder(url);
    }

    public String url() {
      return url;
    }

    public String method() {
      return method;
    }

    public Map<String, String> headers() {
      return headers;
    }

    public String body() {
      return body;
    }

    public Duration timeout() {
      return timeout;
    }

    public static final class Builder {
      private final String url;
      private String method = "GET";
      private final Map<String, String> headers = new HashMap<>();
      private String body;
      private Duration timeout = Duration.ofSeconds(15);

      private Builder(String url) {
        this.url = Objects.requireNonNull(url, "url");
      }

      public Builder method(String method) {
        this.method = Objects.requireNonNull(method, "method");
        return this;
      }

      public Builder header(String name, String value) {
        headers.put(name, value);
        return this;
      }

      public Builder body(String body) {
        this.body = body;
        return this;
      }

      public Builder timeout(Duration timeout) {
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        return this;
      }

      public Request build() {
        return new Request(url, method, Map.copyOf(headers), body, timeout);
      }
    }
  }

  public static final class Response {
    private final int status;
    private final String body;
    private final Map<String, List<String>> headers;

    private Response(int status, String body, Map<String, List<String>> headers) {
      this.status = status;
      this.body = body;
      this.headers = headers;
    }

    private static Response from(HttpResponse<String> response) {
      Map<String, List<String>> map = new HashMap<>();
      response.headers().map().forEach((k, v) -> map.put(k, List.copyOf(v)));
      return new Response(response.statusCode(), response.body() == null ? "" : response.body(), Collections.unmodifiableMap(map));
    }

    public int status() {
      return status;
    }

    public String body() {
      return body;
    }

    public Map<String, List<String>> headers() {
      return headers;
    }
  }
}
