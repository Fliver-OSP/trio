package net.fliver.trio.command;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.bukkit.command.CommandSender;

public final class BrigadierCommands {
  private BrigadierCommands() {}

  public static Node literal(String name) {
    return new Node(Objects.requireNonNull(name, "name"), false);
  }

  public static Node argument(String name) {
    return new Node(Objects.requireNonNull(name, "name"), true);
  }

  public static final class Node {
    private final String name;
    private final boolean argument;
    private final Map<String, Node> children = new LinkedHashMap<>();
    private String permission;
    private Consumer<BrigadierContext> action;
    private BiConsumer<CommandSender, String[]> treeAction;

    private Node(String name, boolean argument) {
      this.name = name;
      this.argument = argument;
    }

    public Node permission(String permission) {
      this.permission = permission;
      return this;
    }

    public Node executes(Consumer<BrigadierContext> action) {
      this.action = action;
      return this;
    }

    public Node executesTree(BiConsumer<CommandSender, String[]> action) {
      this.treeAction = action;
      return this;
    }

    public Node then(Node child) {
      Objects.requireNonNull(child, "child");
      children.put(child.name.toLowerCase(Locale.ROOT), child);
      return this;
    }

    public String name() {
      return name;
    }

    public boolean isArgument() {
      return argument;
    }

    public String permission() {
      return permission;
    }

    Object buildLiteralNode() throws ReflectiveOperationException {
      if (argument) {
        throw new IllegalStateException("Root must be a literal: " + name);
      }
      Object builder = BrigadierReflect.literal(name);
      apply(builder);
      return BrigadierReflect.build(builder);
    }

    private void apply(Object builder) throws ReflectiveOperationException {
      if (permission != null && !permission.isEmpty()) {
        String perm = permission;
        Predicate<Object> requires =
            source -> {
              CommandSender sender = unwrapSender(source);
              return sender != null && sender.hasPermission(perm);
            };
        BrigadierReflect.requires(builder, requires);
      }
      if (action != null || treeAction != null) {
        BrigadierReflect.executes(
            builder,
            source -> {
              CommandSender sender = unwrapSender(source);
              if (sender == null) {
                return 0;
              }
              Object ctx = BrigadierReflect.currentContext();
              if (action != null) {
                action.accept(new BrigadierContext(sender, ctx));
              }
              if (treeAction != null) {
                treeAction.accept(sender, remainingArgs(ctx));
              }
              return 1;
            });
      }
      for (Node child : children.values()) {
        Object childBuilder;
        if (child.argument) {
          childBuilder = BrigadierReflect.argument(child.name);
        } else {
          childBuilder = BrigadierReflect.literal(child.name);
        }
        child.apply(childBuilder);
        BrigadierReflect.then(builder, childBuilder);
      }
    }

    Commands.Tree toTree() {
      Commands.Tree tree = Commands.tree();
      if (permission != null) {
        tree.permission(permission);
      }
      if (treeAction != null) {
        tree.executes(treeAction);
      } else if (action != null) {
        Consumer<BrigadierContext> act = action;
        tree.executes((sender, args) -> act.accept(new BrigadierContext(sender, null, args)));
      }
      List<String> completes = new ArrayList<>();
      for (Node child : children.values()) {
        if (!child.argument) {
          completes.add(child.name);
          tree.then(child.name, child.toTree());
        }
      }
      if (!completes.isEmpty()) {
        tree.completes(completes.toArray(String[]::new));
      }
      return tree;
    }

    private static CommandSender unwrapSender(Object source) {
      if (source instanceof CommandSender sender) {
        return sender;
      }
      if (source == null) {
        return null;
      }
      try {
        Object result = source.getClass().getMethod("getSender").invoke(source);
        if (result instanceof CommandSender sender) {
          return sender;
        }
      } catch (ReflectiveOperationException ignored) {
        // not a Paper CommandSourceStack
      }
      return null;
    }

    static String[] remainingArgs(Object ctx) {
      if (ctx == null) {
        return new String[0];
      }
      try {
        String input = (String) ctx.getClass().getMethod("getInput").invoke(ctx);
        int firstSpace = input.indexOf(' ');
        if (firstSpace < 0) {
          return new String[0];
        }
        String rest = input.substring(firstSpace + 1).trim();
        if (rest.isEmpty()) {
          return new String[0];
        }
        return rest.split("\\s+");
      } catch (ReflectiveOperationException e) {
        return new String[0];
      }
    }
  }

