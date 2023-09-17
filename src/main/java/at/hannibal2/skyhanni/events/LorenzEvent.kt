package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event

abstract class LorenzEvent : Event() {

    private val eventName by lazy {
        this::class.simpleName
    }

    fun postAndCatch(): Boolean {
        return runCatching {
            postWithoutCatch()
        }.onFailure {
            CopyErrorCommand.logError(
                it,
                "Caught an ${it::class.simpleName ?: "error"} at ${eventName}: '${it.message}'"
            )
        }.getOrDefault(isCanceled)
    }

    fun postWithoutCatch() = MinecraftForge.EVENT_BUS.post(this)
}