package at.hannibal2.skyhanni.api.enoughupdates

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.features.dev.NeuRepositoryConfig
import at.hannibal2.skyhanni.data.repo.RepoUtils
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GitHubUtils
import at.hannibal2.skyhanni.utils.json.getJson
import com.google.gson.JsonObject
import java.io.File
import java.io.FileWriter

object EnoughUpdatesRepo {

    private val config get(): NeuRepositoryConfig = SkyHanniMod.feature.dev.neuRepo

    fun downloadRepo() {
        checkRepoLocation()
        if (config.repoAutoUpdate) {
            SkyHanniMod.launchIOCoroutine {
                val success = fetchAndUnpackNeuRepo()
                if (success) {
                    ChatUtils.chat("Updated NEU repo")
                    EnoughUpdatesManager.reloadRepo()
                }
            }
        } else EnoughUpdatesManager.reloadRepo()
    }

    private suspend fun fetchAndUnpackNeuRepo(): Boolean {
        val githubRepoLocation = GitHubUtils.RepoLocation(config.location)
        val hash = githubRepoLocation.sha ?: return false
        val currentCommit = getCurrentCommitHash()
        if (hash == currentCommit) return true

        RepoUtils.recursiveDelete(EnoughUpdatesManager.repoFileLocation)
        EnoughUpdatesManager.repoFileLocation.mkdirs()
        val itemsZip = File(EnoughUpdatesManager.repoFileLocation, "neu-items-master.zip")
        try {
            itemsZip.createNewFile()
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error creating neu repo zip file")
            return false
        }

        if (!githubRepoLocation.downloadCommitZipToFile(itemsZip)) return false

        RepoUtils.unzipIgnoreFirstFolder(
            itemsZip.absolutePath,
            EnoughUpdatesManager.repoFileLocation.absolutePath
        )
        writeCurrentCommitHash(hash)
        return true
    }

    private const val DEFAULT_USER = "NotEnoughUpdates"
    private const val DEFAULT_NAME = "NotEnoughUpdates-REPO"
    private const val DEFAULT_BRANCH = "master"

    fun hasDefaultRepositoryLocation(): Boolean =
        config.location.user == DEFAULT_USER && config.location.name == DEFAULT_NAME && config.location.branch == DEFAULT_BRANCH

    fun getRepoLocation(): String {
        return "${config.location.user}/${config.location.name}/${config.location.branch}"
    }

    private fun checkRepoLocation() {
        if (config.run { location.user.isEmpty() || location.name.isEmpty() || location.branch.isEmpty() }) {
            ChatUtils.chat("Invalid NEU Repo settings detected, resetting default settings.")
            resetRepoLocation()
        }
    }

    fun resetRepoLocation(manual: Boolean = false) {

        if (hasDefaultRepositoryLocation()) {
            ChatUtils.chat("NEU Repo location is already set to default.")
        }
        config.location.user = DEFAULT_USER
        config.location.name = DEFAULT_NAME
        config.location.branch = DEFAULT_BRANCH

        if (manual) {
            ChatUtils.clickableChat(
                "NEU Repo location has been reset to default. " +
                    "Click §aUpdate Repo Now §ein config or run /neuresetrepo to update!",
                onClick = ::downloadRepo,
                "§eClick to update the NEU repo!",
            )
        }
    }

    private fun getCurrentCommitHash(): String? {
        val currentCommitJSON: JsonObject? = File(EnoughUpdatesManager.configFileLocation, "currentCommit.json").getJson()
        return currentCommitJSON?.get("sha")?.asString
    }

    private fun writeCurrentCommitHash(hash: String) {
        val currentCommitJson = JsonObject()
        currentCommitJson.addProperty("sha", hash)
        try {
            FileWriter(File(EnoughUpdatesManager.configFileLocation, "currentCommit.json")).use { writer ->
                ConfigManager.gson.toJson(currentCommitJson, writer)
            }
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error writing current repo commit")
        }
    }
}
