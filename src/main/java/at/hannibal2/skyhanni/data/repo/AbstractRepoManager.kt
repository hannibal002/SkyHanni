package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.repo.ChatProgressUpdates.ChatProgressCategory
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GitHubUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.json.getJson
import at.hannibal2.skyhanni.utils.system.LazyVar
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.mojang.brigadier.arguments.BoolArgumentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes

@Suppress("TooManyFunctions")
abstract class AbstractRepoManager<E : AbstractRepoReloadEvent> {
    open fun getGson() = ConfigManager.gson

    @Suppress("UNCHECKED_CAST")
    private val eventClass: Class<E> by lazy {
        (this::class.java.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<E>
    }

    private val eventCtor by lazy {
        eventClass.getConstructor(AbstractRepoManager::class.java)
    }

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
        // ~/.minecraft/config/[...]/repo/[name]-repo-[def_branch].zip
        // e.g., 'sh-repo-main' or 'neu-repo-master'
        File(repoDirectory, "$commonShortName-repo-${config.location.defaultBranch}.zip")
    }
    private val commitStorage: RepoCommitStorage by lazy {
        // ~/.minecraft/config/[...]/currentCommit.json
        RepoCommitStorage(File(configDirectory, "currentCommit.json"))
    }
    private val successfulConstants = mutableSetOf<String>()
    private val unsuccessfulConstants = mutableSetOf<String>()
    private val githubRepoLocation: GitHubUtils.RepoLocation
        get() = GitHubUtils.RepoLocation(config.location, debugConfig.logRepoErrors)
    val repoMutex = Mutex()

    abstract val updateCommand: String
    abstract val statusCommand: String
    abstract val reloadCommand: String

    var repoFileSystem: RepoFileSystem by LazyVar { DiskRepoFileSystem(repoDirectory) }
        private set

    var localRepoCommit: RepoCommit = RepoCommit()
        private set

    var isUsingBackup: Boolean = false
        private set

    private var shouldManuallyReload: Boolean = false
    private var loadingError: Boolean = false
    private var latestError = SimpleTimeMark.farPast()

    abstract val progressCategory: ChatProgressCategory

    fun getFailedConstants() = unsuccessfulConstants.toList()
    fun getGitHubRepoPath(): String = githubRepoLocation.location

    // Will be invoked by the implementation of this class
    @Suppress("HandleEventInspection")
    fun registerCommands(event: CommandRegistrationEvent) {
        event.registerBrigadier(updateCommand) {
            description = "Remove and re-download the $commonName repo"
            category = CommandCategory.USERS_BUG_FIX
            simpleCallback { updateRepo("/$updateCommand", forceReset = true) }
            argCallback("force", BoolArgumentType.bool()) {
                description = "optionally only re-download if the repo is out of date"
                updateRepo("/$updateCommand force", forceReset = it)
            }
        }
        event.registerBrigadier(statusCommand) {
            description = "Shows the status of the $commonName repo"
            category = CommandCategory.USERS_BUG_FIX
            coroutineSimpleCallback {
                val progress = progressCategory.start("showing status via /$statusCommand")
                displayRepoStatus(progress, joinEvent = true, command = true)
                progress.end("done showing status")
            }
        }
        event.registerBrigadier(reloadCommand) {
            description = "Reloads the local $commonName repo"
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback {
                val progress = progressCategory.start("reloading local repo via /$reloadCommand")
                reloadLocalRepo(progress)
            }
        }
    }

    fun addSuccessfulConstant(fileName: String) = successfulConstants.add(fileName)
    fun addUnsuccessfulConstant(fileName: String) = unsuccessfulConstants.add(fileName)

    @PublishedApi
    internal fun resolvePath(dir: String, name: String) = "$dir/$name.json"

