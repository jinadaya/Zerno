# ADR-002: MVI with unidirectional data flow on `:arch`

Status: accepted · Date: 2026-10-22

## Context

Screens such as the reader, Today and a plan day combine several data sources (Bible files, user database, settings, gamification) and survive process death on Android. We want every screen built the same way so that any screen can be read, tested and changed without learning a new pattern.

## Decision

- Each screen has one store: a `MviViewModel` from `:arch` (package `com.royaal.arch.mvi`). It owns:
  - **State**: one immutable `ScreenState` data class, exposed as a `StateFlow`. The UI renders only from it.
  - **Intent** (user events): a sealed `UserEvent` the UI sends through `onEvent`. It is the only way the UI asks for change.
  - **Inner events**: a sealed `InnerEvent` the store sends to a pure `MviReducer` that returns the next state. Reducers have no side effects and are unit tested.
  - **Effect**: a sealed `SideEffect` for one-off actions (navigate, show a toast, share), delivered through a buffered channel so none is lost while the screen is stopped.
- Long work runs in the store's intent scope (`IntentExecutor`) off the main thread; results come back as inner events.
- What must survive process death goes into a small `ScreenSnapshot` through a `SnapshotHandler`, not the whole state.
- A screen's contract (`XContract.kt`: State, Event, Effect) sits next to its ViewModel in `ui/`; use cases and repositories in `domain/` are pure Kotlin.

## Consequences

- Data flows one way: repository → store → state → UI → event → store. A bug is found by looking at one state and the event that produced it.
- Reducer and store tests run in `commonTest` on every platform with no UI.
- More types per screen than a plain ViewModel; trivial screens may use `SnapshotHandler.noSnapshot` and an empty inner event set to keep it short.
