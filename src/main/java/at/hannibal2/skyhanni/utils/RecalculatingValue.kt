package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import net.minecraft.world.item.ItemStack
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

class AutoUpdatingItemStack(internalName: NeuInternalName) : ReadOnlyProperty<Any?, ItemStack> {

    constructor(rawInternalName: String) : this(rawInternalName.toInternalName())

    private val value: ResettableValue<ItemStack> = ResettableValue {
        internalName.getItemStack()
    }.also { list.add(it) }

    override fun getValue(thisRef: Any?, property: KProperty<*>): ItemStack = value.getValue(thisRef, property)

    @SkyHanniModule
    companion object {
        val list = mutableListOf<ResettableValue<ItemStack>>()

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
