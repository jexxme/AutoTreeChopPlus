# AutoTreeChopPlus

AutoTreeChopPlus is a community-maintained fork of the AutoTreeChop Bukkit/Paper plugin. The fork refreshes the build tooling, renames the plugin namespace, and lays the groundwork for new automation features while staying compatible with existing server setups.

## Project Layout
- `src/main/java` - plugin sources under the `org.atcplus.autotreechopplus` package.
- `src/main/resources` - plugin metadata, configuration templates, and translations.
- `libs` - optional hooks for territory plugins required at compile time.
- `gradle`, `build.gradle`, `settings.gradle` - Gradle wrapper and build logic.

## Development Prerequisites
1. Install JDK 17 (`winget install EclipseAdoptium.Temurin.17.JDK` on Windows or `brew install temurin17` on macOS).
2. Install [Gradle](https://gradle.org/install/) or rely on the bundled wrapper (`gradlew`).
3. Resolve Paperweight tooling via the bundled `run-paper` Gradle plugin (downloaded on first build).
4. (Optional) Install an IDE with Java support such as IntelliJ IDEA.

## Core Commands
- `./gradlew clean build` - compile, run unit tests, and produce distributable jars in `build/libs`.
- `./gradlew shadowJar` - build the shaded plugin jar (`AutoTreeChopPlus-<version>.jar`).
- `./gradlew runServer` - launch a disposable Paper test server with the plugin loaded.
- `./gradlew test --info` - execute tests with verbose logging.

## Publishing & Releases
- Update `version` in `build.gradle` following semantic versioning (e.g., `2.0.0-atcplus.1`).
- Snapshot builds can be uploaded to Modrinth via `./gradlew modrinth` once `MODRINTH_TOKEN` is exported.
- Publish GitHub releases after tagging: `git tag v2.0.0-atcplus.1 && git push origin v2.0.0-atcplus.1`.

## Contributing
- Follow the formatting rules enforced by upcoming lint/format tasks (`./gradlew lint`, `./gradlew spotlessApply`).
- Keep pull requests scoped and include before/after screenshots when behaviour changes.
- Open issues and feature requests at [github.com/ATCPlus/AutoTreeChopPlus](https://github.com/ATCPlus/AutoTreeChopPlus/issues).
