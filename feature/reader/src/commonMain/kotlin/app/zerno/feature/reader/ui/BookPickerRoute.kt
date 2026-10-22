package app.zerno.feature.reader.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// ReaderRoute, the one other features open, is in `:feature:reader:api`.

/** Book picker and chapter grid for the reader's current translation. */
@Serializable
data object BookPickerRoute : NavKey
