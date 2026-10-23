package app.zerno.feature.reflection.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.feature.reflection.api.ReflectionRoute
import app.zerno.feature.reflection.api.Reflections
import app.zerno.feature.reflection.api.SavedReflection
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.viewmodel.MviViewModel
import kotlinx.coroutines.CancellationException

/**
 * One reflection on the route's passage, with earlier ones listed below. After a session, the session's reflection
 * is loaded and saving replaces it; from Saved, the route's reflection is loaded and edited. Skipping is always
 * allowed.
 */
class ReflectionViewModel(
    savedStateHandle: SavedStateHandle,
    private val route: ReflectionRoute,
    private val reflections: Reflections,
) : MviViewModel<ReflectionState, ReflectionEvent, ReflectionInnerEvent, ReflectionEffect, ScreenSnapshot.NoSnapshot>(
    savedStateHandle = savedStateHandle,
    initialState = ReflectionState(),
    reducer = ReflectionReducer,
    snapshotHandler = SnapshotHandler.noSnapshot(ReflectionState()),
) {
    init {
        if (route.reflectionId != null || route.sessionId != null) {
            intent(LOAD) { existing()?.let { emit(ReflectionInnerEvent.Existing(it.reflection.text)) } }
        }
        subscription(EARLIER) {
            reflections.all.collect { all ->
                emit(
                    ReflectionInnerEvent.EarlierChanged(
                        all.filter { it.reflection.id != route.reflectionId && !it.isTheSessions() },
                    ),
                )
            }
        }
    }

    override fun onEvent(event: ReflectionEvent) {
        when (event) {
            is ReflectionEvent.SaveClicked -> save(event.text)
            ReflectionEvent.SkipClicked -> intent(CLOSE) { postEffect(ReflectionEffect.Close) }
        }
    }

    // Storage can fail; the text stays for another try.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun save(text: String) {
        if (text.isBlank() || state.value.saving) return
        intent(SAVE) {
            emit(ReflectionInnerEvent.Saving)
            try {
                val id = route.reflectionId
                if (id != null) {
                    reflections.edit(id, text)
                } else {
                    reflections.save(route.passage, text, route.sessionId, route.afterPlanStep)
                }
                postEffect(ReflectionEffect.Close)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emit(ReflectionInnerEvent.SaveFailed)
            }
        }
    }

    private suspend fun existing(): SavedReflection? {
        route.reflectionId?.let { return reflections.get(it) }
        return route.sessionId?.let { reflections.forSession(it) }
    }

    private fun SavedReflection.isTheSessions(): Boolean = route.sessionId != null && sessionId == route.sessionId

    private companion object {
        const val LOAD = "load"
        const val EARLIER = "earlier"
        const val SAVE = "save"
        const val CLOSE = "close"
    }
}
