package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ComponentsLoadedEvent
import at.hannibal2.skyhanni.mixins.transformers.IItemStackAccessor
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.Collections
import java.util.WeakHashMap

internal class DeferredItemStack(
    private val factory: () -> ItemStack,
    count: Int,
) : ItemStack(Items.AIR, count) {

    private val accessor get() = this as IItemStackAccessor
    private val emptyHolder = accessor.`skyhanni$getItemHolder`()
    private val emptyComponents = accessor.`skyhanni$getPatchedComponents`()

    internal fun rebuild() {
        if (!SafeItemStackUtils.componentsLoaded) return
        val real = factory()
        val realAccessor = real as IItemStackAccessor
        accessor.`skyhanni$setItemHolder`(realAccessor.`skyhanni$getItemHolder`())
        accessor.`skyhanni$setPatchedComponents`(realAccessor.`skyhanni$getPatchedComponents`())
    }

    internal fun invalidate() {
        accessor.`skyhanni$setItemHolder`(emptyHolder)
        accessor.`skyhanni$setPatchedComponents`(emptyComponents)
    }

    init {
        instances.add(this)
        rebuild()
    }

    @SkyHanniModule
    companion object {
        internal val instances: MutableSet<DeferredItemStack> = Collections.newSetFromMap(WeakHashMap())

        @HandleEvent
        fun onComponentsLoaded(event: ComponentsLoadedEvent) {
            instances.forEach { it.rebuild() }
        }
    }
}
