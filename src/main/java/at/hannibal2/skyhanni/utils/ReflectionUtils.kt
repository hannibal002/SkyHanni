package at.hannibal2.skyhanni.utils

import java.lang.reflect.*

val <T> Class<T>.allFields: List<Field>
    get() = this.declaredFields.toList() + (this.superclass?.allFields ?: listOf())
val <T> Class<T>.allAccessibleFields: List<Field>
    get() = allFields.also { it.forEach { it.isAccessible = true } }

val Type.nonGeneric: Class<*>?
    get() = when (this) {
        is ParameterizedType -> this.rawType.nonGeneric
        is Class<*> -> this
        is WildcardType -> this.upperBounds[0].nonGeneric
        is TypeVariable<*> -> this.bounds[0].nonGeneric
        else -> null
    }