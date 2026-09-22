package net.fliver.trio;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.fliver.trio.command.BrigadierCommands;
import net.fliver.trio.command.CommandRegistrar;
import net.fliver.trio.command.Commands;
import net.fliver.trio.config.Configs;
import net.fliver.trio.cooldown.Cooldowns;
import net.fliver.trio.depend.SoftDepends;
import net.fliver.trio.http.Http;
import net.fliver.trio.json.Json;
import net.fliver.trio.lang.Lang;
import net.fliver.trio.menu.Menus;
import net.fliver.trio.message.Messages;
import net.fliver.trio.perm.Permissions;
import net.fliver.trio.platform.Platform;
import net.fliver.trio.schedule.Scheduler;
import net.fliver.trio.storage.SqliteStore;
import net.fliver.trio.storage.YamlStore;
import net.fliver.trio.update.Updates;
import org.bukkit.plugin.java.JavaPlugin;

public final class Trio {
  private final JavaPlugin plugin;
  private final Platform platform;
  private final Configs configs;
  private final Cooldowns cooldowns;
  private final Http http;
  private final SoftDepends softDepends;
  private final Scheduler scheduler;
  private final Menus menus;
  private final Permissions permissions;
  private final ConcurrentHashMap<String, YamlStore> stores = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, SqliteStore> sqliteStores = new ConcurrentHashMap<>();
  private Lang lang;
  private Messages messages;

  private Trio(JavaPlugin plugin) {
    this.plugin = plugin;
    this.platform = Platform.detect();
    this.configs = Configs.of(plugin);
    this.cooldowns = new Cooldowns();
    this.http = Http.of(plugin);
    this.softDepends = new SoftDepends();
    this.scheduler = Scheduler.of(plugin);
    this.menus = Menus.of(plugin);
    this.permissions = new Permissions();
    this.platform.logEnvironment(plugin);
  }

  public static Trio create(JavaPlugin plugin) {
    if (plugin == null) {
      throw new IllegalArgumentException("plugin");
    }
    return new Trio(plugin);
  }

  public JavaPlugin plugin() {
    return plugin;
  }

  public Platform platform() {
    return platform;
  }

  public Configs configs() {
    return configs;
  }

  public Cooldowns cooldowns() {
    return cooldowns;
  }

  public Http http() {
    return http;
  }

  public SoftDepends softDepends() {
    return softDepends;
  }

  public Scheduler scheduler() {
    return scheduler;
  }

  public Menus menus() {
    return menus;
  }

  public Permissions permissions() {
    return permissions;
  }

  public YamlStore storage(String name) {
    return stores.computeIfAbsent(name, key -> YamlStore.of(plugin, key));
  }

  public SqliteStore sqlite(String name) {
    return sqliteStores.computeIfAbsent(name, key -> SqliteStore.of(plugin, key));
  }

  public Lang lang() {
    return lang;
  }

  public Messages messages() {
    return messages;
  }

  public Trio lang(Lang lang) {
    this.lang = lang;
    this.messages = lang == null ? null : Messages.of(lang);
    this.menus.lang(lang);
    return this;
  }

  public Trio loadLang(String code) {
    return lang(Lang.load(plugin, code));
  }

  public void bindCommand(String name, Commands.Tree tree) {
    Commands.bind(plugin, name, tree);
  }

  public boolean registerCommand(String pluginYmlName, BrigadierCommands.Node root) {
    return CommandRegistrar.register(plugin, pluginYmlName, root);
  }

  public Object parseJson(String input) {
    return Json.parse(input);
  }

  public String toJson(Map<String, Object> values) {
    return Json.stringify(values);
  }

  public void checkUpdates(
      String url, Consumer<Updates.Release> onNewVersion, Consumer<Throwable> onError) {
    String current;
    try {
      current = plugin.getDescription().getVersion();
    } catch (Throwable ignored) {
      current = "0";
    }
    Updates.check(http, url, current, onNewVersion, onError);
  }

  public void close() {
    try {
      scheduler.cancelAll();
    } catch (Throwable ignored) {
    }
    for (SqliteStore store : sqliteStores.values()) {
      try {
        store.close();
      } catch (Throwable ignored) {
      }
    }
    for (YamlStore store : stores.values()) {
      try {
        store.saveAll();
      } catch (Throwable ignored) {
      }
    }
    cooldowns.purgeExpired();
  }
}
