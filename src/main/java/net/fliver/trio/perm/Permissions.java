package net.fliver.trio.perm;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

public final class Permissions {
  public void register(String node, PermissionDefault def) {
    register(node, def, (String[]) null);
  }

  public void register(String node, PermissionDefault def, String... parents) {
    if (node == null || node.isEmpty()) {
      throw new IllegalArgumentException("node");
    }
    PermissionDefault value = def == null ? PermissionDefault.OP : def;
    PluginManager pm = Bukkit.getPluginManager();
    Permission existing = pm.getPermission(node);

    if (existing != null) {
      existing.setDefault(value);
      if (parents != null) {
        for (String parent : parents) {
          if (parent == null || parent.isEmpty()) {
            continue;
          }
          Permission parentPerm = pm.getPermission(parent);
          if (parentPerm == null) {
            parentPerm = new Permission(parent, PermissionDefault.OP);
            pm.addPermission(parentPerm);
          }
          parentPerm.getChildren().put(node, true);
          parentPerm.recalculatePermissibles();
        }
      }
      existing.recalculatePermissibles();
      return;
    }

    Permission permission = new Permission(node, value);
    pm.addPermission(permission);

    if (parents != null) {
      for (String parent : parents) {
        if (parent == null || parent.isEmpty()) {
          continue;
        }
        Permission parentPerm = pm.getPermission(parent);
        if (parentPerm == null) {
          parentPerm = new Permission(parent, PermissionDefault.OP);
          pm.addPermission(parentPerm);
        }
        parentPerm.getChildren().put(node, true);
        parentPerm.recalculatePermissibles();
      }
    }
  }

  public boolean has(CommandSender sender, String node) {
    if (sender == null || node == null) {
      return false;
    }
    return sender.hasPermission(node);
  }
}
