package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ExcavatorTooltipHider {

    private val config get() = SkyHanniMod.feature.mining.fossilExcavator.tooltipHider

    private val dirtPattern by RepoPattern.pattern(
        "excavator.dirt.name",
        "§6Dirt"
    )

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!isEnabled()) return

        if (event.slot.inventory !is ContainerLocalMenu) return
        if (!(dirtPattern.matches(event.itemStack.displayName)) && !config.hideAllExcavatorTooltips) return
        event.cancel()
    }

    fun isEnabled() = FossilExcavatorAPI.inInventory && !FossilExcavatorAPI.inExcavatorMenu && config.hideExcavatorTooltips
}
