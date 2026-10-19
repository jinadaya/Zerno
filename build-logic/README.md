# build-logic

Convention plugins shared by every module. A module's `build.gradle.kts` should apply one of them and add only what is specific to it.

| Plugin id | Apply to | What it sets up |
| --- | --- | --- |
| `zerno.kmp.library` | plain Kotlin modules (`:entities`) | Kotlin Multiplatform with Android (KMP library), `iosArm64`, `iosSimulatorArm64`; JVM 17; namespace from the project path; coroutines; `kotlin-test` in `commonTest` |
| `zerno.kmp.compose` | modules with UI (`:designsystem`, `:arch`, `:navigation`) | `zerno.kmp.library` plus Compose Multiplatform (runtime, foundation, ui, resources, preview). No Material |
| `zerno.room` | `:data` | `zerno.kmp.library` plus Room with the bundled SQLite driver and KSP for every target |
| `zerno.feature` | `:feature:x` | `zerno.kmp.compose`, serialization, its own `:feature:x:api` (as `api`), `:arch`, `:designsystem`, `:entities`, `:data`, Koin (core, compose, compose-viewmodel) |
| `zerno.feature.api` | `:feature:x:api` | `zerno.kmp.library`, serialization, `:entities` only. No Compose UI |
| `zerno.sj.tokens` | `:designsystem` | `generateSjTokens`, which turns `tokens/tokens.json` into Kotlin in `commonMain` |

The plan calls these `sj.kmp.library`, `sj.feature` and `sj.feature.api`; the repo uses the `zerno.` prefix for every convention plugin.

## A new feature module

```kotlin
// feature/x/build.gradle.kts
plugins {
    alias(libs.plugins.zerno.feature)
}
```

```kotlin
// feature/x/api/build.gradle.kts
plugins {
    alias(libs.plugins.zerno.feature.api)
}
```

Then add `include(":feature:x", ":feature:x:api")` to `settings.gradle.kts`. A feature reaches another feature only through its `:api` module.

## Adding a plugin

Write the class in `convention/src/main/kotlin`, register its id in `convention/build.gradle.kts`, and add an alias under "Convention plugins from build-logic" in `gradle/libs.versions.toml`.
