package com.capturapantalla.vnc.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ScreenshotStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun clearTempCache_deletesExpiredFiles_andRetainsRecentFiles() {
        val cacheDir = tempFolder.newFolder("capturas")
        val storage = ScreenshotStorage(cacheDir)

        val oldFile = File(cacheDir, "compartir_old.jpg").apply {
            createNewFile()
            // Mark file as 2 hours old
            setLastModified(System.currentTimeMillis() - 7200_000L)
        }

        val freshFile = File(cacheDir, "compartir_fresh.jpg").apply {
            createNewFile()
            // Mark file as 5 minutes old
            setLastModified(System.currentTimeMillis() - 300_000L)
        }

        assertTrue(oldFile.exists())
        assertTrue(freshFile.exists())

        // Purge files older than 30 minutes
        val deletedCount = storage.clearTempCache(olderThanMillis = 30 * 60 * 1000L)

        assertEquals(1, deletedCount)
        assertFalse("El archivo antiguo debió ser eliminado", oldFile.exists())
        assertTrue("El archivo reciente debe preservarse", freshFile.exists())
    }

    @Test
    fun clearTempCache_whenDirectoryDoesNotExist_returnsZero() {
        val nonExistentDir = File(tempFolder.root, "non_existent_dir")
        val storage = ScreenshotStorage(nonExistentDir)

        val deletedCount = storage.clearTempCache()
        assertEquals(0, deletedCount)
    }
}
