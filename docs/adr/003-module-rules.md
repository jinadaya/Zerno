# ADR-003: Module rules

Status: accepted · Date: 2026-10-22

## Context

The app is split into base modules (`:entities`, `:arch`, `:designsystem`, `:data`, `:navigation`) and one module per feature. Without rules, features start importing each other's screens and ViewModels, build times grow and a change in one feature breaks another.

## Decision

- Every feature is two modules: `:feature:x` (implementation) and `:feature:x:api` (routes and contracts other features may use).
- `:feature:x` may depend on its own `:api`, on other features' `:api` modules and on the base modules. It never depends on another `:feature:y`.
- `:feature:x:api` depends only on `:entities`, other `:api` modules and the Navigation 3 runtime (for `NavKey` route keys). No Compose UI and no implementation module.
- `:composeApp` is the only module that sees every feature implementation; it wires them together through Koin and `:navigation`.
- Convention plugins in `build-logic` (`zerno.feature`, `zerno.feature.api`) add the allowed dependencies, so a module's build file lists only what is special to it.
- `checkModuleRules` (SK-04) enforces the two dependency rules on every feature module; it runs in `check` and in CI.

## Consequences

- A feature can be built, tested and changed without compiling other features' implementations.
- Cross-feature calls go through small interfaces in `:api` (for example `GamificationEvents`), bound in Koin by the implementing feature.
- Adding a dependency the rules forbid fails CI; changing a rule means changing this ADR and `ModuleRules.kt` together.
