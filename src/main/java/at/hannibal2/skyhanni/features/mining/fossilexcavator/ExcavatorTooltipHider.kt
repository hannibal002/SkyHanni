package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.SkyHanniToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.inventory.ContainerLocalMenu

@SkyHanniModule
object ExcavatorTooltipHider {

    private val config get() = SkyHanniMod.feature.mining.fossilExcavator.tooltipHider

    /**
     * REGEX-TEST: §6Dirt
     */
    private val dirtPattern by RepoPattern.pattern(
        "excavator.dirt.name",
        "§6Dirt",
    )

    @HandleEvent
    fun onTooltip(event: SkyHanniToolTipEvent) {
        if (!isEnabled()) return

        if (event.slot.inventory !is ContainerLocalMenu) return
        if (config.hideEverything) {
            event.cancel()
            return
        }

        if (config.hideDirt) {
            val isDirt = dirtPattern.matches(event.itemStack.name)
            if (isDirt) {
                event.cancel()
            }
        }
    }

    fun isEnabled() = FossilExcavatorAPI.inInventory && !FossilExcavatorAPI.inExcavatorMenu
}
