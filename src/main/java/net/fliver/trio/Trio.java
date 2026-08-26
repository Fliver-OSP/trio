package net.fliver.trio;

import java.util.concurrent.ConcurrentHashMap;
import net.fliver.trio.command.Commands;
import net.fliver.trio.config.Configs;
import net.fliver.trio.cooldown.Cooldowns;
import net.fliver.trio.depend.SoftDepends;
import net.fliver.trio.http.Http;
import net.fliver.trio.lang.Lang;
import net.fliver.trio.message.Messages;
import net.fliver.trio.storage.YamlStore;
import org.bukkit.plugin.java.JavaPlugin;

public final class Trio {
  private final JavaPlugin plugin;
  private final Configs configs;
  private final Cooldowns cooldowns;
  private final Http http;
  private final SoftDepends softDepends;
  private final ConcurrentHashMap<String, YamlStore> stores = new ConcurrentHashMap<>();
  private Lang lang;
  private Messages messages;

  private Trio(JavaPlugin plugin) {
    this.plugin = plugin;
    this.configs = Configs.of(plugin);
    this.cooldowns = new Cooldowns();
    this.http = Http.of(plugin);
    this.softDepends = new SoftDepends();
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

  public YamlStore storage(String name) {
    return stores.computeIfAbsent(name, key -> YamlStore.of(plugin, key));
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
    return this;
  }

  public Trio loadLang(String code) {
    return lang(Lang.load(plugin, code));
  }

  public void bindCommand(String name, Commands.Tree tree) {
    Commands.bind(plugin, name, tree);
  }
}
