package net.fliver.trio.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class Commands {
  private Commands() {}

  public static Tree tree() {
    return new Tree();
  }

  public static BrigadierCommands.Node brigadier(String literal) {
    return BrigadierCommands.literal(literal);
  }

  public static void bind(JavaPlugin plugin, String name, Tree tree) {
    PluginCommand command = plugin.getCommand(name);
    if (command == null) {
      throw new IllegalStateException("Command not declared in plugin.yml: " + name);
    }
    command.setExecutor(tree);
    command.setTabCompleter(tree);
  }

  public static boolean register(JavaPlugin plugin, String pluginYmlName, BrigadierCommands.Node root) {
    return CommandRegistrar.register(plugin, pluginYmlName, root);
  }

  public static final class Tree implements CommandExecutor, TabCompleter {
    private final Map<String, Tree> children = new LinkedHashMap<String, Tree>();
    private String permission;
    private BiConsumer<CommandSender, String[]> action;
    private String[] completions = new String[0];
    private String usage;
    private String denialMessage;
    private boolean playerOnly;
    private boolean consoleOnly;
    private BiConsumer<CommandSender, String> denialHandler;

    public Tree permission(String permission) {
      this.permission = permission;
      return this;
    }

    public Tree executes(BiConsumer<CommandSender, String[]> action) {
      this.action = action;
      return this;
    }

    public Tree completes(String... values) {
      this.completions = values == null ? new String[0] : values;
      return this;
    }

    public Tree usage(String usage) {
      this.usage = usage;
      return this;
    }

    public Tree denialMessage(String message) {
      this.denialMessage = message;
      return this;
    }

    public Tree denied(BiConsumer<CommandSender, String> handler) {
      this.denialHandler = handler;
      return this;
    }

    public Tree playerOnly() {
      this.playerOnly = true;
      this.consoleOnly = false;
      return this;
    }

    public Tree consoleOnly() {
      this.consoleOnly = true;
      this.playerOnly = false;
      return this;
    }

    public Tree then(String name, Tree child) {
      children.put(name.toLowerCase(Locale.ROOT), child);
      return this;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      return dispatch(this, sender, args, 0);
    }

    @Override
    public List<String> onTabComplete(
        CommandSender sender, Command command, String alias, String[] args) {
      return suggest(this, sender, args, 0);
    }

    private static boolean dispatch(Tree node, CommandSender sender, String[] args, int index) {
      if (node.playerOnly && !(sender instanceof org.bukkit.entity.Player)) {
        sender.sendMessage(
            org.bukkit.ChatColor.translateAlternateColorCodes(
                '&', "&cOnly players can use that command."));
        return true;
      }
      if (node.consoleOnly && (sender instanceof org.bukkit.entity.Player)) {
        sender.sendMessage(
            org.bukkit.ChatColor.translateAlternateColorCodes(
                '&', "&cThat command is console only."));
        return true;
      }
      if (node.permission != null && !node.permission.isEmpty() && !sender.hasPermission(node.permission)) {
        if (node.denialHandler != null) {
          node.denialHandler.accept(sender, node.permission);
          return true;
        }
        String text = node.denialMessage;
        if (text == null || text.isEmpty()) {
          text = "&cYou don't have permission for that.";
        }
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', text));
        return true;
      }

      if (index < args.length && !node.children.isEmpty()) {
        Tree child = node.children.get(args[index].toLowerCase(Locale.ROOT));
        if (child != null) {
          return dispatch(child, sender, args, index + 1);
        }
        if (node.usage != null && !node.usage.isEmpty()) {
          sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', node.usage));
          return true;
        }
      }

      if (node.action != null) {
        String[] rest = new String[Math.max(0, args.length - index)];
        if (rest.length > 0) {
          System.arraycopy(args, index, rest, 0, rest.length);
        }
        node.action.accept(sender, rest);
        return true;
      }

      if (node.usage != null && !node.usage.isEmpty()) {
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', node.usage));
      }
      return true;
    }

    private static List<String> suggest(Tree node, CommandSender sender, String[] args, int index) {
      if (node.permission != null && !node.permission.isEmpty() && !sender.hasPermission(node.permission)) {
        return Collections.emptyList();
      }

      if (index >= args.length) {
        return Collections.emptyList();
      }

      if (index < args.length - 1 && !node.children.isEmpty()) {
        Tree child = node.children.get(args[index].toLowerCase(Locale.ROOT));
        if (child == null) {
          return Collections.emptyList();
        }
        return suggest(child, sender, args, index + 1);
      }

      String partial = args[index].toLowerCase(Locale.ROOT);
      List<String> out = new ArrayList<>();

      if (!node.children.isEmpty()) {
        for (String name : node.children.keySet()) {
          Tree child = node.children.get(name);
          if (child.permission != null
              && !child.permission.isEmpty()
              && !sender.hasPermission(child.permission)) {
            continue;
          }
          if (name.startsWith(partial)) {
            out.add(name);
          }
        }
        return out;
      }

      for (String value : node.completions) {
        if (value.toLowerCase(Locale.ROOT).startsWith(partial)) {
          out.add(value);
        }
      }
      return out;
    }
  }
}
