package app.zerno.feature.reader.domain

import app.zerno.data.prefs.ScriptureSize
import app.zerno.entities.bible.ChapterRef
import app.zerno.entities.bible.Versification

/** What the reader remembers between launches. */
interface ReaderPrefs {
    /** Where the reader was last, or null on a fresh install. */
    suspend fun lastLocation(): ReaderLocation?

    suspend fun saveLastLocation(location: ReaderLocation)

    /**
     * The verse at the top of the page when [chapter] was last read in [versification] numbering. Kept per
     * numbering, not per translation, so RST and NRT share their places. Null for the top of the chapter or a
     * chapter never opened.
     */
    suspend fun position(versification: Versification, chapter: ChapterRef): Int?

    suspend fun savePosition(versification: Versification, chapter: ChapterRef, verse: Int?)

    /** Scripture size in sp, or null when never changed. */
    suspend fun fontSize(): Int?

    suspend fun saveFontSize(size: Int)
}

/** Scripture sizes the reader offers; the same as settings, see [ScriptureSize]. */
object ReaderFontSize {
    const val MIN = ScriptureSize.MIN
    const val MAX = ScriptureSize.MAX
    const val DEFAULT = ScriptureSize.DEFAULT
    const val STEP = ScriptureSize.STEP

    /** [size] moved by [steps] steps, kept in range. */
    fun step(size: Int, steps: Int): Int = ScriptureSize.step(size, steps)
}
