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
| `:data` | Room (KMP) databases, network client, preferences; shared infrastructure only |
| `:navigation` | Navigation 3 graph (placeholder, existing code goes here) |
| `:feature:<name>` | Feature implementation with `domain`, `data`, `ui`, `di` packages |
| `:feature:<name>:api` | Routes and contracts other features may use |

Features: `onboarding`, `today`, `reader`, `plans`, `progress`, `gamification`, `reflection`, `account`, `settings`.

Rules: a feature depends on other features only through their `:api` modules; an `:api` module depends only on `:entities`; the `domain` package is pure Kotlin.

## Convention plugins

Shared Gradle setup lives in `build-logic/convention`:

| Plugin | Does |
| --- | --- |
| `zerno.kmp.library` | Kotlin Multiplatform (Android, iosArm64, iosSimulatorArm64) + Android library, namespace from the module path |
| `zerno.kmp.compose` | `zerno.kmp.library` + Compose Multiplatform (runtime, foundation, ui, resources; no Material) |
| `zerno.room` | `zerno.kmp.library` + Room with KSP for every target and the bundled SQLite driver; schemas go to `<module>/schemas` |
| `zerno.feature.api` | `zerno.kmp.library` + serialization, depends on `:entities` |
| `zerno.feature` | `zerno.kmp.compose` + Koin, depends on its own `:api`, `:arch`, `:designsystem`, `:entities`, `:data` |
| `zerno.sj.tokens` | `generateSjTokens`: turns `tokens/tokens.json` into `SjColors`, `SjSpacing`, `SjRadius`, `SjSize` in `commonMain` |

## Design tokens

`designsystem/tokens/tokens.json` is the one source of truth for colours, spacing, radii and sizes. Edit the json, not Kotlin: every build regenerates `app.zerno.designsystem.tokens` into `designsystem/build/generated/sjTokens`. Run `./gradlew :designsystem:generateSjTokens` to refresh the IDE without a full build.

Versions live in `gradle/libs.versions.toml`.
