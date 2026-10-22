# Zerno

Bible reading app with gentle gamification, built with Kotlin Multiplatform and Compose Multiplatform for Android and iOS.

## Open and run

- **Android Studio**: open the repository root and let Gradle sync. Run the `composeApp` configuration on an emulator or device.
- **iOS**: open `iosApp/iosApp.xcodeproj` in Xcode (or use the iOS run configuration from the Kotlin Multiplatform plugin in Android Studio). Set your team in `iosApp/Configuration/Config.xcconfig` (`TEAM_ID`) to run on a device.

Requires JDK 17+ (Android Studio's bundled JBR works) and Xcode 16+ for iOS.

## Modules

| Module | Holds |
| --- | --- |
| `:composeApp` | Android app and the iOS framework (`ComposeApp`), wires everything together |
| `:entities` | Shared models read by `:data` and the UI |
| `:arch` | MVI / UDF base (placeholder, existing code goes here) |
| `:designsystem` | Tokens, theme, components, mascots |
| `:data` | Room (KMP) `UserDb`, read-only Bible files, network client, preferences; shared infrastructure only |
| `:navigation` | Navigation 3 host: `NavHost`, `Navigator`, `screen<T>` registration from Koin |
| `:feature:<name>` | Feature implementation with `domain`, `data`, `ui`, `di` packages |
| `:feature:<name>:api` | Routes and contracts other features may use |

Features: `onboarding`, `today`, `reader`, `plans`, `progress`, `gamification`, `reflection`, `account`, `settings`.

Rules: a feature depends on other features only through their `:api` modules; an `:api` module depends only on `:entities` (and other `:api` modules); the `domain` package is pure Kotlin. `./gradlew checkModuleRules` (also part of `check` and CI) fails on a broken dependency.

## Convention plugins

Shared Gradle setup lives in `build-logic/convention`:

| Plugin | Does |
| --- | --- |
| `zerno.kmp.library` | Kotlin Multiplatform (Android, iosArm64, iosSimulatorArm64) + Android library, namespace from the module path |
| `zerno.kmp.compose` | `zerno.kmp.library` + Compose Multiplatform (runtime, foundation, ui, resources; no Material) |
| `zerno.room` | `zerno.kmp.library` + Room with KSP for every target and the bundled SQLite driver; schemas go to `<module>/schemas` |
| `zerno.feature.api` | `zerno.kmp.library` + serialization, depends on `:entities` and the Navigation 3 runtime (`NavKey`) |
| `zerno.feature` | `zerno.kmp.compose` + Koin, depends on its own `:api`, `:arch`, `:designsystem`, `:entities`, `:data`, `:navigation` |
| `zerno.sj.tokens` | `generateSjTokens`: turns `tokens/tokens.json` into `SjColors`, `SjSpacing`, `SjRadius`, `SjSize`, `SjTypography` in `commonMain` |

## Navigation

Each feature's routes other features open live in its `:api` module as `@Serializable data object XRoute : NavKey` (or a data class with arguments). The feature registers the screen in its Koin module with `screen<XRoute> { ... }`; `NavHost` in `:navigation` picks up every registration, so nothing central changes when a screen is added. Routes used only inside a feature stay in its `ui` package. Move with `LocalNavigator.current.navigate(route)`.

Debug builds show a small "debug" tag in the bottom corner. It opens the debug menu, which lists every feature's entry screen and the design system catalog.

## Design tokens

`designsystem/tokens/tokens.json` is the one source of truth for colours, type, spacing, radii and sizes. Edit the json, not Kotlin: every build regenerates `app.zerno.designsystem.tokens` into `designsystem/build/generated/sjTokens`. Run `./gradlew :designsystem:generateSjTokens` to refresh the IDE without a full build.

Wrap every screen in `SjTheme` (the app root already does) and read tokens through `SjTheme.colors`, `SjTheme.type`, `SjTheme.spacing`, `SjTheme.shapes` and `SjTheme.motion`. There is no Material theme underneath. `SjTheme.motion` hands out the animation specs, and they all snap when the system asks for reduced motion.

Versions live in `gradle/libs.versions.toml`.

## Decisions

Architecture decisions are in [`docs/adr/`](docs/adr): [001 Compose Multiplatform](docs/adr/001-compose-multiplatform.md), [002 MVI on `:arch`](docs/adr/002-mvi-on-arch.md), [003 module rules](docs/adr/003-module-rules.md), [004 translations and the Bible file format](docs/adr/004-translations-and-bible-file-format.md).
