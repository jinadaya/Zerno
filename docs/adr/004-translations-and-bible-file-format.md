# ADR-004: Translations and the Bible file format

Status: accepted · Date: 2026-10-16

## Context

The reader needs Scripture text offline in several translations (KJV and RST bundled, ASV and NRT downloaded, KM later). The backend prototype already stores each translation as a MyBible-style SQLite file, and those files differ in small ways: extra tables in some, different markup tags, different versification.

## Decision

- Every translation is one read-only SQLite file in the layout of [Bible file schema v1](../bible-schema-v1.md). The canonical copy of that spec lives in the backend repository at `docs/bible-schema-v1.md`; the file in this repo mirrors it and must be updated in the same change.
- The app reads only the required parts: `info` (`language`, `russian_numbering`, `description`), `books`, `verses` and the markup tags listed in the spec. Everything else in a file is ignored.
- Books are identified by MyBible `book_number` (10 to 730), never by name. Plans, bookmarks and progress store `book_number`, `chapter`, `verse`.
- References are read in the versification of the translation they were made in; there is no cross-versification mapping in v1.
- Licence and attribution come from the catalog (BE-04), not from the file.
- Files are bundled or downloaded with their catalog `schemaVersion`; the app refuses a schema version it does not know.

## Consequences

- One `BibleDb` (DA-02) and one `VerseMarkup` parser serve every translation; adding a translation is a data change once its file passes the validator (BE-03).
- A new markup tag or required field is a schema v2 and needs an app release that understands it.
- Switching translation can land on a different psalm or verse number for the same passage until a versification map exists.
