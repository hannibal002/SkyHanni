package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.events.ActionBarValueUpdateEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SkyblockXPInChat {

    val config get() = SkyHanniMod.feature.chat.skyBlockXPInChat

    @SubscribeEvent
    fun onActionBarValueUpdate(event: ActionBarValueUpdateEvent) {
        if (event.updated != ActionBarStatsData.SKYBLOCK_XP) return
        if (!config) return
        ChatUtils.chat(event.updated.value)
    }
}
