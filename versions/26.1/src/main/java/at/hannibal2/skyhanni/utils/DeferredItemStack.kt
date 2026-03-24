package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ComponentsLoadedEvent
import at.hannibal2.skyhanni.mixins.transformers.IItemStackAccessor
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.world.item.ItemStack
import java.util.Collections
import java.util.WeakHashMap

internal class DeferredItemStack(
    private val factory: () -> ItemStack,
    count: Int,
) : ItemStack(null, count, PatchedDataComponentMap(DataComponentMap.EMPTY)) {

    @Suppress("UNCHECKED_CAST", "KotlinConstantConditions")
    private val accessor get() = (this as Any) as IItemStackAccessor
    private val emptyHolder = accessor.`skyhanni$getItemHolder`()
    private val emptyComponents = accessor.`skyhanni$getPatchedComponents`()
    private var isBuilt = false

    override fun isEmpty() = !isBuilt || super.isEmpty()

    internal fun rebuild() {
        if (!SafeItemStackUtils.componentsLoaded) return
        val real = factory()
        @Suppress("UNCHECKED_CAST")
        val realAccessor = (real as Any) as IItemStackAccessor
        accessor.`skyhanni$setItemHolder`(realAccessor.`skyhanni$getItemHolder`())
        accessor.`skyhanni$setPatchedComponents`(realAccessor.`skyhanni$getPatchedComponents`())
        isBuilt = true
    }

    internal fun invalidate() {
        accessor.`skyhanni$setItemHolder`(emptyHolder)
        accessor.`skyhanni$setPatchedComponents`(emptyComponents)
        isBuilt = false
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
