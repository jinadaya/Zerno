package app.zerno.data.db

import android.content.Context
import kotlinx.io.files.Path

/**
 * Where `TranslationStore` keeps Bible files. Not backed up: the files are large (KJV and RST alone pass Auto
 * Backup's 25 MB quota) and come back from the app or the catalog.
 */
fun bibleDirectory(context: Context): Path = Path(context.applicationContext.noBackupFilesDir.absolutePath, "bibles")
