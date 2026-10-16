# Bible file schema v1

Status: frozen · Version: 1 · Applies to: every Bible file the backend serves and the app bundles or downloads

> Mirror of `docs/bible-schema-v1.md` in the ZernoBackend repository, which is the canonical copy. Change both together.

A Zerno Bible file is one SQLite 3 database per translation, in the MyBible module layout the existing files (KJV, ASV, RST, NRT, KM) already use. v1 writes down the subset of that layout the app relies on, so that any file which passes the validator (BE-03) opens in the reader without per-translation code.

Readers must only rely on what this document marks as required. Anything else in a file is allowed and ignored.

## 1. File

| Property | Rule |
| --- | --- |
| Format | SQLite 3, UTF-8 text encoding |
| Name | `{id}.SQLite3`, where `{id}` is the lowercase translation id from the catalog (`kjv`, `asv`, `rst`, `nrt`, `km`) |
| Opened as | Read-only. The app never writes to a Bible file; user data lives in its own database |
| Integrity | The catalog (BE-04) publishes the file's size and SHA-256; the app verifies both after download |
| Scope | Canonical 66 books only. Deuterocanonical books are out of scope for v1 |

## 2. Tables

Three tables are required. Column types are SQLite type affinities; existing files declare `NUMERIC` for the integer columns and store integers in them, which satisfies v1.

### 2.1 `info`

Key/value metadata about the translation.

| Column | Type | Rule |
| --- | --- | --- |
| `name` | TEXT | Key. Unique |
| `value` | TEXT | Value as text. Booleans are the strings `true` and `false` |

### 2.2 `books`

One row per book present in the file.

| Column | Type | Required | Rule |
| --- | --- | --- | --- |
| `book_number` | INTEGER | yes | Book identity, see section 4. Unique |
| `short_name` | TEXT | yes | Abbreviation in the translation's language (`Gen`, `Быт`) |
| `long_name` | TEXT | yes | Full name in the translation's language (`Genesis`, `Бытие`) |
| `book_color` | TEXT | yes | `#rrggbb`. MyBible's per-section colour; the app does not use it in v1 |
| `sorting_order` | INTEGER | no | Display order when it differs from `book_number` order (see 4.2) |

### 2.3 `verses`

One row per verse.

| Column | Type | Rule |
| --- | --- | --- |
| `book_number` | INTEGER | Matches a row in `books` |
| `chapter` | INTEGER | 1-based |
| `verse` | INTEGER | 1-based |
| `text` | TEXT | Verse text with the markup in section 5. Not null, not empty |

Required index, so chapter reads stay fast on a phone:

```sql
CREATE UNIQUE INDEX verses_index ON verses (book_number, chapter, verse);
```

### 2.4 Optional tables

These appear in some of the current files. They are allowed, and v1 readers ignore them.

| Table | Seen in | What it holds |
| --- | --- | --- |
| `books_all` | RST, NRT, KM | Same as `books` plus `is_present`; superseded by `books` |
| `stories` | NRT, KM | Section headings: `book_number, chapter, verse, order_if_several, title`. Candidate for v2 |
| `introductions` | NRT, KM | Book introductions as HTML. Candidate for v2 |

Footnote text is not part of the Bible file. MyBible keeps it in a separate `*.commentaries.SQLite3` module, which v1 does not serve.

## 3. `info` keys

### 3.1 Required

| Key | Value | Example |
| --- | --- | --- |
| `language` | ISO 639-1 code of the text | `en`, `ru`, `tg` |
| `russian_numbering` | `true` if chapters and verses follow Synodal versification, else `false` (see 4.3) | `false` for KJV, `true` for RST |
| `description` | Translation name for display | `King James Version` |

### 3.2 Recognised, optional

