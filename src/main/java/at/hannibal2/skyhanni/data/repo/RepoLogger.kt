package at.hannibal2.hanni.data.repo

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.test.command.ErrorManager
import at.hannibal2.hanni.utils.ChatUtils

// todo this class is a mess, it should get cleaned up and standardized with ChatUtils and ErrorManager
//  should be some genericized way to create loggers that can utilize either ChatUtils or ErrorManager or HanniMod.logger
//  depending on the use case
class RepoLogger(private val loggingPrefix: String) {
    fun debug(message: String) = HanniMod.logger.debug("$loggingPrefix $message")
    fun preDebug(message: String) = println("$loggingPrefix $message")
    fun warn(message: String) = HanniMod.logger.warn("$loggingPrefix $message")
    fun logToChat(message: String, color: String = "§a") = ChatUtils.chat("$color$loggingPrefix $message", prefix = false)
    fun errorToChat(error: String) = ChatUtils.userError("§c$loggingPrefix $error")

    fun logNonDestructiveError(error: String) = HanniMod.logger.error("$loggingPrefix $error")
    fun logError(error: String): Nothing = ErrorManager.hanniError("$loggingPrefix $error")
    fun logErrorWithData(cause: Throwable, error: String): Boolean =
        ErrorManager.logErrorWithData(cause, "$loggingPrefix $error")
    fun logErrorStateWithData(
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
