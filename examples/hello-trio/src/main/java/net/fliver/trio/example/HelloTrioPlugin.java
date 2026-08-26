package net.fliver.trio.example;

import net.fliver.trio.Trio;
import net.fliver.trio.command.Commands;
import net.fliver.trio.storage.YamlStore;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HelloTrioPlugin extends JavaPlugin {
  private static final long HELLO_COOLDOWN_MS = 3_000L;

  private Trio trio;
  private YamlStore players;

  @Override
  public void onEnable() {
    trio = Trio.create(this);
    trio.configs().saveDefaults().reload();
    trio.loadLang(trio.configs().string("language", "en_US"));
    players = trio.storage("players");

    if (trio.softDepends().present("Vault")) {
      getLogger().info("Vault detected.");
    }

    Commands.Tree root =
        Commands.tree()
            .permission("hellotrio.use")
            .executes(this::hello)
            .then(
                "reload",
                Commands.tree()
                    .permission("hellotrio.reload")
                    .executes(
                        (sender, args) -> {
                          trio.configs().reload();
                          trio.loadLang(trio.configs().string("language", "en_US"));
                          trio.messages().send(sender, "reloaded");
                        }))
            .then(
                "http",
                Commands.tree()
                    .permission("hellotrio.http")
                    .executes(
                        (sender, args) ->
                            trio.http()
                                .get(
                                    "https://httpbin.org/get",
                                    response ->
                                        trio.messages()
                                            .send(
                                                sender,
                                                "http-ok",
                                                "status",
                                                String.valueOf(response.status())),
                                    error ->
                                        trio.messages()
                                            .send(
                                                sender,
                                                "http-fail",
                                                "error",
                                                error.getMessage() == null
                                                    ? error.getClass().getSimpleName()
                                                    : error.getMessage()))));

    trio.bindCommand("hello", root);
  }

  private void hello(org.bukkit.command.CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      trio.messages().send(sender, "players-only");
      return;
    }

    if (!trio.cooldowns().ready("hello", player.getUniqueId(), HELLO_COOLDOWN_MS)) {
      long seconds = Math.max(1L, (trio.cooldowns().remainingMs("hello", player.getUniqueId()) + 999L) / 1000L);
      trio.messages().send(sender, "cooldown", "seconds", String.valueOf(seconds));
      return;
    }

    String id = player.getUniqueId().toString();
    int uses = players.integer(id, "uses", 0) + 1;
    players.set(id, "uses", uses);
    players.save(id);

    trio.messages()
        .send(sender, "hello", "player", player.getName(), "uses", String.valueOf(uses));
  }
}
