# Repository Guidelines

This guide keeps AutoTreeChopPlus contributors aligned across code, assets, and release tooling.

## Project Structure & Module Organization
- `src/main/java/org/atcplus/autotreechopplus` holds plugin logic, commands, hooks, and utilities.
- `src/main/resources` stores `plugin.yml`, default config, and localization bundles.
- `libs/` contains optional API jars (Residence, Lands, etc.) that Gradle loads for compileOnly hooks.
- `gradle/`, `build.gradle`, and `settings.gradle` define the Wrapper, repositories, and shaded jar packaging.

## Build, Test, and Development Commands
- `./gradlew clean build` - compile sources, run unit tests, and assemble jars under `build/libs`.
- `./gradlew shadowJar` - produce the shaded `AutoTreeChopPlus-<version>.jar` for server deployment.
- `./gradlew runServer` - boot a Paper test server with the plugin mounted in `run/plugins`.
- `./gradlew modrinth` - publish artifacts once `MODRINTH_TOKEN` is exported in the shell.

## Coding Style & Naming Conventions
- Target Java 17; rely on the Gradle toolchain for local consistency.
- Keep code under the `org.atcplus.autotreechopplus` namespace and mirror package paths with directories.
- Use `autoTreeChopPlus` camelCase for fields, `AutoTreeChopPlus` PascalCase for types, and `ATCPlus_*` when static constants need a prefix.
- Maintain existing TinyTranslations message keys; update locale files when adding new messages.

## Testing Guidelines
- Place any new unit or integration tests under `src/test/java` (create package stubs if missing).
- Prefer JUnit 5 and Mockito for regression coverage; keep Paper-specific tests behind mocks.
- Run `./gradlew test --info` before pushing to ensure database hooks and async helpers remain stable.
- Document manual validation steps (e.g., Paper build number, command matrix) in pull request descriptions.

## Commit & Pull Request Guidelines
- Follow Conventional Commits (`feat:`, `fix:`, `chore:`) with a short scope (e.g., `feat: add folia scheduler guard`).
- Keep pull requests focused (<400 LOC) and list validation steps plus any server console logs.
- Link issues or roadmap items; attach screenshots/GIFs for gameplay changes and paste relevant config diffs when defaults move.
