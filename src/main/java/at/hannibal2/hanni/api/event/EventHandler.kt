package at.hannibal2.hanni.api.event

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.test.command.ErrorManager
import at.hannibal2.hanni.test.command.ErrorManager.maybeSkipError
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.StringUtils
import at.hannibal2.hanni.utils.chat.TextHelper

class EventHandler<T : HanniEvent> private constructor(
    val name: String,
    private val listeners: List<EventListeners.Listener>,
    private val canReceiveCancelled: Boolean,
) {

    val invokeLog = HanniEvents.EventInvokeLog()

    constructor(event: Class<T>, listeners: List<EventListeners.Listener>) : this(
        (event.name.split(".").lastOrNull() ?: event.name).replace("$", "."),
        listeners.sortedBy { it.priority }.toList(),
        listeners.any { it.receiveCancelled },
    )

    fun post(event: T, onError: ((Throwable) -> Unit)? = null): Boolean {
        invokeLog.invokeCount++
        if (this.listeners.isEmpty()) return false

        if (HanniEvents.isDisabledHandler(name)) return false

        var errors = 0

        for (listener in listeners) {
            if (!listener.shouldInvoke(event)) continue
            try {
                listener.invoker.accept(event)
            } catch (originalThrowable: Throwable) {
                val throwable = originalThrowable.maybeSkipError()
                errors++
                if (errors <= 3) {
                    val errorName = throwable::class.simpleName ?: "error"
                    val aOrAn = StringUtils.optionalAn(errorName)
                    val message = "Caught $aOrAn $errorName in ${listener.name} at $name: ${throwable.message}"
                    ErrorManager.logErrorWithData(throwable, message, ignoreErrorCache = onError != null)
                }
                onError?.invoke(throwable)
            }
            if (event.isCancelled && !canReceiveCancelled) break
        }

        if (errors > 3) {
            val hiddenErrors = errors - 3
            ChatUtils.chat(
                TextHelper.text(
                    "§c[Hanni/${HanniMod.VERSION}] $hiddenErrors more errors in $name are hidden!",
                ),
            )
        }
        return event.isCancelled
    }
}
