package net.fliver.trio.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Configs {
  private final JavaPlugin plugin;

  private Configs(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public static Configs of(JavaPlugin plugin) {
    return new Configs(plugin);
  }

  public Configs saveDefaults() {
    plugin.saveDefaultConfig();
    return this;
  }

  public Configs reload() {
    plugin.reloadConfig();
    return this;
  }

  public Configs save() {
    plugin.saveConfig();
    return this;
  }

  public FileConfiguration raw() {
    return plugin.getConfig();
  }

  public boolean has(String path) {
    if (path == null) {
      return false;
    }
    return raw().contains(path);
  }

  public String string(String path, String def) {
    return raw().getString(path, def);
  }

  public int integer(String path, int def) {
    return raw().getInt(path, def);
  }

  public int integer(String path, int def, int min, int max) {
    int value = raw().getInt(path, def);
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }

  public long longValue(String path, long def) {
    return raw().getLong(path, def);
  }

  public boolean bool(String path, boolean def) {
    return raw().getBoolean(path, def);
  }

  public double decimal(String path, double def) {
    return raw().getDouble(path, def);
  }

  public java.util.List<String> stringList(String path) {
    java.util.List<String> found = raw().getStringList(path);
    return found == null ? new java.util.ArrayList<String>() : found;
  }

  public Configs set(String path, Object value) {
    raw().set(path, value);
    return this;
  }

  public Configs setAndSave(String path, Object value) {
    raw().set(path, value);
    save();
    return this;
  }
}
