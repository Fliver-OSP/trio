# Contributing

## Build

```
mvn clean install
```

Java 17+ and the Paper repo in `pom.xml` are enough. The library itself is
compiled to **Java 17** bytecode so one jar runs on Paper/Purpur/Folia from
Minecraft 1.20 through 26.2 (the **server** JVM may still need 21 or 25 —
see the README matrix). Jar lands at `target/trio-<version>.jar`.

```
mvn -f examples/hello-trio/pom.xml package
```

Optional check against the newest Paper API (requires JDK 25):

```
mvn -Pverify-latest clean verify
```

## Scope

Config, lang, messages, commands (tree + Brigadier), scheduler (Folia-safe),
menus, storage. Keep Fliver and Discord integrations out of this repo.

## Pull requests

One concern per PR. Docs and default lang files in English. No comments or
Javadoc in source. New dependencies only when you need them.
