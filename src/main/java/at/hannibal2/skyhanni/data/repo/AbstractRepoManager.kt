package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GitHubUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.json.getJson
import at.hannibal2.skyhanni.utils.json.writeJson
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.minecraft.util.IChatComponent
import java.io.File
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class AbstractRepoManager<C : AbstractRepoConfig<*>, E : AbstractRepoReloadEvent>{
    data class RepoComparison(
        val localSha: String?,
        val localCommitTime: SimpleTimeMark?,
        val latestSha: String?,
        val latestCommitTime: SimpleTimeMark?
    ) {
        val hashesMatch = localSha == latestSha
    }

    open fun getGson() = ConfigManager.gson

    /**
     * Should be user-friendly, e.g. "SkyHanni" or "NotEnoughUpdates".
     * Gets used in error messages and logging.
     */
    abstract val commonName: String

    /**
     * Should be relatively short, e.g. "SH" or "NEU".
     * Gets used in command registration, and as a prefix for constants, etc.
     */
    abstract val commonShortNameCased: String
    private val commonShortName by lazy { commonShortNameCased.lowercase() }

    /**
     * The resource path of the backup repo. (e.g., "assets/skyhanni/repo.zip")
     * This MUST be provided for the backup repo to work.
     */
    open val backupRepoResourcePath: String? = null

    abstract val config: C
    abstract val configDirectory: File
    private val loggingPrefix by lazy { "[Repo - $commonName]" }
    // ~/.minecraft/config/[...]/repo
    val repoDirectory by lazy { File(configDirectory, "repo") }
    // e.g., 'sh-repo-main' or 'neu-repo-master'
    private val repoZipFile by lazy {
        File(repoDirectory, "$commonShortName-repo-${config.location.defaultBranch}.zip")
    }
    private val currentCommitFilePath by lazy { File(configDirectory, "currentCommit.json") }
    private val githubRepoLocation: GitHubUtils.RepoLocation get() = GitHubUtils.RepoLocation(config.location)
    private val successfulConstants = mutableListOf<String>()
    private val unsuccessfulConstants = mutableListOf<String>()

    fun getFailedConstants() = unsuccessfulConstants.toList()

    private val commandShortName = when (commonShortName) {
        "sh" -> ""
        else -> commonShortName
    }

    open val shouldRegisterUpdateCommand: Boolean = true
    open val shouldRegisterStatusCommand: Boolean = true
    open val shouldRegisterReloadCommand: Boolean = true

    private val updateCommand by lazy { "shupdate${commandShortName}repo" }
    private val statusCommand by lazy { "sh${commandShortName}repostatus" }
    private val reloadCommand by lazy { "shreloadlocal${commandShortName}repo" }

    private var currentlyFetching = false
    private var shouldManuallyReload: Boolean = false
    private var loadingError: Boolean = false
    private var downloadFailed: Boolean = false
    private var lastConstant: String? = null

    var commitTime: SimpleTimeMark? = null
        private set

    var isUsingBackup: Boolean = false
        private set

    // <editor-fold desc="Abstraction Helpers">
    fun debug(message: String) = SkyHanniMod.logger.debug("$loggingPrefix $message")
    fun warn(message: String) = SkyHanniMod.logger.warn("$loggingPrefix $message")
    fun logError(error: String): Nothing = ErrorManager.skyHanniError("$loggingPrefix $error")
    fun logErrorWithData(cause: Throwable, error: String): Boolean =
        ErrorManager.logErrorWithData(cause, "$loggingPrefix $error")
    fun throwError(error: String): Nothing = throw RepoError("$loggingPrefix $error")
    fun throwErrorWithCause(error: String, cause: Throwable): Nothing =
        throw RepoError("$loggingPrefix $error", cause)

    fun getGitHubRepoPath(): String = githubRepoLocation.location
    // </editor-fold>

    // Necessary for implementation, so abstract to make people do it
    abstract fun onCommandRegistration(event: CommandRegistrationEvent)

    // Will be invoked by the implementation of this class
    fun registerCommands(event: CommandRegistrationEvent) {
        if (shouldRegisterUpdateCommand) event.registerBrigadier(updateCommand) {
            description = "Download the $commonName repo again"
            category = CommandCategory.USERS_BUG_FIX
            simpleCallback { updateRepo() }
        }
        if (shouldRegisterUpdateCommand) event.registerBrigadier(statusCommand) {
            description = "Shows the status of the $commonName repo"
            category = CommandCategory.USERS_BUG_FIX
            simpleCallback { displayRepoStatus(false) }
        }
        if (shouldRegisterReloadCommand) event.registerBrigadier(reloadCommand) {
            description = "Reloads the local $commonName repo"
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback { reloadLocalRepo() }
        }
    }

    // <editor-fold desc="Constant Reading">
    inline fun <reified T : Any> getConstant(
        fileName: String,
        type: Type? = null,
        gson: Gson = getGson(),
    ): T {
        setLastConstant(fileName)
        return getRepoData("constants", fileName, type, gson)
    }

    inline fun <reified T : Any> getRepoData(
        directory: String,
        fileName: String,
        type: Type? = null,
        gson: Gson = getGson(),
    ): T = runCatching {
        if (!repoDirectory.exists()) throwError("Repo folder does not exist!")

        val jsonFile = File(repoDirectory, "$directory/$fileName.json")
        if (!jsonFile.isFile) throwError("Repo file '$fileName' not found.")

        val jsonContent = jsonFile.getJson()
            ?: throwError("Repo file '$fileName' could not be loaded as a valid JSON file.")

        return if (type == null) gson.fromJson(jsonContent, T::class.java)
        else gson.fromJson(jsonContent, type)
    }.getOrElse { e ->
        if (e is RepoError) throw e
        else throwErrorWithCause("Repo parsing error while trying to read constant '$fileName'", e)
    }
    // </editor-fold>

    // <editor-fold desc="Repo Management">
    fun updateRepo() {
        shouldManuallyReload = true
        checkRepoLocation()
        SkyHanniMod.launchIOCoroutine {
            fetchAndUnpackRepo(command = true)
            reloadRepository("$commonName Repo updated successfully.")
            if (unsuccessfulConstants.isEmpty() && !isUsingBackup) return@launchIOCoroutine
            val informed = ErrorManager.logErrorStateWithData(
                "Error updating reading $commonName Repo",
                "no success",
                "usingBackupRepo" to isUsingBackup,
                "unsuccessfulConstants" to unsuccessfulConstants,
            )
            if (informed) return@launchIOCoroutine
            ChatUtils.chat("§cFailed to load the $commonShortNameCased repo! See above for more infos.")
        }
    }

    private fun checkRepoLocation() {
        if (config.location.run { user.isEmpty() || repoName.isEmpty() || branch.isEmpty() }) {
            ChatUtils.userError("Invalid $commonName Repo settings detected, resetting default settings.")
            resetRepositoryLocation()
        }
    }

    fun resetRepositoryLocation(manual: Boolean = false) = with(config.location) {
        if (hasDefaultSettings()) {
            if (manual) ChatUtils.chat("$commonShortNameCased Repo settings are already on default!")
            return
        }

        reset()
        if (!manual) return@with
        ChatUtils.clickableChat(
            "Reset $commonName Repo settings to default. " +
                "Click §aUpdate Repo Now §ein config or run /${updateCommand} to update!",
            onClick = ::updateRepo,
            "§eClick to update the $commonShortNameCased Repo!",
        )
    }

    fun initRepo() {
        shouldManuallyReload = true
        SkyHanniMod.launchIOCoroutine {
            if (config.repoAutoUpdate) {
                tryFetchRepository(command = false)
                if (downloadFailed) {
                    switchToBackupRepo()
                }
            }
            reloadRepository()
        }
    }

    // Code taken + adapted from NotEnoughUpdates
    private fun switchToBackupRepo() {
        if (backupRepoResourcePath == null) {
            warn("No backup repo resource path provided, cannot switch to backup repo.")
            return
        }

        isUsingBackup = true
        debug("Attempting to switch to backup repo")

        try {
            repoDirectory.mkdirs()
            repoZipFile.createNewFile()

            val inputStream = RepoManager::class.java.classLoader.getResourceAsStream(backupRepoResourcePath)
                ?: throwError("Failed to find backup repo resource at '$backupRepoResourcePath'")

            Files.copy(inputStream, repoZipFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            RepoUtils.unzipIgnoreFirstFolder(
                zipFilePath = repoZipFile.absolutePath,
                destinationDirectory = repoDirectory.absolutePath
            )

            writeCurrentCommit("backup-repo", time = null)
            debug("Successfully switched to backup repo")
        } catch(e: Error) {
            if (e is RepoError) throw e
            else logErrorWithData(e, "Failed to switch to backup repo")
        }
    }

    // todo use a real data class instead of JsonObject and string 'indexing'
    private fun writeCurrentCommit(commit: String?, time: SimpleTimeMark?) {
        val newCurrentCommitJSON = JsonObject()
        newCurrentCommitJSON.addProperty("sha", commit)
        time?.let {
            newCurrentCommitJSON.addProperty("time", it.toMillis())
        }
        currentCommitFilePath.writeJson(newCurrentCommitJSON)
    }

    // todo use a real data class instead of JsonObject and string 'indexing'
    private fun readCurrentCommit(): Pair<String, SimpleTimeMark?>? {
        val currentCommitJSON: JsonObject? = currentCommitFilePath.getJson()
        val sha = currentCommitJSON?.get("sha")?.asString
        val time = currentCommitJSON?.get("time")?.asLong?.asTimeMark()
        return sha?.let { it to time }
    }

    /**
     * todo write kdoc
     */
    private suspend fun tryFetchRepository(command: Boolean, silentError: Boolean = true): Boolean? {
        if (currentlyFetching) return null
        currentlyFetching = true
        val success = fetchAndUnpackRepo(command, silentError)
        currentlyFetching = false
        return success
    }

    private fun RepoComparison.reportRepoUpToDate() = ChatUtils.clickToClipboard(
        "§7The repo is already up to date!",
        lines = buildList {
            add("latest commit sha: §e$localSha")
            latestCommitTime?.let { latestTime ->
                add("latest commit time: §b$latestTime")
                add("  (§b${latestTime.passedSince().format()} ago§7)")
            }
        },
    )

    open fun reportExtraStatusInfo(): Unit = Unit

    private fun RepoComparison.reportRepoOutdated() = ChatUtils.clickToClipboard(
        "Repo is outdated, updating..",
        lines = buildList {
            add("local commit sha: §e$latestSha")
            localCommitTime?.let { localTime ->
                add("local commit time: §b$localTime")
                add("  (§b${localTime.passedSince().format()} ago§7)")
            }
            add("")
            add("latest commit sha: §e$localSha")
            latestCommitTime?.let { latestTime ->
                add("latest commit time: §b$latestTime")
                add("  (§b${latestTime.passedSince().format()} ago§7)")
                localCommitTime?.let { localTime ->
                    val outdatedDuration = latestTime - localTime
                    add("")
                    add("outdated by: §b${outdatedDuration.format()}")
                }
            }
        },
    )

    fun displayRepoStatus(joinEvent: Boolean) {
        if (joinEvent) return onJoinStatusError()

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
        reportExtraStatusInfo()
    }

    private fun onJoinStatusError() {
        if (unsuccessfulConstants.isEmpty()) return
        val text = mutableListOf<IChatComponent>()
        text.add(
            (
                "§c[SkyHanni-${SkyHanniMod.VERSION}] §7Repo Issue! Some features may not work. " +
                    "Please report this error on the Discord!"
                ).asComponent(),
        )
        text.add("§7Repo Auto Update Value: §c${config.repoAutoUpdate}".asComponent())
        text.add("§7Backup Repo Value: §c$isUsingBackup".asComponent())
        text.add("§7If you have Repo Auto Update turned off, please try turning that on.".asComponent())
        text.add("§cUnsuccessful Constants §7(${unsuccessfulConstants.size}):".asComponent())

        for (constant in unsuccessfulConstants) {
            text.add("   §e- §7$constant".asComponent())
        }
        TextHelper.multiline(text).send()
    }

    /**
     * todo write kdoc
     */
    private suspend fun fetchAndUnpackRepo(command: Boolean, silentError: Boolean = true): Boolean {
        val (currentSha, currentCommitTime) = readCurrentCommit() ?: (null to null)
        commitTime = currentCommitTime

        val (latestSha, latestCommitTime) = githubRepoLocation.getLatestCommit(silentError)?.let { response ->
            response.sha to response.commit.committer.date
        } ?: run {
            downloadFailed = true
            null to null
        }

        val diffCheck = RepoComparison(currentSha, currentCommitTime, latestSha, latestCommitTime)

        if (repoDirectory.exists() && diffCheck.hashesMatch && unsuccessfulConstants.isEmpty()) {
            if (command) {
                diffCheck.reportRepoUpToDate()
                shouldManuallyReload = false
            }
            return true
        }

        if (command) diffCheck.reportRepoOutdated()

        RepoUtils.recursiveDelete(repoDirectory)
        repoDirectory.mkdirs()
        try {
            repoZipFile.createNewFile()
        } catch (e: Error) {
            ErrorManager.logErrorWithData(e, "Error creating $commonShortName repo zip file")
            return false
        }

        if (!githubRepoLocation.downloadCommitZipToFile(repoZipFile)) {
            downloadFailed = true
            return false
        }
        RepoUtils.unzipIgnoreFirstFolder(
            zipFilePath = repoZipFile.absolutePath,
            destinationDirectory = repoDirectory.absolutePath,
        )
        writeCurrentCommit(latestSha, latestCommitTime)
        commitTime = latestCommitTime
        downloadFailed = false
        isUsingBackup = false
        return true
    }

    fun reloadLocalRepo(answerMessage: String = "$commonName Repo loaded from local files successfully.") {
        shouldManuallyReload = true
        SkyHanniMod.launchIOCoroutine {
            reloadRepository(answerMessage)
        }
    }

    fun setLastConstant(constant: String) {
        lastConstant = constant
    }

    // Because abstract classes cannot have reified types, this is necessary
    abstract fun fireReloadEvent(manager: AbstractRepoManager<C, E>, onError: (Throwable) -> Unit): Boolean

    /**
     * Called before the repo reload event is fired - shouldn't do anything resource intensive.
     */
    open fun extraReloadWork() = Unit
    /**
     * Called before the repo reload event is fired, but in an IO coroutine.
     */
    open suspend fun extraReloadCoroutineWork() = Unit

    private fun reloadRepository(answerMessage: String = "") {
        if (!shouldManuallyReload) return
        loadingError = false
        successfulConstants.clear()
        unsuccessfulConstants.clear()
        lastConstant = null

        extraReloadWork()

        fireReloadEvent(this) { error ->
            loadingError = true
            lastConstant?.let { unsuccessfulConstants.add(it) }
            lastConstant = null
        }

        SkyHanniMod.launchIOCoroutine {
            extraReloadCoroutineWork()
        }

        if (answerMessage.isNotEmpty() && !loadingError) {
            ChatUtils.chat("§a$answerMessage")
        } else if (loadingError) {
            ChatUtils.clickableChat(
                "Error with the repo detected, try /$updateCommand to fix it!",
                onClick = ::updateRepo,
                "§eClick to update the repo!",
                prefixColor = "§c",
            )
            if (unsuccessfulConstants.isEmpty()) unsuccessfulConstants.add("All Constants")
        }
    }

}
