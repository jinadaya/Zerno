package app.zerno.feature.reader.api

import app.zerno.entities.bible.PassageRef

/** Points the reader at a passage before another feature navigates to [ReaderRoute]. */
interface ReaderLauncher {
    /**
     * The reader shows the start of [passage] next, in the current translation (or the first-run one on a fresh
     * install). [passage] is in KJV numbering, as plans write it; Synodal translations get it mapped.
     */
    suspend fun openAt(passage: PassageRef)
}
