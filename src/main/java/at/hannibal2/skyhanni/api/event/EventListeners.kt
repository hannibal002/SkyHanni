package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.api.event.HandleEvent.Companion.effectivePriority
import at.hannibal2.skyhanni.api.event.HandleEvent.Priority
import at.hannibal2.skyhanni.utils.ReflectionUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import java.lang.reflect.Method
import java.util.function.Consumer

typealias EventPredicate = (event: AbstractSkyHanniEvent) -> Boolean

class EventListeners private constructor(val name: String, private val isGeneric: Boolean) {
    private val listeners: MutableList<Listener> = mutableListOf()

    constructor(event: Class<*>) : this(
        (event.name.split(".").lastOrNull() ?: event.name).replace("$", "."),
        GenericSkyHanniEvent::class.java.isAssignableFrom(event),
    )

    fun removeListener(listener: Any) {
        listeners.removeIf { it.method == listener }
    }

    fun addListener(method: Method, instance: Any, options: HandleEvent, isSuspend: Boolean) {
        val name = buildListenerName(method)
        val logicalParameterCount = method.parameterCount - if (isSuspend) 1 else 0
        val eventConsumer = if (isSuspend) null else when (logicalParameterCount) {
            0 -> createZeroParameterConsumer(method, instance)
            1 -> createSingleParameterConsumer(method, instance)
            else -> error("Validated listener has an invalid parameter count: $name")
        }
        val suspendEventConsumer = if (!isSuspend) null else when (logicalParameterCount) {
            0 -> createZeroParameterSuspendConsumer(method, instance)
            1 -> createSingleParameterSuspendConsumer(method, instance)
            else -> error("Validated listener has an invalid parameter count: $name")
        }
        val generic = if (isGeneric) resolveGenericType(method) else null

        listeners.add(Listener(method, name, eventConsumer, suspendEventConsumer, options, generic))
    }

    private fun buildListenerName(method: Method): String {
        val paramTypesString = method.parameterTypes.joinTo(
            StringBuilder(),
            prefix = "(",
            postfix = ")",
            separator = ", ",
            transform = Class<*>::getTypeName,
        ).toString()

        return "${method.declaringClass.name}.${method.name}$paramTypesString"
    }

    private fun createZeroParameterConsumer(method: Method, instance: Any): (Any) -> Unit {
        val runnable = ReflectionUtils.createRunnableFromMethod(instance, method)
        return { _: Any -> runnable.run() }
    }

    private fun createSingleParameterConsumer(method: Method, instance: Any): (Any) -> Unit {
        val consumer = ReflectionUtils.createConsumerFromMethod(instance, method)
        return { event -> consumer.accept(event) }
    }

    private fun createZeroParameterSuspendConsumer(method: Method, instance: Any): suspend (Any) -> Unit {
        val runnable = SuspendEventInvokerFactory.createRunnable(instance, method)
        return { _: Any -> runnable.run() }
    }

    private fun createSingleParameterSuspendConsumer(method: Method, instance: Any): suspend (Any) -> Unit {
        val consumer = SuspendEventInvokerFactory.createConsumer(instance, method)
        return { event -> consumer.accept(event) }
    }

    private fun resolveGenericType(method: Method): Class<*> =
        method.genericParameterTypes.getOrNull(0)?.let { genericType ->
            ReflectionUtils.resolveUpperBoundSuperClassGenericParameter(
                genericType,
                GenericSkyHanniEvent::class.java.typeParameters[0],
            ) ?: error(
                "Generic event handler type parameter is not present in " +
                    "event class hierarchy for type $genericType",
            )
        } ?: error("Method ${method.name} does not have a generic parameter type.")

    fun getListeners(): List<Listener> = listeners

    class Listener(
        val method: Method,
        val name: String,
        val invoker: Consumer<Any>?,
        val suspendInvoker: (suspend (Any) -> Unit)?,
        options: HandleEvent,
        private val generic: Class<*>?,
        extraPredicates: List<EventPredicate> = listOf(),
    ) {
        val priority: Priority = options.effectivePriority
        val receiveCancelled: Boolean = options.receiveCancelled
        val indices: List<Int> = ListenerCollection.createListenerIndices(options)

        @Suppress("JoinDeclarationAndAssignment")
        private val cachedPredicates: List<EventPredicate>
        private var lastCacheGeneration = -1
        private var cachedPredicateValue = false

        private val predicates: List<EventPredicate>

        fun shouldInvoke(event: AbstractSkyHanniEvent): Boolean {
            val generation = SkyHanniEvents.getListenerCacheGeneration()
            if (generation != lastCacheGeneration) {
                cachedPredicateValue = cachedPredicates.all { it(event) }
                lastCacheGeneration = generation
            }
            return cachedPredicateValue && predicates.all { it(event) }
        }

        init {
            cachedPredicates = buildList {
                options.onlyOnSkyblockOrFeatures.takeIfNotEmpty()?.let { features ->
                    @Suppress("DEPRECATION")
                    add { _ -> SkyBlockUtils.inSkyBlock || features.any { it.isSelected() } }
                }
                add { _ -> !SkyHanniEvents.isDisabledInvoker(name) }
            }
            // These predicates can't be cached since they depend on info about the actual event
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
