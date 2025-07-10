package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GitHubUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.json.getJson
import com.google.gson.Gson
import net.minecraft.util.IChatComponent
import java.io.File
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Suppress("TooManyFunctions")
abstract class AbstractRepoManager(
    val eventConstructor: (AbstractRepoManager) -> AbstractRepoReloadEvent,
) {
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

    private val debugConfig get() = SkyHanniMod.feature.dev.debug
    abstract val config: AbstractRepoConfig<*>
    abstract val configDirectory: File
    val logger by lazy { RepoLogger("[Repo - $commonName]") }
    val repoDirectory by lazy {
        // ~/.minecraft/config/[...]/repo
        File(configDirectory, "repo")
    }
    private val repoZipFile by lazy {
        // e.g., 'sh-repo-main' or 'neu-repo-master'
        File(repoDirectory, "$commonShortName-repo-${config.location.defaultBranch}.zip")
    }
    private val currentCommitFile by lazy { File(configDirectory, "currentCommit.json") }
    private val commitStorage: RepoCommitStorage by lazy { RepoCommitStorage(currentCommitFile) }
    private val successfulConstants = mutableListOf<String>()
    private val unsuccessfulConstants = mutableListOf<String>()
    private val commandShortName by lazy { commonShortName.takeIf { it != "sh" }.orEmpty() }
    private val githubRepoLocation: GitHubUtils.RepoLocation
        get() = GitHubUtils.RepoLocation(config.location, debugConfig.logRepoErrors)

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

    var commitTime: SimpleTimeMark? = null
        private set

    var isUsingBackup: Boolean = false
        private set

    fun getFailedConstants() = unsuccessfulConstants.toList()
    fun getGitHubRepoPath(): String = githubRepoLocation.location

    // Will be invoked by the implementation of this class
    fun registerCommands(event: CommandRegistrationEvent) {
        if (shouldRegisterUpdateCommand) event.registerBrigadier(updateCommand) {
            description = "Download the $commonName repo again"
            category = CommandCategory.USERS_BUG_FIX
            simpleCallback { updateRepo() }
        }
        if (shouldRegisterStatusCommand) event.registerBrigadier(statusCommand) {
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

    var reportedRepoFiles: Boolean = false

    inline fun <reified T : Any> getRepoData(
        directory: String,
        fileName: String,
        type: Type? = null,
        gson: Gson = getGson(),
    ): T = runCatching {
        if (!repoDirectory.exists()) logger.throwError("Repo folder does not exist!")

        val jsonFile = File(repoDirectory, "$directory/$fileName.json")
        if (!jsonFile.isFile) logger.throwError("Repo file '$fileName' not found.")

        val jsonContent = jsonFile.getJson()
            ?: logger.throwError("Repo file '$fileName' could not be loaded as a valid JSON file.")

        return if (type == null) gson.fromJson(jsonContent, T::class.java)
        else gson.fromJson(jsonContent, type)
    }.getOrElse { e ->
        logger.throwErrorWithCause("Repo parsing error while trying to read constant '$fileName'", e)
    }

    // <editor-fold desc="Repo Management">
    fun updateRepo() {
        shouldManuallyReload = true
        if (!config.location.valid) {
            ChatUtils.userError("Invalid $commonName Repo settings detected, resetting default settings.")
            resetRepositoryLocation()
        }

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

    fun resetRepositoryLocation(manual: Boolean = false) = with(config.location) {
        if (hasDefaultSettings()) {
            if (manual) ChatUtils.chat("$commonShortNameCased Repo settings are already on default!")
            return
        }

        reset()
        if (!manual) return@with
        ChatUtils.clickableChat(
            "Reset $commonName Repo settings to default. " +
                "Click §aUpdate Repo Now §ein config or run /$updateCommand to update!",
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
            logger.warn("No backup repo resource path provided, cannot switch to backup repo.")
            return
        }

        isUsingBackup = true
        logger.debug("Attempting to switch to backup repo")

        try {
            repoDirectory.mkdirs()
            repoZipFile.createNewFile()

            val inputStream = RepoManager::class.java.classLoader.getResourceAsStream(backupRepoResourcePath)
                ?: logger.throwError("Failed to find backup repo resource at '$backupRepoResourcePath'")

            Files.copy(inputStream, repoZipFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            RepoUtils.unzipIgnoreFirstFolder(
                zipFilePath = repoZipFile.absolutePath,
                destinationDirectory = repoDirectory.absolutePath,
            )

            commitStorage.writeToFile(RepoCommit("backup-repo", time = null))
            logger.debug("Successfully switched to backup repo")
        } catch (e: Error) {
            logger.logErrorWithData(e, "Failed to switch to backup repo")
        }
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


    open fun reportExtraStatusInfo(): Unit = Unit

    fun displayRepoStatus(joinEvent: Boolean) {
        if (joinEvent) return onJoinStatusError()

        val (currentDownloadedCommit, _) = commitStorage.readFromFile() ?: RepoCommit()
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
        val (currentSha, currentCommitTime) = commitStorage.readFromFile() ?: RepoCommit()
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
        commitStorage.writeToFile(RepoCommit(latestSha, latestCommitTime))
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

        extraReloadWork()

        eventConstructor.invoke(this).post { error ->
            loadingError = true
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
