package at.hannibal2.skyhanni.utils

import io.github.notenoughupdates.moulconfig.observer.Observer
import io.github.notenoughupdates.moulconfig.observer.Property

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

}
