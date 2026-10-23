package app.zerno.feature.progress.domain

import kotlinx.datetime.LocalDate

/** "22 Oct 2026" or "22 окт. 2026" for the history list. */
fun LocalDate.shortText(language: String): String {
    val months = if (language == "ru") RussianShortMonths else EnglishShortMonths
    return "$day ${months[month.ordinal]} $year"
}

/** "October 2026" or "Октябрь 2026" for the calendar header. */
fun LocalDate.monthTitle(language: String): String {
    val months = if (language == "ru") RussianMonths else EnglishMonths
    return "${months[month.ordinal]} $year"
}

private val EnglishShortMonths =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val RussianShortMonths =
    listOf("янв.", "февр.", "марта", "апр.", "мая", "июня", "июля", "авг.", "сент.", "окт.", "нояб.", "дек.")
private val EnglishMonths = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
private val RussianMonths = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
)
