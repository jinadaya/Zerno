package app.zerno.feature.reflection.domain

import app.zerno.entities.bible.BookNameLanguage
import app.zerno.entities.bible.PassageRef
import app.zerno.entities.bible.Versification
import app.zerno.entities.bible.format
import app.zerno.entities.bible.mapVersification

/** [this] (KJV numbering) as the UI language writes it: "Ps 23", or "Пс 22" in the Synodal numbering. */
fun PassageRef.display(language: BookNameLanguage): String = when (language) {
    BookNameLanguage.English -> format(language)
    BookNameLanguage.Russian -> mapVersification(Versification.English, Versification.Russian).format(language)
}
