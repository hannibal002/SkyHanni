package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.minecraftevents.ClientEvents
import at.hannibal2.skyhanni.data.jsonobjects.repo.DisabledEventVersionedJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.DisabledEventsJson
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIfKey
import at.hannibal2.skyhanni.utils.system.ModVersion
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicInteger

@SkyHanniModule
object SkyHanniEvents {
    private val listeners: MutableMap<Class<out SkyHanniEvent>, EventListeners> = mutableMapOf()
    private val handlers: MutableMap<Class<out SkyHanniEvent>, EventHandler<out SkyHanniEvent>> = mutableMapOf()
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
        val (options, eventType) = getEventData(method) ?: return
        listeners.getOrPut(eventType) { EventListeners(eventType) }
            .addListener(method, instance, options)
    }

    @JvmStatic
    val eventPrimaryFunctionNames: Map<String, Class<out SkyHanniEvent>> =
        GeneratedEventPrimaryFunctionNames.map

    private val Method.fullyQualifiedName: String get() = "${declaringClass.name}.$name"

    private fun handleZeroParameterMethod(
        method: Method,
        options: HandleEvent,
    ): Pair<HandleEvent, Class<out SkyHanniEvent>>? {
        val primaryFunctionEventType = eventPrimaryFunctionNames[method.name]
        if (primaryFunctionEventType != null) return options to primaryFunctionEventType

        if (options.eventType != SkyHanniEvent::class) return options to options.eventType.java

        ErrorManager.crashInDevEnv(
            "Function ${method.fullyQualifiedName} must have an event parameter, a primary " +
                "function name, or an explicit event specification because it is annotated " +
                "with @HandleEvent",
        )
        return null
    }

    private fun handleSingleParameterMethod(
        method: Method,
        options: HandleEvent,
    ): Pair<HandleEvent, Class<out SkyHanniEvent>>? {
        val eventType = method.parameterTypes.first()

        if (!SkyHanniEvent::class.java.isAssignableFrom(eventType)) {
            ErrorManager.crashInDevEnv(
                "Function ${method.fullyQualifiedName} must have an event assignable from " +
                    "SkyHanniEvent because it is annotated with @HandleEvent",
            )
            return null
        }

        @Suppress("UNCHECKED_CAST")
        return options to (eventType as Class<out SkyHanniEvent>)
    }

    private fun getEventData(method: Method): Pair<HandleEvent, Class<out SkyHanniEvent>>? {
        val options = method.getAnnotation(HandleEvent::class.java) ?: return null
        if (!method.declaringClass.isAnnotationPresent(SkyHanniModule::class.java)) {
            ErrorManager.crashInDevEnv(
                "Function ${method.fullyQualifiedName} must be declared directly inside a class " +
                    "annotated with @SkyHanniModule because it is annotated with @HandleEvent",
            )
            return null
        }
        return when (method.parameterCount) {
            0 -> handleZeroParameterMethod(method, options)
            1 -> handleSingleParameterMethod(method, options)
            else -> {
                ErrorManager.crashInDevEnv(
                    "Function ${method.fullyQualifiedName} has too many parameters. It must have " +
                        "exactly one event parameter, or be parameterless with a primary " +
                        "function name or an explicit event specification because it is " +
                        "annotated with @HandleEvent",
                )
                null
            }
        }
    }

    private fun unregisterMethod(method: Method) {
        val (_, eventType) = getEventData(method) ?: return
        unregisterHandler(eventType)
        listeners.values.forEach { it.removeListener(method) }
    }

    private fun unregisterHandler(clazz: Class<out SkyHanniEvent>) {
        handlers.removeIfKey { it.isAssignableFrom(clazz) }
    }

    private val listenerCacheGeneration = AtomicInteger(0)
    private val currentStateIndex = AtomicInteger(ListenerCollection.OUTSIDE)

    fun markEventCacheDirty(type: DirtyReason) {
        when (type) {
            DirtyReason.REPO_RELOAD,
            DirtyReason.OUTSIDE_SB_FEATURE_CHANGED,
            -> listenerCacheGeneration.incrementAndGet()

            DirtyReason.LOCATION_CHANGED -> {
                listenerCacheGeneration.incrementAndGet()
                currentStateIndex.set(ListenerCollection.getCurrentStateIndex())
            }

            DirtyReason.SERVER_DISCONNECTED -> {
                listenerCacheGeneration.incrementAndGet()
                currentStateIndex.set(ListenerCollection.OUTSIDE)
            }
        }
    }

    fun getListenerCacheGeneration(): Int = listenerCacheGeneration.get()
    fun getCurrentStateIndex(): Int = currentStateIndex.get()

    enum class DirtyReason {
        LOCATION_CHANGED,
        OUTSIDE_SB_FEATURE_CHANGED,
        SERVER_DISCONNECTED,
        REPO_RELOAD,
    }

    // This is marked highest priority to let it
    // disable other RepositoryReloadEvent listeners before they happen
    @HandleEvent(priority = HandleEvent.HIGHEST)
    private fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<DisabledEventsJson>("DisabledEvents")
        val version = SkyHanniMod.modVersion

        val mcVersion = PlatformUtils.MC_VERSION
        disabledHandlers = data.disabledHandlers + data.disabledHandlersVersioned.activeNames(version, mcVersion)
        disabledHandlerInvokers = data.disabledInvokers + data.disabledInvokersVersioned.activeNames(version, mcVersion)
        markEventCacheDirty(DirtyReason.REPO_RELOAD)
    }

    private fun Set<DisabledEventVersionedJson>.activeNames(
        version: ModVersion,
        mcVersion: String,
    ): Set<String> =
        filter {
            (it.minVersion == null || version >= it.minVersion) &&
                (it.maxVersion == null || version <= it.maxVersion) &&
                (it.mcVersions == null || mcVersion in it.mcVersions)
        }.map { it.name }.toSet()

    val seconds = listOf(10, 60, 60 * 5)

    @HandleEvent
    private fun onSecondPassed(event: SecondPassedEvent) {
        try {
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
        } catch (_: Exception) {
            // ignore this error on 1.21 for now
        }
    }

    class EventInvokeData(var oldValue: Long, var diff: Long)

    class EventInvokeLog {
        var invokeCount: Long = 0L

        var overTimeLog = mutableMapOf<Int, EventInvokeData>()
    }

    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
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
