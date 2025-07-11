package at.hannibal2.skyhanni.utils.renderables.item

import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import net.minecraft.item.ItemStack

abstract class AbstractItemStackProvider {
    abstract val stack: ItemStack
}

class StaticItemStackProvider(itemStack: ItemStack) : AbstractItemStackProvider() {
    override val stack: ItemStack = itemStack
}

class NeuItemStackProvider(private val internalName: NeuInternalName) : AbstractItemStackProvider() {
    private var _lastNeuItemCount: Int = neuItemCount
    private var _cachedStack: ItemStack = rebuildFromNeu()

    private fun rebuildFromNeu() = with(NeuItems) {
        _cachedStack = internalName.getItemStack()
        _lastNeuItemCount = neuItemCount
        _cachedStack
    }

    override val stack: ItemStack get() = _cachedStack.takeIf {
        _lastNeuItemCount == neuItemCount
    } ?: rebuildFromNeu()

    @SkyHanniModule
    companion object {
        private var neuItemCount: Int = 0

        @HandleEvent(priority = HandleEvent.LOW)
        fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
            neuItemCount = EnoughUpdatesManager.getItemInformation().size
        }
    }
}
