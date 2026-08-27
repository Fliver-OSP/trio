package net.fliver.trio.platform;

import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class Platform {
  public enum Kind {
    PAPER,
    PURPUR,
    FOLIA,
    SPIGOT,
    UNKNOWN
  }

  private static final ServerVersion V_1_20_5 = ServerVersion.of(1, 20, 5);
  private static final ServerVersion V_1_20_6 = ServerVersion.of(1, 20, 6);
  private static final ServerVersion V_1_21 = ServerVersion.of(1, 21);
  private static final ServerVersion V_26_1 = ServerVersion.of(26, 1);

  private static volatile Platform cached;

  private final Kind kind;
  private final ServerVersion minecraftVersion;
  private final boolean folia;
  private final boolean paper;
  private final boolean purpur;
  private final boolean brigadierLifecycle;
  private final int recommendedJava;
  private volatile boolean logged;

  private Platform(
      Kind kind,
      ServerVersion minecraftVersion,
      boolean folia,
      boolean paper,
      boolean purpur,
      boolean brigadierLifecycle,
      int recommendedJava) {
    this.kind = kind;
    this.minecraftVersion = minecraftVersion;
    this.folia = folia;
    this.paper = paper;
    this.purpur = purpur;
    this.brigadierLifecycle = brigadierLifecycle;
    this.recommendedJava = recommendedJava;
  }

  public static Platform detect() {
    Platform local = cached;
    if (local != null) {
      return local;
    }
    synchronized (Platform.class) {
      if (cached != null) {
        return cached;
      }
      boolean folia = classPresent("io.papermc.paper.threadedregions.RegionizedServer");
      boolean purpur =
          classPresent("org.purpurmc.purpur.PurpurConfig")
              || classPresent("org.purpurmc.purpur.PurpurServer");
      boolean paper =
          folia
              || purpur
              || classPresent("com.destroystokyo.paper.PaperConfig")
              || classPresent("io.papermc.paper.configuration.Configuration")
              || Bukkit.getName().toLowerCase(Locale.ROOT).contains("paper");
      boolean spigot = classPresent("org.spigotmc.SpigotConfig");

      Kind kind;
      if (folia) {
        kind = Kind.FOLIA;
      } else if (purpur) {
        kind = Kind.PURPUR;
      } else if (paper) {
        kind = Kind.PAPER;
      } else if (spigot) {
        kind = Kind.SPIGOT;
      } else {
        kind = Kind.UNKNOWN;
      }

      ServerVersion version = readMinecraftVersion();
      boolean brigadier = probeBrigadierLifecycle();
      int java = recommendedJavaFor(version);
      cached =
          new Platform(kind, version, folia, paper || folia || purpur, purpur, brigadier, java);
      return cached;
    }
  }

  public static void resetForTests() {
    cached = null;
  }

  public Kind kind() {
    return kind;
  }

  public ServerVersion minecraftVersion() {
    return minecraftVersion;
  }

  public boolean isFolia() {
    return folia;
  }

  public boolean isPaper() {
    return paper;
  }

  public boolean isPurpur() {
    return purpur;
  }

  public boolean supportsBrigadierLifecycle() {
    return brigadierLifecycle;
  }

  public int recommendedJava() {
    return recommendedJava;
  }

  public void logEnvironment(Plugin plugin) {
    if (logged || plugin == null) {
      return;
    }
    logged = true;
    Logger log = plugin.getLogger();
    int runtime = Runtime.version().feature();
    StringBuilder line = new StringBuilder();
    line.append("Trio on ")
        .append(kind.name())
        .append(' ')
        .append(minecraftVersion.raw())
        .append(", Java ")
        .append(runtime)
        .append(" (recommended ")
        .append(recommendedJava)
        .append(')');
    if (purpur && kind != Kind.PURPUR) {
      line.append(" [Purpur]");
    }
    log.info(line.toString());
    if (runtime < recommendedJava) {
      log.warning(
          "Server Java "
              + runtime
              + " is below the recommended Java "
              + recommendedJava
              + " for Minecraft "
              + minecraftVersion.raw()
              + ".");
    }
  }

  private static ServerVersion readMinecraftVersion() {
    try {
      Object version = Bukkit.getServer().getClass().getMethod("getMinecraftVersion").invoke(Bukkit.getServer());
      if (version instanceof String text && !text.isBlank()) {
        return ServerVersion.parse(text);
      }
    } catch (ReflectiveOperationException ignored) {
      // older servers
    }
    try {
      String bukkit = Bukkit.getBukkitVersion();
      int dash = bukkit.indexOf('-');
      String core = dash > 0 ? bukkit.substring(0, dash) : bukkit;
      return ServerVersion.parse(core);
    } catch (Throwable t) {
      return ServerVersion.of(1, 20);
    }
  }

  private static int recommendedJavaFor(ServerVersion version) {
    if (version.atLeast(V_26_1)) {
      return 25;
    }
    if (version.atLeast(V_1_21) || version.atLeast(V_1_20_5)) {
      return 21;
    }
    return 17;
  }

  private static boolean probeBrigadierLifecycle() {
    if (!classPresent("io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents")) {
      return false;
    }
    try {
      Class<?> events =
          Class.forName("io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents");
      events.getField("COMMANDS");
      return true;
    } catch (ReflectiveOperationException e) {
      return false;
    }
  }

  private static boolean classPresent(String name) {
    try {
      Class.forName(name);
      return true;
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      return false;
    }
  }

  /** Exposed for tests that cannot boot a server. */
  static Platform forTests(
      Kind kind,
      ServerVersion version,
      boolean folia,
      boolean paper,
      boolean purpur,
      boolean brigadier,
      int recommendedJava) {
    return new Platform(kind, version, folia, paper, purpur, brigadier, recommendedJava);
  }

  static boolean supportsBrigadierLifecycleFor(ServerVersion version) {
    return version.atLeast(V_1_20_6);
  }
}
