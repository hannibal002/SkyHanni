package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GitHubUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import com.google.gson.JsonObject
import net.minecraft.util.IChatComponent
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@SkyHanniModule
object RepoManager {

    private val gson get() = ConfigManager.gson
    private val configFileLocation = ConfigManager.configDirectory
    val repoFileLocation: File = File(configFileLocation, "repo")
    private var error = false
    private var lastRepoUpdate = SimpleTimeMark.now()
    private var repoDownloadFailed = false

    private val config get() = SkyHanniMod.feature.dev.repo

    val successfulConstants = mutableListOf<String>()
    val unsuccessfulConstants = mutableListOf<String>()
    var usingBackupRepo = false

    private var lastConstant: String? = null

    fun setLastConstant(constant: String) {
        lastConstant?.let {
            successfulConstants.add(it)
        }
        lastConstant = constant
    }

    fun getRepoLocation(): String {
        return "${config.location.user}/${config.location.repoName}/${config.location.branch}"
    }

    fun initRepo() {
        shouldManuallyReload = true
        SkyHanniMod.launchIOCoroutine {
            if (config.repoAutoUpdate) {
                fetchRepository(command = false)
                if (repoDownloadFailed) {
                    switchToBackupRepo()
                }
            }
            reloadRepository()
        }
    }

    private var shouldManuallyReload = false

    private var currentlyFetching = false
    var commitTime: SimpleTimeMark? = null
        private set

    @JvmStatic
    fun updateRepo() {
        shouldManuallyReload = true
        checkRepoLocation()
        SkyHanniMod.launchIOCoroutine {
            fetchRepository(command = true)
            reloadRepository("Repo updated successfully.")
            if (unsuccessfulConstants.isNotEmpty() || usingBackupRepo) {
                if (!ErrorManager.logErrorStateWithData(
                        "Error updating reading Sh Repo",
                        "no success",
                        "usingBackupRepo" to usingBackupRepo,
                        "unsuccessfulConstants" to unsuccessfulConstants,
                    )
                ) {
                    ChatUtils.chat("§cFailed to load the repo! See above for more infos.")
                }
                return@launchIOCoroutine
            }
        }
    }

    fun reloadLocalRepo() {
        shouldManuallyReload = true
        SkyHanniMod.launchIOCoroutine {
            reloadRepository("Repo loaded from local files successfully.")
        }
    }

    private suspend fun fetchRepository(command: Boolean, silentError: Boolean = true): Boolean? {
        if (currentlyFetching) return null
        currentlyFetching = true
        val success = fetchAndUnpackRepo(command, silentError)
        currentlyFetching = false
        return success
    }

