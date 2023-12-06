package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.EventCounter
import at.hannibal2.skyhanni.mixins.transformers.AccessorEventBus
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.IEventListener

abstract class LorenzEvent : Event() {

    private val eventName by lazy {
        this::class.simpleName!!
    }

    fun postAndCatch() = postAndCatchAndBlock {}

    fun postAndCatchAndBlock(
        printError: Boolean = true,
        stopOnFirstError: Boolean = false,
        ignoreErrorCache: Boolean = false,
        onError: (Throwable) -> Unit,
    ): Boolean {
        EventCounter.count(eventName)
        val visibleErrors = 3
        var errors = 0
        for (listener in getListeners()) {
            try {
                listener.invoke(this)
            } catch (throwable: Throwable) {
                errors++
                if (printError && errors <= visibleErrors) {
                    val callerName = listener.toString().split(" ")[1].split("@")[0].split(".").last()
                    val errorName = throwable::class.simpleName ?: "error"
                    val message = "Caught an $errorName at $eventName in $callerName: '${throwable.message}'"
                    ErrorManager.logError(throwable, message, ignoreErrorCache)
                }
                onError(throwable)
                if (stopOnFirstError) break
            }
        }
        if (errors > visibleErrors) {
            val hiddenErrors = errors - visibleErrors
            LorenzUtils.error("$hiddenErrors more errors in $eventName are hidden!")
        }
        return if (isCancelable) isCanceled else false
    }

    private fun getListeners(): Array<out IEventListener> {
        val accessorEventBus = MinecraftForge.EVENT_BUS as AccessorEventBus
        return listenerList.getListeners(accessorEventBus.busId)
    }

    fun postWithoutCatch() = MinecraftForge.EVENT_BUS.post(this)
}
