package app.zerno.feature.reader.domain

import app.zerno.data.prefs.AppPreferences
import app.zerno.entities.bible.PassageRef
import app.zerno.feature.reader.api.PassageProgress
import app.zerno.feature.reader.api.ReaderLauncher

/** [ReaderLauncher] on the shared [ReaderSession]: starts it the way the reader would, then opens the passage. */
class SessionReaderLauncher(
    private val session: ReaderSession,
    private val appPrefs: AppPreferences,
    private val deviceLanguage: () -> String,
) : ReaderLauncher {
    override suspend fun openAt(passage: PassageRef, resume: Boolean) {
        session.start(appPrefs.prefs.value.translation, deviceLanguage())
        session.openPassage(passage, resume)
    }

    override suspend fun progressIn(passage: PassageRef): PassageProgress? = session.progressIn(passage)
}
