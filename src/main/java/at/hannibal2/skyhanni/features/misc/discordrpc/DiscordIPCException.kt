package at.hannibal2.skyhanni.features.misc.discordrpc

/**
 * Thrown when the Discord IPC client encounters a connectivity or protocol-level error.
 *
 * @param isSandboxIssue True when the failure is caused by a sandbox filesystem restriction
 *   rather than Discord not running. The [message] will contain the actionable fix.
 */
class DiscordIPCException(
    message: String,
    cause: Throwable? = null,
    val isSandboxIssue: Boolean = false,
) : Exception(message, cause)
