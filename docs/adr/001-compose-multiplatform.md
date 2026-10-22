# ADR-001: Compose Multiplatform for the whole UI

Status: accepted · Date: 2026-10-22

## Context

Zerno ships on Android and iOS with one small team. The design (the Sj design system: stickers, grain, serif reading text, mascots) is custom on every screen, so neither platform's stock components would be used as they are. Reading, plans, streaks and reflections behave the same on both platforms.

## Decision

- All UI is Compose Multiplatform in `commonMain`. Android and iOS hosts are thin: `MainActivity` and `MainViewController` start Koin and call `App()`, nothing more.
- Business logic, data and navigation are Kotlin Multiplatform as well. Platform code lives in `androidMain`/`iosMain` behind `expect`/`actual` or an interface bound in Koin, and stays small (file paths, reduced motion, notifications).
- No Material or Cupertino components: `:designsystem` builds every component on `foundation` and its own tokens, so both platforms look the same.
- Strings, fonts and images are Compose resources (`composeResources/`), in English and Russian.

## Consequences

- One codebase and one set of screenshots to review; a fix lands on both platforms at once.
- Platform conventions (back gesture, text selection, accessibility) are our job to get right on iOS, where Compose is younger than SwiftUI; we test them on a device before release.
- iOS builds need a macOS runner (CI job `ios`), and Kotlin/Native compile times are the slowest part of the build.
