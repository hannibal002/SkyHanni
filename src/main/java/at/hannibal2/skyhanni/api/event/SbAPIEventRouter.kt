package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ReflectionUtils
import at.hannibal2.skyhanni.utils.StringUtils
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.SkyBlockEvent
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import java.lang.reflect.Method

object SbAPIEventRouter {
    private val sbaSubscriptions = mutableMapOf<Any, MutableList<(SkyBlockEvent) -> Unit>>()

    private fun convertPriority(skyHanniPriority: Int): Int = when (skyHanniPriority) {
        HandleEvent.HIGHEST -> Subscription.HIGHEST
        HandleEvent.HIGH -> Subscription.HIGH
        0 -> 0
        HandleEvent.LOW -> Subscription.LOW
        HandleEvent.LOWEST -> Subscription.LOWEST
        else -> skyHanniPriority * 100000 // Fallback conversion
    }

    /**
     * Directly registers a @HandleEvent method into SBA's native Event Bus
     */
    fun register(
        instance: Any,
        method: Method,
        eventType: Class<out SkyBlockEvent>,
        options: HandleEvent
    ) {
        val sbaPriority = convertPriority(options.priority)
        val eventName = SkyHanniEvents.getEventName(eventType)
        val listenerName = ReflectionUtils.buildMethodName(method)
        val eventConsumer = EventListeners.createConsumerFromMethod(method, instance)
        val indices = ListenerCollection.createListenerIndices(options)

        val callback: (SkyBlockEvent) -> Unit = { event ->
            if (SkyHanniEvents.getCurrentStateIndex() in indices) {
                try {
                    eventConsumer(event)
                } catch (throwable: Throwable) {
                    val errorName = throwable::class.simpleName ?: "error"
                    val aOrAn = StringUtils.optionalAn(errorName)
                    val message = "Caught $aOrAn $errorName in $listenerName at $eventName: ${throwable.message}"
                    ErrorManager.logErrorWithData(throwable, message)
                }
            }
        }

        SkyBlockAPI.eventBus.register(
            type = eventType,
            priority = sbaPriority,
            receiveCancelled = options.receiveCancelled,
            callback = callback
        )

        sbaSubscriptions.getOrPut(instance) { mutableListOf() }.add(callback)
    }

    fun unregister(instance: Any) {
        val callbacks = sbaSubscriptions.remove(instance) ?: return
        callbacks.forEach { SkyBlockAPI.eventBus.unregister(it) }
    }
}
