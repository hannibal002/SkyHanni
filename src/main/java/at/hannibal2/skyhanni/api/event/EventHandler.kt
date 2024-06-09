package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.chat.Text

class EventHandler<T : SkyHanniEvent> private constructor(
    val name: String,
    private val listeners: List<EventListeners.Listener>,
    private val canReceiveCancelled: Boolean,
) {

    var invokeCount: Long = 0L
        private set

    constructor(event: Class<T>, listeners: List<EventListeners.Listener>) : this(
        (event.name.split(".").lastOrNull() ?: event.name).replace("$", "."),
        listeners.sortedBy { it.options.priority }.toList(),
        listeners.any { it.options.receiveCancelled }
    )

    fun post(event: T, onError: ((Throwable) -> Unit)? = null): Boolean {
        invokeCount++
        if (this.listeners.isEmpty()) return false

        if (SkyHanniEvents.isDisabledHandler(name)) return false

        var errors = 0

        for (listener in listeners) {
            if (!shouldInvoke(event, listener)) continue
            try {
                listener.invoker.accept(event)
            } catch (throwable: Throwable) {
                errors++
                if (errors <= 3) {
                    val errorName = throwable::class.simpleName ?: "error"
                    val message = "Caught an $errorName in ${listener.name} at $name: ${throwable.message}"
                    ErrorManager.logErrorWithData(throwable, message, ignoreErrorCache = onError != null)
                }
                onError?.invoke(throwable)
            }
            if (event.isCancelled && !canReceiveCancelled) break
        }

        if (errors > 3) {
            val hiddenErrors = errors - 3
            ChatUtils.chat(
                Text.text(
                    "§c[SkyHanni/${SkyHanniMod.version}] $hiddenErrors more errors in $name are hidden!"
                )
            )
        }
        return event.isCancelled
    }

    private fun shouldInvoke(event: SkyHanniEvent, listener: EventListeners.Listener): Boolean {
        if (SkyHanniEvents.isDisabledInvoker(listener.name)) return false
        if (listener.options.onlyOnSkyblock && !LorenzUtils.inSkyBlock) return false
        if (listener.options.onlyOnIsland != IslandType.ANY && !listener.options.onlyOnIsland.isInIsland()) return false
        if (event.isCancelled && !listener.options.receiveCancelled) return false
        if (
            event is GenericSkyHanniEvent<*> &&
            listener.generic != null &&
            !listener.generic.isAssignableFrom(event.type)
        ) {
            return false
        }
        return true
    }
}
