package at.hannibal2.skyhanni.utils

import io.github.moulberry.moulconfig.observer.Observer
import io.github.moulberry.moulconfig.observer.Property
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.isAccessible

object CollectionUtils {

    fun <E> ConcurrentLinkedQueue<E>.drainTo(list: MutableCollection<E>) {
        while (true)
            list.add(this.poll() ?: break)
    }

    // Let garbage collector handle the removal of entries in this list
    fun <T> weakReferenceList(): MutableSet<T> = Collections.newSetFromMap(WeakHashMap<T, Boolean>())

    fun <T> MutableCollection<T>.filterToMutable(predicate: (T) -> Boolean) = filterTo(mutableListOf(), predicate)

    fun <T> T.transformIf(condition: T.() -> Boolean, transofmration: T.() -> T) =
        if (condition()) transofmration(this) else this

    fun <T> T.conditionalTransform(condition: Boolean, ifTrue: T.() -> Any, ifFalse: T.() -> Any) =
        if (condition) ifTrue(this) else ifFalse(this)

    fun <T> List<T>.indexOfFirst(vararg args: T) = args.map { indexOf(it) }.firstOrNull { it != -1 }

    // TODO nea?
//    fun <T> dynamic(block: () -> KMutableProperty0<T>?): ReadWriteProperty<Any?, T?> {
//        return object : ReadWriteProperty<Any?, T?> {
//            override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
//                return block()?.get()
//            }
//
//            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
//                if (value != null)
//                    block()?.set(value)
//            }
//        }
//    }

    fun <T, R> dynamic(root: KProperty0<R?>, child: KMutableProperty1<R, T>) =
        object : ReadWriteProperty<Any?, T?> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
                val rootObj = root.get() ?: return null
                return child.get(rootObj)
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
                if (value == null) return
                val rootObj = root.get() ?: return
                child.set(rootObj, value)
            }
        }

    inline fun <reified T : Any> Any.getPropertiesWithType() =
        this::class.memberProperties
            .filter { it.returnType.isSubtypeOf(T::class.starProjectedType) }
            .map {
                it.isAccessible = true
                (it as KProperty1<Any, T>).get(this)
            }

    fun Field.makeAccessible() = also { isAccessible = true }

    fun <T> Constructor<T>.makeAccessible() = also { isAccessible = true }

    fun Field.removeFinal(): Field {
        javaClass.getDeclaredField("modifiers").makeAccessible().set(this, modifiers and (Modifier.FINAL.inv()))
        return this
    }

    infix fun <K, V> MutableMap<K, V>.put(pairs: Pair<K, V>) {
        this[pairs.first] = pairs.second
    }

    // Taken and modified from Skytils
    @JvmStatic
    fun <T> T.equalsOneOf(vararg other: T): Boolean {
        for (obj in other) {
            if (this == obj) return true
        }
        return false
    }

    fun <E> List<E>.getOrNull(index: Int): E? {
        return if (index in indices) {
            get(index)
        } else null
    }

    fun <T : Any> T?.toSingletonListOrEmpty(): List<T> {
        if (this == null) return emptyList()
        return listOf(this)
    }

    fun <K> MutableMap<K, Int>.addOrPut(key: K, number: Int): Int =
        this.merge(key, number, Int::plus)!! // Never returns null since "plus" can't return null

    fun <K> MutableMap<K, Long>.addOrPut(key: K, number: Long): Long =
        this.merge(key, number, Long::plus)!! // Never returns null since "plus" can't return null

    fun <K> MutableMap<K, Double>.addOrPut(key: K, number: Double): Double =
        this.merge(key, number, Double::plus)!! // Never returns null since "plus" can't return null

    fun <K, N : Number> Map<K, N>.sumAllValues(): Double {
        if (values.isEmpty()) return 0.0

        return when (values.first()) {
            is Double -> values.sumOf { it.toDouble() }
            is Float -> values.sumOf { it.toDouble() }
            is Long -> values.sumOf { it.toLong() }.toDouble()
            else -> values.sumOf { it.toInt() }.toDouble()
        }
    }

    fun List<String>.nextAfter(after: String, skip: Int = 1) = nextAfter({ it == after }, skip)

    fun List<String>.nextAfter(after: (String) -> Boolean, skip: Int = 1): String? {
        var missing = -1
        for (line in this) {
            if (after(line)) {
                missing = skip - 1
                continue
            }
            if (missing == 0) {
                return line
            }
            if (missing != -1) {
                missing--
            }
        }
        return null
    }

    // MoulConfig is in Java, I don't want to downgrade this logic
    fun <T> onChange(vararg properties: Property<out T>, observer: Observer<T>) {
        for (property in properties) {
            property.whenChanged { a, b -> observer.observeChange(a, b) }
        }
    }

    fun <T> onToggle(vararg properties: Property<out T>, observer: Runnable) {
        onChange(*properties) { _, _ -> observer.run() }
    }

    fun <T> Property<out T>.onToggle(observer: Runnable) {
        whenChanged { _, _ -> observer.run() }
    }

    fun <T> Property<out T>.afterChange(observer: T.() -> Unit) {
        whenChanged { _, new -> observer(new) }
    }

    fun <K, V> Map<K, V>.editCopy(function: MutableMap<K, V>.() -> Unit) =
        toMutableMap().also { function(it) }.toMap()

    fun <T> List<T>.editCopy(function: MutableList<T>.() -> Unit) =
        toMutableList().also { function(it) }.toList()

    fun <K, V> Map<K, V>.moveEntryToTop(matcher: (Map.Entry<K, V>) -> Boolean): Map<K, V> {
        val entry = entries.find(matcher)
        if (entry != null) {
            val newMap = linkedMapOf(entry.key to entry.value)
            newMap.putAll(this)
            return newMap
        }
        return this
    }

    fun <E> MutableList<List<E>>.addAsSingletonList(text: E) {
        add(Collections.singletonList(text))
    }

    fun <K, V : Comparable<V>> List<Pair<K, V>>.sorted(): List<Pair<K, V>> {
        return sortedBy { (_, value) -> value }
    }

    fun <K, V : Comparable<V>> Map<K, V>.sorted(): Map<K, V> {
        return toList().sorted().toMap()
    }

    fun <K, V : Comparable<V>> Map<K, V>.sortedDesc(): Map<K, V> {
        return toList().sorted().reversed().toMap()
    }
}
