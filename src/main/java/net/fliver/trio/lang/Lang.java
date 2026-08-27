package net.fliver.trio.lang;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Lang {
  private static final String DEFAULT_CODE = "en_US";
  private static final Pattern SAFE_CODE = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");

  private final YamlConfiguration messages;
  private final YamlConfiguration defaults;

  private Lang(YamlConfiguration messages, YamlConfiguration defaults) {
    this.messages = messages;
    this.defaults = defaults != null ? defaults : new YamlConfiguration();
  }

  public static Lang load(JavaPlugin plugin, String requestedCode) {
    Logger logger = plugin.getLogger();
    File langFolder = new File(plugin.getDataFolder(), "lang");
    YamlConfiguration bundled = loadBundled(plugin, logger);

    File defaultOnDisk = new File(langFolder, DEFAULT_CODE + ".yml");
    if (!defaultOnDisk.exists()) {
      plugin.saveResource("lang/" + DEFAULT_CODE + ".yml", false);
    }

    String code = requestedCode;
    if (code == null || !SAFE_CODE.matcher(code).matches()) {
      logger.warning("Invalid language code - falling back to " + DEFAULT_CODE + ".");
      code = DEFAULT_CODE;
    }

    File requested = new File(langFolder, code + ".yml");
    if (!requested.isFile()) {
      if (!code.equals(DEFAULT_CODE)) {
        logger.warning(
            "Language file \"" + code + ".yml\" not found - falling back to " + DEFAULT_CODE + ".");
      }
      requested = defaultOnDisk;
    }

    YamlConfiguration disk;
    if (requested.isFile()) {
      disk = YamlConfiguration.loadConfiguration(requested);
    } else {
      logger.warning("Could not read a language file from disk - using bundled defaults only.");
      disk = new YamlConfiguration();
    }

    int added = mergeMissingKeys(disk, bundled);
    if (added > 0 && requested.getParentFile() != null) {
      try {
        if (!requested.getParentFile().exists()) {
          requested.getParentFile().mkdirs();
        }
        disk.save(requested);
        logger.info(
            "Added "
                + added
                + " missing language key(s) to lang/"
                + requested.getName()
                + " (existing translations kept).");
      } catch (Exception e) {
        logger.warning(
            "Could not write missing language keys to lang/"
                + requested.getName()
                + ": "
                + e.getMessage()
                + " — bundled defaults still used at runtime.");
      }
    }

    return new Lang(disk, bundled);
  }

  private static YamlConfiguration loadBundled(JavaPlugin plugin, Logger logger) {
    InputStream in = plugin.getResource("lang/" + DEFAULT_CODE + ".yml");
    if (in == null) {
      logger.warning("Bundled lang/" + DEFAULT_CODE + ".yml missing from jar.");
      return new YamlConfiguration();
    }
    return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
  }

  static int mergeMissingKeys(YamlConfiguration disk, YamlConfiguration bundled) {
    if (bundled == null) {
      return 0;
    }
    Set<String> keys = bundled.getKeys(true);
    int added = 0;
    for (String key : keys) {
      if (bundled.isConfigurationSection(key)) {
        continue;
      }
      if (!disk.contains(key)) {
        disk.set(key, bundled.get(key));
        added++;
      }
    }
    return added;
  }

  public boolean has(String key) {
    return resolveRaw(key) != null;
  }

  public String template(String key) {
    return resolveRaw(key);
  }

  public String raw(String key, String... placeholders) {
    String template = resolveRaw(key);
    if (template == null) {
      return key;
    }
    return apply(template, placeholders);
  }

  public String colored(String key, String... placeholders) {
    String text = raw(key, placeholders);
    if (AdventureBridge.looksLikeMiniMessage(text) && AdventureBridge.available()) {
      return AdventureBridge.toLegacy(text);
    }
    return ChatColor.translateAlternateColorCodes('&', text);
  }

  private String resolveRaw(String key) {
    String template = messages.getString(key);
    if (template == null) {
      template = defaults.getString(key);
    }
    return template;
  }

  private static String apply(String template, String... placeholders) {
    String out = template;
    if (placeholders == null) {
      return out;
    }
    for (int i = 0; i + 1 < placeholders.length; i += 2) {
      out = out.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
    }
    return out;
  }
}
