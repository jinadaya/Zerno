package app.zerno.feature.reflection.ui

import app.zerno.feature.reflection.api.SavedReflection
import com.royaal.arch.mvi.InnerEvent
import com.royaal.arch.mvi.MviReducer
import com.royaal.arch.mvi.ScreenState
import com.royaal.arch.mvi.SideEffect
import com.royaal.arch.mvi.UserEvent

/**
 * [existing] is the session's saved reflection, which the field starts with and saving replaces. The text being
 * typed lives in the field, not here, so typing never waits on the store. [earlier] lists the other reflections.
 */
data class ReflectionState(
    val existing: String? = null,
    val earlier: List<SavedReflection> = emptyList(),
    val saving: Boolean = false,
    val failed: Boolean = false,
) : ScreenState

sealed interface ReflectionEvent : UserEvent {
    data class SaveClicked(val text: String) : ReflectionEvent

    data object SkipClicked : ReflectionEvent
}

sealed interface ReflectionInnerEvent : InnerEvent {
    data class Existing(val text: String) : ReflectionInnerEvent

    data class EarlierChanged(val earlier: List<SavedReflection>) : ReflectionInnerEvent

    data object Saving : ReflectionInnerEvent

    data object SaveFailed : ReflectionInnerEvent
}

sealed interface ReflectionEffect : SideEffect {
    data object Close : ReflectionEffect
}

object ReflectionReducer : MviReducer<ReflectionState, ReflectionInnerEvent> {
    override fun reduce(oldState: ReflectionState, event: ReflectionInnerEvent): ReflectionState = when (event) {
        is ReflectionInnerEvent.Existing -> oldState.copy(existing = event.text)
        is ReflectionInnerEvent.EarlierChanged -> oldState.copy(earlier = event.earlier)
        ReflectionInnerEvent.Saving -> oldState.copy(saving = true, failed = false)
        ReflectionInnerEvent.SaveFailed -> oldState.copy(saving = false, failed = true)
    }
}
