package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import com.google.gson.Gson

abstract class AbstractRepoReloadEvent(
    open val manager: AbstractRepoManager<*>,
) : SkyHanniEvent() {
    val repoDirectory by lazy { manager.repoDirectory }
    val gson by lazy { manager.getGson() }

    inline fun <reified T : Any> getConstant(constant: String, gson: Gson = this.gson): T = runCatching {
        // This will throw an error if the constant is not found
        val constantData = manager.getRepoData<T>("constants", constant, gson)
        // So we can safely assume it exists and is successfully loaded
        manager.addSuccessfulConstant(constant)
        // Then return the constant data
        return constantData
    }.getOrElse {
        manager.addUnsuccessfulConstant(constant)
        manager.logger.throwErrorWithCause("Could not load constant '$constant'", it)
    }
}
