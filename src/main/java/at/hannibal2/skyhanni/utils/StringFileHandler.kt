package at.hannibal2.skyhanni.utils

import java.io.File
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class StringFileHandler(private val file: File) {
    private val backupFile = File(file.parentFile, "${file.name}.bak")
    private val tempFile = File(file.parentFile, "${file.name}.tmp")

    @Throws(IOException::class)
    fun load(): String = try {
        file.readText()
    } catch (e: Exception) {
        if (backupFile.exists()) backupFile.readText()
        else throw e
    }

    @Throws(IOException::class)
    fun save(content: String, attempt: Int = 0) {
        try {
            tempFile.writeText(content)
            if (file.exists()) file.copyTo(backupFile, overwrite = true)
            Files.move(
                tempFile.toPath(),
                file.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
            backupFile.delete()
        } catch (e: AccessDeniedException) {
            if (attempt >= 5) throw e
            Thread.sleep(50L)
            save(content, attempt + 1)
        } catch (e: Exception) {
            tempFile.delete()
            if (backupFile.exists()) backupFile.copyTo(file, overwrite = true)
            throw e
        }
    }
}
