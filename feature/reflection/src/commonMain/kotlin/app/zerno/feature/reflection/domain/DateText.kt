package app.zerno.feature.reflection.domain

import kotlinx.datetime.LocalDate

/** "22 Oct 2026" or "22 окт. 2026". */
fun LocalDate.shortText(language: String): String {
    val months = if (language == "ru") RussianMonths else EnglishMonths
    return "$day ${months[month.ordinal]} $year"
}

private val EnglishMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val RussianMonths =
    listOf("янв.", "февр.", "марта", "апр.", "мая", "июня", "июля", "авг.", "сент.", "окт.", "нояб.", "дек.")
