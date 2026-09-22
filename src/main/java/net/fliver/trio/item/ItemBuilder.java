package net.fliver.trio.item;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ItemBuilder {
  private final ItemStack item;
  private final ItemMeta meta;

  private ItemBuilder(Material material, int amount) {
    if (material == null) {
      throw new IllegalArgumentException("material");
    }
    int fixed = Math.max(1, Math.min(64, amount));
    this.item = new ItemStack(material, fixed);
    this.meta = item.getItemMeta();
  }

  public static ItemBuilder of(Material material) {
    return new ItemBuilder(material, 1);
  }

  public static ItemBuilder of(Material material, int amount) {
    return new ItemBuilder(material, amount);
  }

  public ItemBuilder name(String text) {
    if (meta != null && text != null) {
      meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', text));
    }
    return this;
  }

  public ItemBuilder lore(String... lines) {
    if (meta == null || lines == null) {
      return this;
    }
    List<String> colored = new ArrayList<String>(lines.length);
    for (String line : lines) {
      colored.add(ChatColor.translateAlternateColorCodes('&', line == null ? "" : line));
    }
    meta.setLore(colored);
    return this;
  }

  public ItemBuilder lore(List<String> lines) {
    if (meta == null || lines == null) {
      return this;
    }
    List<String> colored = new ArrayList<String>(lines.size());
    for (String line : lines) {
      colored.add(ChatColor.translateAlternateColorCodes('&', line == null ? "" : line));
    }
    meta.setLore(colored);
    return this;
  }

  public ItemBuilder amount(int amount) {
    item.setAmount(Math.max(1, Math.min(64, amount)));
    return this;
  }

  public ItemStack build() {
    if (meta != null) {
      item.setItemMeta(meta);
    }
    return item.clone();
  }
}