| Key | Value | App use in v1 |
| --- | --- | --- |
| `chapter_string` | Word for "Chapter" in the text's language | Reader chapter title |
| `chapter_string_ps` | Word used instead in Psalms ("Psalm", "Псалом") | Reader chapter title in Psalms |
| `right_to_left` | `true` or `false`, default `false` | Text direction |
| `strong_numbers` | `true` if verses carry `<S>` tags | None; tags are stripped |
| `contains_accents` | `true` if the text has stress marks | None |
| `detailed_info` | About-the-edition text, HTML | Translation info sheet, later |
| `introduction_string` | Title for `detailed_info` | Same |
| `history_of_changes` | Text changelog from the source module | None |
| `html_style`, `add_space_before_footnote_marker` | MyBible rendering hints | None |

Licence and attribution are not `info` keys. They live in the backend's translation metadata and reach the app through the catalog (BE-01, BE-04), so a licence change never means re-publishing a file.

Any other key is allowed and ignored.

## 4. Books

### 4.1 Book numbering

`book_number` uses MyBible's fixed numbering. The number identifies the book in every translation, so reading plans, bookmarks and progress store `book_number`, never a name. Book names always come from the file's `books` table; the names below are only a reference.

| `book_number` | KJV short | Book | KJV chapters |
| --- | --- | --- | --- |
| 10 | Gen | Genesis | 50 |
| 20 | Exo | Exodus | 40 |
| 30 | Lev | Leviticus | 27 |
| 40 | Num | Numbers | 36 |
| 50 | Deu | Deuteronomy | 34 |
| 60 | Josh | Joshua | 24 |
| 70 | Judg | Judges | 21 |
| 80 | Ruth | Ruth | 4 |
| 90 | 1Sam | 1 Samuel | 31 |
| 100 | 2Sam | 2 Samuel | 24 |
| 110 | 1Kin | 1 Kings | 22 |
| 120 | 2Kin | 2 Kings | 25 |
| 130 | 1Chr | 1 Chronicles | 29 |
| 140 | 2Chr | 2 Chronicles | 36 |
| 150 | Ezr | Ezra | 10 |
| 160 | Neh | Nehemiah | 13 |
| 190 | Esth | Esther | 10 |
| 220 | Job | Job | 42 |
| 230 | Ps | Psalms | 150 |
| 240 | Prov | Proverbs | 31 |
| 250 | Eccl | Ecclesiastes | 12 |
| 260 | Song | Song of Solomon | 8 |
| 290 | Isa | Isaiah | 66 |
| 300 | Jer | Jeremiah | 52 |
| 310 | Lam | Lamentations | 5 |
| 330 | Ezek | Ezekiel | 48 |
| 340 | Dan | Daniel | 12 |
| 350 | Hos | Hosea | 14 |
| 360 | Joel | Joel | 3 |
| 370 | Am | Amos | 9 |
| 380 | Oba | Obadiah | 1 |
| 390 | Jona | Jonah | 4 |
| 400 | Mic | Micah | 7 |
| 410 | Nah | Nahum | 3 |
| 420 | Hab | Habakkuk | 3 |
| 430 | Zeph | Zephaniah | 3 |
| 440 | Hag | Haggai | 2 |
| 450 | Zech | Zechariah | 14 |
| 460 | Mal | Malachi | 4 |
| 470 | Mat | Matthew | 28 |
| 480 | Mar | Mark | 16 |
| 490 | Luk | Luke | 24 |
| 500 | John | John | 21 |
| 510 | Acts | Acts | 28 |
| 520 | Rom | Romans | 16 |
| 530 | 1Cor | 1 Corinthians | 16 |
| 540 | 2Cor | 2 Corinthians | 13 |
| 550 | Gal | Galatians | 6 |
| 560 | Eph | Ephesians | 6 |
| 570 | Phil | Philippians | 4 |
| 580 | Col | Colossians | 4 |
| 590 | 1Ths | 1 Thessalonians | 5 |
| 600 | 2Ths | 2 Thessalonians | 3 |
| 610 | 1Tim | 1 Timothy | 6 |
| 620 | 2Tim | 2 Timothy | 4 |
| 630 | Tit | Titus | 3 |
| 640 | Phlm | Philemon | 1 |
| 650 | Heb | Hebrews | 13 |
| 660 | Jam | James | 5 |
| 670 | 1Pet | 1 Peter | 5 |
| 680 | 2Pet | 2 Peter | 3 |
| 690 | 1Jn | 1 John | 5 |
| 700 | 2Jn | 2 John | 1 |
| 710 | 3Jn | 3 John | 1 |
| 720 | Jud | Jude | 1 |
| 730 | Rev | Revelation | 22 |

