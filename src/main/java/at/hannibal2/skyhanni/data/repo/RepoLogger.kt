package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager

class RepoLogger(private val loggingPrefix: String) {
    fun debug(message: String) = SkyHanniMod.logger.debug("$loggingPrefix $message")
    fun preDebug(message: String) = println("$loggingPrefix $message")
    fun warn(message: String) = SkyHanniMod.logger.warn("$loggingPrefix $message")

    fun logNonDestructiveError(error: String) = SkyHanniMod.logger.error("$loggingPrefix $error")
    fun logError(error: String): Nothing = ErrorManager.skyHanniError("$loggingPrefix $error")
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
