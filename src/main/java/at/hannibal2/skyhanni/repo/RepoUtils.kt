package at.hannibal2.skyhanni.repo

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipInputStream

object RepoUtils {

    fun recursiveDelete(file: File) {
        if (file.isDirectory && !Files.isSymbolicLink(file.toPath())) {
            for (child in file.listFiles()) {
                recursiveDelete(child)
            }
        }
        file.delete()
    }

    /**
     * Modified from https://www.journaldev.com/960/java-unzip-file-example
     */
    fun unzipIgnoreFirstFolder(zipFilePath: String, destDir: String) {
        val dir = File(destDir)
        // create output directory if it doesn't exist
        if (!dir.exists()) dir.mkdirs()
        val fis: FileInputStream
        //buffer for read and write data to file
        val buffer = ByteArray(1024)
        try {
            fis = FileInputStream(zipFilePath)
            val zis = ZipInputStream(fis)
            var ze = zis.nextEntry
            while (ze != null) {
                if (!ze.isDirectory) {
                    var fileName = ze.name
                    fileName = fileName.substring(fileName.split("/").toTypedArray()[0].length + 1)
                    val newFile = File(destDir + File.separator + fileName)
                    //create directories for sub directories in zip
                    File(newFile.parent).mkdirs()
                    if (!isInTree(dir, newFile)) {
                        throw RuntimeException(
                            "SkyHanni detected an invalid zip file. This is a potential security risk, please report this on the SkyHanni discord."
                        )
                    }
                    val fos = FileOutputStream(newFile)
                    var len: Int
                    while (zis.read(buffer).also { len = it } > 0) {
                        fos.write(buffer, 0, len)
                    }
                    fos.close()
                }
                //close this ZipEntry
                zis.closeEntry()
                ze = zis.nextEntry
            }
            //close last ZipEntry
            zis.closeEntry()
            zis.close()
            fis.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

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

    fun getConstant(repoLocation: File, constant: String, gson: Gson): JsonObject? {
        return getConstant(repoLocation, constant, gson, JsonObject::class.java)
    }

    private fun <T> getConstant(repo: File, constant: String, gson: Gson, clazz: Class<T>?): T? {
        if (repo.exists()) {
            val jsonFile = File(repo, "constants/$constant.json")
            try {
                BufferedReader(
                    InputStreamReader(
                        FileInputStream(jsonFile),
                        StandardCharsets.UTF_8
                    )
                ).use { reader ->
                    return gson.fromJson(reader, clazz)
                }
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }
}