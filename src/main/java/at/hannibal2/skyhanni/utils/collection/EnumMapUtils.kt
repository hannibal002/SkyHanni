@file:Suppress("unused")

package at.hannibal2.skyhanni.utils.collection

import java.util.EnumMap

inline fun <reified E : Enum<E>, V> emptyEnumMap(): EnumMap<E, V> = EnumMap<E, V>(E::class.java)

inline fun <reified E : Enum<E>, V> enumMapOf(): EnumMap<E, V> = emptyEnumMap<E, V>()

inline fun <reified E : Enum<E>, V> enumMapOf(pair: Pair<E, V>): EnumMap<E, V> {
    return enumMapOf<E, V>().apply { put(pair.first, pair.second) }
}

inline fun <reified E : Enum<E>, V> enumMapOf(vararg pairs: Pair<E, V>): EnumMap<E, V> {
    return pairs.toMap(enumMapOf<E, V>())
}

inline fun <reified E : Enum<E>, V> fullEnumMapOf(defaultValue: V): EnumMap<E, V> {
    return enumMapOf<E, V>().apply {
        for (enum in enumValues<E>()) put(enum, defaultValue)
    }
}

inline fun <reified E : Enum<E>, V> fullEnumMapOf(defaultValue: (E) -> V): EnumMap<E, V> {
    return enumMapOf<E, V>().apply {
        for (enum in enumValues<E>()) put(enum, defaultValue(enum))
    }
}

inline fun <reified E : Enum<E>, V> Map<E, V>.toEnumMap(): EnumMap<E, V> {
    return if (isEmpty()) emptyEnumMap<E, V>() else EnumMap<E, V>(this)
}
