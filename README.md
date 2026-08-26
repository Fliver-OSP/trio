<p align="center">
  <a href="https://github.com/Fliver-OSP/trio">
    <img src=".github/assets/trio-logo.png" alt="Trio" width="128" />
  </a>
</p>

<p align="center">
  <strong>Trio</strong>
</p>

<p align="center">
  <a href="https://github.com/Fliver-OSP"><img src="https://img.shields.io/badge/made%20by-Fliver--OSP-111111?style=flat-square" alt="Made by Fliver-OSP" /></a>
  <a href="https://github.com/Fliver-OSP/trio/releases"><img src="https://img.shields.io/badge/version-0.1.0--beta-blue?style=flat-square" alt="Version" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-green?style=flat-square" alt="License" /></a>
  <a href="https://papermc.io"><img src="https://img.shields.io/badge/Paper-1.20%2B-brightgreen?style=flat-square" alt="Paper" /></a>
</p>

---

## Getting Started

Trio helps you wire config, language files, MiniMessage messages, and nested
commands in a Paper plugin. It does not talk to Fliver or any cloud service —
shade it into your jar like any other library.

- Example: [`examples/hello-trio`](examples/hello-trio)
- Coordinates: `net.fliver:trio:0.1.0-beta` (Java 17+)

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
  <version>v0.1.0-beta</version>
</dependency>
```

```gradle
repositories { maven { url "https://jitpack.io" } }
dependencies {
  implementation("com.github.Fliver-OSP:trio:v0.1.0-beta")
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

---

## Contributing

Contributions are welcome. See [Contributing](CONTRIBUTING.md) for build
steps and PR guidelines.
