package app.zerno.entities.bible

/** Language of book names when parsing or formatting a [PassageRef]. */
enum class BookNameLanguage(val code: String) {
    English("en"),
    Russian("ru"),
    ;

    companion object {
        /** Language for a code such as "ru"; anything unknown falls back to English. */
        fun fromCode(code: String): BookNameLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: English
    }
}

/** Short is "John" / "Ин", long is "John" / "От Иоанна". */
enum class BookNameStyle { Short, Long }

/**
 * Reference names and chapter counts for the 66 books, keyed by MyBible number.
 *
 * Reader screens show names from the translation file; these are for references typed by people
 * or written in plan JSON, and for places that have no file open. Chapter counts are the same in
 * KJV and RST, so they hold for both numberings.
 */
object BookNames {

    /** Book for a name or abbreviation in English or Russian, or null when unknown or ambiguous. */
    fun find(name: String): BookId? {
        val key = normalize(name)
        if (key.isEmpty()) return null
        byKey[key]?.let { return BookId(it.id) }
        // An unlisted abbreviation ("Deuter", "Апок"): accept it when it starts only one book's spellings.
        val candidates = byKey.filterKeys { it.length > key.length && it.startsWith(key) }.values.toSet()
        return if (key.length >= MIN_PREFIX && candidates.size == 1) BookId(candidates.single().id) else null
    }

    fun name(book: BookId, language: BookNameLanguage, style: BookNameStyle = BookNameStyle.Short): String {
        val entry = entry(book)
        return when (language) {
            BookNameLanguage.English -> if (style == BookNameStyle.Short) entry.enShort else entry.enName
            BookNameLanguage.Russian -> if (style == BookNameStyle.Short) entry.ruShort else entry.ruName
        }
    }

    fun chapterCount(book: BookId): Int = entry(book).chapters

    val all: List<BookId> by lazy { books.map { BookId(it.id) } }

    internal fun entry(book: BookId): Entry =
        byId[book.value] ?: throw IllegalArgumentException("No canonical book with number $book")

    /**
     * Lower case, ё as е, no spaces, dots or hyphens, ordinals and Roman numerals as a digit:
     * "1-е Кор." and "I Cor" become "1кор" and "1cor".
     */
    internal fun normalize(name: String): String {
        var text = name.trim().lowercase().replace('ё', 'е')
        text = romanPrefix(text) ?: text
        if (text.isNotEmpty() && text[0].isDigit()) {
            val rest = text.substring(1)
            val ordinal = ORDINALS.firstOrNull { suffix ->
                rest.startsWith(suffix) && rest.length > suffix.length && !rest[suffix.length].isLetter()
            }
            if (ordinal != null) text = text[0] + rest.substring(ordinal.length)
        }
        return text.filter { it.isLetterOrDigit() }
    }

    private fun romanPrefix(text: String): String? {
        for ((roman, digit) in ROMAN) {
            if (text.startsWith(roman) && text.length > roman.length) {
                val next = text[roman.length]
                if (next == ' ' || next == '.') return digit + text.substring(roman.length)
            }
        }
        return null
    }

    internal class Entry(
        val id: Int,
        val chapters: Int,
        val enName: String,
        val enShort: String,
        val ruName: String,
        val ruShort: String,
        val aliases: List<String>,
    ) {
        /** Every spelling that finds this book, normalized. */
        val keys: List<String>
            get() = (listOf(enName, enShort, ruName, ruShort) + aliases).map(::normalize).distinct()
    }

    private const val MIN_PREFIX = 3
    private val ORDINALS = listOf("-ое", "-ая", "-е", "-я", "st", "nd", "rd", "th")
    private val ROMAN = listOf("iii" to "3", "iv" to "4", "ii" to "2", "i" to "1")

    private fun book(
        id: Int,
        chapters: Int,
        enName: String,
        enShort: String,
        ruName: String,
        ruShort: String,
        vararg aliases: String,
    ) = Entry(id, chapters, enName, enShort, ruName, ruShort, aliases.toList())

