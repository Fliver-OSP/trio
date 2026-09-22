package net.fliver.trio.menu;

import java.util.Objects;
import java.util.function.Consumer;
import net.fliver.trio.lang.Lang;
import net.fliver.trio.schedule.RegionScheduler;
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
  private Lang lang;
  private boolean listening;

  private Menus(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public static Menus of(JavaPlugin plugin) {
    return new Menus(plugin);
  }

  public Menus lang(Lang lang) {
    this.lang = lang;
    return this;
  }

  public Menu chest(String title, int rows) {
    ensureListening();
    if (rows < 1 || rows > 6) {
      throw new IllegalArgumentException("rows");
    }
    return new Menu(plugin, title == null ? "" : title, rows * 9);
  }

  public Menu chestMini(String langKey, int rows) {
    if (lang == null) {
      throw new IllegalStateException("Menus.lang(Lang) must be set before chestMini");
    }
    return chest(lang.colored(langKey), rows);
  }

  private void ensureListening() {
    if (listening) {
      return;
    }
    listening = true;
    Bukkit.getPluginManager().registerEvents(new MenuListener(), plugin);
  }

  public static final class Menu implements InventoryHolder {
    private final JavaPlugin plugin;
    private final Inventory inventory;
    private final Consumer<MenuClick>[] handlers;
    private Consumer<Player> closeHandler;

    @SuppressWarnings("unchecked")
    private Menu(JavaPlugin plugin, String title, int size) {
      this.plugin = plugin;
      this.inventory = Bukkit.createInventory(this, size, title);
      this.handlers = (Consumer<MenuClick>[]) new Consumer[size];
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

    public Menu fillBorder(ItemStack item) {
      int size = inventory.getSize();
      int rows = size / 9;
      if (rows < 2) {
        return this;
      }
      for (int col = 0; col < 9; col++) {
        setIfEmpty(col, item);
        setIfEmpty(size - 9 + col, item);
      }
      for (int row = 1; row < rows - 1; row++) {
        setIfEmpty(row * 9, item);
        setIfEmpty(row * 9 + 8, item);
      }
      return this;
    }

    public Menu clear() {
      inventory.clear();
      for (int i = 0; i < handlers.length; i++) {
        handlers[i] = null;
      }
      return this;
    }

    public Menu onClose(Consumer<Player> handler) {
      this.closeHandler = handler;
      return this;
    }

    public void open(Player player) {
      Objects.requireNonNull(player, "player").openInventory(inventory);
    }

    public void close(Player player) {
      if (player != null) {
        player.closeInventory();
      }
    }

    public void refresh() {
      for (org.bukkit.entity.HumanEntity viewer : inventory.getViewers()) {
        if (viewer instanceof Player) {
          ((Player) viewer).updateInventory();
        }
      }
    }

    public Inventory inventory() {
      return inventory;
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }

    private void setIfEmpty(int slot, ItemStack item) {
      if (inventory.getItem(slot) == null) {
        inventory.setItem(slot, item);
      }
    }

    void handleClose(Player player) {
      if (closeHandler != null && player != null) {
        final Player target = player;
        final Consumer<Player> handler = closeHandler;
        RegionScheduler.runForEntity(
            plugin,
            target,
            new Runnable() {
              @Override
              public void run() {
                handler.accept(target);
              }
            },
            null);
      }
    }

    void handleClick(InventoryClickEvent event) {
      event.setCancelled(true);
      if (!(event.getWhoClicked() instanceof Player)) {
        return;
      }
      final Player player = (Player) event.getWhoClicked();
      int slot = event.getRawSlot();
      if (slot < 0 || slot >= handlers.length) {
        return;
      }
      final Consumer<MenuClick> handler = handlers[slot];
      if (handler == null) {
        return;
      }
      final MenuClick click = new MenuClick(player, slot, event.getClick(), event, this);
      RegionScheduler.runForEntity(
          plugin,
          player,
          new Runnable() {
            @Override
            public void run() {
              handler.accept(click);
            }
          },
          null);
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
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
      if (!(event.getInventory().getHolder() instanceof Menu)) {
        return;
      }
      if (!(event.getPlayer() instanceof Player)) {
        return;
      }
      ((Menu) event.getInventory().getHolder()).handleClose((Player) event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
      Inventory top = event.getView().getTopInventory();
      if (!(top.getHolder() instanceof Menu)) {
        return;
      }
      Menu menu = (Menu) top.getHolder();
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
