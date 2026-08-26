# Trio

Paper plugin foundation library for config, language files, MiniMessage
messages, and nested commands. Open source under Apache-2.0.

Trio is **not** Fliver, Live Chat, or Dynamic Forms. It has no Fliver
pairing, tunnels, or cloud dependency. Use it in any Paper plugin.

- **Repository:** https://github.com/Fliver-OSP/trio
- **Coordinates:** `net.fliver:trio:0.1.0-beta`
- **Paper:** 1.20+ (compiled against Paper API 1.20.1)
- **Java:** 17+

## Install (JitPack)

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.Fliver-OSP</groupId>
  <artifactId>trio</artifactId>
  <version>v0.1.0-beta</version>
</dependency>
```

Gradle:

```gradle
repositories {
  maven { url "https://jitpack.io" }
}

dependencies {
  compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
  implementation("com.github.Fliver-OSP:trio:v0.1.0-beta")
}
```

Shade `trio` into your plugin jar (or publish it as a soft-depend later).
Until a Git tag exists, build from source with Maven instead.

## Build from source

```
mvn clean install
```

Example plugin (after install):

```
mvn -f examples/hello-trio/pom.xml package
```

## Minimal usage

Ship `config.yml` and `lang/en_US.yml` in your plugin resources. Declare
commands in `plugin.yml`. Then:

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

Language values use [MiniMessage](https://docs.advntr.dev/minimessage/format.html)
with `%placeholder%` substitution:

```yaml
prefix: "<gray>[MyPlugin]</gray> "
hello: "<green>Hello, %player%!</green>"
```

## Modules

| Class | Role |
|---|---|
| `Trio` | Entry point |
| `Configs` | Default config save/reload and typed getters |
| `Lang` | YAML language files under `lang/` |
| `Messages` | Prefixed MiniMessage sends |
| `Commands` | Nested subcommands, permissions, tab complete |

## License

Apache License 2.0. See [LICENSE](LICENSE).
