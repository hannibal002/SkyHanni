package at.hannibal2.skyhanni.utils

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

class StringFileHandler(private val file: File) {
    private val tempFile = File(file.parentFile, "${file.name}.tmp")

    @Throws(IOException::class)
    fun load(): String = try {
        file.readText()
    } catch (e: Exception) {
        if (tempFile.exists()) tempFile.readText()
        else throw e
    }

    @Throws(IOException::class)
    fun save(content: String) {
        repeat(6) { attempt ->
            try {
                saveOnce(content)
                return
            } catch (e: AccessDeniedException) {
                if (attempt == 5) throw e
                Thread.sleep(50L)
            }
        }
    }

    private fun saveOnce(content: String) {
        try {
            // Write + fsync temp file.
            FileOutputStream(tempFile).channel.use { channel ->
                channel.write(ByteBuffer.wrap(content.toByteArray(Charsets.UTF_8)))
                channel.force(true)
            }

            // Atomic replace. Old or new file, never half-written.
            Files.move(
                tempFile.toPath(),
                file.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )

            // Parent directory holds the filename, not the file itself.
            // Fsync it so the rename survives power loss.
            forceParentDirectory()
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    private fun forceParentDirectory() {
        try {
            FileChannel.open(
                file.parentFile.toPath(),
                StandardOpenOption.READ,
            ).use {
                it.force(true)
            }
        } catch (_: Exception) {
            // Directory fsync can fail on Windows.
        }
    }
}
