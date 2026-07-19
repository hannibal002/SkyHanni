package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.data.repo.RepoCommitStorage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class RepoCommitStorageTest {

    @Test
    fun `readFromFile deletes current commit json if getJson fails`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("currentCommit.json").toFile()
        file.writeText("{")

        assertNull(RepoCommitStorage(file).readFromFile())
        assertFalse(file.exists())
    }

    @Test
    fun `readFromFile deletes current commit json if fromJson fails`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("currentCommit.json").toFile()
        file.writeText("\u0001")

        assertNull(RepoCommitStorage(file).readFromFile())
        assertFalse(file.exists())
    }

    @Test
    fun `readFromFile deletes non-object current commit json`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("currentCommit.json").toFile()
        file.writeText("\"not an object\"")

        assertNull(RepoCommitStorage(file).readFromFile())
        assertFalse(file.exists())
    }

    @Test
    fun `readFromFile keeps valid current commit json`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("currentCommit.json").toFile()
        file.writeText("""{"sha":"abc123"}""")

        val commit = RepoCommitStorage(file).readFromFile()

        assertEquals("abc123", commit?.sha)
        assertTrue(file.exists())
    }
}
