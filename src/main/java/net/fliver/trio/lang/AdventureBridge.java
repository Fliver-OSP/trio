package net.fliver.trio.lang;

import java.lang.reflect.Method;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class AdventureBridge {
  private static final boolean AVAILABLE;
  private static final Object MINI;
  private static final Object LEGACY_SERIALIZER;
  private static final Method DESERIALIZE;
  private static final Method SEND_MESSAGE;
  private static final Method SERIALIZE_LEGACY;
  private static final Class<?> AUDIENCE_CLASS;

  static {
    boolean available = false;
    Object mini = null;
    Object legacySerializer = null;
    Method deserialize = null;
    Method sendMessage = null;
    Method serializeLegacy = null;
    Class<?> audienceClass = null;
    try {
      Class<?> miniClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
      Method miniMessage = miniClass.getMethod("miniMessage");
      mini = miniMessage.invoke(null);
      Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
      deserialize = miniClass.getMethod("deserialize", String.class);
      audienceClass = Class.forName("net.kyori.adventure.audience.Audience");
      sendMessage = audienceClass.getMethod("sendMessage", componentClass);
      Class<?> legacy =
          Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
      Method legacySection = legacy.getMethod("legacySection");
      legacySerializer = legacySection.invoke(null);
      serializeLegacy = legacySerializer.getClass().getMethod("serialize", componentClass);
      available = true;
    } catch (Throwable t) {
      available = false;
      mini = null;
      legacySerializer = null;
      deserialize = null;
      sendMessage = null;
      serializeLegacy = null;
      audienceClass = null;
    }
    AVAILABLE = available;
    MINI = mini;
    LEGACY_SERIALIZER = legacySerializer;
    DESERIALIZE = deserialize;
    SEND_MESSAGE = sendMessage;
    SERIALIZE_LEGACY = serializeLegacy;
    AUDIENCE_CLASS = audienceClass;
  }

  private AdventureBridge() {}

  public static boolean available() {
    return AVAILABLE;
  }

  public static boolean looksLikeMiniMessage(String text) {
    return text != null && text.indexOf('<') >= 0 && text.indexOf('>') > text.indexOf('<');
  }

  public static boolean send(CommandSender sender, String miniOrLegacy) {
    if (!AVAILABLE || sender == null || miniOrLegacy == null || AUDIENCE_CLASS == null) {
      return false;
    }
    if (!AUDIENCE_CLASS.isInstance(sender)) {
      return false;
    }
    try {
      Object component = DESERIALIZE.invoke(MINI, miniOrLegacy);
      SEND_MESSAGE.invoke(sender, component);
      return true;
    } catch (Throwable t) {
      return false;
    }
  }

  public static String toLegacy(String miniOrLegacy) {
    if (miniOrLegacy == null) {
      return "";
    }
    if (!AVAILABLE || !looksLikeMiniMessage(miniOrLegacy)) {
      return ChatColor.translateAlternateColorCodes('&', miniOrLegacy);
    }
    try {
      Object component = DESERIALIZE.invoke(MINI, miniOrLegacy);
      Object legacy = SERIALIZE_LEGACY.invoke(LEGACY_SERIALIZER, component);
      return legacy == null
          ? ChatColor.translateAlternateColorCodes('&', miniOrLegacy)
          : String.valueOf(legacy);
    } catch (Throwable t) {
      return ChatColor.translateAlternateColorCodes('&', miniOrLegacy);
    }
  }
}
