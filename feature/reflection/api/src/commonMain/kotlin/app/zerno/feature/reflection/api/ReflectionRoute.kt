package app.zerno.feature.reflection.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Writing a reflection after a reading session. */
@Serializable
data object ReflectionRoute : NavKey
