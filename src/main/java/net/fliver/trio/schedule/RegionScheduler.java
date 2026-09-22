package net.fliver.trio.schedule;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import net.fliver.trio.platform.Platform;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class RegionScheduler {
  private static volatile Boolean folia;
  private static Method getGlobalRegionScheduler;
  private static Method getAsyncScheduler;
  private static Method getRegionScheduler;
  private static Method globalRun;
  private static Method globalRunDelayed;
  private static Method globalRunAtFixedRate;
  private static Method regionRunDelayed;
  private static Method asyncRunNow;
  private static Method entityGetScheduler;
  private static Method entityRun;
  private static Method taskCancel;

  private static final Map<Plugin, List<Object>> TRACKED = new ConcurrentHashMap<Plugin, List<Object>>();

  private RegionScheduler() {}

  public static boolean isFolia() {
    return Platform.detect().isFolia() || ensureInit();
  }

  private static synchronized boolean ensureInit() {
    if (folia != null) {
      return folia.booleanValue();
    }
    boolean isFolia = false;
    try {
      Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
      Class<?> serverClass = Bukkit.getServer().getClass();
      getGlobalRegionScheduler = serverClass.getMethod("getGlobalRegionScheduler");
      getAsyncScheduler = serverClass.getMethod("getAsyncScheduler");
      Object global = getGlobalRegionScheduler.invoke(Bukkit.getServer());
      globalRun = global.getClass().getMethod("run", Plugin.class, Consumer.class);
      globalRunDelayed =
          global
              .getClass()
              .getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
      try {
        globalRunAtFixedRate =
            global
                .getClass()
                .getMethod(
                    "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
      } catch (NoSuchMethodException e) {
        globalRunAtFixedRate = null;
      }
      Object async = getAsyncScheduler.invoke(Bukkit.getServer());
      asyncRunNow = async.getClass().getMethod("runNow", Plugin.class, Consumer.class);
      try {
        getRegionScheduler = serverClass.getMethod("getRegionScheduler");
        Object region = getRegionScheduler.invoke(Bukkit.getServer());
        try {
          regionRunDelayed =
              region
                  .getClass()
                  .getMethod(
                      "runDelayed", Plugin.class, Location.class, Consumer.class, long.class);
        } catch (NoSuchMethodException e) {
          regionRunDelayed = null;
        }
      } catch (NoSuchMethodException e) {
        getRegionScheduler = null;
        regionRunDelayed = null;
      }
      entityGetScheduler = Entity.class.getMethod("getScheduler");
      Class<?> entitySched = entityGetScheduler.getReturnType();
      try {
        entityRun =
            entitySched.getMethod("run", Plugin.class, Consumer.class, Runnable.class);
      } catch (NoSuchMethodException e) {
        entityRun =
            entitySched.getMethod(
                "execute", Plugin.class, Runnable.class, Runnable.class, long.class);
      }
      taskCancel =
          Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask")
              .getMethod("cancel");
      isFolia = true;
    } catch (Throwable t) {
      isFolia = false;
      getGlobalRegionScheduler = null;
    }
    folia = Boolean.valueOf(isFolia);
    return isFolia;
  }

  public static Object runSync(Plugin plugin, Runnable task) {
    if (!ensureInit()) {
      Object handle = Bukkit.getScheduler().runTask(plugin, task);
      track(plugin, handle);
      return handle;
    }
    try {
      Object global = getGlobalRegionScheduler.invoke(Bukkit.getServer());
      Object handle = globalRun.invoke(global, plugin, consumer(task));
      track(plugin, handle);
      return handle;
    } catch (Throwable t) {
      plugin.getLogger().log(Level.WARNING, "Folia sync schedule failed: " + t.getMessage(), t);
      return null;
    }
  }

  public static Object runAsync(Plugin plugin, Runnable task) {
    if (!ensureInit()) {
      Object handle = Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
      track(plugin, handle);
      return handle;
    }
    try {
      Object async = getAsyncScheduler.invoke(Bukkit.getServer());
      Object handle = asyncRunNow.invoke(async, plugin, consumer(task));
      track(plugin, handle);
      return handle;
    } catch (Throwable t) {
      plugin.getLogger().log(Level.WARNING, "Folia async schedule failed: " + t.getMessage(), t);
      return null;
    }
  }

  public static Object runLater(Plugin plugin, Runnable task, long delayTicks) {
    long delay = Math.max(0L, delayTicks);
    if (!ensureInit()) {
      Object handle = Bukkit.getScheduler().runTaskLater(plugin, task, delay);
      track(plugin, handle);
      return handle;
    }
    try {
      Object global = getGlobalRegionScheduler.invoke(Bukkit.getServer());
      Object handle = globalRunDelayed.invoke(global, plugin, consumer(task), Math.max(1L, delay));
      track(plugin, handle);
      return handle;
    } catch (Throwable t) {
      plugin.getLogger().log(Level.WARNING, "Folia delayed schedule failed: " + t.getMessage(), t);
      return null;
    }
  }

  public static Object runAtLocation(Plugin plugin, Location location, Runnable task, long delayTicks) {
    if (plugin == null || task == null) {
      throw new IllegalArgumentException("plugin/task");
    }
    long delay = Math.max(0L, delayTicks);
    if (!ensureInit() || getRegionScheduler == null || regionRunDelayed == null || location == null) {
      return runLater(plugin, task, delay);
    }
    try {
      Object region = getRegionScheduler.invoke(Bukkit.getServer());
      Object handle =
          regionRunDelayed.invoke(region, plugin, location, consumer(task), Math.max(1L, delay));
      track(plugin, handle);
      return handle;
    } catch (Throwable t) {
      plugin.getLogger().log(Level.WARNING, "Folia region schedule failed: " + t.getMessage(), t);
      return runLater(plugin, task, delay);
    }
  }

  public static Object runLaterAsync(Plugin plugin, Runnable task, long delayTicks) {
    long delay = Math.max(0L, delayTicks);
    if (!ensureInit()) {
      return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
    }
    // Folia has no delayed async helper in older builds — hop to async after a global delay.
    final AtomicReference<Object> handle = new AtomicReference<Object>();
    Object delayed =
        runLater(
            plugin,
            new Runnable() {
              @Override
              public void run() {
                Object asyncHandle = runAsync(plugin, task);
                if (asyncHandle != null) {
                  handle.set(asyncHandle);
                }
              }
            },
            delay);
    return delayed != null ? delayed : handle.get();
  }

  public static Object runTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
    long delay = Math.max(0L, delayTicks);
    long period = Math.max(1L, periodTicks);
    if (!ensureInit()) {
      Object handle = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
      track(plugin, handle);
      return handle;
    }
    try {
      Object global = getGlobalRegionScheduler.invoke(Bukkit.getServer());
      Object handle;
      if (globalRunAtFixedRate != null) {
        handle =
            globalRunAtFixedRate.invoke(
                global, plugin, consumer(task), Math.max(1L, delay), period);
      } else {
        handle = scheduleSelfReschedule(plugin, task, delay, period);
      }
      track(plugin, handle);
      return handle;
    } catch (Throwable t) {
      plugin.getLogger().log(Level.WARNING, "Folia timer schedule failed: " + t.getMessage(), t);
      return null;
    }
  }

  public static Object runTimerAsync(
      Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
    long delay = Math.max(0L, delayTicks);
    long period = Math.max(1L, periodTicks);
    if (!ensureInit()) {
      return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, period);
    }
    AtomicReference<Object> handle = new AtomicReference<>();
    AtomicReference<Boolean> cancelled = new AtomicReference<>(Boolean.FALSE);
    Runnable tick =
        new Runnable() {
          @Override
          public void run() {
            if (Boolean.TRUE.equals(cancelled.get())) {
              return;
            }
            runAsync(plugin, task);
            Object next = runLater(plugin, this, period);
            handle.set(next);
          }
        };
    Object first = runLater(plugin, tick, delay);
    handle.set(first);
    return new Cancellable() {
      @Override
      public void cancel() {
        cancelled.set(Boolean.TRUE);
        RegionScheduler.cancel(handle.get());
      }
    };
  }

  public static void runForEntity(
      Plugin plugin, Entity entity, Runnable task, Runnable retired) {
    if (entity == null) {
      if (retired != null) {
        retired.run();
      }
      return;
    }
    if (!ensureInit()) {
      if (Bukkit.isPrimaryThread()) {
        task.run();
      } else {
        runSync(plugin, task);
      }
      return;
    }
    try {
      Object sched = entityGetScheduler.invoke(entity);
      if ("run".equals(entityRun.getName())) {
        entityRun.invoke(sched, plugin, consumer(task), retired);
      } else {
        entityRun.invoke(sched, plugin, task, retired, Long.valueOf(0L));
      }
    } catch (Throwable t) {
      plugin.getLogger().log(Level.WARNING, "Folia entity schedule failed: " + t.getMessage(), t);
      if (retired != null) {
        retired.run();
      }
    }
  }

  public static void cancel(Object handle) {
    if (handle == null) {
      return;
    }
    if (handle instanceof Cancellable) {
      ((Cancellable) handle).cancel();
      return;
    }
    if (handle instanceof BukkitTask) {
      ((BukkitTask) handle).cancel();
      return;
    }
    if (handle instanceof Number) {
      try {
        Bukkit.getScheduler().cancelTask(((Number) handle).intValue());
      } catch (Throwable ignored) {
      }
      return;
    }
    if (taskCancel != null) {
      try {
        taskCancel.invoke(handle);
      } catch (Throwable ignored) {
      }
    }
  }

  public static void cancelAll(Plugin plugin) {
    if (plugin == null) {
      return;
    }
    List<Object> handles = TRACKED.remove(plugin);
    if (handles != null) {
      List<Object> copy;
      synchronized (handles) {
        copy = new ArrayList<Object>(handles);
      }
      for (Object handle : copy) {
        try {
          cancel(handle);
        } catch (Throwable ignored) {
        }
      }
    }
    try {
      Bukkit.getScheduler().cancelTasks(plugin);
    } catch (Throwable ignored) {
    }
  }

  private static void track(Plugin plugin, Object handle) {
    if (plugin == null || handle == null) {
      return;
    }
    List<Object> list = TRACKED.get(plugin);
    if (list == null) {
      List<Object> created = java.util.Collections.synchronizedList(new ArrayList<Object>());
      List<Object> prev = TRACKED.putIfAbsent(plugin, created);
      list = prev == null ? created : prev;
    }
    list.add(handle);
    if (list.size() > 500) {
      synchronized (list) {
        while (list.size() > 500) {
          list.remove(0);
        }
      }
    }
  }

  private static Object scheduleSelfReschedule(
      Plugin plugin, Runnable task, long delay, long period) {
    AtomicReference<Object> handle = new AtomicReference<>();
    AtomicReference<Boolean> cancelled = new AtomicReference<>(Boolean.FALSE);
    Runnable tick =
        new Runnable() {
          @Override
          public void run() {
            if (Boolean.TRUE.equals(cancelled.get())) {
              return;
            }
            task.run();
            Object next = runLater(plugin, this, period);
            handle.set(next);
          }
        };
    Object first = runLater(plugin, tick, delay);
    handle.set(first);
    return new Cancellable() {
      @Override
      public void cancel() {
        cancelled.set(Boolean.TRUE);
        RegionScheduler.cancel(handle.get());
      }
    };
  }

  private static Consumer<Object> consumer(final Runnable task) {
    return new Consumer<Object>() {
      @Override
      public void accept(Object scheduledTask) {
        task.run();
      }
    };
  }

  public interface Cancellable {
    void cancel();
  }
}
