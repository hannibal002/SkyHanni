package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.EventCounter
import at.hannibal2.skyhanni.mixins.hooks.getValue
import at.hannibal2.skyhanni.mixins.hooks.setValue
import at.hannibal2.skyhanni.mixins.transformers.AccessorEventBus
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.IEventListener

abstract class LorenzEvent : Event() {

    private val eventName by lazy {
        this::class.simpleName!!
    }

    fun postAndCatch() = postAndCatchAndBlock {}

    companion object {

        var eventHandlerDepth by object : ThreadLocal<Int>() {
            override fun initialValue(): Int {
                return 0
            }
        }
        val isInGuardedEventHandler get() = eventHandlerDepth > 0 || LorenzUtils.isInDevEnvironment()
    }

    fun postAndCatchAndBlock(
        printError: Boolean = true,
        stopOnFirstError: Boolean = false,
        ignoreErrorCache: Boolean = false,
        onError: (Throwable) -> Unit,
    ): Boolean {
        EventCounter.count(eventName)
        val visibleErrors = 3
        var errors = 0
        eventHandlerDepth++
        for (listener in getListeners()) {
            try {
                listener.invoke(this)
            } catch (throwable: Throwable) {
                errors++
                if (printError && errors <= visibleErrors) {
                    val callerName = listener.toString().split(" ")[1].split("@")[0].split(".").last()
                    val errorName = throwable::class.simpleName ?: "error"
                    val message = "Caught an $errorName in $callerName at $eventName: ${throwable.message}"
                    ErrorManager.logErrorWithData(throwable, message, ignoreErrorCache = ignoreErrorCache)
                }
                onError(throwable)
                if (stopOnFirstError) break
            }
        }
        eventHandlerDepth--
        if (errors > visibleErrors) {
            val hiddenErrors = errors - visibleErrors
            ChatUtils.error("$hiddenErrors more errors in $eventName are hidden!")
        }
        return if (isCancelable) isCanceled else false
    }

    private fun getListeners(): Array<out IEventListener> {
        val accessorEventBus = MinecraftForge.EVENT_BUS as AccessorEventBus
        return listenerList.getListeners(accessorEventBus.busId)
    }

    fun postWithoutCatch() = MinecraftForge.EVENT_BUS.post(this)

    // TODO let walker use this function for all 101 other uses
    fun cancel() {
        isCanceled = true
    }
}
