package de.hype.bingonet.shared.constants

/**
 * Enumeration representing internal reasons constants for a specific functionality.
 * These constants are used to provide detailed information about various scenarios or states.
 *
 *
 * The following are the possible reasons represented by this enumeration:
 *
 *  * [.INVALID_PARAMETER]: Invalid parameter was provided.
 *  * [.MISSING_PARAMETER]: Required parameter is missing.
 *  * [.INSUFFICIENT_PRIVILEGES]: Insufficient privileges to perform the operation.
 *  * [.MUTED]: User is muted.
 *  * [.BANNED]: User is banned.
 *  * [.API_UNSUPPORTED]: API feature is unsupported.
 *  * [.INVALID_LOGIN]: Invalid login attempt.
 *  * [.KICKED]: User was kicked from the server.
 *  * [.ANOTHER_LOGIN]: Another login detected for the same user.
 *  * [.SERVER_RESTART]: Server is restarting.
 *  * [.NOT_REGISTERED]: User is not registered.
 *  * [.ON_COOLDOWN]: Operation is on cooldown.
 *  * [.OTHER]: Other unspecified reason.
 *
 */
enum class InternalReasonConstants {
    INVALID_PARAMETER,
    MISSING_PARAMETER,
    INSUFFICIENT_PRIVILEGES,
    MUTED,
    BANNED,
    API_UNSUPPORTED,
    INVALID_LOGIN,
    KICKED,
    ANOTHER_LOGIN,
    SERVER_RESTART,
    NOT_REGISTERED,
    ON_COOLDOWN,
    SYSTEM,
    OTHER
}
