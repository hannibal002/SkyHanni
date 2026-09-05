package at.hannibal2.skyhanni.api.event

import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import kotlin.coroutines.Continuation

fun interface SuspendEventConsumer {
    suspend fun accept(event: Any)
}

fun interface SuspendEventRunnable {
    suspend fun run()
}

object SuspendEventInvokerFactory {
    fun createConsumer(instance: Any, method: Method): SuspendEventConsumer {
        require(method.parameterTypes.size == 2 && method.parameterTypes.last() == Continuation::class.java) {
            "Method ${instance.javaClass.name}#${method.name} is not a suspend function with one parameter"
        }
        return createFunctionalInterface(
            instance,
            method,
            SuspendEventConsumer::class.java,
            "accept",
            MethodType.methodType(Any::class.java, Any::class.java, Continuation::class.java),
            MethodType.methodType(Any::class.java, method.parameterTypes[0], Continuation::class.java),
        )
    }

    fun createRunnable(instance: Any, method: Method): SuspendEventRunnable {
        require(method.parameterTypes.contentEquals(arrayOf(Continuation::class.java))) {
            "Method ${instance.javaClass.name}#${method.name} is not a suspend function without parameters"
        }
        return createFunctionalInterface(
            instance,
            method,
            SuspendEventRunnable::class.java,
            "run",
            MethodType.methodType(Any::class.java, Continuation::class.java),
            MethodType.methodType(Any::class.java, Continuation::class.java),
        )
    }

    // LambdaMetafactory returns an erased SAM instance whose concrete type is guaranteed by functionalClass.
    @Suppress("UNCHECKED_CAST")
    private fun <T> createFunctionalInterface(
        instance: Any,
        method: Method,
        functionalClass: Class<out T>,
        samName: String,
        samMethodType: MethodType,
        instantiatedMethodType: MethodType,
    ): T = runCatching {
        val lookup = MethodHandles.privateLookupIn(method.declaringClass, MethodHandles.lookup())
        val handle = lookup.unreflect(method)
        LambdaMetafactory.metafactory(
            lookup,
            samName,
            MethodType.methodType(functionalClass, instance::class.java),
            samMethodType,
            handle,
            instantiatedMethodType,
        ).target.bindTo(instance).invoke() as T
    }.getOrElse { error ->
        val message = "Method ${instance.javaClass.name}#${method.name} is not a valid ${functionalClass.simpleName}"
        throw IllegalArgumentException(message, error)
    }
}
