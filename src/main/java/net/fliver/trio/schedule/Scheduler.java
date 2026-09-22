package net.fliver.trio.schedule;

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

  public Object sync(Runnable task) {
    return RegionScheduler.runSync(plugin, task);
  }

  public Object async(Runnable task) {
    return RegionScheduler.runAsync(plugin, task);
  }

  public Object later(Runnable task, long delayTicks) {
    return RegionScheduler.runLater(plugin, task, delayTicks);
  }

  public Object laterAsync(Runnable task, long delayTicks) {
    return RegionScheduler.runLaterAsync(plugin, task, delayTicks);
  }

  public Object timer(Runnable task, long delayTicks, long periodTicks) {
    return RegionScheduler.runTimer(plugin, task, delayTicks, periodTicks);
  }

  public Object timerAsync(Runnable task, long delayTicks, long periodTicks) {
    return RegionScheduler.runTimerAsync(plugin, task, delayTicks, periodTicks);
  }

  public void runForEntity(
      org.bukkit.entity.Entity entity, Runnable task, Runnable retired) {
    RegionScheduler.runForEntity(plugin, entity, task, retired);
  }

  public Object runAtLocation(org.bukkit.Location location, Runnable task, long delayTicks) {
    return RegionScheduler.runAtLocation(plugin, location, task, delayTicks);
  }

  public Object runAtLocation(org.bukkit.Location location, Runnable task) {
    return RegionScheduler.runAtLocation(plugin, location, task, 0L);
  }

  public void cancel(Object task) {
    RegionScheduler.cancel(task);
  }

  public void cancelAll() {
    RegionScheduler.cancelAll(plugin);
  }

  /** @deprecated Prefer {@link #cancel(Object)}; kept for callers that hold BukkitTask. */
  @Deprecated
  public void cancel(BukkitTask task) {
    RegionScheduler.cancel(task);
  }
}
