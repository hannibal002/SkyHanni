package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.events.mining.CompactUpdateEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BlocksSinceMineshaftDisplay {
    private val config get() = SkyHanniMod.feature.mining.blocksSinceMineshaft

    @SubscribeEvent
    fun onCompactUpdate(event: CompactUpdateEvent) {
        ChatUtils.debug("Mined ${event.amount} ${event.block.blockName}")
    }

    fun isEnabled() = MiningAPI.inGlacialTunnels() && config
}
