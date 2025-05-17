@file:Suppress("unused")

package at.hannibal2.skyhanni.utils.collection

import java.util.EnumSet

inline fun <reified E : Enum<E>> emptyEnumSet(): EnumSet<E> = EnumSet.noneOf(E::class.java)

inline fun <reified E : Enum<E>> enumSetOf(): EnumSet<E> = emptyEnumSet<E>()

inline fun <reified E : Enum<E>> enumSetOf(element: E) = enumSetOf<E>().apply { add(element) }

inline fun <reified E : Enum<E>> enumSetOf(vararg elements: E): EnumSet<E> = elements.toCollection(enumSetOf<E>())

inline fun <reified E : Enum<E>> fullEnumSetOf(): EnumSet<E> = EnumSet.allOf(E::class.java)

inline fun <reified E : Enum<E>> Collection<E>.toEnumSet(): EnumSet<E> {
    return if (isEmpty()) emptyEnumSet<E>() else EnumSet.copyOf(this)
}
