package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ComponentsLoadedEvent
import at.hannibal2.skyhanni.mixins.transformers.IItemStackAccessor
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.PatchedDataComponentMap
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.lang.invoke.MethodHandles
import java.util.Collections
import java.util.WeakHashMap
import kotlin.jvm.java

internal class DeferredItemStack(
    private val sourceItem: Item,
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
        if (!BuiltInRegistries.ITEM.wrapAsHolder(sourceItem).areComponentsBound()) return
        val real = factory()
        @Suppress("UNCHECKED_CAST")
        val realAccessor = (real as Any) as IItemStackAccessor
        itemHandle.set(this, realAccessor.`skyhanni$getItemHolder`())
        componentsHandle.set(this, realAccessor.`skyhanni$getPatchedComponents`())
        isBuilt = true
    }

    internal fun invalidate() {
        itemHandle.set(this, emptyHolder)
        componentsHandle.set(this, emptyComponents)
        isBuilt = false
    }

    init {
        instances.add(this)
        rebuild()
    }

    @SkyHanniModule
    companion object {
        private val lookup = MethodHandles.privateLookupIn(ItemStack::class.java, MethodHandles.lookup())
        private val itemHandle = lookup.findVarHandle(ItemStack::class.java, "item", Holder::class.java)
        private val componentsHandle = lookup.findVarHandle(ItemStack::class.java, "components", PatchedDataComponentMap::class.java)

        internal val instances: MutableSet<DeferredItemStack> = Collections.newSetFromMap(WeakHashMap())

        @HandleEvent
        fun onComponentsLoaded(event: ComponentsLoadedEvent) {
            instances.forEach { it.rebuild() }
        }
    }
}
