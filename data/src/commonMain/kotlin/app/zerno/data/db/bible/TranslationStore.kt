package app.zerno.data.db.bible

import app.zerno.data.db.user.AppMeta
import app.zerno.data.db.user.AppMetaDao
import app.zerno.entities.bible.TranslationId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * A translation file on this device, bundled or downloaded. Licence and attribution are not here: they come from
 * the catalog (BE-04), not from the file.
 *
 * @property name the file's `description`, e.g. "King James Version".
 */
data class InstalledTranslation(
    val id: TranslationId,
    val language: String,
    val name: String,
    val bundled: Boolean,
    val path: String,
)

/**
 * The Bible files in one app-private directory (`bibleDirectory()` per platform): the bundled ones, copied out of
 * the app on first launch, and the ones [TranslationDownloads] fetches next to them.
 *
 * [list] and [open] copy the bundled files first if needed, so a fresh install reads offline straight away. Which
 * bundled version was copied is kept in [AppMeta] under `bundled.{id}`; a higher [BundledTranslation.version] in an
 * app update copies the file again. Keep one store per app.
 */
class TranslationStore(
    private val directory: Path,
    private val meta: AppMetaDao,
    private val bundled: List<BundledTranslation> = BundledTranslation.all,
    private val source: BundledBibleSource = ResourceBundledBibles,
    private val fileSystem: FileSystem = SystemFileSystem,
) {
    private val installLock = Mutex()
    private var installed = false

    /** Copies bundled files that are missing or older than the app's own copy. Does the work once per store. */
    suspend fun installBundled() = installLock.withLock {
        if (installed) return@withLock
        withContext(Dispatchers.IO) {
            fileSystem.createDirectories(directory)
            for (translation in bundled) {
                val target = fileOf(translation.id)
                val key = "bundled.${translation.id}"
                if (fileSystem.exists(target) && meta.get(key) == translation.version.toString()) continue
                copy(source.read(translation.resource), target)
                meta.put(AppMeta(key, translation.version.toString()))
            }
        }
        installed = true
    }

    /** Every readable Bible file in the directory: bundled first in [BundledTranslation.all] order, then by id. */
    suspend fun list(): List<InstalledTranslation> {
        installBundled()
        val bundledIds = bundled.map { it.id }
        val files = withContext(Dispatchers.IO) {
            fileSystem.list(directory).filter { it.name.endsWith(BIBLE_FILE_SUFFIX) }
        }
        return files
            .mapNotNull { describe(it, bundledIds) }
            .sortedWith(
                compareBy({
                    bundledIds.indexOf(it.id).let { i ->
                        if (i <
                            0
                        ) {
                            Int.MAX_VALUE
                        } else {
                            i
                        }
                    }
                }, { it.id.value }),
            )
    }

    /** Opens [id] read-only; the caller closes it. Fails when the translation is not on this device. */
    suspend fun open(id: TranslationId): BibleFile {
        installBundled()
        val file = fileOf(id)
        return withContext(Dispatchers.IO) {
            require(fileSystem.exists(file)) { "Translation $id is not installed" }
            BibleFile.open(file.toString(), id)
        }
    }

    /**
     * Removes a downloaded translation and any partial download of it. Bundled translations cannot be removed:
     * the app needs them offline. Close the translation's [BibleFile] first.
     */
    suspend fun delete(id: TranslationId) {
        require(bundled.none { it.id == id }) { "Translation $id is bundled" }
        withContext(Dispatchers.IO) {
            val file = fileOf(id)
            fileSystem.deleteSidecars(directory, file)
            fileSystem.delete(file, mustExist = false)
            fileSystem.delete(partFile(id), mustExist = false)
        }
    }

    /** Where [TranslationDownloads] writes [id] until it is complete and checked. */
    internal fun partFile(id: TranslationId) = Path(directory, "${id.value}$BIBLE_FILE_SUFFIX.part")

    /** Moves a complete, checked [partFile] into place over any older copy. */
    internal suspend fun installDownloaded(id: TranslationId) = withContext(Dispatchers.IO) {
        place(partFile(id), fileOf(id))
    }

    private fun fileOf(id: TranslationId) = Path(directory, "${id.value}$BIBLE_FILE_SUFFIX")

    /** Writes next to [target] and moves it into place, so a crash never leaves a half-copied Bible file. */
    private fun copy(bytes: ByteArray, target: Path) {
        val temporary = Path(directory, "${target.name}.tmp")
        fileSystem.sink(temporary).buffered().use { it.write(bytes) }
        place(temporary, target)
    }

    private fun place(complete: Path, target: Path) {
        fileSystem.deleteSidecars(directory, target)
        fileSystem.atomicMove(complete, target)
    }

    /** Reads the required `info` keys; a file that does not open or lacks `language` is left out. */
    private suspend fun describe(file: Path, bundledIds: List<TranslationId>): InstalledTranslation? {
        val id = TranslationId(file.name.removeSuffix(BIBLE_FILE_SUFFIX))
        return runCatching {
            BibleFile.open(file.toString(), id).use { bible ->
                val language = bible.infoValue("language") ?: return@use null
                InstalledTranslation(
                    id = id,
                    language = language,
                    name = bible.infoValue("description") ?: id.value,
                    bundled = id in bundledIds,
                    path = file.toString(),
                )
            }
        }.getOrNull()
    }
}

/** Sidecars of an old file (KJV and RST are WAL-mode files) must not be applied to a new one. */
private fun FileSystem.deleteSidecars(directory: Path, file: Path) =
    listOf("-wal", "-shm").forEach { delete(Path(directory, "${file.name}$it"), mustExist = false) }
