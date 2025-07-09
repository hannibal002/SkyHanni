package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.GitHubUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import java.io.File

abstract class AbstractRepoManager<C : AbstractRepoLocationConfig, E : AbstractRepoReloadEvent> {
    open fun getGson() = ConfigManager.gson

    abstract val commonRepoName: String
    abstract val config: C
    abstract val configFileLocation: File
    private val repoFileLocation by lazy { File(configFileLocation, "repo") }
    private val loggingPrefix by lazy { "[Repo - $commonRepoName ]" }
    private val successfulConstants = mutableListOf<String>()
    private val unsuccessfulConstants = mutableListOf<String>()

    private val githubRepoLocation: GitHubUtils.RepoLocation
        get() = GitHubUtils.RepoLocation(config)

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

    fun getRepoLocation(): String = githubRepoLocation.location

    fun reloadLocalRepo() {
        shouldManuallyReload = true
    }

    fun setLastConstant(constant: String) {
        lastConstant = constant
    }

    private fun reloadRepository(answerMessage: String = "") {

    }

}
