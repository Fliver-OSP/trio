<p align="center">
  <a href="https://github.com/Fliver-OSP/trio">
    <img src=".github/assets/trio-logo.png" alt="Trio" width="128" />
  </a>
</p>

<p align="center">
  <a href="https://github.com/Fliver-OSP"><img src="https://img.shields.io/badge/made%20by-Fliver-111111?style=flat-square" alt="Made by Fliver-OSP" /></a>
  <a href="https://github.com/Fliver-OSP/trio/releases"><img src="https://img.shields.io/badge/version-0.6.0--beta-blue?style=flat-square" alt="Version" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-green?style=flat-square" alt="License" /></a>
  <a href="https://papermc.io"><img src="https://img.shields.io/badge/Spigot%2FPaper%2FPurpur%2FFolia-1.8.8%E2%80%9326.2-brightgreen?style=flat-square" alt="Servers" /></a>
</p>

---

> **Documentation:** [docs.fliver.net/sdk/trio](https://docs.fliver.net/sdk/trio)

## Getting Started

Trio covers the boring parts so you can build the fun parts: config, language
files, colored messages, nested commands (tree + optional Brigadier), cooldowns,
outbound HTTP, soft-depends, YAML and SQLite storage, Folia-safe scheduler
helpers, chest menus, tiny JSON, update checks, and permissions. It does not
call home to Fliver or any cloud service — shade it (and `sqlite-jdbc`) into
your jar like any other library.

- Example: [`examples/hello-trio`](examples/hello-trio)
- Coordinates: `net.fliver:trio:0.6.0-beta` (compiled as **Java 8** bytecode)

### Support matrix

| Server | Minecraft | Notes |
| --- | --- | --- |
| Spigot | 1.8.8 → latest | Floor compile target |
| Paper | 1.8.8 → 26.2 | Primary modern target |
| Purpur | Paper range | Paper fork — no Purpur-specific code |
| Folia | when available | Set `folia-supported: true` in your `plugin.yml` |

### Server Java vs Trio bytecode

Trio ships **Java 8** class files so one jar loads on every JVM from 8 up.
The **server** still needs the Java version that Minecraft build requires:

| Minecraft | Server JVM |
| --- | --- |
| 1.8.8 – 1.16.x | Java 8+ |
| 1.17 – 1.20.4 | Java 17+ |
| 1.20.5+ / 1.21.x | Java 21+ |
| 26.1 / 26.2 | Java 25 |

`Trio.create()` logs the detected platform and warns if the runtime Java is
below the recommended version for that Minecraft build.

### Install

Artifacts are published to **https://fliver.net/maven** automatically when
`products/trio` lands on `main` (GitHub Action → `web/public/maven` → Vercel).

Add the Fliver Maven repo, then depend on **`net.fliver:trio`**:

```xml
<repository>
  <id>fliver</id>
  <url>https://fliver.net/maven</url>
</repository>

<dependency>
  <groupId>net.fliver</groupId>
  <artifactId>trio</artifactId>
  <version>0.6.0-beta</version>
</dependency>
```

```gradle
repositories { maven { url = uri("https://fliver.net/maven") } }
dependencies {
  implementation("net.fliver:trio:0.6.0-beta")
}
```

Shade Trio (and its `sqlite-jdbc` transitive) into your plugin jar. Example:
[`examples/hello-trio`](examples/hello-trio).

### Usage

Ship `config.yml` and `lang/en_US.yml`, declare the command in `plugin.yml`
(omit `api-version` for the widest load range; set `folia-supported: true`
if you run on Folia).

Messages use `&` color codes and `%placeholder%` substitution (same style as
Fliver Zen). On Paper with Adventure, MiniMessage-looking tags (`<green>…`)
are also accepted via a reflection bridge.

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

```yaml
prefix: "&8[&aMyPlugin&8] &r"
hello: "&aHello, %player%!"
```

Also available:

```java
trio.platform().kind();
trio.cooldowns().ready("shop", player.getUniqueId(), 5_000L);
trio.http().get(url, response -> {}, error -> {});
trio.http().post(url, "{\"hello\":\"world\"}", response -> {}, error -> {});
trio.scheduler().later(() -> {}, 20L);
trio.scheduler().runAtLocation(location, () -> {}, 20L);
trio.menus().chest("Shop", 3).set(13, item, click -> {}).open(player);
trio.menus().chestMini("menu-title", 3);
trio.parseJson("{\"version\":\"1.0\"}");
trio.checkUpdates(feedUrl, release -> {}, error -> {});
```

Call `trio.close()` from `onDisable`: it cancels your tasks, closes SQLite handles, and flushes YAML stores.

### Migration 0.5 → 0.6

- Everything from 0.5 still compiles. This release only adds overloads and helpers.
- Cooldowns accept plain string ids now (`ready("shop", "console", ms)`), old `UUID` methods stay.
- `Http` pool is bounded (2 to 8 threads) and has `post/put/delete` plus `shutdown()`.
- `Commands.tree()` understands `usage()`, `playerOnly()`, `consoleOnly()`, and tells the sender why a command refused instead of staying quiet.
- `SqliteStore` is thread-safe with `transaction()`, `queryLong()`, and `queryStrings()`.
- New bits: `Json`, `Updates`, `ItemBuilder`, `SoftDepends.onEnable`, menu `fillBorder/onClose`, message `title/actionbar/sendList`.

---

## Contributing

See [Contributing](CONTRIBUTING.md).
