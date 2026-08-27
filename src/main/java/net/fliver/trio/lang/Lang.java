package net.fliver.trio.lang;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Lang {
  private static final String DEFAULT_CODE = "en_US";
  private static final Pattern SAFE_CODE = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");

  private final YamlConfiguration messages;

  private Lang(YamlConfiguration messages) {
    this.messages = messages;
  }

  public static Lang load(JavaPlugin plugin, String requestedCode) {
    Logger logger = plugin.getLogger();
    File langFolder = new File(plugin.getDataFolder(), "lang");

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
      requested = new File(langFolder, DEFAULT_CODE + ".yml");
    }

    if (requested.isFile()) {
      return new Lang(YamlConfiguration.loadConfiguration(requested));
    }

    logger.warning("Could not read a language file from disk - loading bundled defaults.");
    InputStream bundled = plugin.getResource("lang/" + DEFAULT_CODE + ".yml");
    if (bundled == null) {
      return new Lang(new YamlConfiguration());
    }
    return new Lang(
        YamlConfiguration.loadConfiguration(new InputStreamReader(bundled, StandardCharsets.UTF_8)));
  }

  public boolean has(String key) {
    return messages.contains(key);
  }

  public String template(String key) {
    return messages.getString(key);
  }

  public String raw(String key, String... placeholders) {
    String template = messages.getString(key);
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
