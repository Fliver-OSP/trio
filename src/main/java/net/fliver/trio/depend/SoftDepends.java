package net.fliver.trio.depend;

import java.util.function.Function;
import org.bukkit.Bukkit;
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
}
