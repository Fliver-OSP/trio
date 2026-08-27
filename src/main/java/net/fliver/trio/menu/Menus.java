package net.fliver.trio.menu;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import net.fliver.trio.lang.Lang;
import net.fliver.trio.schedule.RegionScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
    return new Menu(plugin, title == null ? "" : title, null, rows * 9);
  }

  public Menu chest(Component title, int rows) {
    ensureListening();
    if (rows < 1 || rows > 6) {
      throw new IllegalArgumentException("rows");
    }
    Component safe = title == null ? Component.empty() : title;
    return new Menu(plugin, null, safe, rows * 9);
  }

  public Menu chestMini(String langKey, int rows) {
    if (lang == null) {
      throw new IllegalStateException("Menus.lang(Lang) must be set before chestMini");
    }
    return chest(lang.component(langKey), rows);
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

    @SuppressWarnings("unchecked")
    private Menu(JavaPlugin plugin, String stringTitle, Component componentTitle, int size) {
      this.plugin = plugin;
      this.inventory = MenuFactory.create(this, size, stringTitle, componentTitle);
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
      MenuClick click = new MenuClick(player, slot, event.getClick(), event, this);
      RegionScheduler.runForEntity(plugin, player, () -> handler.accept(click), null);
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

  static final class MenuFactory {
    private static final Method COMPONENT_CREATE;
    private static final boolean HAS_COMPONENT;

    static {
      Method method = null;
      boolean has = false;
      try {
        method =
            Bukkit.class.getMethod(
                "createInventory", InventoryHolder.class, int.class, Component.class);
        has = true;
      } catch (NoSuchMethodException e) {
        method = null;
        has = false;
      }
      COMPONENT_CREATE = method;
      HAS_COMPONENT = has;
    }

    private MenuFactory() {}

    static Inventory create(
        InventoryHolder holder, int size, String stringTitle, Component componentTitle) {
      if (componentTitle != null && HAS_COMPONENT) {
        try {
          return (Inventory) COMPONENT_CREATE.invoke(null, holder, size, componentTitle);
        } catch (ReflectiveOperationException e) {
          String fallback = LegacyComponentSerializer.legacySection().serialize(componentTitle);
          return Bukkit.createInventory(holder, size, fallback);
        }
      }
      if (componentTitle != null) {
        String fallback = LegacyComponentSerializer.legacySection().serialize(componentTitle);
        return Bukkit.createInventory(holder, size, fallback);
      }
      return Bukkit.createInventory(holder, size, stringTitle == null ? "" : stringTitle);
    }
  }
}
