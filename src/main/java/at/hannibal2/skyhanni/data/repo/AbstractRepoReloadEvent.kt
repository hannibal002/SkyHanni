package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import kotlinx.coroutines.runBlocking

abstract class AbstractRepoReloadEvent(
    open val manager: AbstractRepoManager<*>,
) : SkyHanniEvent() {
    @Deprecated("Use suspended version of this function instead", replaceWith = ReplaceWith("getConstantAsync"))
    inline fun <reified T : Any> getConstant(constant: String): T = runBlocking {
        getConstantAsync(constant)
    }

    suspend inline fun <reified T : Any> getConstantAsync(constant: String): T = runCatching {
        // This will throw an error if the constant is not found
        val constantData = manager.getRepoDataAsync<T>("constants", constant)
        // So we can safely assume it exists and is successfully loaded
        manager.addSuccessfulConstant(constant)
        // Then return the constant data
        return constantData
    }.getOrElse {
        manager.addUnsuccessfulConstant(constant)
        manager.logger.throwErrorWithCause("Could not load constant '$constant'", it)
    }
}
