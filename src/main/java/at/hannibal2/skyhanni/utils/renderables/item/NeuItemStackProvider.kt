package at.hannibal2.skyhanni.utils.renderables.item

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import net.minecraft.item.ItemStack
import java.util.WeakHashMap
import kotlin.reflect.KProperty

class NeuItemStackProvider(
    private val internalName: NeuInternalName,
    private val extraOps: (ItemStack.() -> Unit)? = null,
) {
    val stack get() = providerCache[this] ?: rebuildFromNeu().also { providerCache[this] = it }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): ItemStack = stack

    private fun rebuildFromNeu(): ItemStack = with(NeuItems) {
        internalName.getItemStack().also { extraOps?.invoke(it) }
    }

    @SkyHanniModule
    companion object {
        private val providerCache = WeakHashMap<NeuItemStackProvider, ItemStack>()

        @HandleEvent(priority = HandleEvent.LOW)
        fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
            providerCache.forEach { (provider, _) ->
                providerCache[provider] = provider.rebuildFromNeu()
            }
        }
    }
}
