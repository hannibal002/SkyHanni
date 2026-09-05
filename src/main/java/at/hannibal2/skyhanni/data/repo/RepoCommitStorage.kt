package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringFileHandler
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.annotations.Expose
import java.io.File

@JvmInline
value class RepoCommitStorage private constructor(private val fileHandler: StringFileHandler) {
    constructor(file: File) : this(StringFileHandler(file))

    fun readFromFile(): RepoCommit? {
        return runCatching {
            val currentCommitJson = fileHandler.load()
            ConfigManager.gson.fromJson<RepoCommit>(currentCommitJson)
        }.getOrElse { deleteFile() }
    }

    fun writeToFile(commit: RepoCommit): Boolean {
        val newCurrentCommitJson = ConfigManager.gson.toJson(commit)
        runCatching { fileHandler.save(newCurrentCommitJson) }.getOrNull() ?: return false
        return true
    }

    private fun deleteFile(): Nothing? {
        fileHandler.delete()
        return null
    }
}

data class RepoCommit(
    @Expose var sha: String? = null,
    @Expose var time: SimpleTimeMark? = null,
)
