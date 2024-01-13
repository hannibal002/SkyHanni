package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.events.ActionBarValueUpdate
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SkyblockXPInChat {

    val config get() = SkyHanniMod.feature.chat.skyBlockXPInChat

    @SubscribeEvent
    fun onActionBarValueUpdate(event: ActionBarValueUpdate) {
        if (event.updated != ActionBarStatsData.SKYBLOCK_XP) return
        if (!config) return
        LorenzUtils.chat(event.updated.value)
    }
}
