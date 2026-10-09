package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.api.event.AsyncSkyHanniEvent
import kotlin.coroutines.cancellation.CancellationException

abstract class AbstractRepoReloadEvent(open val manager: AbstractRepoManager<*>) : AsyncSkyHanniEvent() {
    suspend inline fun <reified T : Any> getConstant(constant: String): T = try {
        // This will throw an error if the constant is not found
        manager.getRepoDataAsync<T>("constants", constant).also {
            // So we can safely assume it exists and is successfully loaded
            manager.addSuccessfulConstant(constant)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        // Will log and re-throw the error
        manager.addUnsuccessfulConstant(constant, e)
    }
}