### 4.2 Display order

Without `sorting_order`, books are listed by ascending `book_number`, which is the Protestant order above. A file may carry `sorting_order` to list books in another tradition's order; NRT does this to place James through Jude straight after Acts, as Russian Bibles do. Ordering never changes a book's identity.

### 4.3 Versification

Chapter and verse numbers are the translation's own. `russian_numbering = true` means the file follows Synodal versification, which differs from the KJV tradition in places, for example:

- Most of KJV Psalms 10 to 147 are numbered one lower: KJV Psalm 23 is RST Psalm 22. Many psalm titles are counted as verse 1.
- Romans 16 has 27 verses in KJV and 24 in RST; the doxology sits at the end of Romans 14 instead.

v1 does not map references between versifications. A stored reference (`book_number`, `chapter`, `verse`) is read against the translation it was made in, and a reading plan states which numbering its references use. A mapping table is a candidate for v2.

## 5. Verse markup

Verse text is plain UTF-8 with a small set of MyBible tags. Tags are case-sensitive and carry no attributes. `<pb/>` and `<br/>` are self-closing; every other tag is a pair. There are no HTML entities; `&`, `<` and `>` never appear as text.

| Tag | Meaning | App v1 |
| --- | --- | --- |
| `<S>1063</S>` | Strong's number for the preceding word | Removed with its content |
| `<pb/>` | Paragraph break before what follows | Starts a new paragraph |
| `<br/>` | Line break | Starts a new line |
| `<t>…</t>` | Poetry or quoted line | Starts on its own line; indentation is a reader style choice |
| `<J>…</J>` | Words of Jesus | Kept as an annotation; rendered unstyled for now |
| `<i>…</i>` | Words supplied by the translators (KJV italics) | Italic |
| `<em>…</em>` | Emphasis | Italic |
| `<e>…</e>` | Emphasis | Italic |
| `<n>…</n>` | Note inside the text, such as a psalm title in ASV | Kept, rendered in a muted style |
| `<f>…</f>` | Footnote marker, such as `[1]`; the note itself is in the commentaries module | Removed with its content |

Any other tag makes the file fail validation.

Spans do not cross verse boundaries. A reader closes any span still open at the end of a verse and ignores a closing tag with no opener; the validator reports both as warnings.

Leading and trailing whitespace in `text` is not significant, and runs of whitespace render as one space.

## 6. Versioning

This document is schema version 1. The catalog publishes `schemaVersion` for every file, and the app refuses a file whose schema version it does not know.

- Stays v1: adding an optional table, an optional `info` key, or a column readers can ignore.
- Needs v2: a new markup tag, a new required table, column or key, or a change in meaning of anything above.

Old versions of this document stay in the repository next to new ones.

## 7. Current files against v1

Checked against the files in the backend prototype on 2026-10-16. Full checks, including verse counts per book, are BE-03's job.

| File | Books | Verses | `russian_numbering` | Tags used | Notes |
| --- | --- | --- | --- | --- | --- |
| KJV | 66 | 31,102 | false | `S pb i t J` | 2 `</t>` without an opener |
| ASV | 66 | 31,085 | false | `S n` | Fewer verses than KJV; check in BE-03 |
| RST | 66 | 31,162 | true | `S pb i t J f e br` | Passes |
| NRT | 66 | 31,163 | true | `pb i t J f e` | Passes. Licensed text: not served until the licence is signed |
| KM | 66 | 31,164 | true | `pb t J f e n` | 32 `<J>` without a closing tag; `language` is `tg` |

All five carry the three required tables, the required `info` keys and `verses_index`, and number their books 10 to 730 as in 4.1.
