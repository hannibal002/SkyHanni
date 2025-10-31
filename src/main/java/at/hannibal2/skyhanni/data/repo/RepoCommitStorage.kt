package at.hannibal2.hanni.data.repo

import at.hannibal2.hanni.config.ConfigManager
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.json.fromJson
import at.hannibal2.hanni.utils.json.getJson
import at.hannibal2.hanni.utils.json.writeJson
import com.google.gson.annotations.Expose
import java.io.File

class RepoCommitStorage(val file: File) {
    fun readFromFile(): RepoCommit? {
        val currentCommitJson = file.getJson() ?: return null
        return ConfigManager.gson.fromJson<RepoCommit>(currentCommitJson)
    }

    fun writeToFile(commit: RepoCommit): Boolean {
        val newCurrentCommitJson = ConfigManager.gson.toJsonTree(commit).asJsonObject
        return file.writeJson(newCurrentCommitJson)
    }
}

data class RepoCommit(
    @Expose var sha: String? = null,
    @Expose var time: SimpleTimeMark? = null,
)
