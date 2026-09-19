package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.SafeItemStack
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration

class RecalculatingValue<T>(private val expireTime: Duration, private val calculation: () -> T) : ReadOnlyProperty<Any?, T> {

    private var currentValue: Any? = UNINITIALIZED_VALUE
    private var lastAccessTime = SimpleTimeMark.farPast()

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (lastAccessTime.passedSince() > expireTime) {
            currentValue = calculation()
            lastAccessTime = SimpleTimeMark.now()
        }
        @Suppress("UNCHECKED_CAST")
        return currentValue as T
    }

    companion object {
        private val UNINITIALIZED_VALUE = Any()
    }
}

class ResettableValue<T>(private val calculation: () -> T) : ReadOnlyProperty<Any?, T> {

    private var currentValue: Any? = UNINITIALIZED_VALUE
    private var dirty = true

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (dirty) {
            currentValue = calculation()
            dirty = false
        }
        @Suppress("UNCHECKED_CAST")
        return currentValue as T
    }

    fun reset() {
        dirty = true
    }

    companion object {
        private val UNINITIALIZED_VALUE = Any()
    }
}

class StableOrTransientValue<T>(
    private val transientExpireTime: Duration,
    private val calculation: () -> Result<T>,
) : ReadOnlyProperty<Any?, T> {

    private var currentValue: Any? = UNINITIALIZED_VALUE
    private var lastCalculationTime = SimpleTimeMark.farPast()
    private var stable = false

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

    fun get(): T {
        if (currentValue === UNINITIALIZED_VALUE || (!stable && lastCalculationTime.passedSince() > transientExpireTime)) {
            val result = calculation()
            currentValue = result.value
            stable = result.stable
            lastCalculationTime = SimpleTimeMark.now()
        }
        @Suppress("UNCHECKED_CAST")
        return currentValue as T
    }

    fun reset() {
        currentValue = UNINITIALIZED_VALUE
        lastCalculationTime = SimpleTimeMark.farPast()
        stable = false
    }

    data class Result<T>(val value: T, val stable: Boolean)

    companion object {
        private val UNINITIALIZED_VALUE = Any()

        fun <T> stable(value: T) = Result(value, stable = true)

        fun <T> transient(value: T) = Result(value, stable = false)
    }
}

class AutoUpdatingItemStack(internalName: NeuInternalName) : ReadOnlyProperty<Any?, SafeItemStack> {

    private val value: ResettableValue<SafeItemStack> = ResettableValue {
        internalName.getItemStack()
    }.also { list.add(it) }

    override fun getValue(thisRef: Any?, property: KProperty<*>): SafeItemStack = value.getValue(thisRef, property)

    @SkyHanniModule
    companion object {
        // We can't have a real constructor that uses a string, as NeuInternalName is a string on runtime, and they would have
        // the same jvm signature. Using an invoke operator makes it look like a fake constructor
        fun of(internalName: String) = AutoUpdatingItemStack(internalName.toInternalName())
        operator fun invoke(internalName: String) = of(internalName)

        val list = mutableListOf<ResettableValue<SafeItemStack>>()

        @HandleEvent(RepositoryReloadEvent::class)
        fun onRepoReload() {
            list.forEach { it.reset() }
        }

        @HandleEvent(NeuRepositoryReloadEvent::class)
        fun onNeuRepoReload() {
            list.forEach { it.reset() }
        }
    }
}
