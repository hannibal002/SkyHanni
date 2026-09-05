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
import kotlin.coroutines.Continuation
import kotlin.reflect.jvm.kotlinFunction

@SkyHanniModule
object SkyHanniEvents {
    private val listeners: MutableMap<Class<out AbstractSkyHanniEvent>, EventListeners> = mutableMapOf()
    private val handlers: MutableMap<Class<out SkyHanniEvent>, EventHandler<out SkyHanniEvent>> = mutableMapOf()
    private val asyncHandlers: MutableMap<Class<out AsyncSkyHanniEvent>, AsyncEventHandler<out AsyncSkyHanniEvent>> =
        mutableMapOf()
    private var disabledHandlers = emptySet<String>()
    private var disabledHandlerInvokers = emptySet<String>()

    fun init(instances: List<Any>) = instances.forEach(::register)

    fun register(instance: Any) = register(instance, validateModule = true)

    /**
     * Registers an ad-hoc listener object that is deliberately not a `@SkyHanniModule`, which only unit tests
     * exercising dispatch behaviour are allowed to do.
     */
    internal fun registerForTest(instance: Any) = register(instance, validateModule = false)

    private fun register(instance: Any, validateModule: Boolean) {
        instance.javaClass.declaredMethods.forEach {
            registerMethod(it, instance, validateModule)
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

    // The map key and EventHandler constructor preserve T, but the heterogeneous cache erases it.
    @Suppress("UNCHECKED_CAST")
    fun <T : AsyncSkyHanniEvent> getAsyncEventHandler(event: Class<T>): AsyncEventHandler<T> =
        asyncHandlers.getOrPut(event) {
            AsyncEventHandler(
                event,
                getEventClasses(event).mapNotNull { listeners[it] }.flatMap(EventListeners::getListeners),
            )
        } as AsyncEventHandler<T>

    fun isDisabledHandler(handler: String): Boolean = handler in disabledHandlers
    fun isDisabledInvoker(invoker: String): Boolean = invoker in disabledHandlerInvokers

    private fun registerMethod(method: Method, instance: Any, validateModule: Boolean) {
        val (options, eventTypes, isSuspend) = getEventData(method, validateModule) ?: return
        eventTypes.forEach { eventType ->
            listeners.getOrPut(eventType) { EventListeners(eventType) }
                .addListener(method, instance, options, isSuspend)
            unregisterHandler(eventType)
        }
    }

    @JvmStatic
    val eventPrimaryFunctionNames: Map<String, Class<out AbstractSkyHanniEvent>> =
        GeneratedEventPrimaryFunctionNames.map

    private val Method.fullyQualifiedName: String get() = "${declaringClass.name}.$name"

    private fun resolveZeroParameterEventTypes(
        method: Method,
        options: HandleEvent,
    ): List<Class<out AbstractSkyHanniEvent>> {
        eventPrimaryFunctionNames[method.name]?.let { return listOf(it) }

        if (options.eventType != AbstractSkyHanniEvent::class) return listOf(options.eventType.java)

        require(options.eventTypes.isNotEmpty()) {
            "Function ${method.fullyQualifiedName} must have an event parameter, a primary " +
                "function name, or an explicit event specification because it is annotated " +
                "with @HandleEvent"
        }
        return options.eventTypes.map { it.java }
    }

    private fun resolveSingleParameterEventTypes(method: Method): List<Class<out AbstractSkyHanniEvent>> {
        val eventType = method.parameterTypes.first()

        require(AbstractSkyHanniEvent::class.java.isAssignableFrom(eventType)) {
            "Function ${method.fullyQualifiedName} must have an event assignable from " +
                "AbstractSkyHanniEvent because it is annotated with @HandleEvent"
        }

        @Suppress("UNCHECKED_CAST")
        return listOf(eventType as Class<out AbstractSkyHanniEvent>)
    }

    /**
     * @param validateModule Whether to reject handlers declared outside a `@SkyHanniModule` class. Only meaningful
     *                       while registering, as module membership does not affect how a listener is removed.
     */
    private fun getEventData(method: Method, validateModule: Boolean): EventData? {
        val options = method.getAnnotation(HandleEvent::class.java) ?: return null
        if (validateModule && !method.declaringClass.isAnnotationPresent(SkyHanniModule::class.java)) {
            ErrorManager.crashInDevEnv(
                "Function ${method.fullyQualifiedName} must be declared directly inside a class " +
                    "annotated with @SkyHanniModule because it is annotated with @HandleEvent",
            )
            return null
        }
        val kotlinFunction = method.kotlinFunction
        val isSuspend = kotlinFunction?.isSuspend == true
        val hasContinuation = method.parameterTypes.lastOrNull() == Continuation::class.java
        require(!hasContinuation || isSuspend) {
            "Function ${method.fullyQualifiedName} imitates a Kotlin suspend signature but is " +
                "not a Kotlin suspend function"
        }
        if (isSuspend) {
            require(kotlinFunction.returnType.classifier == Unit::class) {
                "Suspend event handler ${method.fullyQualifiedName} must return Unit"
            }
        }

        val logicalParameterCount = method.parameterCount - if (isSuspend) 1 else 0
        val eventTypes = when (logicalParameterCount) {
            0 -> resolveZeroParameterEventTypes(method, options)
            1 -> resolveSingleParameterEventTypes(method)
            else -> throw IllegalArgumentException(
                "Function ${method.fullyQualifiedName} has too many parameters. It must have " +
                    "exactly one event parameter, or be parameterless with a primary " +
                    "function name or an explicit event specification because it is " +
                    "annotated with @HandleEvent",
            )
        }

        val hasSyncEvents = eventTypes.any { SkyHanniEvent::class.java.isAssignableFrom(it) }
        val hasAsyncEvents = eventTypes.any { AsyncSkyHanniEvent::class.java.isAssignableFrom(it) }
        require(
            hasSyncEvents.xor(hasAsyncEvents) && eventTypes.all {
                SkyHanniEvent::class.java.isAssignableFrom(it) || AsyncSkyHanniEvent::class.java.isAssignableFrom(it)
            },
        ) {
            "Function ${method.fullyQualifiedName} must only handle events from one synchronous " +
                "or asynchronous family"
        }
        require(isSuspend == hasAsyncEvents) {
            val requiredKind = if (hasAsyncEvents) "a suspend" else "an ordinary"
            "Function ${method.fullyQualifiedName} must be $requiredKind function for its event family"
        }
        return EventData(options, eventTypes, isSuspend)
    }

    private data class EventData(
        val options: HandleEvent,
        val eventTypes: List<Class<out AbstractSkyHanniEvent>>,
        val isSuspend: Boolean,
    )

    private fun unregisterMethod(method: Method) {
        val (_, eventTypes) = getEventData(method, validateModule = false) ?: return
        eventTypes.forEach { event ->
            unregisterHandler(event)
            listeners.values.forEach { it.removeListener(method) }
        }
    }

    private fun unregisterHandler(clazz: Class<out AbstractSkyHanniEvent>) {
        handlers.removeIfKey { clazz.isAssignableFrom(it) }
        asyncHandlers.removeIfKey { clazz.isAssignableFrom(it) }
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
    @HandleEvent(priorityLevel = HIGHEST)
    private suspend fun onRepoReload(event: RepositoryReloadEvent) {
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
            val list = allHandlerLogs()

            for (second in seconds) {
                if (event.repeatSeconds(second)) {
                    for ((_, log) in list) {
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

    private fun allHandlerLogs(): List<Pair<String, EventInvokeLog>> =
        handlers.values.map { it.name to it.invokeLog } + asyncHandlers.values.map { it.name to it.invokeLog }

    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Events")
        event.addIrrelevant {
            add("- <event name> (<total invoke count> invokes per second: <last 10s, 60s, 5m, total>)")
            allHandlerLogs()
                .filter { (_, log) -> log.invokeCount > 0 }
                .sortedWith(compareBy({ (_, log) -> -log.invokeCount }, { (name, _) -> name }))
                .forEach { (name, log) ->

                    add(
                        buildString {
                            append("- $name ")
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
     * Returns a list of all event super classes and the class itself up to [AbstractSkyHanniEvent].
     */
    private fun getEventClasses(clazz: Class<*>): List<Class<*>> {
        val classes = mutableListOf<Class<*>>()
        classes.add(clazz)

        var current = clazz
        @Suppress("LoopWithTooManyJumpStatements")
        while (current.superclass != null) {
            val superClass = current.superclass
            if (superClass == AbstractSkyHanniEvent::class.java) break
            if (superClass == SkyHanniEvent::class.java) break
            if (superClass == AsyncSkyHanniEvent::class.java) break
            if (superClass == GenericSkyHanniEvent::class.java) break
            if (superClass == RenderingSkyHanniEvent::class.java) break
            if (superClass == CancellableSkyHanniEvent::class.java) break
            classes.add(superClass)
            current = superClass
        }
        return classes
    }
}
