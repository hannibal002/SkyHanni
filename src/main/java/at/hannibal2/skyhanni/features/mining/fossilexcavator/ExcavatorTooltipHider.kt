package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ExcavatorTooltipHider {

    private val config get() = SkyHanniMod.feature.mining.fossilExcavator

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!isEnabled()) return
        if (event.slot.inventory !is ContainerLocalMenu) return
        event.cancel()
    }

    fun isEnabled() = FossilExcavatorAPI.inInventory && !FossilExcavatorAPI.inExcavatorMenu && config.hideExcavatorTooltips
}
