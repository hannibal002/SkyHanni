package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object BlockNotSpawnable {
    private val config get() = SlayerApi.config

    /**
     * REGEX-TEST: Only inside The Rift!
     * REGEX-TEST: Doesn't exist here!
     */
    private val notSpawnablePattern by RepoPattern.pattern(
        "slayer.notspawnable.colorless",
        "Only inside The Rift!|Doesn't exist here!",
    )

    @HandleEvent
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return

        val slot = event.slot ?: return
        if (InventoryUtils.openInventoryName() != "Slayer") return

        val stack = slot.item.orNull() ?: return
        if (notSpawnablePattern.anyMatches(stack.getCleanLore())) {
            event.cancel()
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.blockNotSpawnable
}
