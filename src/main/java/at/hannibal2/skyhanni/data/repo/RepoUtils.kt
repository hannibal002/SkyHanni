package at.hannibal2.skyhanni.data.repo

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

object RepoUtils {

    fun recursiveDelete(file: File) {
        if (file.isDirectory && !Files.isSymbolicLink(file.toPath())) {
            for (child in file.listFiles()) {
                recursiveDelete(child)
            }
        }
        file.delete()
    }

    fun unzipIgnoreFirstFolder(zipFilePath: String, fs: RepoFileSystem) {
        ZipFile(zipFilePath).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory }
                .forEach { entry ->
                    val relative = entry.name
                        .substringAfter('/', "")
                        .takeIf { it.isNotBlank() }
                        ?: return@forEach

                    if (fs is DiskRepoFileSystem) {
                        // Security: ensure the file is within the root directory
                        val outPath = fs.root.toPath().resolve(relative).normalize()
                        if (!outPath.startsWith(fs.root.toPath())) throw RuntimeException(
                            "SkyHanni detected an invalid zip file. This is a potential security risk, " +
                                "please report this on the SkyHanni discord."
                        )
                    }

                    val data = zip.getInputStream(entry).readBytes()
                    fs.write(relative, data)
                }
        }
    }

    @Suppress("NAME_SHADOWING")
    @Throws(IOException::class)
    private fun isInTree(rootDirectory: File, file: File): Boolean {
        var rootDirectory = rootDirectory
        var file: File? = file
        file = file!!.canonicalFile
        rootDirectory = rootDirectory.canonicalFile
        while (file != null) {
            if (file == rootDirectory) return true
            file = file.parentFile
        }
        return false
    }
}