    @PublishedApi
    internal fun readJsonElement(path: String): JsonElement? {
        if (repoFileSystem.exists(path)) {
            return repoFileSystem.readAllBytesAsJsonElement(path)
        }
        val onDisk = repoDirectory.resolve(path)
        return if (!onDisk.isFile) {
            logger.logNonDestructiveError("Repo file not found: $path")
            null
        } else onDisk.getJson()
    }

    @PublishedApi
    internal inline fun <reified T : Any> getRepoData(
        directory: String,
        fileName: String,
        type: Type? = null,
        gson: Gson = getGson(),
    ): T = runCatching {
        val path = resolvePath(directory, fileName)
        val json = readJsonElement(path)
            ?: logger.throwError("Repo file '$fileName' not found.")
        if (type == null) gson.fromJson<T>(json)
        else gson.fromJson(json, type)
    }.getOrElse { e ->
        logger.throwErrorWithCause("Repo parsing error while trying to read constant '$fileName'", e)
    }

    // <editor-fold desc="Repo Management">
    fun updateRepo(reason: String, forceReset: Boolean = false) {
        val progress = progressCategory.start("updateRepo")
        progress.update("reason: $reason")
        progress.update("Remove and re-download, forceReset=$forceReset")
        shouldManuallyReload = true
        if (!config.location.valid) {
            logger.errorToChat("Invalid $commonName repo settings detected, resetting default settings.")
            resetRepositoryLocation()
        }

        SkyHanniMod.launchIOCoroutine("$commonName updateRepo", timeout = 2.minutes) {
            if (!fetchAndUnpackRepo(progress, command = true, forceReset = forceReset).canContinue) {
                logger.warn("Failed to fetch & unpack repo - aborting repository reload.")
                return@launchIOCoroutine
            }
            reloadRepository(progress, "$commonName repo updated successfully.")
            if (unsuccessfulConstants.isEmpty() && !isUsingBackup) return@launchIOCoroutine
            val informed = logger.logErrorStateWithData(
                "Error updating reading $commonName repo",
                "no success",
                "usingBackupRepo" to isUsingBackup,
                "unsuccessfulConstants" to unsuccessfulConstants,
            )
            if (informed) return@launchIOCoroutine
            logger.logToChat("§cFailed to load the $commonShortNameCased repo! See above for more infos.")
        }
    }

    private fun resetRepositoryLocation(manual: Boolean = false) = with(config.location) {
        if (hasDefaultSettings()) {
            if (manual) logger.logToChat("$commonShortNameCased repo settings are already on default!")
            return
        }

        reset()
        if (!manual) return@with
        ChatUtils.clickableChat(
            "Reset $commonName repo settings to default. " +
                "Click §aUpdate repo Now §ein config or run /$updateCommand to update!",
            onClick = { updateRepo("click in chat after reset") },
            "§eClick to update the $commonShortNameCased repo!",
        )
    }

    fun initRepo() {
        val progress = progressCategory.start("auto loading on init")
        shouldManuallyReload = true
        val loaded = AtomicBoolean(false)
        val job = SkyHanniMod.launchIOCoroutine("$commonName repo init", timeout = 2.minutes) {
            if (config.repoAutoUpdate && !fetchAndUnpackRepo(progress, command = false).canContinue) {
                progress.end("Failed to fetch & unpack repo - aborting repository reload.")
                logger.warn("Failed to fetch & unpack repo - aborting repository reload.")
                return@launchIOCoroutine
            }
            loaded.set(true)
            reloadRepository(progress)
        }
        job.invokeOnCompletion {
            if (!loaded.get()) {
                progress.end("reached timeout")
            }
        }
    }

