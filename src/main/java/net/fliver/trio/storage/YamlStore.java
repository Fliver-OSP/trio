package net.fliver.trio.storage;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class YamlStore {
  private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9_.-]{1,64}$");
  private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");

  private final JavaPlugin plugin;
  private final String name;
  private final File folder;
  private final ConcurrentHashMap<String, FileConfiguration> cache = new ConcurrentHashMap<>();

  private YamlStore(JavaPlugin plugin, String name) {
    this.plugin = plugin;
    this.name = name;
    this.folder = new File(plugin.getDataFolder(), "storage/" + name);
  }

  public static YamlStore of(JavaPlugin plugin, String name) {
    if (plugin == null) {
      throw new IllegalArgumentException("plugin");
    }
    if (name == null || !SAFE_NAME.matcher(name).matches()) {
      throw new IllegalArgumentException("name");
    }
    return new YamlStore(plugin, name);
  }

  public String name() {
    return name;
  }

  public boolean has(String id) {
    String safe = requireId(id);
    if (cache.containsKey(safe)) {
      return true;
    }
    return file(safe).isFile();
  }

  public FileConfiguration raw(String id) {
    return load(requireId(id));
  }

  public void set(String id, String path, Object value) {
    load(requireId(id)).set(path, value);
  }

  public String string(String id, String path, String def) {
    return load(requireId(id)).getString(path, def);
  }

  public int integer(String id, String path, int def) {
    return load(requireId(id)).getInt(path, def);
  }

  public boolean bool(String id, String path, boolean def) {
    return load(requireId(id)).getBoolean(path, def);
  }

  public double decimal(String id, String path, double def) {
    return load(requireId(id)).getDouble(path, def);
  }

  public void save(String id) {
    String safe = requireId(id);
    FileConfiguration cfg = load(safe);
    if (!folder.exists() && !folder.mkdirs()) {
      plugin.getLogger().warning("Could not create storage folder: " + folder.getAbsolutePath());
      return;
    }
    try {
      cfg.save(file(safe));
    } catch (IOException e) {
      plugin.getLogger().warning("Could not save storage \"" + name + "/" + safe + "\": " + e.getMessage());
    }
  }

  public void reload(String id) {
    String safe = requireId(id);
    cache.remove(safe);
    load(safe);
  }

  public void unload(String id) {
    cache.remove(requireId(id));
  }

  public void unloadAll() {
    cache.clear();
  }

  public void saveAll() {
    for (String id : cache.keySet()) {
      try {
        save(id);
      } catch (Throwable ignored) {
      }
    }
  }

  public boolean delete(String id) {
    String safe = requireId(id);
    cache.remove(safe);
    java.io.File target = file(safe);
    if (target.isFile()) {
      return target.delete();
    }
    return false;
  }

  public java.util.List<String> ids() {
    java.util.List<String> out = new java.util.ArrayList<String>();
    if (folder.isDirectory()) {
      java.io.File[] files = folder.listFiles();
      if (files != null) {
        for (java.io.File file : files) {
          String name = file.getName();
          if (name.endsWith(".yml")) {
            out.add(name.substring(0, name.length() - 4));
          }
        }
      }
    }
    for (String cached : cache.keySet()) {
      if (!out.contains(cached)) {
        out.add(cached);
      }
    }
    java.util.Collections.sort(out);
    return out;
  }

  private FileConfiguration load(String safe) {
    return cache.computeIfAbsent(
        safe,
        key -> {
          File target = file(key);
          if (target.isFile()) {
            return YamlConfiguration.loadConfiguration(target);
          }
          return new YamlConfiguration();
        });
  }

  private File file(String safe) {
    return new File(folder, safe + ".yml");
  }

  private static String requireId(String id) {
    if (id == null || !SAFE_ID.matcher(id).matches()) {
      throw new IllegalArgumentException("id");
    }
    return id;
  }
}
