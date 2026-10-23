package app.zerno.feature.reflection.ui

import androidx.lifecycle.SavedStateHandle
import app.zerno.feature.reflection.api.ReflectionRoute
import app.zerno.feature.reflection.api.Reflections
import com.royaal.arch.mvi.ScreenSnapshot
import com.royaal.arch.mvi.SnapshotHandler
import com.royaal.arch.mvi.viewmodel.MviViewModel
import kotlinx.coroutines.CancellationException

/**
 * One reflection on the route's passage, with earlier ones listed below. After a session, the session's reflection
 * is loaded and saving replaces it. Skipping is always allowed.
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
        route.sessionId?.let { session ->
            intent(LOAD) {
                reflections.forSession(session)?.let { emit(ReflectionInnerEvent.Existing(it.reflection.text)) }
            }
        }
        subscription(EARLIER) {
            reflections.all.collect { all ->
                emit(
                    ReflectionInnerEvent.EarlierChanged(
                        all.filter {
                            route.sessionId == null ||
                                it.sessionId != route.sessionId
                        },
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
                reflections.save(route.passage, text, route.sessionId, route.afterPlanStep)
                postEffect(ReflectionEffect.Close)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emit(ReflectionInnerEvent.SaveFailed)
            }
        }
    }

    private companion object {
        const val LOAD = "load"
        const val EARLIER = "earlier"
        const val SAVE = "save"
        const val CLOSE = "close"
    }
}
