package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SkyHanniLogger
import java.io.File

class RepoLogger(manager: AbstractRepoManager<*>) : SkyHanniLogger(manager.commonName) {

    private val loggingPrefix = "[Repo - ${manager.commonName}]"
    override val logsDir = File(manager.repoDirectory, "logs")

    fun debug(message: String) = log("[DEBUG] $loggingPrefix $message")
    fun warn(message: String) = log("[WARN] $loggingPrefix $message")
    fun error(message: String) = log("[ERROR] $loggingPrefix $message")

    fun chat(message: String, color: String = "§a") = ChatUtils.chat("$color$loggingPrefix $message", prefix = false)
    fun chatError(error: String) = ChatUtils.userError("§c$loggingPrefix $error")

    fun errorWithData(cause: Throwable, error: String): Boolean =
        ErrorManager.logErrorWithData(cause, "$loggingPrefix $error")

    fun errorStateWithData(
        userMessage: String,
        internalMessage: String,
        vararg extraData: Pair<String, Any?>,
        ignoreErrorCache: Boolean = false,
        noStackTrace: Boolean = false,
        betaOnly: Boolean = false,
        condition: () -> Boolean = { true },
    ) = ErrorManager.logErrorStateWithData(
        userMessage,
        "$loggingPrefix $internalMessage",
        *extraData,
        ignoreErrorCache = ignoreErrorCache,
        noStackTrace = noStackTrace,
        betaOnly = betaOnly,
        condition = condition,
    )

    fun throwError(error: String): Nothing = throw RepoError("$loggingPrefix $error")
    fun throwErrorWithCause(error: String, cause: Throwable): Nothing =
        throw RepoError("$loggingPrefix $error", cause)
}
