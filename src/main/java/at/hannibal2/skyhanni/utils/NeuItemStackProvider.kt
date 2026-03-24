package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.renderables.ItemStackProvider
import java.util.WeakHashMap

class NeuItemStackProvider(
    private val internalName: NeuInternalName,
    private val extraOps: (SafeItemStack.() -> Unit)? = null,
) : ItemStackProvider {
    override val stack get() = providerCache[this] ?: rebuildFromNeu().also { providerCache[this] = it }

    private fun rebuildFromNeu(): SafeItemStack = with(NeuItems) {
        internalName.getItemStack().also { extraOps?.invoke(it) }
    }

    @SkyHanniModule
    companion object {
        private val providerCache = WeakHashMap<NeuItemStackProvider, SafeItemStack>()

        @HandleEvent(NeuRepositoryReloadEvent::class, priority = HandleEvent.LOW)
        fun onNeuRepoReload() {
            providerCache.forEach { (provider, _) ->
                providerCache[provider] = provider.rebuildFromNeu()
            }
        }
    }
}
