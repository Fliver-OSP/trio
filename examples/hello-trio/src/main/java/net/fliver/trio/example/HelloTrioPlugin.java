package net.fliver.trio.example;

import net.fliver.trio.Trio;
import net.fliver.trio.command.Commands;
import org.bukkit.plugin.java.JavaPlugin;

public final class HelloTrioPlugin extends JavaPlugin {
  private Trio trio;

  @Override
  public void onEnable() {
    trio = Trio.create(this);
    trio.configs().saveDefaults().reload();
    trio.loadLang(trio.configs().string("language", "en_US"));

    Commands.Tree root =
        Commands.tree()
            .permission("hellotrio.use")
            .executes(
                (sender, args) ->
                    trio.messages().send(sender, "hello", "player", sender.getName()))
            .then(
                "reload",
                Commands.tree()
                    .permission("hellotrio.reload")
                    .executes(
                        (sender, args) -> {
                          trio.configs().reload();
                          trio.loadLang(trio.configs().string("language", "en_US"));
                          trio.messages().send(sender, "reloaded");
                        }));

    trio.bindCommand("hello", root);
  }
}
