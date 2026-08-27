package net.fliver.trio.http;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import net.fliver.trio.schedule.RegionScheduler;
import org.bukkit.plugin.java.JavaPlugin;

public final class Http {
  private static final String USER_AGENT = "Trio/0.5.1-beta";
  private static final int DEFAULT_MAX_BYTES = 4 * 1024 * 1024;

  private static final ExecutorService WORKERS =
      Executors.newCachedThreadPool(
          new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
              Thread t = new Thread(r, "trio-http");
              t.setDaemon(true);
              return t;
            }
          });

  private final JavaPlugin plugin;

  private Http(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public static Http of(JavaPlugin plugin) {
    return new Http(plugin);
  }

  public void get(String url, Consumer<Response> onMain, Consumer<Throwable> onError) {
    request(Request.get(url), onMain, onError);
  }

  public void request(
      final Request req, final Consumer<Response> onMain, final Consumer<Throwable> onError) {
    if (req == null) {
      throw new IllegalArgumentException("req");
    }
    WORKERS.execute(
        new Runnable() {
          @Override
          public void run() {
            try {
              final Response response = execute(req);
              runMain(
                  new Runnable() {
                    @Override
                    public void run() {
                      if (onMain != null) {
                        onMain.accept(response);
                      }
                    }
                  });
            } catch (final Throwable error) {
              runMain(
                  new Runnable() {
                    @Override
                    public void run() {
                      if (onError != null) {
                        onError.accept(error);
                      }
                    }
                  });
            }
          }
        });
  }

  private Response execute(Request req) throws Exception {
    HttpURLConnection connection = (HttpURLConnection) new URL(req.url()).openConnection();
    connection.setRequestMethod(req.method().toUpperCase());
    connection.setConnectTimeout((int) Math.min(Integer.MAX_VALUE, req.timeout().toMillis()));
    connection.setReadTimeout((int) Math.min(Integer.MAX_VALUE, req.timeout().toMillis()));
    connection.setRequestProperty("User-Agent", USER_AGENT);
    connection.setInstanceFollowRedirects(true);

    for (Map.Entry<String, String> header : req.headers().entrySet()) {
      connection.setRequestProperty(header.getKey(), header.getValue());
    }

    String method = req.method().toUpperCase();
    String body = req.body();
    boolean hasBody =
        body != null && !body.isEmpty() && !"GET".equals(method) && !"DELETE".equals(method);
    connection.setDoOutput(hasBody);
    if (hasBody) {
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      connection.setFixedLengthStreamingMode(bytes.length);
      OutputStream out = connection.getOutputStream();
      try {
        out.write(bytes);
      } finally {
        out.close();
      }
    }

    int status = connection.getResponseCode();
    InputStream stream =
        status >= 400 ? connection.getErrorStream() : connection.getInputStream();
    String responseBody = stream == null ? "" : readAll(stream, DEFAULT_MAX_BYTES);
    Map<String, List<String>> headers = connection.getHeaderFields();
    Map<String, List<String>> copy = new HashMap<String, List<String>>();
    if (headers != null) {
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        if (entry.getKey() == null) {
          continue;
        }
        copy.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
      }
    }
    connection.disconnect();
    return new Response(status, responseBody, Collections.unmodifiableMap(copy));
  }

  private void runMain(Runnable task) {
    RegionScheduler.runSync(plugin, task);
  }

  private static String readAll(InputStream stream, int maxBytes) throws Exception {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] chunk = new byte[4096];
    int read;
    int total = 0;
    try {
      while ((read = stream.read(chunk)) != -1) {
        total += read;
        if (total > maxBytes) {
          throw new Exception("response too large (limit " + maxBytes + " bytes)");
        }
        buffer.write(chunk, 0, read);
      }
    } finally {
      stream.close();
    }
    return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
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
      private final Map<String, String> headers = new HashMap<String, String>();
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
        return new Request(
            url, method, Collections.unmodifiableMap(new HashMap<String, String>(headers)), body, timeout);
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
