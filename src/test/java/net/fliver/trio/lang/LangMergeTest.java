package net.fliver.trio.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class LangMergeTest {

  @Test
  void mergeAddsMissingLeafKeys() {
    YamlConfiguration disk = new YamlConfiguration();
    YamlConfiguration bundled = new YamlConfiguration();
    bundled.set("hello.world", "Hello");
    bundled.set("hello.other", "Other");

    int added = Lang.mergeMissingKeys(disk, bundled);

    assertEquals(2, added);
    assertEquals("Hello", disk.getString("hello.world"));
    assertEquals("Other", disk.getString("hello.other"));
  }

  @Test
  void mergeKeepsExistingTranslations() {
    YamlConfiguration disk = new YamlConfiguration();
    disk.set("hello.world", "Merhaba");
    YamlConfiguration bundled = new YamlConfiguration();
    bundled.set("hello.world", "Hello");
    bundled.set("hello.new", "New");

    int added = Lang.mergeMissingKeys(disk, bundled);

    assertEquals(1, added);
    assertEquals("Merhaba", disk.getString("hello.world"));
    assertEquals("New", disk.getString("hello.new"));
  }

  @Test
  void mergeSkipsConfigurationSections() {
    YamlConfiguration disk = new YamlConfiguration();
    YamlConfiguration bundled = new YamlConfiguration();
    bundled.createSection("hello");
    bundled.set("hello.world", "Hello");

    int added = Lang.mergeMissingKeys(disk, bundled);

    assertEquals(1, added);
    assertEquals("Hello", disk.getString("hello.world"));
  }

  @Test
  void mergeNullBundledAddsNothing() {
    YamlConfiguration disk = new YamlConfiguration();
    assertEquals(0, Lang.mergeMissingKeys(disk, null));
  }
}
