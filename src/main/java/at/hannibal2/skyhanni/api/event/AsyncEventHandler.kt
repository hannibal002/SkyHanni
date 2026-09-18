package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.EventListeners.Listener
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.test.command.ErrorManager.maybeSkipError
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting
import kotlin.coroutines.cancellation.CancellationException

class AsyncEventHandler<T : AsyncSkyHanniEvent> private constructor(
    val name: String,
    private val listenerCollection: ListenerCollection,
    private val canReceiveCancelled: Boolean,
) {
    val invokeLog = SkyHanniEvents.EventInvokeLog()

    constructor(event: Class<T>, listeners: List<Listener>) : this(
        (event.name.split(".").lastOrNull() ?: event.name).replace("$", "."),
        ListenerCollection(listeners),
        listeners.any { it.receiveCancelled },
    )

    suspend fun post(event: T, onError: ((Throwable) -> Unit)? = null): Boolean {
        invokeLog.invokeCount++
        if (SkyHanniEvents.isDisabledHandler(name)) return false

        var errors = 0
        listenerCollection.forEachCurrent { listener ->
            if (!listener.shouldInvoke(event)) return@forEachCurrent true

            try {
                requireNotNull(listener.suspendInvoker) {
                    "Synchronous listener ${listener.name} registered for asynchronous event $name"
                }(event)
            } catch (e: CancellationException) {
                throw e
            } catch (originalThrowable: Throwable) {
                val throwable = originalThrowable.maybeSkipError()
                errors++
                if (errors <= 3) {
                    val errorName = throwable::class.simpleName ?: "error"
                    val message = "Caught ${StringUtils.optionalAn(errorName)} $errorName in " +
                        "${listener.name} at $name: ${throwable.message}"
                    runCatching {
                        ErrorManager.logErrorWithData(throwable, message, ignoreErrorCache = onError != null)
                    }
                }
                onError?.invoke(throwable)
            }

            !event.isCancelled || canReceiveCancelled
        }

        if (errors > 3) {
            ChatUtils.chat(
                componentBuilder {
                    append("[SkyHanni/${SkyHanniMod.VERSION}] ${errors - 3} more errors in $name are hidden!")
                    withColor(ChatFormatting.RED)
                },
            )
        }
        return event.isCancelled
    }
}