  public static final class BrigadierContext {
    private final CommandSender sender;
    private final Object context;
    private final String[] args;

    BrigadierContext(CommandSender sender, Object context) {
      this(sender, context, null);
    }

    BrigadierContext(CommandSender sender, Object context, String[] args) {
      this.sender = sender;
      this.context = context;
      this.args = args == null ? new String[0] : args;
    }

    public CommandSender sender() {
      return sender;
    }

    public String[] args() {
      if (context != null) {
        return Node.remainingArgs(context);
      }
      return args;
    }

    public String argument(String name) {
      if (context == null) {
        return null;
      }
      try {
        Method getArgument =
            context.getClass().getMethod("getArgument", String.class, Class.class);
        return (String) getArgument.invoke(context, name, String.class);
      } catch (ReflectiveOperationException | IllegalArgumentException e) {
        return null;
      }
    }
  }

  static final class BrigadierReflect {
    private static final ThreadLocal<Object> CURRENT_CTX = new ThreadLocal<>();

    private BrigadierReflect() {}

    static Object currentContext() {
      return CURRENT_CTX.get();
    }

    static Object literal(String name) throws ReflectiveOperationException {
      Class<?> clazz = Class.forName("com.mojang.brigadier.builder.LiteralArgumentBuilder");
      Method literal = clazz.getMethod("literal", String.class);
      return literal.invoke(null, name);
    }

    static Object argument(String name) throws ReflectiveOperationException {
      Class<?> stringType = Class.forName("com.mojang.brigadier.arguments.StringArgumentType");
      Method greedy = stringType.getMethod("greedyString");
      Object argType = greedy.invoke(null);
      Class<?> required = Class.forName("com.mojang.brigadier.builder.RequiredArgumentBuilder");
      Method argument = required.getMethod("argument", String.class, Class.forName("com.mojang.brigadier.arguments.ArgumentType"));
      return argument.invoke(null, name, argType);
    }

    static void requires(Object builder, Predicate<Object> predicate)
        throws ReflectiveOperationException {
      Method requires = builder.getClass().getMethod("requires", Predicate.class);
      requires.invoke(builder, predicate);
    }

    static void executes(Object builder, java.util.function.ToIntFunction<Object> command)
        throws ReflectiveOperationException {
      Class<?> commandClass = Class.forName("com.mojang.brigadier.Command");
      Object proxy =
          java.lang.reflect.Proxy.newProxyInstance(
              commandClass.getClassLoader(),
              new Class<?>[] {commandClass},
              (proxyObj, method, args) -> {
                if ("run".equals(method.getName()) && args != null && args.length == 1) {
                  Object ctx = args[0];
                  CURRENT_CTX.set(ctx);
                  try {
                    Object source = ctx.getClass().getMethod("getSource").invoke(ctx);
                    return Integer.valueOf(command.applyAsInt(source));
                  } finally {
                    CURRENT_CTX.remove();
                  }
                }
                if ("equals".equals(method.getName())) {
                  return Boolean.valueOf(proxyObj == args[0]);
                }
                if ("hashCode".equals(method.getName())) {
                  return Integer.valueOf(System.identityHashCode(proxyObj));
                }
                if ("toString".equals(method.getName())) {
                  return "TrioBrigadierCommand";
                }
                return null;
              });
      Method executes = builder.getClass().getMethod("executes", commandClass);
      executes.invoke(builder, proxy);
    }

    static void then(Object parent, Object child) throws ReflectiveOperationException {
      Method then = null;
      for (Method method : parent.getClass().getMethods()) {
        if (!"then".equals(method.getName()) || method.getParameterCount() != 1) {
          continue;
        }
        Class<?> param = method.getParameterTypes()[0];
        if (param.isInstance(child) || param.getName().contains("ArgumentBuilder")) {
          then = method;
          break;
        }
      }
      if (then == null) {
        throw new NoSuchMethodException("then(ArgumentBuilder)");
      }
      then.invoke(parent, child);
    }

    static Object build(Object builder) throws ReflectiveOperationException {
      return builder.getClass().getMethod("build").invoke(builder);
    }
  }
}
