//? if >= 26.1 {
@file:Suppress("VanillaItemStackImport")

package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import java.util.Collections
import java.util.WeakHashMap

internal class DeferredItemStack private constructor(
    private val sourceItem: Item,
    private val factory: () -> ItemStackTemplate,
    count: Int,
) : ItemStack(Holder.direct(sourceItem), count, DataComponentPatch.EMPTY) {

    private var isBuilt = false

    override fun isEmpty() = super.isEmpty()

    internal fun rebuild() {
        if (isBuilt) return
        if (!BuiltInRegistries.ITEM.wrapAsHolder(sourceItem).areComponentsBound()) return
        val pendingPatch = componentsPatch
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
        isBuilt = true
    }

    override fun copy(): ItemStack =
        if (isBuilt) super.copy()
        else DeferredItemStack(sourceItem, factory, this.count).also {
            it.applyComponents(componentsPatch)
            it.popTime = popTime
        }

    init {
        instances.add(this)
        rebuild()
    }

    @SkyHanniModule
    companion object {
        internal val instances: MutableSet<DeferredItemStack> = Collections.newSetFromMap(WeakHashMap())

        operator fun invoke(sourceItem: Item, factory: () -> ItemStackTemplate, count: Int): DeferredItemStack =
            DeferredItemStack(sourceItem, factory, count)

        @HandleEvent
        fun onComponentsLoaded() {
            instances.toList().forEach { it.rebuild() }
        }
    }
}
//?}
