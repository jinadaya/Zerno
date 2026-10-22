package app.zerno.data.db.bible

import app.zerno.entities.bible.VerseSpan
import app.zerno.entities.bible.VerseStyle
import app.zerno.entities.bible.VerseText

/**
 * Turns the MyBible markup of schema v1 (section 5) into [VerseText].
 *
 * `<S>` and `<f>` go with their content; `<pb/>`, `<br/>` and `<t>` become line breaks (and a paragraph start for
 * `<pb/>`); `<J>`, `<i>`, `<em>`, `<e>` and `<n>` become spans. Lenient like the spec asks of a reader: a span still
 * open at the end of the verse is closed there, a closing tag with no opener is ignored, and an unknown tag is
 * dropped with its content kept (the validator, not the reader, rejects such files).
 */
object VerseMarkup {
    private val tag = Regex("<(/?)([A-Za-z]+)\\s*(/?)>")

    fun parse(raw: String): VerseText = Builder().apply {
        var at = 0
        for (match in tag.findAll(raw)) {
            text(raw, at, match.range.first)
            val (closing, name, selfClosing) = match.destructured
            when {
                selfClosing.isNotEmpty() -> empty(name)
                closing.isNotEmpty() -> close(name)
                else -> open(name)
            }
            at = match.range.last + 1
        }
        text(raw, at, raw.length)
    }.build()

    private class Builder {
        private val out = StringBuilder()
        private val spans = mutableListOf<VerseSpan>()
        private val paragraphs = mutableListOf<Int>()
        private val open = mutableListOf<Pair<String, Int>>()
        private var pendingSpace = false
        private var skipDepth = 0
        private var poetryDepth = 0

        fun text(raw: String, from: Int, to: Int) {
            if (skipDepth > 0) return
            for (i in from until to) {
                val c = raw[i]
                if (c.isWhitespace()) {
                    pendingSpace = out.isNotEmpty() && !out.last().isWhitespace()
                } else {
                    flushSpace()
                    out.append(c)
                }
            }
        }

        fun empty(name: String) {
            if (skipDepth > 0) return
            when (name) {
                "pb" -> {
                    lineBreak()
                    if (paragraphs.lastOrNull() != out.length) paragraphs += out.length
                }

                "br" -> lineBreak()
            }
        }

        fun open(name: String) {
            if (name in SKIPPED) {
                skipDepth++
                return
            }
            if (skipDepth > 0) return
            if (name == "t") {
                poetryDepth++
                lineBreak()
            } else if (name in STYLES) {
                flushSpace()
                open += name to out.length
            }
        }

        fun close(name: String) {
            if (name in SKIPPED) {
                if (skipDepth > 0) skipDepth--
                return
            }
            if (skipDepth > 0) return
            if (name == "t") {
                if (poetryDepth > 0) {
                    poetryDepth--
                    lineBreak()
                }
                return
            }
            val index = open.indexOfLast { it.first == name }
            if (index < 0) return
            val (_, start) = open.removeAt(index)
            addSpan(name, start, out.length)
        }

        fun build(): VerseText {
            open.forEach { (name, start) -> addSpan(name, start, out.length) }
            while (out.isNotEmpty() && out.last() == '\n') out.setLength(out.length - 1)
            val length = out.length
            return VerseText(
                text = out.toString(),
                spans = spans.mapNotNull { it.trimmed(length) }.sortedBy { it.start },
                paragraphStarts = paragraphs.filter { it == 0 || it < length },
            )
        }

        /** Clamped to the text and without whitespace at either end ("<i> there</i>"), or null when nothing is left. */
        private fun VerseSpan.trimmed(length: Int): VerseSpan? {
            var from = start
            var to = end.coerceAtMost(length)
            while (from < to && out[from].isWhitespace()) from++
            while (to > from && out[to - 1].isWhitespace()) to--
            return if (to > from) VerseSpan(style, from, to) else null
        }

        private fun flushSpace() {
            if (pendingSpace) out.append(' ')
            pendingSpace = false
        }

        private fun lineBreak() {
            pendingSpace = false
            if (out.isNotEmpty() && out.last() != '\n') out.append('\n')
        }

        private fun addSpan(name: String, start: Int, end: Int) {
            if (end > start) spans += VerseSpan(STYLES.getValue(name), start, end)
        }
    }

    private val SKIPPED = setOf("S", "f")
    private val STYLES = mapOf(
        "J" to VerseStyle.WordsOfJesus,
        "i" to VerseStyle.Italic,
        "em" to VerseStyle.Italic,
        "e" to VerseStyle.Italic,
        "n" to VerseStyle.Note,
    )
}
