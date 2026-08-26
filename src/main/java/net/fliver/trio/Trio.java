package net.fliver.trio;

import net.fliver.trio.command.Commands;
import net.fliver.trio.config.Configs;
import net.fliver.trio.lang.Lang;
import net.fliver.trio.message.Messages;
import org.bukkit.plugin.java.JavaPlugin;

public final class Trio {
  private final JavaPlugin plugin;
  private final Configs configs;
  private Lang lang;
  private Messages messages;

  private Trio(JavaPlugin plugin) {
    this.plugin = plugin;
    this.configs = Configs.of(plugin);
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
