package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.repo.RepoManager.updateRepo
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GitHubUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import java.io.File

abstract class AbstractRepoManager<C : AbstractRepoConfig<*>, E : AbstractRepoReloadEvent>{
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
    abstract val commonShortName: String

    abstract val config: C
    abstract val configFileLocation: File
    private val loggingPrefix by lazy { "[Repo - $commonName]" }
    private val repoFileLocation by lazy { File(configFileLocation, "repo") }
    private val githubRepoLocation: GitHubUtils.RepoLocation get() = GitHubUtils.RepoLocation(config)

    private val successfulConstants = mutableListOf<String>()
    private val unsuccessfulConstants = mutableListOf<String>()

    private var currentlyFetching = false
    private var shouldManuallyReload: Boolean = false
    private var loadingError: Boolean = false
    private var downloadFailed: Boolean = false
    private var lastUpdatedAt = SimpleTimeMark.now()
    private var lastConstant: String? = null

    var commitTime: SimpleTimeMark? = null
        private set

    var isUsingBackup: Boolean = false
        private set

    fun logError(error: String): Nothing = ErrorManager.skyHanniError("$loggingPrefix $error")
    fun throwError(error: String): Nothing = throw RepoError("$loggingPrefix $error")
    fun throwErrorWithCause(error: String, cause: Throwable): Nothing =
        throw RepoError("$loggingPrefix $error", cause)

    fun getGitHubRepoPath(): String = githubRepoLocation.location
    fun getRepoFileLocation() = repoFileLocation

    fun reloadLocalRepo() {
        shouldManuallyReload = true
    }

    fun setLastConstant(constant: String) {
        lastConstant = constant
    }

    // Because abstract classes cannot have reified types, this is necessary
    abstract fun fireReloadEvent(manager: AbstractRepoManager<C, E>, onError: (Throwable) -> Unit): Boolean

    private fun reloadRepository(answerMessage: String = "") {
        if (!shouldManuallyReload) return
        loadingError = false
        successfulConstants.clear()
        unsuccessfulConstants.clear()
        lastConstant = null

        fireReloadEvent(this) {
            loadingError = true
            lastConstant?.let { unsuccessfulConstants.add(it) }
            lastConstant = null
        }

        if (answerMessage.isNotEmpty() && !loadingError) {
            ChatUtils.chat("§a$answerMessage")
        } else if (loadingError) {
            ChatUtils.clickableChat(
                "Error with the repo detected, try /shupdaterepo to fix it!",
                onClick = {
                    updateRepo()
                },
                "§eClick to update the repo!",
                prefixColor = "§c",
            )
        }
    }

}
