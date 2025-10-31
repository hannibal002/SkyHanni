package at.hannibal2.hanni.features.mining.fossilexcavator

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.inventory.ContainerLocalMenu

@HanniModule
object ExcavatorTooltipHider {

    private val config get() = HanniMod.feature.mining.fossilExcavator.tooltipHider

    /**
     * REGEX-TEST: §6Dirt
     */
    private val dirtPattern by RepoPattern.pattern(
        "excavator.dirt.name",
        "§6Dirt",
    )

    @HandleEvent
    fun onToolTip(event: ToolTipEvent) {
        if (!isEnabled()) return

        if (event.slot.inventory !is ContainerLocalMenu) return
        if (config.hideEverything) {
            event.cancel()
            return
        }

        if (config.hideDirt) {
            val isDirt = dirtPattern.matches(event.itemStack.displayName)
            if (isDirt) {
                event.cancel()
            }
        }
    }

    fun isEnabled() = FossilExcavatorApi.inInventory && !FossilExcavatorApi.inExcavatorMenu
}
