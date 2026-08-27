# Contributing

## Build

```
mvn clean install
```

A JDK 8+ toolchain is enough (set `JAVA_HOME`). The library compiles to
**Java 8** bytecode against Spigot API **1.8.8** so one jar runs on
Spigot/Paper/Purpur/Folia from Minecraft 1.8.8 through 26.2. Jar lands at
`target/trio-<version>.jar`.

```
mvn -f examples/hello-trio/pom.xml package
```

## Scope

Config, lang, messages, commands (tree + Brigadier when present), scheduler
(Folia-safe), menus, storage. Keep Fliver and Discord integrations out of
this repo.

## Pull requests

One concern per PR. Docs and default lang files in English. No comments or
Javadoc in source. New dependencies only when you need them.
