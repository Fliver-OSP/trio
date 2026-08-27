<p align="center">
  <a href="https://github.com/Fliver-OSP/trio">
    <img src=".github/assets/trio-logo.png" alt="Trio" width="128" />
  </a>
</p>

<p align="center">
  <a href="https://github.com/Fliver-OSP"><img src="https://img.shields.io/badge/made%20by-Fliver-111111?style=flat-square" alt="Made by Fliver-OSP" /></a>
  <a href="https://github.com/Fliver-OSP/trio/releases"><img src="https://img.shields.io/badge/version-0.4.0--beta-blue?style=flat-square" alt="Version" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-green?style=flat-square" alt="License" /></a>
  <a href="https://papermc.io"><img src="https://img.shields.io/badge/Paper%2FPurpur%2FFolia-1.20%E2%80%9326.2-brightgreen?style=flat-square" alt="Paper" /></a>
</p>

---

## Getting Started

Trio helps you wire the usual Paper plugin plumbing: config, language files,
MiniMessage messages, nested commands (tree + Brigadier), cooldowns, outbound
HTTP, soft-depends, YAML and SQLite storage, Folia-safe scheduler helpers,
chest menus (String or Adventure `Component`), and permissions. It does not
talk to Fliver or any cloud service — shade it (and `sqlite-jdbc`) into your
jar like any other library.

- Example: [`examples/hello-trio`](examples/hello-trio)
- Coordinates: `net.fliver:trio:0.4.0-beta` (compiled as **Java 17** bytecode)

### Support matrix

| Server | Minecraft | Notes |
| --- | --- | --- |
| Paper | 1.20 → 26.2 | Primary target |
| Purpur | 1.20 → 26.2 | Paper fork — no Purpur-specific code |
| Folia | 1.20 → 26.2 | Set `folia-supported: true` in your `plugin.yml` |
| Spigot | 1.20+ | Works for core APIs; Brigadier lifecycle needs Paper |

### Server Java vs Trio bytecode

Trio ships **Java 17** class files so one jar loads on every JVM from 17 up.
The **server** still needs the Java version Mojang/Paper require for that
Minecraft build:

| Minecraft | Server JVM |
| --- | --- |
| 1.20.x | Java 17+ |
| 1.20.5+ / 1.21.x | Java 21+ |
| 26.1 / 26.2 | Java 25 |

`Trio.create()` logs the detected platform and warns if the runtime Java is
below the recommended version for that Minecraft build.

### Install

JitPack (needs a git tag):

```xml
<repository>
  <id>jitpack.io</id>
  <url>https://jitpack.io</url>
</repository>

<dependency>
  <groupId>com.github.Fliver-OSP</groupId>
  <artifactId>trio</artifactId>
  <version>v0.4.0-beta</version>
</dependency>
```

```gradle
repositories { maven { url "https://jitpack.io" } }
dependencies {
  implementation("com.github.Fliver-OSP:trio:v0.4.0-beta")
}
```

Or from this repo:

```
mvn clean install
mvn -f examples/hello-trio/pom.xml package
```

Optional compile check against the latest Paper API (Java 25 toolchain):

```
mvn -Pverify-latest clean verify
```

### Usage

Ship `config.yml` and `lang/en_US.yml`, declare the command in `plugin.yml`
(omit `api-version` for the widest load range; set `folia-supported: true`
if you run on Folia):

```java
Trio trio = Trio.create(this);
trio.configs().saveDefaults().reload();
trio.loadLang(trio.configs().string("language", "en_US"));

trio.bindCommand(
    "hello",
    Commands.tree()
        .permission("myplugin.hello")
        .executes((sender, args) ->
            trio.messages().send(sender, "hello", "player", sender.getName())));
```

Brigadier (Paper lifecycle when available, otherwise the same tree fallback):

```java
BrigadierCommands.Node root =
    Commands.brigadier("hello")
        .permission("myplugin.hello")
        .executes(ctx -> trio.messages().send(ctx.sender(), "hello"));
trio.registerCommand("hello", root);
```

```yaml
prefix: "<gray>[MyPlugin]</gray> "
hello: "<green>Hello, %player%!</green>"
```

Also available on the same `Trio` instance:

```java
trio.platform().kind(); // PAPER / PURPUR / FOLIA / …
trio.cooldowns().ready("shop", playerId, 5_000L);
trio.http().get(url, response -> {}, error -> {});
trio.softDepends().present("Vault");
trio.scheduler().later(() -> {}, 20L);
trio.scheduler().runForEntity(player, () -> {}, null); // Folia-safe
trio.menus().chest("Shop", 3).set(13, item, click -> {}).open(player);
trio.menus().chest(trio.lang().component("menu-title"), 3);
trio.permissions().register("myplugin.use", PermissionDefault.TRUE);
YamlStore store = trio.storage("players");
store.set(uuid, "coins", 10);
store.save(uuid);
SqliteStore db = trio.sqlite("players");
db.execute("CREATE TABLE IF NOT EXISTS coins (uuid TEXT PRIMARY KEY, amount INT)");
```

### Purpur smoke checklist

After dropping a Trio-powered plugin on Purpur: start the server, run your
main command, open a menu if you use one, and confirm HTTP/storage paths you
rely on. Purpur is treated as Paper — no extra dependency.

### Migration 0.3 → 0.4

- Version bump to `0.4.0-beta`.
- Prefer omitting `api-version` in consumer `plugin.yml`; add
  `folia-supported: true` for Folia.
- `Scheduler` methods now return `Object` handles (BukkitTask or Folia
  ScheduledTask). Use `scheduler().cancel(handle)`.
- Menus accept `Component` / `chestMini(langKey, rows)` in addition to String.
- New: `platform()`, `Commands.brigadier(...)`, `registerCommand(...)`.

---

## Contributing

Contributions are welcome. See [Contributing](CONTRIBUTING.md) for build
steps and PR guidelines.
