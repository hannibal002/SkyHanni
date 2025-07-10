package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import com.google.gson.Gson
import java.lang.reflect.Type

abstract class AbstractRepoReloadEvent(
    open val manager: AbstractRepoManager,
) : SkyHanniEvent() {
    val repoDirectory by lazy { manager.repoDirectory }
    val gson by lazy { manager.getGson() }

    inline fun <reified T : Any> getConstant(
        constant: String,
        type: Type? = null,
        gson: Gson = this.gson,
    ): T = manager.getRepoData("constants", constant, type, gson)
}
