package net.fliver.trio.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import org.bukkit.plugin.java.JavaPlugin;

public final class SqliteStore {
  private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9_-]{1,32}$");

  private final JavaPlugin plugin;
  private final String name;
  private final File file;
  private Connection connection;

  private SqliteStore(JavaPlugin plugin, String name) {
    this.plugin = plugin;
    this.name = name;
    File folder = new File(plugin.getDataFolder(), "storage");
    this.file = new File(folder, name + ".db");
  }

  public static SqliteStore of(JavaPlugin plugin, String name) {
    if (plugin == null) {
      throw new IllegalArgumentException("plugin");
    }
    if (name == null || !SAFE_NAME.matcher(name).matches()) {
      throw new IllegalArgumentException("name");
    }
    return new SqliteStore(plugin, name);
  }

  public String name() {
    return name;
  }

  public Connection connection() {
    ensureOpen();
    return connection;
  }

  public void execute(String sql, Object... params) {
    ensureOpen();
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, params);
      statement.executeUpdate();
    } catch (SQLException e) {
      throw new IllegalStateException("sqlite execute failed: " + e.getMessage(), e);
    }
  }

  public int queryInt(String sql, int def, Object... params) {
    ensureOpen();
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, params);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        }
        return def;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("sqlite queryInt failed: " + e.getMessage(), e);
    }
  }

  public String queryString(String sql, String def, Object... params) {
    ensureOpen();
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, params);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          String value = rs.getString(1);
          return value == null ? def : value;
        }
        return def;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("sqlite queryString failed: " + e.getMessage(), e);
    }
  }

  public boolean queryBool(String sql, boolean def, Object... params) {
    ensureOpen();
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      bind(statement, params);
      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          return rs.getBoolean(1);
        }
        return def;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("sqlite queryBool failed: " + e.getMessage(), e);
    }
  }

  public void close() {
    if (connection == null) {
      return;
    }
    try {
      connection.close();
    } catch (SQLException e) {
      plugin.getLogger().warning("Could not close sqlite \"" + name + "\": " + e.getMessage());
    } finally {
      connection = null;
    }
  }

  private void ensureOpen() {
    try {
      if (connection != null && !connection.isClosed()) {
        return;
      }
    } catch (SQLException e) {
      connection = null;
    }
    File folder = file.getParentFile();
    if (folder != null && !folder.exists() && !folder.mkdirs()) {
      throw new IllegalStateException("Could not create storage folder: " + folder.getAbsolutePath());
    }
    try {
      Class.forName("org.sqlite.JDBC");
      connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    } catch (ClassNotFoundException | SQLException e) {
      throw new IllegalStateException("Could not open sqlite \"" + name + "\": " + e.getMessage(), e);
    }
  }

  private static void bind(PreparedStatement statement, Object... params) throws SQLException {
    if (params == null) {
      return;
    }
    for (int i = 0; i < params.length; i++) {
      statement.setObject(i + 1, params[i]);
    }
  }
}
