package net.fliver.trio.menu;

import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class Menus {
  private final JavaPlugin plugin;
  private boolean listening;

  private Menus(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public static Menus of(JavaPlugin plugin) {
    return new Menus(plugin);
  }

  public Menu chest(String title, int rows) {
    ensureListening();
    if (rows < 1 || rows > 6) {
      throw new IllegalArgumentException("rows");
    }
    return new Menu(plugin, title == null ? "" : title, rows * 9);
  }

  private void ensureListening() {
    if (listening) {
      return;
    }
    listening = true;
    Bukkit.getPluginManager().registerEvents(new MenuListener(), plugin);
  }

  public static final class Menu implements InventoryHolder {
    private final Inventory inventory;
    private final Consumer<MenuClick>[] handlers;

    @SuppressWarnings("unchecked")
    private Menu(JavaPlugin plugin, String title, int size) {
      this.inventory = Bukkit.createInventory(this, size, title);
      this.handlers = new Consumer[size];
    }

    public Menu set(int slot, ItemStack item) {
      return set(slot, item, null);
    }

    public Menu set(int slot, ItemStack item, Consumer<MenuClick> onClick) {
      if (slot < 0 || slot >= inventory.getSize()) {
        throw new IllegalArgumentException("slot");
      }
      inventory.setItem(slot, item);
      handlers[slot] = onClick;
      return this;
    }

    public void open(Player player) {
      Objects.requireNonNull(player, "player").openInventory(inventory);
    }

    public Inventory inventory() {
      return inventory;
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }

    void handleClick(InventoryClickEvent event) {
      event.setCancelled(true);
      if (!(event.getWhoClicked() instanceof Player player)) {
        return;
      }
      int slot = event.getRawSlot();
      if (slot < 0 || slot >= handlers.length) {
        return;
      }
      Consumer<MenuClick> handler = handlers[slot];
      if (handler == null) {
        return;
      }
      handler.accept(new MenuClick(player, slot, event.getClick(), event, this));
    }
  }

  public static final class MenuClick {
    private final Player player;
    private final int slot;
    private final ClickType clickType;
    private final InventoryClickEvent event;
    private final Menu menu;

    private MenuClick(
        Player player, int slot, ClickType clickType, InventoryClickEvent event, Menu menu) {
      this.player = player;
      this.slot = slot;
      this.clickType = clickType;
      this.event = event;
      this.menu = menu;
    }

    public Player player() {
      return player;
    }

    public int slot() {
      return slot;
    }

    public ClickType clickType() {
      return clickType;
    }

    public InventoryClickEvent event() {
      return event;
    }

    public Menu menu() {
      return menu;
    }
  }

  private static final class MenuListener implements Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
      Inventory top = event.getView().getTopInventory();
      if (!(top.getHolder() instanceof Menu menu)) {
        return;
      }
      if (event.getClickedInventory() == null) {
        event.setCancelled(true);
        return;
      }
      if (event.getRawSlot() >= top.getSize()) {
        event.setCancelled(true);
        return;
      }
      menu.handleClick(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
      if (event.getInventory().getHolder() instanceof Menu) {
        event.setCancelled(true);
      }
    }
  }
}
