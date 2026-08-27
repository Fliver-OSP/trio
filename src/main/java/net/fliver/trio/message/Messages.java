package net.fliver.trio.message;

import net.fliver.trio.lang.AdventureBridge;
import net.fliver.trio.lang.Lang;
import org.bukkit.ChatColor;
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
    String combined = combineRaw(key, placeholders);
    if (AdventureBridge.available() && AdventureBridge.looksLikeMiniMessage(combined)) {
      if (AdventureBridge.send(sender, combined)) {
        return;
      }
      sender.sendMessage(AdventureBridge.toLegacy(combined));
      return;
    }
    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', combined));
  }

  public String prefixed(String key, String... placeholders) {
    String combined = combineRaw(key, placeholders);
    if (AdventureBridge.available() && AdventureBridge.looksLikeMiniMessage(combined)) {
      return AdventureBridge.toLegacy(combined);
    }
    return ChatColor.translateAlternateColorCodes('&', combined);
  }

  private String combineRaw(String key, String... placeholders) {
    String body = lang.raw(key, placeholders);
    if (prefixKey == null || prefixKey.isEmpty() || !lang.has(prefixKey)) {
      return body;
    }
    return lang.raw(prefixKey) + body;
  }
}
