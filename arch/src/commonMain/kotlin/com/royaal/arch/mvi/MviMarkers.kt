package com.royaal.arch.mvi

/**
 *  Marker interface for immutable (preferably data) class, that holds
 *  UI state of the screen.
 */
interface ScreenState

/**
 *  Marker interface for saving state with [SavedStateHandle][androidx.lifecycle.SavedStateHandle]
 */
interface ScreenSnapshot {

    /**
     *  Singleton object to use when no snapshot needed
     */
    object NoSnapshot : ScreenSnapshot
}

/**
 *  This interface is used to map state to snapshot and backward.
 *  It is needed for two reasons:
 *  1. We usually don't need to save whole state.
 *  2. We can easliy change state without migration, since it is stored via SavedStateHandle.
 */
interface SnapshotHandler<State : ScreenState, Snapshot : ScreenSnapshot> {

    /**
     *  Restore full state from snapshot.
     */
    fun restore(snapshot: Snapshot): State

    /**
     *  Create snapshot from current state.
     */
    fun snapshot(state: State): Snapshot?

    companion object {
        fun <State : ScreenState> noSnapshot(initialState: State) =
            object : SnapshotHandler<State, ScreenSnapshot.NoSnapshot> {
                override fun restore(snapshot: ScreenSnapshot.NoSnapshot): State = initialState

                override fun snapshot(state: State): ScreenSnapshot.NoSnapshot? = null
            }
    }
}

/**
 *  Marker interface for events that are sent by user actions.
 */
interface UserEvent

/**
 *  Marker interface for inner events that view model will send to reducer.
 *  It will always be inside ViewModel and will never be explicitly available.
 */
interface InnerEvent

/**
 *  Marker interface for single side effects that do not affect UI state.
 */
interface SideEffect