    // todo this is basically entirely duplicated with EnoughUpdatesRepo.kt, refactor
    private suspend fun fetchAndUnpackRepo(command: Boolean, silentError: Boolean = true): Boolean {
        val githubRepoLocation = GitHubUtils.RepoLocation(config.location, shouldError = !silentError)

        val (currentSha, currentTime) = readCurrentCommit() ?: (null to null)
        commitTime = currentTime

        val (latestRepoCommit, latestRepoCommitTime) = githubRepoLocation.getLatestCommit(silentError)?.let { response ->
            response.sha to response.commit.committer.date
        } ?: run {
            repoDownloadFailed = true
            null to null
        }

        if (repoFileLocation.exists() && currentSha == latestRepoCommit && unsuccessfulConstants.isEmpty()) {
            if (command) {
                ChatUtils.clickToClipboard(
                    "§7The repo is already up to date!",
                    lines = buildList {
                        add("latest commit sha: §e$currentSha")
                        latestRepoCommitTime?.let { latestTime ->
                            add("latest commit time: §b$latestTime")
                            add("  (§b${latestTime.passedSince().format()} ago§7)")
                        }
                    },
                )
                shouldManuallyReload = false
            }
            return true
        }

        if (command) {
            ChatUtils.clickToClipboard(
                "Repo is outdated, updating..",
                lines = buildList {
                    add("local commit sha: §e$latestRepoCommit")
                    currentTime?.let { localTime ->
                        add("local commit time: §b$localTime")
                        add("  (§b${localTime.passedSince().format()} ago§7)")
                    }
                    add("")
                    add("latest commit sha: §e$currentSha")
                    latestRepoCommitTime?.let { latestTime ->
                        add("latest commit time: §b$latestTime")
                        add("  (§b${latestTime.passedSince().format()} ago§7)")
                        currentTime?.let { localTime ->
                            val outdatedDuration = latestTime - localTime
                            add("")
                            add("outdated by: §b${outdatedDuration.format()}")
                        }
                    }
                },
            )
        }
        lastRepoUpdate = SimpleTimeMark.now()

        RepoUtils.recursiveDelete(repoFileLocation)
        repoFileLocation.mkdirs()
        val itemsZip = File(repoFileLocation, "sh-repo-main.zip")
        try {
            itemsZip.createNewFile()
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error creating sh repo zip file")
            return false
        }

        if (!githubRepoLocation.downloadCommitZipToFile(itemsZip)) {
            repoDownloadFailed = true
            return false
        }
        RepoUtils.unzipIgnoreFirstFolder(
            itemsZip.absolutePath,
            repoFileLocation.absolutePath,
        )
        if (currentSha == null || currentSha != latestRepoCommit) {
            writeCurrentCommit(latestRepoCommit, latestRepoCommitTime)
        }
        commitTime = latestRepoCommitTime
        repoDownloadFailed = false
        usingBackupRepo = false
        return true
    }

    private fun reloadRepository(answerMessage: String = "") {
        if (!shouldManuallyReload) return
        error = false
        successfulConstants.clear()
        unsuccessfulConstants.clear()
        lastConstant = null

        RepositoryReloadEvent(repoFileLocation, gson).post {
            error = true
            lastConstant?.let {
                unsuccessfulConstants.add(it)
            }
            lastConstant = null
        }
        if (answerMessage.isNotEmpty() && !error) {
            ChatUtils.chat("§a$answerMessage")
        }
        if (error) {
            ChatUtils.clickableChat(
                "Error with the repo detected, try /shupdaterepo to fix it!",
                onClick = {
                    updateRepo()
                },
                "§eClick to update the repo!",
                prefixColor = "§c",
            )
            if (unsuccessfulConstants.isEmpty()) {
                unsuccessfulConstants.add("All Constants")
            }
        }
    }

    private fun writeCurrentCommit(commit: String?, time: SimpleTimeMark?) {
        val newCurrentCommitJSON = JsonObject()
        newCurrentCommitJSON.addProperty("sha", commit)
        time?.let {
            newCurrentCommitJSON.addProperty("time", it.toMillis())
        }
        try {
            writeJson(newCurrentCommitJSON, File(configFileLocation, "currentCommit.json"))
        } catch (ignored: IOException) {
        }
    }

    private fun readCurrentCommit(): Pair<String, SimpleTimeMark?>? {
        val currentCommitJSON: JsonObject? = getJsonFromFile(File(configFileLocation, "currentCommit.json"))
        val sha = currentCommitJSON?.get("sha")?.asString
        val time = currentCommitJSON?.get("time")?.asLong?.asTimeMark()
        return sha?.let { it to time }
    }

