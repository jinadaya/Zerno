package app.zerno.feature.today.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The Today tab: the day's reading card and streak week. */
@Serializable
data object TodayRoute : NavKey