    // Code taken + adapted from NotEnoughUpdates
    private fun switchToBackupRepo(progress: ChatProgressUpdates): FetchUnpackResult = runCatching {
        progress.update("switchToBackupRepo")
        if (PlatformUtils.isDevEnvironment) {
            progress.end("Can not use backup repo in dev env.")
            return@runCatching FetchUnpackResult.FAILED
        }

        if (backupRepoResourcePath == null) {
            progress.update("No backup repo resource path provided, cannot switch to backup repo.")
            logger.warn("No backup repo resource path provided, cannot switch to backup repo.")
            return FetchUnpackResult.FAILED
        }

        progress.update("Attempting to switch to backup repo")
        logger.debug("Attempting to switch to backup repo")
        val inputStream = javaClass.classLoader.getResourceAsStream(backupRepoResourcePath)
            ?: run {
                progress.update("Failed to find backup resource '$backupRepoResourcePath'")
                logger.throwError("Failed to find backup resource '$backupRepoResourcePath'")
            }

        progress.update("prepCleanRepoFileSystem")
        prepCleanRepoFileSystem(progress)

        Files.copy(inputStream, repoZipFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        if (!repoFileSystem.loadFromZip(progress, repoZipFile, logger)) {
            progress.update("Failed to load backup repo from zip file: ${repoZipFile.absolutePath}")
            logger.throwError("Failed to load backup repo from zip file: ${repoZipFile.absolutePath}")
        }

        isUsingBackup = true
        progress.update("writeToFile: switchToBackupRepo")
        commitStorage.writeToFile(RepoCommit("backup-repo", time = null))
        progress.update("Successfully switched to backup repo")
        logger.debug("Successfully switched to backup repo")
        return FetchUnpackResult.SWITCHED_TO_BACKUP
    }.onFailure { e ->
        logger.logNonDestructiveError("Failed to switch to backup repo: ${e.message}")
        progress.update("reason: ${e.message ?: "no reason"}")
        progress.end("Failed to switch to backup repo")
    }.getOrDefault(FetchUnpackResult.FAILED)

    open fun reportExtraStatusInfo(): Unit = Unit

    private suspend fun isRepeatErrorOrFixed(progress: ChatProgressUpdates): Boolean {
        progress.update("isRepeatErrorOrFixed")
        if (latestError.passedSince() < 5.minutes || !config.repoAutoUpdate) {
            progress.end("is repeat error or auto update disabled")
            return true
        }
        latestError = SimpleTimeMark.now()

        val comparison = getCommitComparison(silentError = false)
        val isOutdated = comparison?.let { !it.hashesMatch } ?: run {
            logger.logNonDestructiveError("Failed to fetch latest commit for repo status check.")
            false
        }
        if (isOutdated) {
            logger.logToChat("Repo Issue caught, however the repo is outdated.\n§aTrying to update it now...")
            val result = fetchAndUnpackRepo(progress, command = false)
            if (result == FetchUnpackResult.SUCCESS) {
                logger.logToChat("§a$commonName repo updated successfully!")
                progress.update("repo update successfully!")
                return true
            } else {
                logger.logToChat("§cFailed to update the $commonName repo.")
                progress.update("Failed to update the $commonName repo.")
            }
        }
        return false
    }

    suspend fun displayRepoStatus(progress: ChatProgressUpdates, joinEvent: Boolean, command: Boolean = false) {
        progress.update("displayRepoStatus for $commonName")
        if (joinEvent) return onJoinStatusError(progress)

        val (currentDownloadedCommit, _) = commitStorage.readFromFile() ?: RepoCommit()
        if (unsuccessfulConstants.isEmpty() && successfulConstants.isNotEmpty()) {
            logger.logToChat("$commonName repo working fine! Commit hash: §b$currentDownloadedCommit§r")
            reportExtraStatusInfo()
            return
        }

        if (!command && isRepeatErrorOrFixed(progress)) return
        logger.errorToChat("$commonName repo has errors! Commit hash: §b$currentDownloadedCommit§r")

        if (successfulConstants.isNotEmpty()) logger.logToChat("Successful Constants §7(${successfulConstants.size}):")
        for (constant in successfulConstants) logger.logToChat("   - §7$constant")

        logger.logToChat("Unsuccessful Constants §7(${unsuccessfulConstants.size}):", color = "§e")
        for (constant in unsuccessfulConstants) logger.logToChat("   - §7$constant", color = "§e")

        progress.update("reportExtraStatusInfo")
        reportExtraStatusInfo()
        progress.update("done with displayRepoStatus")
    }

    private suspend fun onJoinStatusError(progress: ChatProgressUpdates) {
        progress.update("onJoinStatusError")
        if (unsuccessfulConstants.isEmpty() || isRepeatErrorOrFixed(progress)) return
        // Last sanity check, we want to make sure repo is up to date before displaying
        val text = buildList {
            add("§c[SkyHanni-${SkyHanniMod.VERSION}] §7$commonName repo Issue!")
            add("§cSome features may not work. Please report this error on the Discord if it persists!")
            add("§7Repo Auto Update Value: §c${config.repoAutoUpdate}")
            add("§7Backup repo Value: §c$isUsingBackup")
            if (!config.repoAutoUpdate) add("§4You have repo Auto Update turned off, please try turning that on.")
            add("§cUnsuccessful Constants §7(${unsuccessfulConstants.size}):")
            for (constant in unsuccessfulConstants) {
                add("   §e- §7$constant")
            }
        }.map { it.asComponent() }
        TextHelper.multiline(text).send()
    }

    private enum class FetchUnpackResult(val canContinue: Boolean = true) {
        SUCCESS,
        SWITCHED_TO_BACKUP,
        FAILED(false),
    }

    /**
     * Returns a [RepoComparison] object that represents the 'diff' between the local commit,
     * and the latest commit from GitHub.
     * May return null if the latest commit could not be fetched.
     *
     * @param silentError If true, will not show errors to the user.
     */
    private suspend fun getCommitComparison(silentError: Boolean): RepoComparison? {
        localRepoCommit = commitStorage.readFromFile() ?: RepoCommit()
        val latestRepoCommit = githubRepoLocation.getLatestCommit(silentError) ?: return null
        return RepoComparison(commonName, localRepoCommit, latestRepoCommit)
    }

    /**
     * Determines the latest commit on the GitHub repo and compares it to the current commit.
     * If out of date, will download the latest commit zip file and unpack it into the repo directory.
     * Will automatically switch to the backup repo if the download fails or the unpacking fails,
     * unless `switchToBackupOnFail` is false.
     *
     * @param command If true, will report the status of the repo to the user.
     * @param silentError If true, will not log errors to the console.
     * @param forceReset If true, will always download the latest commit zip file, even if the repo is up to date.
     * @param switchToBackupOnFail If true, will switch to the backup repo if the download or unpacking fails.
     * @return FetchUnpackResult.SUCCESS if the repo was successfully fetched and unpacked,
     *         FetchUnpackResult.SWITCHED_TO_BACKUP if the backup repo was used,
     *         FetchUnpackResult.FAILED if the repo could not be fetched or unpacked and no backup repo is available.
     */
    private suspend fun fetchAndUnpackRepo(
        progress: ChatProgressUpdates,
        command: Boolean,
        silentError: Boolean = true,
        forceReset: Boolean = false,
        switchToBackupOnFail: Boolean = true,
    ): FetchUnpackResult = repoMutex.withLock {
        progress.update("fetchAndUnpackRepo")
        val comparison = getCommitComparison(silentError) ?: run {
            return if (switchToBackupOnFail) switchToBackupRepo(progress)
            else FetchUnpackResult.FAILED
        }
        if (comparison.hashesMatch && !forceReset && repoDirectory.exists() && unsuccessfulConstants.isEmpty()) {
            if (command) {
                comparison.reportRepoUpToDate()
                shouldManuallyReload = false
            }
            return FetchUnpackResult.SUCCESS
        } else if (command) {
            if (!comparison.hashesMatch) {
                progress.update("hashes don't match, outdated!")
                comparison.reportRepoOutdated()
            } else if (forceReset) comparison.reportForceRebuild()
        }

        progress.update("prepCleanRepoFileSystem")
        prepCleanRepoFileSystem(progress)

        progress.update("downloadCommitZipToFile")
        if (!githubRepoLocation.downloadCommitZipToFile(repoZipFile)) {
            progress.update("Failed to download the repo zip file from GitHub.")
            logger.logNonDestructiveError("Failed to download the repo zip file from GitHub.")
            return if (switchToBackupOnFail) switchToBackupRepo(progress)
            else {
                progress.update("FetchUnpackResult.FAILED")
                FetchUnpackResult.FAILED
            }
        }

        progress.update("loadFromZip")
        // Actually unpack the repo zip file into our local 'file system'
        if (!repoFileSystem.loadFromZip(progress, repoZipFile, logger)) {
            progress.update("Failed to unpack the downloaded zip file.")
            logger.logNonDestructiveError("Failed to unpack the downloaded zip file.")
            return if (switchToBackupOnFail) switchToBackupRepo(progress)
            else FetchUnpackResult.FAILED
        }

        progress.update("writeToFile: fetchAndUnpackRepo")
        commitStorage.writeToFile(comparison.latest)
        isUsingBackup = false
        return FetchUnpackResult.SUCCESS
    }

    private fun prepCleanRepoFileSystem(progress: ChatProgressUpdates) {
        progress.update("deleteRecursively")
        repoDirectory.deleteRecursively()
        progress.update("createAndClean")
        repoFileSystem = RepoFileSystem.createAndClean(repoDirectory, config.unzipToMemory)
        progress.update("mkdirs")
        repoDirectory.mkdirs()
        progress.update("createNewFile")
        repoZipFile.createNewFile()
        progress.update("done with prepCleanRepoFileSystem")
    }

    fun reloadLocalRepo(progress: ChatProgressUpdates, answerMessage: String = "$commonName repo loaded from local files successfully.") {
        progress.update("reloadLocalRepo")
        shouldManuallyReload = true
        SkyHanniMod.launchIOCoroutine("$commonName reloadLocalRepo", timeout = 2.minutes) {
            reloadRepository(progress, answerMessage)
        }
    }

    /**
     * Called before the repo reload event is fired, but inside the IO coroutine.
     */
    open suspend fun extraReloadCoroutineWork(progress: ChatProgressUpdates) = Unit

    private suspend fun reloadRepository(progress: ChatProgressUpdates, answerMessage: String = "") = repoMutex.withLock {
        progress.update("reloadRepository")
        if (!shouldManuallyReload) {
            progress.end("should not manually reload")
            return
        }
        loadingError = false
        successfulConstants.clear()
        unsuccessfulConstants.clear()
        progress.update("extraReloadCoroutineWork")
        extraReloadCoroutineWork(progress)

        val event = eventCtor.newInstance(this)
        progress.update("posting events: ${event.javaClass.simpleName}")
        event.post { error ->
            if (loadingError) return@post
            progress.update("Error while posting repo reload event: ${error.message}")
            logger.logErrorWithData(error, "Error while posting repo reload event")
            loadingError = true
        }
        progress.update("post done")
        // Only check if we can dispose after the event has been posted, as we may see speed increases using
        // the MemoryRepoFileSystem for the event, and writing to disk after the event.
        progress.update("transitionAfterReload")
        repoFileSystem = repoFileSystem.transitionAfterReload(progress)

        progress.update("transitionAfterReload done")
        if (answerMessage.isNotEmpty() && !loadingError) {
            progress.end("answerMessage: $answerMessage")
            logger.logToChat("§a$answerMessage")
        } else if (loadingError) {
            progress.end("Error with the $commonShortName repo detected")
            ChatUtils.clickableChat(
                "Error with the $commonShortName repo detected, try /$updateCommand to fix it!",
                onClick = { updateRepo("click on chat after error") },
                "§eClick to update the repo!",
                prefixColor = "§c",
            )
            if (unsuccessfulConstants.isEmpty()) unsuccessfulConstants.add("All Constants")
        } else {
            progress.end("done reloading $commonShortName repo")
        }
    }
}
