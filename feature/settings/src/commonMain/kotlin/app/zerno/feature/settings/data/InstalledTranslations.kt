package app.zerno.feature.settings.data

import app.zerno.data.db.bible.InstalledTranslation
import app.zerno.data.db.bible.TranslationStore

/** The translations on this device, for the translation choice and the attributions. */
interface InstalledTranslations {
    suspend fun list(): List<InstalledTranslation>
}

internal class StoreInstalledTranslations(private val store: TranslationStore) : InstalledTranslations {
    override suspend fun list(): List<InstalledTranslation> = store.list()
}
