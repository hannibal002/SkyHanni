package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.api.minecraftevents.ClientEvents
import at.hannibal2.skyhanni.data.jsonobjects.repo.DisabledEventsJson
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIfKey
import java.lang.reflect.Method

@SkyHanniModule
object SkyHanniEvents {

    private val listeners: MutableMap<Class<*>, EventListeners> = mutableMapOf()
    private val handlers: MutableMap<Class<*>, EventHandler<*>> = mutableMapOf()
    private var disabledHandlers = emptySet<String>()
    private var disabledHandlerInvokers = emptySet<String>()

    fun init(instances: List<Any>) = instances.forEach(::register)

    fun register(instance: Any) {
        instance.javaClass.declaredMethods.forEach {
            registerMethod(it, instance)
        }
    }

    fun unregister(instance: Any) = instance.javaClass.declaredMethods.forEach(::unregisterMethod)

    @Suppress("UNCHECKED_CAST")
    fun <T : SkyHanniEvent> getEventHandler(event: Class<T>): EventHandler<T> = handlers.getOrPut(event) {
        EventHandler(
            event,
            getEventClasses(event).mapNotNull { listeners[it] }.flatMap(EventListeners::getListeners),
        )
    } as EventHandler<T>

    fun isDisabledHandler(handler: String): Boolean = handler in disabledHandlers
    fun isDisabledInvoker(invoker: String): Boolean = invoker in disabledHandlerInvokers

    private fun registerMethod(method: Method, instance: Any) {
        val options = method.getAnnotation(HandleEvent::class.java) ?: return
        registerNoEventType(options, method, instance)
        registerSingleEventType(options, method, instance)
        registerMultipleEventTypes(options, method, instance)
    }

    @JvmStatic
    val eventPrimaryFunctionNames: Map<String, Class<out SkyHanniEvent>> =
        GeneratedEventPrimaryFunctionNames.map

    private fun registerNoEventType(options: HandleEvent, method: Method, instance: Any) {
        if (method.parameterTypes.any()) return
        val eventType = eventPrimaryFunctionNames[method.name] ?: return
        if (!SkyHanniEvent::class.java.isAssignableFrom(eventType)) return
        listeners.getOrPut(eventType) { EventListeners(eventType) }
            .addListener(method, instance, options)
    }

    @Suppress("UNCHECKED_CAST")
    private fun registerSingleEventType(options: HandleEvent, method: Method, instance: Any) {
        val eventType = method.parameterTypes.getOrNull(0) ?: options.eventType.java
        if (!SkyHanniEvent::class.java.isAssignableFrom(eventType)) return
        listeners.getOrPut(eventType as Class<SkyHanniEvent>) { EventListeners(eventType) }
            .addListener(method, instance, options)
    }

    @Suppress("UNCHECKED_CAST")
    private fun registerMultipleEventTypes(options: HandleEvent, method: Method, instance: Any) {
        options.eventTypes.map { it.java }.forEach { eventType ->
            if (!SkyHanniEvent::class.java.isAssignableFrom(eventType)) return
            listeners.getOrPut(eventType as Class<SkyHanniEvent>) { EventListeners(eventType) }
                .addListener(method, instance, options)
        }
    }


    private fun unregisterMethod(method: Method) {
        if (method.parameterCount != 1) return
        method.getAnnotation(HandleEvent::class.java) ?: return
        val event = method.parameterTypes[0]
        if (!SkyHanniEvent::class.java.isAssignableFrom(event)) return
        unregisterHandler(event)
        listeners.values.forEach { it.removeListener(method) }
    }

    private fun unregisterHandler(clazz: Class<*>) {
        this.handlers.removeIfKey { it.isAssignableFrom(clazz) }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<DisabledEventsJson>("DisabledEvents")
        disabledHandlers = data.disabledHandlers
        disabledHandlerInvokers = data.disabledInvokers
    }

    val seconds = setOf(10, 60, 60 * 5)

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        val list = handlers.values.toMutableList()

        for (second in seconds) {
            if (event.repeatSeconds(second)) {

                for (handler in list) {
                    val log = handler.invokeLog
                    val current = log.invokeCount

                    val storage = log.overTimeLog[second]
                    if (storage == null) {
                        log.overTimeLog[second] = EventInvokeData(current, 0)
                    } else {
                        storage.diff = current - storage.oldValue
                        storage.oldValue = current
                    }
                }
            }
        }

    }

    class EventInvokeData(var oldValue: Long, var diff: Long)

    class EventInvokeLog {

        var invokeCount: Long = 0L

        var overTimeLog = mutableMapOf<Int, EventInvokeData>()
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Events")
        event.addIrrelevant {
            add("- <event name> (<total invoke count> invokes per second: <last 10s, 60s, 5m, total>)")
            handlers.values
                .filter { it.invokeLog.invokeCount > 0 }
                .sortedWith(compareBy({ -it.invokeLog.invokeCount }, { it.name }))
                .forEach {
                    val log = it.invokeLog

                    add(
                        buildString {
                            append("- ${it.name} ")
                            append(log.invokeCount.addSeparators())

                            for (second in seconds) {
                                val totalDiff = log.overTimeLog[second]?.diff ?: 0
                                val perSecond = totalDiff / second
                                append(" ")
                                append("${perSecond.addSeparators()}/s")
                            }

                            append(" ")
                            append("${(log.invokeCount / (ClientEvents.totalTicks / 20)).addSeparators()}/s")

                        },
                    )
                }
        }
    }

    /**
     * Returns a list of all super classes and the class itself up to [SkyHanniEvent].
     */
    private fun getEventClasses(clazz: Class<*>): List<Class<*>> {
        val classes = mutableListOf<Class<*>>()
        classes.add(clazz)

        var current = clazz
        @Suppress("LoopWithTooManyJumpStatements")
        while (current.superclass != null) {
            val superClass = current.superclass
            if (superClass == SkyHanniEvent::class.java) break
            if (superClass == GenericSkyHanniEvent::class.java) break
            if (superClass == RenderingSkyHanniEvent::class.java) break
            if (superClass == CancellableSkyHanniEvent::class.java) break
            classes.add(superClass)
            current = superClass
        }
        return classes
    }
}
