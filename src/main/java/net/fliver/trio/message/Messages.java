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
    if (sender == null || key == null) {
      return;
    }
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

  public void sendList(CommandSender sender, String key, String... placeholders) {
    if (sender == null || key == null) {
      return;
    }
    for (String line : lang.coloredList(key, placeholders)) {
      if (prefixKey != null && !prefixKey.isEmpty() && lang.has(prefixKey)) {
        sender.sendMessage(withPrefix(line));
      } else {
        sender.sendMessage(line);
      }
    }
  }

  public void title(org.bukkit.entity.Player player, String titleKey, String subtitleKey) {
    title(player, titleKey, subtitleKey, new String[0]);
  }

  public void title(
      org.bukkit.entity.Player player, String titleKey, String subtitleKey, String... placeholders) {
    if (player == null) {
      return;
    }
    String title = titleKey == null ? "" : lang.colored(titleKey, placeholders);
    String subtitle = subtitleKey == null ? "" : lang.colored(subtitleKey, placeholders);
    try {
      player.getClass().getMethod("sendTitle", String.class, String.class).invoke(player, title, subtitle);
    } catch (Throwable ignored) {
      try {
        player
            .getClass()
            .getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class)
            .invoke(player, title, subtitle, 10, 40, 10);
      } catch (Throwable ignoredAgain) {
        player.sendMessage(title);
        if (subtitle != null && !subtitle.isEmpty()) {
          player.sendMessage(subtitle);
        }
      }
    }
  }

  public void actionbar(org.bukkit.entity.Player player, String key, String... placeholders) {
    if (player == null || key == null) {
      return;
    }
    String text = lang.colored(key, placeholders);
    try {
      Class<?> chatType = Class.forName("net.md_5.bungee.api.ChatMessageType");
      Object actionbar = null;
      for (Object constant : chatType.getEnumConstants()) {
        if ("ACTION_BAR".equals(String.valueOf(constant))) {
          actionbar = constant;
          break;
        }
      }
      Class<?> textComponent = Class.forName("net.md_5.bungee.api.chat.TextComponent");
      Object component = textComponent.getConstructor(String.class).newInstance(text);
      player
          .getClass()
          .getMethod("spigot")
          .invoke(player)
          .getClass()
          .getMethod("sendMessage", chatType, Class.forName("net.md_5.bungee.api.chat.BaseComponent"))
          .invoke(player.getClass().getMethod("spigot").invoke(player), actionbar, component);
    } catch (Throwable ignored) {
      player.sendMessage(text);
    }
  }

  public String prefixed(String key, String... placeholders) {
    String combined = combineRaw(key, placeholders);
    if (AdventureBridge.available() && AdventureBridge.looksLikeMiniMessage(combined)) {
      return AdventureBridge.toLegacy(combined);
    }
    return ChatColor.translateAlternateColorCodes('&', combined);
  }

  private String withPrefix(String line) {
    String prefix = lang.raw(prefixKey);
    String combined = (prefix == null ? "" : prefix) + line;
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
