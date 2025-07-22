package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GitHubUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.json.getJson
import at.hannibal2.skyhanni.utils.system.LazyVar
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.mojang.brigadier.arguments.BoolArgumentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.minecraft.util.IChatComponent
import java.io.File
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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

    open val shouldRegisterUpdateCommand: Boolean = true
    open val shouldRegisterStatusCommand: Boolean = true
    open val shouldRegisterReloadCommand: Boolean = true

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
    private var downloadFailed: Boolean = false

    fun getFailedConstants() = unsuccessfulConstants.toList()
    fun getGitHubRepoPath(): String = githubRepoLocation.location

    // Will be invoked by the implementation of this class
    @Suppress("HandleEventInspection")
    fun registerCommands(event: CommandRegistrationEvent) {
        if (shouldRegisterUpdateCommand) event.registerBrigadier(updateCommand) {
            description = "Remove and re-download the $commonName repo"
            category = CommandCategory.USERS_BUG_FIX
            simpleCallback { updateRepo(forceReset = true) }
            argCallback("force", BoolArgumentType.bool()) {
                description = "optionally only re-download if the repo is out of date"
                updateRepo(forceReset = it)
            }
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
        if (!onDisk.isFile) logger.throwError("Repo file not found: $path")
        return onDisk.getJson()
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
    fun updateRepo(forceReset: Boolean = false) {
        shouldManuallyReload = true
        if (!config.location.valid) {
            ChatUtils.userError("Invalid $commonName Repo settings detected, resetting default settings.")
            resetRepositoryLocation()
        }

        SkyHanniMod.launchIOCoroutine {
            fetchAndUnpackRepo(command = true, forceReset = forceReset)
            reloadRepository("$commonName Repo updated successfully.")
            if (unsuccessfulConstants.isEmpty() && !isUsingBackup) return@launchIOCoroutine
            val informed = logger.logErrorStateWithData(
                "Error updating reading $commonName Repo",
                "no success",
                "usingBackupRepo" to isUsingBackup,
                "unsuccessfulConstants" to unsuccessfulConstants,
            )
            if (informed) return@launchIOCoroutine
            ChatUtils.chat("§cFailed to load the $commonShortNameCased repo! See above for more infos.")
        }
    }

    private fun resetRepositoryLocation(manual: Boolean = false) = with(config.location) {
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
                fetchAndUnpackRepo(command = false)
                if (downloadFailed) switchToBackupRepo()
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
            val inputStream = javaClass.classLoader.getResourceAsStream(backupRepoResourcePath)
                ?: logger.throwError("Failed to find backup resource '$backupRepoResourcePath'")

            prepCleanRepoFileSystem()

            Files.copy(inputStream, repoZipFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            if (!repoFileSystem.loadFromZip(repoZipFile, logger)) {
                logger.throwError("Failed to load backup repo from zip file: ${repoZipFile.absolutePath}")
            }

            commitStorage.writeToFile(RepoCommit("backup-repo", time = null))
            logger.debug("Successfully switched to backup repo")
        } catch (e: Error) {
            logger.logErrorWithData(e, "Failed to switch to backup repo")
        }
    }

    open fun reportExtraStatusInfo(): Unit = Unit

    fun displayRepoStatus(joinEvent: Boolean) {
        if (joinEvent) return onJoinStatusError()

        val (currentDownloadedCommit, _) = commitStorage.readFromFile() ?: RepoCommit()
        if (unsuccessfulConstants.isEmpty() && successfulConstants.isNotEmpty()) {
            ChatUtils.chat("$commonName Repo working fine! Commit hash: $currentDownloadedCommit", prefixColor = "§a")
            reportExtraStatusInfo()
            return
        }
        ChatUtils.chat("$commonName Repo has errors! Commit hash: $currentDownloadedCommit", prefixColor = "§c")
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
                "§c[SkyHanni-${SkyHanniMod.VERSION}] §7$commonName Repo Issue! Some features may not work. " +
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
     * Determines the latest commit on the GitHub repo and compares it to the current commit.
     * If out of date, will download the latest commit zip file and unpack it into the repo directory.
     *
     * @param command If true, will report the status of the repo to the user.
     * @param silentError If true, will not log errors to the console.
     */
    private suspend fun fetchAndUnpackRepo(
        command: Boolean,
        silentError: Boolean = true,
        forceReset: Boolean = false,
    ) = repoMutex.withLock {
        localRepoCommit = commitStorage.readFromFile() ?: RepoCommit()
        val (currentSha, currentCommitTime) = localRepoCommit

        val (latestSha, latestCommitTime) = githubRepoLocation.getLatestCommit(silentError)?.let { response ->
            response.sha to response.commit.committer.date
        } ?: ((null to null).also { downloadFailed = true })

        val diffCheck = RepoComparison(currentSha, currentCommitTime, latestSha, latestCommitTime)
        val outdated = !diffCheck.hashesMatch

        if (!outdated && !forceReset && repoDirectory.exists() && unsuccessfulConstants.isEmpty()) {
            if (command) {
                diffCheck.reportRepoUpToDate()
                shouldManuallyReload = false
            }
            return
        } else if ((command && outdated) || forceReset) diffCheck.reportRepoOutdated()

        prepCleanRepoFileSystem()

        if (!githubRepoLocation.downloadCommitZipToFile(repoZipFile)) {
            downloadFailed = true
            logger.logError("Failed to download the repo zip file from GitHub.")
        }

        // Actually unpack the repo zip file into our local 'file system'
        if (!repoFileSystem.loadFromZip(repoZipFile, logger)) {
            downloadFailed = true
            logger.logError("Failed to unpack the downloaded zip file.")
        } else {
            localRepoCommit = RepoCommit(latestSha, latestCommitTime)
            commitStorage.writeToFile(localRepoCommit)
            downloadFailed = false
            isUsingBackup = false
        }
    }

    private fun prepCleanRepoFileSystem() {
        repoDirectory.deleteRecursively()
        repoFileSystem = RepoFileSystem.createAndClean(repoDirectory, config.unzipToMemory)
        repoDirectory.mkdirs()
        repoZipFile.createNewFile()
    }

    fun reloadLocalRepo(answerMessage: String = "$commonName Repo loaded from local files successfully.") {
        shouldManuallyReload = true
        SkyHanniMod.launchIOCoroutine {
            reloadRepository(answerMessage)
        }
    }

    /**
     * Called before the repo reload event is fired, but inside the IO coroutine.
     */
    open suspend fun extraReloadCoroutineWork() = Unit

    private suspend fun reloadRepository(answerMessage: String = "") = repoMutex.withLock {
        if (!shouldManuallyReload) return
        loadingError = false
        successfulConstants.clear()
        unsuccessfulConstants.clear()
        extraReloadCoroutineWork()

        eventCtor.newInstance(this).post { error ->
            logger.logErrorWithData(error, "Error while posting repo reload event")
            loadingError = true
        }
        // Only check if we can dispose after the event has been posted, as we may see speed increases using
        // the MemoryRepoFileSystem for the event, and writing to disk after the event.
        repoFileSystem = repoFileSystem.transitionAfterReload()

        if (answerMessage.isNotEmpty() && !loadingError) {
            ChatUtils.chat("§a$answerMessage")
        } else if (loadingError) {
            ChatUtils.clickableChat(
                "Error with the $commonShortName Repo detected, try /$updateCommand to fix it!",
                onClick = ::updateRepo,
                "§eClick to update the Repo!",
                prefixColor = "§c",
            )
            if (unsuccessfulConstants.isEmpty()) unsuccessfulConstants.add("All Constants")
        }
    }
}
