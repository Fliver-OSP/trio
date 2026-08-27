package net.fliver.trio.command;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import net.fliver.trio.platform.Platform;
import org.bukkit.plugin.java.JavaPlugin;

public final class CommandRegistrar {
  private CommandRegistrar() {}

  public static boolean register(
      JavaPlugin plugin, String pluginYmlName, BrigadierCommands.Node root) {
    Objects.requireNonNull(plugin, "plugin");
    Objects.requireNonNull(pluginYmlName, "pluginYmlName");
    Objects.requireNonNull(root, "root");

    if (Platform.detect().supportsBrigadierLifecycle() && tryLifecycle(plugin, root)) {
      plugin.getLogger().info("Registered /" + root.name() + " via Brigadier lifecycle.");
      return true;
    }

    Commands.bind(plugin, pluginYmlName, root.toTree());
    plugin.getLogger().info("Registered /" + pluginYmlName + " via command tree fallback.");
    return false;
  }

  public static void registerOrBind(
      JavaPlugin plugin,
      String pluginYmlName,
      BrigadierCommands.Node brigadierRoot,
      Commands.Tree treeFallback) {
    Objects.requireNonNull(plugin, "plugin");
    Objects.requireNonNull(pluginYmlName, "pluginYmlName");
    if (brigadierRoot != null
        && Platform.detect().supportsBrigadierLifecycle()
        && tryLifecycle(plugin, brigadierRoot)) {
      plugin.getLogger().info("Registered /" + brigadierRoot.name() + " via Brigadier lifecycle.");
      return;
    }
    Commands.Tree tree =
        treeFallback != null
            ? treeFallback
            : (brigadierRoot == null ? null : brigadierRoot.toTree());
    if (tree == null) {
      throw new IllegalArgumentException("treeFallback");
    }
    Commands.bind(plugin, pluginYmlName, tree);
  }

  private static boolean tryLifecycle(JavaPlugin plugin, BrigadierCommands.Node root) {
    try {
      Method getLifecycleManager = plugin.getClass().getMethod("getLifecycleManager");
      Object manager = getLifecycleManager.invoke(plugin);
      Class<?> lifecycleEvents =
          Class.forName("io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents");
      Object commandsType = lifecycleEvents.getField("COMMANDS").get(null);

      Object node = root.buildLiteralNode();
      Consumer<Object> handler =
          event -> {
            try {
              Object registrar = event.getClass().getMethod("registrar").invoke(event);
              Method register = findRegister(registrar.getClass(), node);
              if (register == null) {
                throw new IllegalStateException("No registrar.register method found");
              }
              if (register.getParameterCount() == 1) {
                register.invoke(registrar, node);
              } else if (register.getParameterCount() == 2) {
                register.invoke(registrar, root.name(), node);
              } else {
                register.invoke(registrar, node);
              }
            } catch (ReflectiveOperationException e) {
              throw new IllegalStateException("Brigadier register failed: " + e.getMessage(), e);
            }
          };

      Method registerEventHandler = findRegisterEventHandler(manager.getClass());
      if (registerEventHandler == null) {
        return false;
      }
      registerEventHandler.invoke(manager, commandsType, handler);
      return true;
    } catch (Throwable t) {
      plugin
          .getLogger()
          .warning("Brigadier lifecycle unavailable, falling back: " + t.getMessage());
      return false;
    }
  }

  private static Method findRegister(Class<?> registrarClass, Object node) {
    for (Method method : registrarClass.getMethods()) {
      if (!"register".equals(method.getName())) {
        continue;
      }
      Class<?>[] params = method.getParameterTypes();
      if (params.length == 1 && params[0].isInstance(node)) {
        return method;
      }
      if (params.length == 1 && params[0].getName().contains("LiteralCommandNode")) {
        return method;
      }
      if (params.length == 2
          && params[0] == String.class
          && (params[1].isInstance(node) || params[1].getName().contains("LiteralCommandNode"))) {
        return method;
      }
    }
    return null;
  }

  private static Method findRegisterEventHandler(Class<?> managerClass) {
    for (Method method : managerClass.getMethods()) {
      if (!"registerEventHandler".equals(method.getName())) {
        continue;
      }
      Class<?>[] params = method.getParameterTypes();
      if (params.length == 2 && Consumer.class.isAssignableFrom(params[1])) {
        return method;
      }
    }
    return null;
  }
}
