package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.mixins.transformers.AccessorEventBus
import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.IEventListener

abstract class LorenzEvent : Event() {

    private val eventName by lazy {
        this::class.simpleName
    }

    fun postAndCatch() = postAndCatchAndBlock {}

    fun postAndCatchAndBlock(
        printError: Boolean = true,
        stopOnError: Boolean = false,
        onError: (Throwable) -> Unit,
    ): Boolean {
        for (listener in getListeners()) {
            try {
                listener.invoke(this)
            } catch (e: Throwable) {
                if (printError) {
                    val callerName = listener.toString().split(" ")[1].split("@")[0].split(".").last()
                    ErrorManager.logError(
                        e,
                        "Caught an ${e::class.simpleName ?: "error"} at $eventName in $callerName: '${e.message}'"
                    )
                }
                onError(e)
                if (stopOnError) break
            }
        }
        return if (isCancelable) isCanceled else false
    }

    private fun getListeners(): Array<out IEventListener> {
        val accessorEventBus = MinecraftForge.EVENT_BUS as AccessorEventBus
        return listenerList.getListeners(accessorEventBus.busId)
    }

    fun postWithoutCatch() = MinecraftForge.EVENT_BUS.post(this)
}
