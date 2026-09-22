package net.fliver.trio.depend;

import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

public final class SoftDepends {
  public boolean present(String pluginName) {
    return plugin(pluginName) != null;
  }

  public Plugin plugin(String pluginName) {
    if (pluginName == null || pluginName.isEmpty()) {
      return null;
    }
    Plugin found = Bukkit.getPluginManager().getPlugin(pluginName);
    if (found == null || !found.isEnabled()) {
      return null;
    }
    return found;
  }

  public <T> T map(String pluginName, Function<Plugin, T> fn) {
    Plugin found = plugin(pluginName);
    if (found == null || fn == null) {
      return null;
    }
    return fn.apply(found);
  }

  public void onEnable(Plugin owner, String pluginName, final Consumer<Plugin> handler) {
    if (owner == null || pluginName == null || handler == null) {
      throw new IllegalArgumentException("owner/pluginName/handler");
    }
    Plugin already = plugin(pluginName);
    if (already != null) {
      handler.accept(already);
      return;
    }
    final String wanted = pluginName;
    Bukkit.getPluginManager()
        .registerEvents(
            new Listener() {
              @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
              public void onPluginEnable(PluginEnableEvent event) {
                if (event.getPlugin() != null && wanted.equals(event.getPlugin().getName())) {
                  handler.accept(event.getPlugin());
                }
              }
            },
            owner);
  }
}