    private val books: List<Entry> = listOf(
        book(10, 50, "Genesis", "Gen", "Бытие", "Быт", "Ge", "Gn"),
        book(20, 40, "Exodus", "Exod", "Исход", "Исх", "Exo", "Ex"),
        book(30, 27, "Leviticus", "Lev", "Левит", "Лев", "Lv"),
        book(40, 36, "Numbers", "Num", "Числа", "Чис", "Nu", "Nm", "Числ"),
        book(50, 34, "Deuteronomy", "Deut", "Второзаконие", "Втор", "Deu", "Dt"),
        book(60, 24, "Joshua", "Josh", "Иисус Навин", "Нав", "Jos", "Ис Нав", "Иис Нав", "Иисуса Навина"),
        book(70, 21, "Judges", "Judg", "Судьи", "Суд", "Jdg", "Jg"),
        book(80, 4, "Ruth", "Ruth", "Руфь", "Руф", "Ru", "Rth"),
        book(90, 31, "1 Samuel", "1 Sam", "1 Царств", "1 Цар", "1 Sa", "1 Sm", "1 Kingdoms"),
        book(100, 24, "2 Samuel", "2 Sam", "2 Царств", "2 Цар", "2 Sa", "2 Sm", "2 Kingdoms"),
        book(110, 22, "1 Kings", "1 Kgs", "3 Царств", "3 Цар", "1 Kin", "1 Ki", "3 Kingdoms"),
        book(120, 25, "2 Kings", "2 Kgs", "4 Царств", "4 Цар", "2 Kin", "2 Ki", "4 Kingdoms"),
        book(130, 29, "1 Chronicles", "1 Chr", "1 Паралипоменон", "1 Пар", "1 Chron", "1 Ch"),
        book(140, 36, "2 Chronicles", "2 Chr", "2 Паралипоменон", "2 Пар", "2 Chron", "2 Ch"),
        book(150, 10, "Ezra", "Ezra", "Ездра", "Езд", "Ezr", "1 Ездры"),
        book(160, 13, "Nehemiah", "Neh", "Неемия", "Неем", "Ne"),
        book(190, 10, "Esther", "Esth", "Есфирь", "Есф", "Est"),
        book(220, 42, "Job", "Job", "Иов", "Иов", "Jb"),
        book(230, 150, "Psalms", "Ps", "Псалтирь", "Пс", "Psalm", "Psa", "Pss", "Псалом", "Псалмы", "Псал"),
        book(240, 31, "Proverbs", "Prov", "Притчи", "Притч", "Pro", "Prv", "Pr", "Прит"),
        book(250, 12, "Ecclesiastes", "Eccl", "Екклесиаст", "Еккл", "Ecc", "Eccles", "Qoh", "Ек", "Екк", "Екклезиаст"),
        book(
            260, 8, "Song of Solomon", "Song", "Песнь песней", "Песн",
            "Song of Songs", "SoS", "Canticles", "Cant", "Песня песней",
        ),
        book(290, 66, "Isaiah", "Isa", "Исаия", "Ис", "Is", "Иса"),
        book(300, 52, "Jeremiah", "Jer", "Иеремия", "Иер", "Je", "Jr"),
        book(310, 5, "Lamentations", "Lam", "Плач Иеремии", "Плач", "La"),
        book(330, 48, "Ezekiel", "Ezek", "Иезекииль", "Иез", "Eze", "Ezk"),
        book(340, 12, "Daniel", "Dan", "Даниил", "Дан", "Da", "Dn"),
        book(350, 14, "Hosea", "Hos", "Осия", "Ос", "Ho"),
        book(360, 3, "Joel", "Joel", "Иоиль", "Иоил", "Jl"),
        book(370, 9, "Amos", "Amos", "Амос", "Ам", "Am"),
        book(380, 1, "Obadiah", "Obad", "Авдий", "Авд", "Oba", "Ob"),
        book(390, 4, "Jonah", "Jonah", "Иона", "Ион", "Jon", "Jona", "Jnh"),
        book(400, 7, "Micah", "Mic", "Михей", "Мих", "Mi"),
        book(410, 3, "Nahum", "Nah", "Наум", "Наум", "Na"),
        book(420, 3, "Habakkuk", "Hab", "Аввакум", "Авв", "Hb"),
        book(430, 3, "Zephaniah", "Zeph", "Софония", "Соф", "Zep", "Zp"),
        book(440, 2, "Haggai", "Hag", "Аггей", "Агг", "Hg"),
        book(450, 14, "Zechariah", "Zech", "Захария", "Зах", "Zec", "Zc"),
        book(460, 4, "Malachi", "Mal", "Малахия", "Мал", "Ml"),
        book(470, 28, "Matthew", "Matt", "От Матфея", "Мф", "Mat", "Mt", "Матфея", "Матф", "Мат", "Мт"),
        book(480, 16, "Mark", "Mark", "От Марка", "Мк", "Mar", "Mrk", "Mk", "Марка", "Мар", "Марк"),
        book(490, 24, "Luke", "Luke", "От Луки", "Лк", "Luk", "Lk", "Луки", "Лук", "Лука"),
        book(500, 21, "John", "John", "От Иоанна", "Ин", "Jn", "Jhn", "Joh", "Иоанна", "Иоан"),
        book(510, 28, "Acts", "Acts", "Деяния", "Деян", "Act", "Ac", "Деяния апостолов", "Деяния святых апостолов"),
        book(520, 16, "Romans", "Rom", "Римлянам", "Рим", "Ro", "Rm"),
        book(530, 16, "1 Corinthians", "1 Cor", "1 Коринфянам", "1 Кор", "1 Co"),
        book(540, 13, "2 Corinthians", "2 Cor", "2 Коринфянам", "2 Кор", "2 Co"),
        book(550, 6, "Galatians", "Gal", "Галатам", "Гал", "Ga"),
        book(560, 6, "Ephesians", "Eph", "Ефесянам", "Еф", "Ephes", "Эф", "Эфесянам"),
        book(570, 4, "Philippians", "Phil", "Филиппийцам", "Флп", "Php", "Pp", "Фил", "Филип"),
        book(580, 4, "Colossians", "Col", "Колоссянам", "Кол"),
        book(590, 5, "1 Thessalonians", "1 Thess", "1 Фессалоникийцам", "1 Фес", "1 Thes", "1 Th", "1 Ths", "1 Фесс"),
        book(600, 3, "2 Thessalonians", "2 Thess", "2 Фессалоникийцам", "2 Фес", "2 Thes", "2 Th", "2 Ths", "2 Фесс"),
        book(610, 6, "1 Timothy", "1 Tim", "1 Тимофею", "1 Тим", "1 Ti"),
        book(620, 4, "2 Timothy", "2 Tim", "2 Тимофею", "2 Тим", "2 Ti"),
        book(630, 3, "Titus", "Titus", "Титу", "Тит", "Tit"),
        book(640, 1, "Philemon", "Phlm", "Филимону", "Флм", "Philem", "Phm", "Филим"),
        book(650, 13, "Hebrews", "Heb", "Евреям", "Евр", "He"),
        book(660, 5, "James", "Jas", "Иакова", "Иак", "Jam", "Jm", "Иаков"),
        book(670, 5, "1 Peter", "1 Pet", "1 Петра", "1 Пет", "1 Pe", "1 Pt"),
        book(680, 3, "2 Peter", "2 Pet", "2 Петра", "2 Пет", "2 Pe", "2 Pt"),
        book(690, 5, "1 John", "1 John", "1 Иоанна", "1 Ин", "1 Jn", "1 Jhn", "1 Jo", "1 Joh", "1 Иоан"),
        book(700, 1, "2 John", "2 John", "2 Иоанна", "2 Ин", "2 Jn", "2 Jhn", "2 Jo", "2 Joh", "2 Иоан"),
        book(710, 1, "3 John", "3 John", "3 Иоанна", "3 Ин", "3 Jn", "3 Jhn", "3 Jo", "3 Joh", "3 Иоан"),
        book(720, 1, "Jude", "Jude", "Иуды", "Иуд", "Jud", "Jd", "Иуда"),
        book(730, 22, "Revelation", "Rev", "Откровение", "Откр", "Re", "Rv", "Revelations", "Apocalypse", "Отк", "Апокалипсис"),
    )

    private val byId: Map<Int, Entry> = books.associateBy { it.id }

    private val byKey: Map<String, Entry> = buildMap {
        for (entry in books) {
            for (key in entry.keys) {
                val previous = put(key, entry)
                check(previous == null || previous === entry) {
                    "Book key '$key' is used by both ${previous?.id} and ${entry.id}"
                }
            }
        }
    }
}
