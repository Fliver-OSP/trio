package net.fliver.trio.message;

import net.fliver.trio.lang.Lang;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public final class Messages {
  private final Lang lang;
  private String prefixKey = "prefix";

  private Messages(Lang lang) {
    this.lang = lang;
  }

  public static Messages of(Lang lang) {
    return new Messages(lang);
  }

  public Messages prefixKey(String key) {
    this.prefixKey = key;
    return this;
  }

  public void send(CommandSender sender, String key, String... placeholders) {
    sender.sendMessage(prefixed(key, placeholders));
  }

  public Component prefixed(String key, String... placeholders) {
    Component body = lang.component(key, placeholders);
    if (prefixKey == null || prefixKey.isEmpty() || !lang.has(prefixKey)) {
      return body;
    }
    return lang.component(prefixKey).append(body);
  }
}
