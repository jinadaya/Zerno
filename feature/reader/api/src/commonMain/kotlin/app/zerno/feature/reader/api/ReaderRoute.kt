package app.zerno.feature.reader.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The reader, at the place the reading session holds (the Bible tab). */
@Serializable
data object ReaderRoute : NavKey
