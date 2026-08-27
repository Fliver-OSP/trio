<p align="center">
  <a href="https://github.com/Fliver-OSP/trio">
    <img src=".github/assets/trio-logo.png" alt="Trio" width="128" />
  </a>
</p>

<p align="center">
  <a href="https://github.com/Fliver-OSP"><img src="https://img.shields.io/badge/made%20by-Fliver-111111?style=flat-square" alt="Made by Fliver-OSP" /></a>
  <a href="https://github.com/Fliver-OSP/trio/releases"><img src="https://img.shields.io/badge/version-0.5.1--beta-blue?style=flat-square" alt="Version" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-green?style=flat-square" alt="License" /></a>
  <a href="https://papermc.io"><img src="https://img.shields.io/badge/Spigot%2FPaper%2FPurpur%2FFolia-1.8.8%E2%80%9326.2-brightgreen?style=flat-square" alt="Servers" /></a>
</p>

---

## Getting Started

Trio helps you wire the usual plugin plumbing: config, language files, colored
messages, nested commands (tree + optional Brigadier), cooldowns, outbound HTTP,
soft-depends, YAML and SQLite storage, Folia-safe scheduler helpers, chest menus,
and permissions. It does not talk to Fliver or any cloud service — shade it (and
`sqlite-jdbc`) into your jar like any other library.

- Example: [`examples/hello-trio`](examples/hello-trio)
- Coordinates: `net.fliver:trio:0.5.1-beta` (compiled as **Java 8** bytecode)

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

Add the Fliver Maven repo, then depend on **`net.fliver:trio`** (same coordinates
everywhere — not JitPack’s `com.github…` group):

```xml
<repository>
  <id>fliver</id>
  <url>https://fliver.net/maven</url>
</repository>

<dependency>
  <groupId>net.fliver</groupId>
  <artifactId>trio</artifactId>
  <version>0.5.1-beta</version>
</dependency>
```

```gradle
repositories { maven { url = uri("https://fliver.net/maven") } }
dependencies {
  implementation("net.fliver:trio:0.5.1-beta")
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
trio.cooldowns().ready("shop", playerId, 5_000L);
trio.http().get(url, response -> {}, error -> {});
trio.scheduler().later(() -> {}, 20L);
trio.menus().chest("Shop", 3).set(13, item, click -> {}).open(player);
trio.menus().chestMini("menu-title", 3);
```

### Migration 0.4 → 0.5

- Floor moved to Spigot **1.8.8** / Java **8** bytecode (was Paper 1.20 / Java 17).
- Lang/Messages are `&` + `%ph%` first; `Component` / compile-time MiniMessage removed.
- HTTP uses `HttpURLConnection` (no `java.net.http`).
- Menus: `chest(String, rows)` and `chestMini(langKey, rows)` only.

---

## Contributing

See [Contributing](CONTRIBUTING.md).
