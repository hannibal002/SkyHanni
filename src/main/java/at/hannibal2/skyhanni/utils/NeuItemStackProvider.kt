package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.renderables.ItemStackProvider
import net.minecraft.world.item.ItemStack
import java.util.WeakHashMap

class NeuItemStackProvider(
    private val internalName: NeuInternalName,
    private val extraOps: (ItemStack.() -> Unit)? = null,
) : ItemStackProvider {
    override val stack get() = providerCache[this] ?: rebuildFromNeu().also { providerCache[this] = it }

    private fun rebuildFromNeu(): ItemStack = with(NeuItems) {
        internalName.getItemStack().also { extraOps?.invoke(it) }
    }

    @SkyHanniModule
    companion object {
        private val providerCache = WeakHashMap<NeuItemStackProvider, ItemStack>()

        @HandleEvent(NeuRepositoryReloadEvent::class, priority = HandleEvent.LOW)
        fun onNeuRepoReload() {
            providerCache.forEach { (provider, _) ->
                providerCache[provider] = provider.rebuildFromNeu()
            }
        }
    }
}
