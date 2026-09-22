package net.fliver.trio.example;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.fliver.trio.Trio;
import net.fliver.trio.command.BrigadierCommands;
import net.fliver.trio.command.CommandRegistrar;
import net.fliver.trio.command.Commands;
import net.fliver.trio.item.ItemBuilder;
import net.fliver.trio.json.Json;
import net.fliver.trio.menu.Menus;
import net.fliver.trio.storage.SqliteStore;
import net.fliver.trio.storage.YamlStore;
import net.fliver.trio.update.Updates;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

public final class HelloTrioPlugin extends JavaPlugin {
  private static final long HELLO_COOLDOWN_MS = 3000L;

  private Trio trio;
  private YamlStore players;
  private SqliteStore sqlite;

  @Override
  public void onEnable() {
    trio = Trio.create(this);
    trio.configs().saveDefaults().reload();
    trio.loadLang(trio.configs().string("language", "en_US"));
    players = trio.storage("players");
    sqlite = trio.sqlite("demo");
    sqlite.execute(
        "CREATE TABLE IF NOT EXISTS greets (uuid TEXT PRIMARY KEY, amount INT NOT NULL)");

    trio.permissions().register("hellotrio.admin", PermissionDefault.OP);
    trio.permissions().register("hellotrio.use", PermissionDefault.TRUE, "hellotrio.admin");
    trio.permissions().register("hellotrio.reload", PermissionDefault.OP, "hellotrio.admin");
    trio.permissions().register("hellotrio.http", PermissionDefault.OP, "hellotrio.admin");
    trio.permissions().register("hellotrio.menu", PermissionDefault.TRUE, "hellotrio.admin");

    trio.softDepends().onEnable(this, "Vault", new Consumer<org.bukkit.plugin.Plugin>() {
      @Override
      public void accept(org.bukkit.plugin.Plugin found) {
        getLogger().info("Vault is around, hooks can go here.");
      }
    });

    final HelloTrioPlugin self = this;

    Commands.Tree treeFallback =
        Commands.tree()
            .permission("hellotrio.use")
            .usage("&eUsage: /hello [reload|http|menu|json|title]")
            .executes(
                new BiConsumer<CommandSender, String[]>() {
                  @Override
                  public void accept(CommandSender sender, String[] args) {
                    self.hello(sender, args);
                  }
                })
            .then(
                "reload",
                Commands.tree()
                    .permission("hellotrio.reload")
                    .usage("&eUsage: /hello reload")
                    .executes(
                        new BiConsumer<CommandSender, String[]>() {
                          @Override
                          public void accept(CommandSender sender, String[] args) {
                            self.reload(sender);
                          }
                        }))
            .then(
                "http",
                Commands.tree()
                    .permission("hellotrio.http")
                    .usage("&eUsage: /hello http")
                    .executes(
                        new BiConsumer<CommandSender, String[]>() {
                          @Override
                          public void accept(CommandSender sender, String[] args) {
                            self.http(sender);
                          }
                        }))
            .then(
                "menu",
                Commands.tree()
                    .permission("hellotrio.menu")
                    .usage("&eUsage: /hello menu")
                    .playerOnly()
                    .executes(
                        new BiConsumer<CommandSender, String[]>() {
                          @Override
                          public void accept(CommandSender sender, String[] args) {
                            self.menu(sender);
                          }
                        }))
            .then(
                "json",
                Commands.tree()
                    .permission("hellotrio.use")
                    .usage("&eUsage: /hello json")
                    .executes(
                        new BiConsumer<CommandSender, String[]>() {
                          @Override
                          public void accept(CommandSender sender, String[] args) {
                            self.json(sender);
                          }
                        }))
            .then(
                "title",
                Commands.tree()
                    .permission("hellotrio.use")
                    .usage("&eUsage: /hello title")
                    .playerOnly()
                    .executes(
                        new BiConsumer<CommandSender, String[]>() {
                          @Override
                          public void accept(CommandSender sender, String[] args) {
                            self.title(sender);
                          }
                        }));

    BrigadierCommands.Node brigadier =
        Commands.brigadier("hello")
            .permission("hellotrio.use")
            .usage("&eUsage: /hello [reload|http|menu|json|title]")
            .executes(
                new Consumer<BrigadierCommands.BrigadierContext>() {
                  @Override
                  public void accept(BrigadierCommands.BrigadierContext ctx) {
                    self.hello(ctx.sender(), ctx.args());
                  }
                })
            .then(
                BrigadierCommands.literal("reload")
                    .permission("hellotrio.reload")
                    .executes(
                        new Consumer<BrigadierCommands.BrigadierContext>() {
                          @Override
                          public void accept(BrigadierCommands.BrigadierContext ctx) {
                            self.reload(ctx.sender());
                          }
                        }))
            .then(
                BrigadierCommands.literal("http")
                    .permission("hellotrio.http")
                    .executes(
                        new Consumer<BrigadierCommands.BrigadierContext>() {
                          @Override
                          public void accept(BrigadierCommands.BrigadierContext ctx) {
                            self.http(ctx.sender());
                          }
                        }))
            .then(
                BrigadierCommands.literal("menu")
                    .permission("hellotrio.menu")
                    .playerOnly()
                    .executes(
                        new Consumer<BrigadierCommands.BrigadierContext>() {
                          @Override
                          public void accept(BrigadierCommands.BrigadierContext ctx) {
                            self.menu(ctx.sender());
                          }
                        }))
            .then(
                BrigadierCommands.literal("json")
                    .executes(
                        new Consumer<BrigadierCommands.BrigadierContext>() {
                          @Override
                          public void accept(BrigadierCommands.BrigadierContext ctx) {
                            self.json(ctx.sender());
                          }
                        }))
            .then(
                BrigadierCommands.literal("title")
                    .playerOnly()
                    .executes(
                        new Consumer<BrigadierCommands.BrigadierContext>() {
                          @Override
                          public void accept(BrigadierCommands.BrigadierContext ctx) {
                            self.title(ctx.sender());
                          }
                        }));

    CommandRegistrar.registerOrBind(this, "hello", brigadier, treeFallback);
  }

