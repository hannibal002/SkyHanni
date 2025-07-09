package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager

class RepoLogger(private val loggingPrefix: String) {
    fun debug(message: String) = SkyHanniMod.logger.debug("$loggingPrefix $message")
    fun warn(message: String) = SkyHanniMod.logger.warn("$loggingPrefix $message")

    fun logError(error: String): Nothing = ErrorManager.skyHanniError("$loggingPrefix $error")
    fun logErrorWithData(cause: Throwable, error: String): Boolean =
        ErrorManager.logErrorWithData(cause, "$loggingPrefix $error")

    fun throwError(error: String): Nothing = throw RepoError("$loggingPrefix $error")
    fun throwErrorWithCause(error: String, cause: Throwable): Nothing =
        throw RepoError("$loggingPrefix $error", cause)
}
