package app.zerno.data.db

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask

/**
 * Where `TranslationStore` keeps Bible files: Application Support, excluded from iCloud backup because the files
 * are large and come back from the app or the catalog.
 */
@OptIn(ExperimentalForeignApi::class)
fun bibleDirectory(): Path {
    val support = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val dir = requireNotNull(support?.URLByAppendingPathComponent("bibles")) { "No Application Support directory" }
    NSFileManager.defaultManager.createDirectoryAtURL(
        dir,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    dir.setResourceValue(NSNumber(bool = true), forKey = NSURLIsExcludedFromBackupKey, error = null)
    return Path(requireNotNull(dir.path))
}
