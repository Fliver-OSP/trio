package net.fliver.trio.schedule;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class Scheduler {
  private final JavaPlugin plugin;

  private Scheduler(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public static Scheduler of(JavaPlugin plugin) {
    return new Scheduler(plugin);
  }

  public BukkitTask sync(Runnable task) {
    return Bukkit.getScheduler().runTask(plugin, task);
  }

  public BukkitTask async(Runnable task) {
    return Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
  }

  public BukkitTask later(Runnable task, long delayTicks) {
    return Bukkit.getScheduler().runTaskLater(plugin, task, Math.max(0L, delayTicks));
  }

  public BukkitTask laterAsync(Runnable task, long delayTicks) {
    return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, Math.max(0L, delayTicks));
  }

  public BukkitTask timer(Runnable task, long delayTicks, long periodTicks) {
    return Bukkit.getScheduler()
        .runTaskTimer(plugin, task, Math.max(0L, delayTicks), Math.max(1L, periodTicks));
  }

  public BukkitTask timerAsync(Runnable task, long delayTicks, long periodTicks) {
    return Bukkit.getScheduler()
        .runTaskTimerAsynchronously(
            plugin, task, Math.max(0L, delayTicks), Math.max(1L, periodTicks));
  }

  public void cancel(BukkitTask task) {
    if (task != null) {
      task.cancel();
    }
  }
}
