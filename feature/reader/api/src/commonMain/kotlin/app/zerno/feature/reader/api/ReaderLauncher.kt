package app.zerno.feature.reader.api

import app.zerno.entities.bible.PassageRef

/** Points the reader at a passage before another feature navigates to [ReaderRoute]. */
interface ReaderLauncher {
    /**
     * The reader shows [passage] next, in the current translation (or the first-run one on a fresh install): at its
     * start, or with [resume] at the verse the reader was left at in its first chapter. [passage] is in KJV
     * numbering, as plans write it; Synodal translations get it mapped.
     */
    suspend fun openAt(passage: PassageRef, resume: Boolean = false)

    /**
     * How far the reader got in the first chapter of [passage], in the current translation. Null when it was never
     * opened there, or is still at the passage's start.
     */
    suspend fun progressIn(passage: PassageRef): PassageProgress?
}

/** [verse] of [verses] is at the top of the page. */
data class PassageProgress(val verse: Int, val verses: Int) {
    val fraction: Float
        get() = verse.toFloat() / verses
}
