package at.hannibal2.skyhanni.utils

import java.lang.invoke.LambdaMetafactory
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.GenericDeclaration
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.util.function.Consumer

object ReflectionUtils {
    fun Any.getPublicField(fieldName: String): Field = javaClass.getField(fieldName)
    fun Class<*>.getPublicField(fieldName: String): Field = getField(fieldName)
    fun Any.getPublicFieldValue(fieldName: String): Any = requireNotNull(getPublicField(fieldName).get(this)) {
        "Field '$fieldName' on ${javaClass.name} is null"
    }

    fun Class<*>.getPublicFieldValue(
        fieldName: String,
        classInstance: Any?,
    ): Any = requireNotNull(getPublicField(fieldName).get(classInstance)) {
        "Field '$fieldName' on ${this.name} is null"
    }

    /**
     * The 6 functions below for fetching & accessing private fields are fickle. They can, and will, throw:
     * - [NoSuchFieldException] - no field with that name/index exists on the class
     * - [SecurityException] - the security manager has denied reflective access to the field
     * - [NullPointerException] - the field exists but its value is null at the time of access
     * You've been warned. -David
     */
    fun Any.getPrivateField(fieldName: String): Field = javaClass.getDeclaredField(fieldName).makeAccessible()
    fun Any.getPrivateField(fieldIndex: Int): Field = javaClass.declaredFields[fieldIndex].makeAccessible()
    fun Class<*>.getPrivateField(fieldName: String): Field = getDeclaredField(fieldName).makeAccessible()
    fun Any.getPrivateFieldValue(fieldName: String): Any = requireNotNull(getPrivateField(fieldName).get(this)) {
        "Field '$fieldName' on ${javaClass.name} is null"
    }

    fun Any.getPrivateFieldValue(fieldIndex: Int): Any = requireNotNull(getPrivateField(fieldIndex).get(this)) {
        "Field at index $fieldIndex on ${javaClass.name} is null"
    }

    fun Class<*>.getPrivateFieldValue(
        fieldName: String,
        classInstance: Any?,
    ): Any = requireNotNull(getPrivateField(fieldName).get(classInstance)) {
        "Field '$fieldName' on ${this.name} is null"
    }

    fun Field.makeAccessible() = also { isAccessible = true }

    fun <T> Constructor<T>.makeAccessible() = also { isAccessible = true }

    fun StackTraceElement.getClassInstance(): Class<*> {
        return Class.forName(this.className)
    }

    private data class TypeKey(val declaration: GenericDeclaration, val name: String)

    private val TypeVariable<*>.key get() = TypeKey(genericDeclaration, name)

    /**
     * Resolve all super class generic type parameters to their respective bound types in the class inheriting them.
     * Note that this is only done once, so a class declaration like
     * ```kotlin
     * class Parent<ParentT>
     * class Child<OtherT> : Parent<OtherT>
     * class GrandChild : Child<String>
     * ```
     * would result in `mapOf(TypeKey(Child, "OtherT") to String, TypeKey(Parent, "ParentT") to TypeKey(Child, "OtherT"))`.
     * Variables bound to variables need to be manually unraveled.
     * Note also that wild cards like
     * ```kotlin
     * class WildChild : Parent<out String>
     * ```
     * are left untouched: `mapOf(TypeKey(Parent, "ParentT") to WildCardType(arrayOf(String), arrayOf()))`
     */
    private fun findSuperClassTypeParameters(type: Type?, universe: MutableMap<TypeKey, Type>) {
        when (type) {
            is ParameterizedType -> {
                val rawType = type.rawType as? Class<*> ?: return
                rawType.typeParameters.zip(type.actualTypeArguments).associateTo(universe) { (k, v) -> k.key to v }
                findSuperClassTypeParameters(rawType.genericSuperclass, universe)
            }

            is Class<*> -> findSuperClassTypeParameters(type.genericSuperclass, universe)
            is TypeVariable<*> -> findSuperClassTypeParameters(universe[type.key] ?: return, universe)
        }
    }

    /**
     * Resolve the upper bound of a type variable from a child classes type parameters using [findSuperClassTypeParameters].
     *
     * This method performs the mentioned resolving of type parameters and wild card resolutions.
     * Note that the returned class may not actually be allowed by all bounds along the chain, so might be a super class of
     * what you would expect.
     */
    fun resolveUpperBoundSuperClassGenericParameter(type: Type, variable: TypeVariable<*>): Class<*>? {
        val universe = mutableMapOf<TypeKey, Type>()
        findSuperClassTypeParameters(type, universe)
        var p: Type = variable
        while (true) {
            p = when (p) {
                is TypeVariable<*> -> universe[p.key] ?: return null
                is WildcardType -> p.upperBounds[0]
                is Class<*> -> return p
                else -> return null
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> createFunctionalInterface(
        instance: Any,
        method: Method,
        functionalClass: Class<out T>,
        samName: String,
        samMethodType: MethodType,
        instantiatedMethodType: MethodType,
    ): T = runCatching {
        val handle = MethodHandles.lookup().unreflect(method)
        LambdaMetafactory.metafactory(
            MethodHandles.lookup(),
            samName,
            MethodType.methodType(functionalClass, instance::class.java),
            samMethodType,
            handle,
            instantiatedMethodType,
        ).target.bindTo(instance).invoke() as T
    }.getOrElse { e ->
        val illegalMessage = "Method ${instance.javaClass.name}#${method.name} is not a valid ${functionalClass.simpleName}"
        throw IllegalArgumentException(illegalMessage, e)
    }

    fun createConsumerFromMethod(instance: Any, method: Method): Consumer<Any> {
        require(method.parameterTypes.isNotEmpty()) {
            "Method ${instance.javaClass.name}#${method.name} has no parameters, cannot be a Consumer"
        }
        val unScopedConsumer = createFunctionalInterface(
            instance, method,
            Consumer::class.java, "accept",
            MethodType.methodType(Nothing::class.javaPrimitiveType, Any::class.java),
            MethodType.methodType(Nothing::class.javaPrimitiveType, method.parameterTypes[0]),
        )
        @Suppress("UNCHECKED_CAST")
        return unScopedConsumer as Consumer<Any>
    }

    fun createRunnableFromMethod(instance: Any, method: Method): Runnable = createFunctionalInterface(
        instance, method,
        Runnable::class.java, "run",
        MethodType.methodType(Nothing::class.javaPrimitiveType),
        MethodType.methodType(Nothing::class.javaPrimitiveType),
    )

    inline fun <reified Stop : Any, reified T> Any.findGenericSuperclassTypeArgument(index: Int = 0): Class<T> {
        var type = javaClass.genericSuperclass
        while (type !is ParameterizedType || (type.rawType as Class<*>) != Stop::class.java) {
            type = (type as? Class<*>)?.genericSuperclass
                ?: error("Reached top of hierarchy without finding ${Stop::class.simpleName}")
        }
        @Suppress("UNCHECKED_CAST")
        return type.actualTypeArguments[index] as Class<T>
    }
}
