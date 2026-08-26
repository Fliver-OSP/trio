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

  public FileConfiguration raw() {
    return plugin.getConfig();
  }

  public String string(String path, String def) {
    return raw().getString(path, def);
  }

  public int integer(String path, int def) {
    return raw().getInt(path, def);
  }

  public boolean bool(String path, boolean def) {
    return raw().getBoolean(path, def);
  }

  public double decimal(String path, double def) {
    return raw().getDouble(path, def);
  }
}