  @Override
  public void onDisable() {
    if (trio != null) {
      trio.close();
    } else if (sqlite != null) {
      sqlite.close();
    }
  }

  private void reload(CommandSender sender) {
    trio.configs().reload();
    trio.loadLang(trio.configs().string("language", "en_US"));
    trio.messages().send(sender, "reloaded");
  }

  private void http(final CommandSender sender) {
    trio.http()
        .get(
            "https://httpbin.org/get",
            new Consumer<net.fliver.trio.http.Http.Response>() {
              @Override
              public void accept(net.fliver.trio.http.Http.Response response) {
                trio.messages()
                    .send(sender, "http-ok", "status", String.valueOf(response.status()));
              }
            },
            new Consumer<Throwable>() {
              @Override
              public void accept(Throwable error) {
                trio.messages()
                    .send(
                        sender,
                        "http-fail",
                        "error",
                        error.getMessage() == null
                            ? error.getClass().getSimpleName()
                            : error.getMessage());
              }
            });
  }

  private void json(CommandSender sender) {
    Map<String, Object> parsed =
        Json.asObject(Json.parse("{\"hello\":\"world\",\"count\":2}"));
    String name = Json.asString(parsed.get("hello"), "nothing");
    trio.messages().send(sender, "json-ok", "value", name);
  }

  private void title(CommandSender sender) {
    Player player = (Player) sender;
    trio.messages().title(player, "title-main", "title-sub");
  }

  private void menu(CommandSender sender) {
    openMenu((Player) sender);
  }

  private void openMenu(Player player) {
    ItemStack frame =
        ItemBuilder.of(Material.GLASS, 1).name("&8").build();
    ItemStack hello =
        ItemBuilder.of(Material.EMERALD).name("&aSay hi").lore("&7Click to get a hello").build();
    Menus.Menu menu = trio.menus().chestMini("menu-title", 3);
    menu.fillBorder(frame);
    menu.set(
        13,
        hello,
        new Consumer<Menus.MenuClick>() {
          @Override
          public void accept(final Menus.MenuClick click) {
            click.player().closeInventory();
            trio.scheduler()
                .later(
                    new Runnable() {
                      @Override
                      public void run() {
                        trio.messages().send(click.player(), "menu-clicked");
                      }
                    },
                    10L);
          }
        });
    menu.onClose(new Consumer<Player>() {
      @Override
      public void accept(Player left) {
        getLogger().info(left.getName() + " closed the demo menu.");
      }
    });
    menu.open(player);
  }

  private void hello(CommandSender sender, String[] args) {
    if (!(sender instanceof Player)) {
      trio.messages().send(sender, "players-only");
      return;
    }
    Player player = (Player) sender;

    if (!trio.permissions().has(player, "hellotrio.use")) {
      trio.messages().send(sender, "no-permission");
      return;
    }

    if (!trio.cooldowns().ready("hello", player.getUniqueId(), HELLO_COOLDOWN_MS)) {
      long seconds = trio.cooldowns().remainingSec("hello", player.getUniqueId());
      trio.messages().send(sender, "cooldown", "seconds", String.valueOf(Math.max(1L, seconds)));
      return;
    }

    String id = player.getUniqueId().toString();
    int uses = players.integer(id, "uses", 0) + 1;
    players.set(id, "uses", uses);
    players.save(id);

    int greets = sqlite.queryInt("SELECT amount FROM greets WHERE uuid=?", 0, id) + 1;
    sqlite.execute("INSERT OR REPLACE INTO greets(uuid, amount) VALUES(?, ?)", id, Integer.valueOf(greets));

    trio.messages()
        .send(
            sender,
            "hello",
            "player",
            player.getName(),
            "uses",
            String.valueOf(uses),
            "greets",
            String.valueOf(greets));
  }

  @SuppressWarnings("unused")
  private void updateHint(CommandSender sender, String feedUrl) {
    trio.checkUpdates(
        feedUrl,
        new Consumer<Updates.Release>() {
          @Override
          public void accept(Updates.Release release) {
            getLogger().info("New build out: " + release.version);
          }
        },
        new Consumer<Throwable>() {
          @Override
          public void accept(Throwable error) {
            getLogger().warning("Update check stumbled: " + error.getMessage());
          }
        });
  }
}
