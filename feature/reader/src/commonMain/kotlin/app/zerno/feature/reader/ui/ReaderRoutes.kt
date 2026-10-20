package app.zerno.feature.reader.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The reader, at the place `ReaderSession` holds. Lives here rather than in `:feature:reader:api` until another
 * feature opens the reader (Today in PR-04, the daily session in PR-06); the api module has no navigation types yet.
 */
@Serializable
data object ReaderRoute : NavKey

/** Book picker and chapter grid for the reader's current translation. */
@Serializable
data object BookPickerRoute : NavKey
