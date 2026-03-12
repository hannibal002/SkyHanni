package at.hannibal2.skyhanni.utils.system

import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PropertyVar<T>(initializer: () -> Property<T>) : ReadWriteProperty<Any?, T> {
    private val property: Property<T> by lazy(initializer)
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = this.property.get()
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = this.property.set(value)

    constructor(initialValue: T) : this({ Property.of(initialValue) })
    constructor(property: Property<T>) : this({ property })
}
