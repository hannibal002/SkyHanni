package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.api.minecraftevents.ClientEvents
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.ReflectionUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.function.Consumer

typealias EventPredicate = (event: SkyHanniEvent) -> Boolean

class EventListeners private constructor(val name: String, private val isGeneric: Boolean) {

    private val listeners: MutableList<Listener> = mutableListOf()

    constructor(event: Class<*>) : this(
        (event.name.split(".").lastOrNull() ?: event.name).replace("$", "."),
        GenericSkyHanniEvent::class.java.isAssignableFrom(event),
    )

    fun removeListener(listener: Any) {
        listeners.removeIf { it.invoker == listener }
    }

    fun addListener(method: Method, instance: Any, options: HandleEvent) {
        require(Modifier.isPublic(method.modifiers)) {
            "Method ${method.name}() in ${instance.javaClass.name} is not public. Make sure to set it to public."
        }
        val name = buildListenerName(method)
        val eventConsumer = when (method.parameterCount) {
            0 -> createZeroParameterConsumer(method, instance, options)
            1 -> createSingleParameterConsumer(method, instance)
            else -> throw IllegalArgumentException(
                "Method ${method.name} must have either 0 or 1 parameters."
            )
        }
        val generic = if (isGeneric) resolveGenericType(method) else null

        listeners.add(Listener(name, eventConsumer, options, generic))
    }

    private fun buildListenerName(method: Method): String {
        val paramTypesString = method.parameterTypes.joinTo(
            StringBuilder(),
            prefix = "(",
            postfix = ")",
            separator = ", ",
            transform = Class<*>::getTypeName
        ).toString()

        return "${method.declaringClass.name}.${method.name}$paramTypesString"
    }

    private fun createZeroParameterConsumer(method: Method, instance: Any, options: HandleEvent): (Any) -> Unit {
        if (options.eventTypes.isNotEmpty()) {
            options.eventTypes.onEach { kClass ->
                require(SkyHanniEvent::class.java.isAssignableFrom(kClass.java)) {
                    "Each event in eventTypes in @HandleEvent must extend SkyHanniEvent. Provided: $kClass"
                }
            }
        } else if (options.eventType == SkyHanniEvent::class) {
            require(SkyHanniEvents.eventPrimaryFunctionNames.containsKey(method.name)) {
                "Method ${method.name} has no parameters and no eventType was provided, " +
                    "and no matching primary function name was found in eventPrimaryFunctionNames.\n" +
                    "eventPrimaryFunctionNames: ${SkyHanniEvents.eventPrimaryFunctionNames}" +
                    "\nMethod: ${method.name} in ${method.declaringClass.name}"
            }
        } else {
            require(SkyHanniEvent::class.java.isAssignableFrom(options.eventType.java)) {
                "eventType in @HandleEvent must extend SkyHanniEvent. Provided: ${options.eventType.java}"
            }
        }

        val runnable = ReflectionUtils.createRunnableFromMethod(instance, method)
        return { _: Any -> runnable.run() }
    }

    private fun createSingleParameterConsumer(method: Method, instance: Any): (Any) -> Unit {
        require(SkyHanniEvent::class.java.isAssignableFrom(method.parameterTypes[0])) {
            "Method ${method.name} parameter must be a subclass of SkyHanniEvent."
        }

        val consumer = ReflectionUtils.createConsumerFromMethod(instance, method)
        return { event -> consumer.accept(event) }
    }

    private fun resolveGenericType(method: Method): Class<*> =
        method.genericParameterTypes.getOrNull(0)?.let { genericType ->
            ReflectionUtils.resolveUpperBoundSuperClassGenericParameter(
                genericType,
                GenericSkyHanniEvent::class.java.typeParameters[0]
            ) ?: error(
                "Generic event handler type parameter is not present in " +
                    "event class hierarchy for type $genericType"
            )
        } ?: error("Method ${method.name} does not have a generic parameter type.")

    fun getListeners(): List<Listener> = listeners

    class Listener(
        val name: String,
        val invoker: Consumer<Any>,
        options: HandleEvent,
        private val generic: Class<*>?,
        extraPredicates: List<EventPredicate> = listOf(),
    ) {
        val priority: Int = options.priority
        val receiveCancelled: Boolean = options.receiveCancelled

        @Suppress("JoinDeclarationAndAssignment")
        private val cachedPredicates: List<EventPredicate>
        private var lastTick = -1
        private var cachedPredicateValue = false

        private val predicates: List<EventPredicate>

        fun shouldInvoke(event: SkyHanniEvent): Boolean {
            if (SkyHanniEvents.isDisabledInvoker(name)) return false
            if (lastTick != ClientEvents.totalTicks) {
                cachedPredicateValue = cachedPredicates.all { it(event) }
                lastTick = ClientEvents.totalTicks
            }
            return cachedPredicateValue && predicates.all { it(event) }
        }

        init {
            cachedPredicates = buildList {
                if (options.onlyOnSkyblock) add { _ -> SkyBlockUtils.inSkyBlock }

                if (options.onlyOnIsland != IslandType.ANY) {
                    val island = options.onlyOnIsland
                    add { _ -> island.isCurrent() }
                }

                if (options.onlyOnIslands.isNotEmpty()) {
                    val set = options.onlyOnIslands.toSet()
                    add { _ -> SkyBlockUtils.inAnyIsland(set) }
                }
            }
            // These predicates cant be cached since they depend on info about the actual event
            predicates = buildList {
                if (!receiveCancelled) add { event -> !event.isCancelled }

                if (generic != null) {
                    add { event ->
                        event is GenericSkyHanniEvent<*> && generic.isAssignableFrom(event.type)
                    }
                }
                // Makes it possible to be able to add more predicates from other sources, such as other annotations
                addAll(extraPredicates)
            }
        }
    }

}
