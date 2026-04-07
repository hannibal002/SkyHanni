package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.mixins.transformers.IItemStackAccessor
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.Collections
import java.util.WeakHashMap

internal class DeferredItemStack private constructor(
    private val sourceItem: Item,
    private val factory: () -> ItemStack,
    private val deferredComponents: DeferredPatchedDataComponentMap,
    count: Int,
) : ItemStack(Holder.direct(sourceItem), count, deferredComponents) {

    private var isBuilt = false

    override fun isEmpty() = !isBuilt || super.isEmpty()

    internal fun rebuild() {
        if (!BuiltInRegistries.ITEM.wrapAsHolder(sourceItem).areComponentsBound()) return
        val real = factory()
        @Suppress("UNCHECKED_CAST")
        val realComponents = ((real as Any) as IItemStackAccessor).`skyhanni$getPatchedComponents`()
        deferredComponents.realDelegate = realComponents
        @Suppress("DEPRECATION")
        item = real.typeHolder()
        isBuilt = true
    }

    internal fun invalidate() {
        deferredComponents.realDelegate = null
        @Suppress("DEPRECATION")
        item = null
        isBuilt = false
    }

    override fun copy(): ItemStack = DeferredItemStack(sourceItem, factory, this.count)

    init {
        instances.add(this)
        rebuild()
    }

    @SkyHanniModule
    companion object {
        internal val instances: MutableSet<DeferredItemStack> = Collections.newSetFromMap(WeakHashMap())

        operator fun invoke(sourceItem: Item, factory: () -> ItemStack, count: Int): DeferredItemStack =
            DeferredItemStack(sourceItem, factory, DeferredPatchedDataComponentMap(), count)

        @HandleEvent
        fun onComponentsLoaded() {
            instances.forEach { it.rebuild() }
        }
    }
}
