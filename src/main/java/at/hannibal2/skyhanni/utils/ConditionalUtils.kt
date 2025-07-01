package at.hannibal2.skyhanni.utils

import io.github.notenoughupdates.moulconfig.observer.Observer
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

object ConditionalUtils {

    fun <T> T.transformIf(condition: T.() -> Boolean, transformation: T.() -> T) =
        if (condition()) transformation(this) else this

    fun <T> T.conditionalTransform(condition: Boolean, ifTrue: T.() -> Any, ifFalse: T.() -> Any) =
        if (condition) ifTrue(this) else ifFalse(this)

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

    fun Property<out Boolean>.onEnable(observer: Runnable) {
        whenChanged { _, _ ->
            if (this.get()) {
                observer.run()
            }
        }
    }

    fun Property<out Boolean>.onDisable(observer: Runnable) {
        whenChanged { _, _ ->
            if (!this.get()) {
                observer.run()
            }
        }
    }

    fun <T : Comparable<T>, K> comparatorFirst(pair1: Pair<T?, K>, pair2: Pair<T?, K>): Int {
        val first1 = pair1.first
        val first2 = pair2.first

        // Handle null cases
        if (first1 == null && first2 == null) return 0
        if (first1 == null) return -1
        if (first2 == null) return 1

        // Compare the non-null first values
        return first1.compareTo(first2)
    }

    fun <T, K : Comparable<K>> comparatorSecond(pair1: Pair<T, K?>, pair2: Pair<T, K?>): Int {
        val second1 = pair1.second
        val second2 = pair2.second

        // Handle null cases
        if (second1 == null && second2 == null) return 0
        if (second1 == null) return 1
        if (second2 == null) return -1

        // Compare the non-null second values
        return second1.compareTo(second2)
    }

    /**
     * Recursively scans each given 'root' for any fields of type Property<*>,
     * then calls the existing onToggle(...) overload with all discovered properties.
     */
    fun onAnyToggled(vararg roots: Any, observer: Runnable) = roots.forEach { root ->
        collectProperties(root, mutableSetOf()).forEach {
            onToggle(it, observer = observer)
        }
    }

    private fun collectProperties(
        current: Any,
        visited: MutableSet<Any>
    ): List<Property<*>> = buildList {
        if (!visited.add(current)) return@buildList

        for (prop in current::class.memberProperties) {
            if (prop.javaField == null && prop.javaGetter == null || prop.hasAnnotation<Transient>()) continue
            if (runCatching { prop.isAccessible = true }.isFailure) continue
            val value = runCatching { prop.getter.call(current) }.getOrNull() ?: continue
            when (value) {
                is Property<*> -> add(value)
                else -> addAll(
                    collectProperties(value, visited)
                )
            }
        }
    }

}
