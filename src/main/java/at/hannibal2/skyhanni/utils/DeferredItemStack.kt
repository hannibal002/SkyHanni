//? if >= 26.1 {
@file:Suppress("VanillaItemStackImport")

package at.hannibal2.skyhanni.utils

import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.core.component.TypedDataComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate

internal class DeferredItemStack private constructor(
    private val sourceItem: Item,
    private val factory: () -> ItemStackTemplate,
    count: Int,
) : ItemStack(Holder.direct(sourceItem), count, DataComponentPatch.EMPTY) {

    private var isBuilt = false
    private val removedComponents = mutableSetOf<DataComponentType<*>>()

    override fun isEmpty() = super.isEmpty()

    internal fun bindComponentsIfReady() {
        if (isBuilt) return
        if (!SafeItemStackUtils.canBindComponents(sourceItem)) return
        val pendingPatch = pendingComponentsPatch()
        val real = factory().create()
        if (real.isEmpty) {
            count = 0
            isBuilt = true
            return
        }
        @Suppress("DEPRECATION")
        item = real.typeHolder()
        @Suppress("DEPRECATION")
        components = PatchedDataComponentMap.fromPatch(real.typeHolder().components(), real.componentsPatch).also {
            it.applyPatch(pendingPatch)
        }
        removedComponents.clear()
        isBuilt = true
    }

    override fun getComponents(): DataComponentMap {
        bindComponentsIfReady()
        return super.getComponents()
    }

    override fun getPrototype(): DataComponentMap {
        bindComponentsIfReady()
        return super.getPrototype()
    }

    override fun immutableComponents(): DataComponentMap {
        bindComponentsIfReady()
        return super.immutableComponents()
    }

    override fun hasNonDefault(type: DataComponentType<*>): Boolean {
        bindComponentsIfReady()
        return super.hasNonDefault(type)
    }

    override fun <T : Any> set(type: DataComponentType<T>, value: T?): T? {
        if (!isBuilt) {
            if (value == null) removedComponents.add(type)
            else removedComponents.remove(type)
        }
        return super.set(type, value)
    }

    override fun <T : Any> set(value: TypedDataComponent<T>): T? {
        if (!isBuilt) removedComponents.remove(value.type())
        return super.set(value)
    }

    override fun <T : Any> remove(type: DataComponentType<out T>): T? {
        if (!isBuilt) removedComponents.add(type)
        return super.remove(type)
    }

    override fun applyComponents(patch: DataComponentPatch) {
        if (!isBuilt) {
            patch.entrySet().forEach { (type, value) ->
                if (value.isPresent) removedComponents.remove(type)
                else removedComponents.add(type)
            }
        }
        super.applyComponents(patch)
    }

    override fun applyComponents(components: DataComponentMap) {
        if (!isBuilt) components.keySet().forEach(removedComponents::remove)
        super.applyComponents(components)
    }

    override fun copy(): ItemStack =
        if (isBuilt) super.copy()
        else DeferredItemStack(sourceItem, factory, this.count).also {
            it.applyComponents(pendingComponentsPatch())
            it.popTime = popTime
        }

    private fun pendingComponentsPatch(): DataComponentPatch {
        val pendingPatch = super.getComponentsPatch()
        if (removedComponents.isEmpty()) return pendingPatch

        val builder = DataComponentPatch.builder()
        pendingPatch.entrySet().forEach { (type, value) ->
            if (value.isPresent) builder.setUnchecked(type, value.get())
            else builder.removeUnchecked(type)
        }
        removedComponents.forEach(builder::removeUnchecked)
        return builder.build()
    }

    companion object {
        operator fun invoke(sourceItem: Item, factory: () -> ItemStackTemplate, count: Int): DeferredItemStack =
            DeferredItemStack(sourceItem, factory, count)
    }
}

// DataComponentPatch.Builder keeps component value type safety through matching type/value pairs.
@Suppress("UNCHECKED_CAST")
private fun DataComponentPatch.Builder.setUnchecked(type: DataComponentType<*>, value: Any) {
    set(type as DataComponentType<Any>, value)
}

// DataComponentPatch.Builder.remove does not use a value, but still requires a concrete component type.
@Suppress("UNCHECKED_CAST")
private fun DataComponentPatch.Builder.removeUnchecked(type: DataComponentType<*>) {
    remove(type as DataComponentType<Any>)
}
//?}
