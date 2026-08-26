<p align="center">
  <a href="https://github.com/Fliver-OSP/trio">
    <img src=".github/assets/trio-logo.png" alt="Trio" width="128" />
  </a>
</p>

<p align="center">
  <a href="https://github.com/Fliver-OSP"><img src="https://img.shields.io/badge/made%20by-Fliver-111111?style=flat-square" alt="Made by Fliver-OSP" /></a>
  <a href="https://github.com/Fliver-OSP/trio/releases"><img src="https://img.shields.io/badge/version-0.3.0--beta-blue?style=flat-square" alt="Version" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-green?style=flat-square" alt="License" /></a>
  <a href="https://papermc.io"><img src="https://img.shields.io/badge/Paper-1.20%2B-brightgreen?style=flat-square" alt="Paper" /></a>
</p>

---

## Getting Started

Trio helps you wire the usual Paper plugin plumbing: config, language files,
MiniMessage messages, nested commands, cooldowns, outbound HTTP, soft-depends,
YAML and SQLite storage, scheduler helpers, chest menus, and permissions. It
does not talk to Fliver or any cloud service — shade it (and `sqlite-jdbc`)
into your jar like any other library.

- Example: [`examples/hello-trio`](examples/hello-trio)
- Coordinates: `net.fliver:trio:0.3.0-beta` (Java 17+)

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
  <version>v0.3.0-beta</version>
</dependency>
```

```gradle
repositories { maven { url "https://jitpack.io" } }
dependencies {
  implementation("com.github.Fliver-OSP:trio:v0.3.0-beta")
}
```

Or from this repo:

```
mvn clean install
mvn -f examples/hello-trio/pom.xml package
```

### Usage

Ship `config.yml` and `lang/en_US.yml`, declare the command in `plugin.yml`:

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
prefix: "<gray>[MyPlugin]</gray> "
hello: "<green>Hello, %player%!</green>"
```

Also available on the same `Trio` instance:

```java
trio.cooldowns().ready("shop", playerId, 5_000L);
trio.http().get(url, response -> {}, error -> {});
trio.softDepends().present("Vault");
trio.scheduler().later(() -> {}, 20L);
trio.menus().chest("Shop", 3).set(13, item, click -> {}).open(player);
trio.permissions().register("myplugin.use", PermissionDefault.TRUE);
YamlStore store = trio.storage("players");
store.set(uuid, "coins", 10);
store.save(uuid);
SqliteStore db = trio.sqlite("players");
db.execute("CREATE TABLE IF NOT EXISTS coins (uuid TEXT PRIMARY KEY, amount INT)");
```

---

## Contributing

Contributions are welcome. See [Contributing](CONTRIBUTING.md) for build
steps and PR guidelines.
