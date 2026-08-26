# Contributing

## Build

```
mvn clean install
```

Java 17+ and the Paper API repository (declared in `pom.xml`) are required.
The library jar is written to `target/trio-<version>.jar`.

Example plugin:

```
mvn -f examples/hello-trio/pom.xml package
```

## Scope

Trio is a Paper foundation library only: config, lang, messages, and
commands. Do not add Fliver product integrations, network tunnels, or
Discord clients here.

## Pull requests

- Keep changes focused — one concern per PR.
- Public surfaces stay in English (docs, default lang files, user-facing strings).
- Do not add explanatory comments or Javadoc to source files.
- Avoid new runtime dependencies unless there is a strong reason.

## License

Contributions are accepted under the Apache License 2.0.
