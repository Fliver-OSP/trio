# Contributing

## Build

```
mvn clean install
```

Java 17+ and the Paper repo in `pom.xml` are enough. Jar lands at
`target/trio-<version>.jar`.

```
mvn -f examples/hello-trio/pom.xml package
```

## Scope

Config, lang, messages, commands. Keep Fliver and Discord integrations out
of this repo.

## Pull requests

One concern per PR. Docs and default lang files in English. No comments or
Javadoc in source. New dependencies only when you need them.
