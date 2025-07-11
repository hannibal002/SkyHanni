package at.hannibal2.skyhanni.utils.renderables.item

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import net.minecraft.item.ItemStack
import kotlin.reflect.KProperty

class NeuItemStackProvider(
    private val internalName: NeuInternalName,
    private val extraOps: (ItemStack.() -> Unit)? = null,
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): ItemStack = stack

    private var _lastNeuItemsHash: Int = neuItemsHash
    private var _cachedStack: ItemStack = rebuildFromNeu()

    private fun rebuildFromNeu() = with(NeuItems) {
        _cachedStack = internalName.getItemStack().also {
            extraOps?.invoke(it)
        }
        _lastNeuItemsHash = neuItemsHash
        _cachedStack
    }

    init {
        SkyHanniMod.logger.info("Setting up NeuItemStackProvider for $internalName, init with item count: $neuItemsHash")
    }

    private val stack: ItemStack get() = _cachedStack.takeIf {
        _lastNeuItemsHash == neuItemsHash
    } ?: run {
        SkyHanniMod.logger.info("Rebuilding NeuItemStack for $internalName, item count changed from $_lastNeuItemsHash to $neuItemsHash")
        rebuildFromNeu()
    }

    @SkyHanniModule
    companion object {
        private var neuItemsHash: Int = EnoughUpdatesManager.getItemInformation().hashCode()

        @HandleEvent(priority = HandleEvent.LOW)
        fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
            SkyHanniMod.logger.info("Neu repository reloaded, updating neu item count. Current hash: $neuItemsHash")
            neuItemsHash = EnoughUpdatesManager.getItemInformation().hashCode()
            SkyHanniMod.logger.info("New neu item hash: $neuItemsHash")
        }
    }
}