    fun displayRepoStatus(joinEvent: Boolean) {
        if (joinEvent) {
            if (unsuccessfulConstants.isNotEmpty()) {
                val text = mutableListOf<IChatComponent>()
                text.add(
                    (
                        "§c[SkyHanni-${SkyHanniMod.VERSION}] §7Repo Issue! Some features may not work. " +
                            "Please report this error on the Discord!"
                        ).asComponent(),
                )
                text.add("§7Repo Auto Update Value: §c${config.repoAutoUpdate}".asComponent())
                text.add("§7Backup Repo Value: §c$usingBackupRepo".asComponent())
                text.add("§7If you have Repo Auto Update turned off, please try turning that on.".asComponent())
                text.add("§cUnsuccessful Constants §7(${unsuccessfulConstants.size}):".asComponent())

                for (constant in unsuccessfulConstants) {
                    text.add("   §e- §7$constant".asComponent())
                }
                TextHelper.multiline(text).send()
            }
            return
        }
        val (currentDownloadedCommit, _) = readCurrentCommit() ?: (null to null)
        if (unsuccessfulConstants.isEmpty() && successfulConstants.isNotEmpty()) {
            ChatUtils.chat("Repo working fine! Commit hash: $currentDownloadedCommit", prefixColor = "§a")
            return
        }
        ChatUtils.chat("Repo has errors! Commit hash: $currentDownloadedCommit", prefixColor = "§c")
        if (successfulConstants.isNotEmpty()) ChatUtils.chat(
            "Successful Constants §7(${successfulConstants.size}):",
            prefixColor = "§a",
        )
        for (constant in successfulConstants) {
            ChatUtils.chat("   §a- §7$constant", false)
        }
        ChatUtils.chat("Unsuccessful Constants §7(${unsuccessfulConstants.size}):")
        for (constant in unsuccessfulConstants) {
            ChatUtils.chat("   §e- §7$constant", false)
        }
    }

    /**
     * Parses a file in to a JsonObject.
     */
    private fun getJsonFromFile(file: File?): JsonObject? {
        try {
            BufferedReader(
                InputStreamReader(
                    FileInputStream(file),
                    StandardCharsets.UTF_8,
                ),
            ).use { reader ->
                return gson.fromJson(reader, JsonObject::class.java)
            }
        } catch (e: java.lang.Exception) {
            return null
        }
    }

    @Throws(IOException::class)
    fun writeJson(json: JsonObject?, file: File) {
        file.createNewFile()
        BufferedWriter(
            OutputStreamWriter(
                FileOutputStream(file),
                StandardCharsets.UTF_8,
            ),
        ).use { writer -> writer.write(gson.toJson(json)) }
    }

    fun resetRepositoryLocation(manual: Boolean = false) {

        with(config.location) {
            if (hasDefaultSettings()) {
                if (manual) {
                    ChatUtils.chat("Repo settings are already on default!")
                }
                return
            }

            reset()
            if (manual) {
                ChatUtils.clickableChat(
                    "Reset Repo settings to default. " +
                        "Click §aUpdate Repo Now §ein config or run /shupdaterepo to update!",
                    onClick = {
                        updateRepo()
                    },
                    "§eClick to update the repo!",
                )
            }
        }
    }

    private fun checkRepoLocation() {
        if (config.location.run { user.isEmpty() || repoName.isEmpty() || branch.isEmpty() }) {
            ChatUtils.userError("Invalid Repo settings detected, resetting default settings.")
            resetRepositoryLocation()
        }
    }

    // Code taken from NotEnoughUpdates
    private fun switchToBackupRepo() {
        usingBackupRepo = true
        println("Attempting to switch to backup repo")

        try {
            repoFileLocation.mkdirs()
            val destinationFile = File(repoFileLocation, "sh-repo-main.zip").apply { createNewFile() }
            val destinationPath = destinationFile.toPath()

            val inputStream = RepoManager::class.java.classLoader.getResourceAsStream("assets/skyhanni/repo.zip")
                ?: throw IOException("Failed to find backup repo")

            Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING)
            RepoUtils.unzipIgnoreFirstFolder(destinationPath.toAbsolutePath().toString(), repoFileLocation.absolutePath)
            writeCurrentCommit("backup-repo", time = null)

            println("Successfully switched to backup repo")
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed to switch to backup repo")
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shupdaterepo") {
            description = "Download the SkyHanni repo again"
            category = CommandCategory.USERS_BUG_FIX
            simpleCallback { updateRepo() }
        }
        event.registerBrigadier("shrepostatus") {
            description = "Shows the status of all the mods constants"
            category = CommandCategory.USERS_BUG_FIX
            simpleCallback { displayRepoStatus(false) }
        }
        event.registerBrigadier("shreloadlocalrepo") {
            description = "Reloading the local repo data"
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback { reloadLocalRepo() }
        }
    }
}
