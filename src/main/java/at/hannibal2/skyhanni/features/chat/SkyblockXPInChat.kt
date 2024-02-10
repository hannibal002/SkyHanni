package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ActionBarStatsData
<<<<<<< HEAD
import at.hannibal2.skyhanni.events.ActionBarValueUpdateEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
=======
import at.hannibal2.skyhanni.events.ActionBarValueUpdate
import at.hannibal2.skyhanni.utils.ChatUtils
>>>>>>> 1af3c89a8daf1eac3921c8b26a49e379f0d18f08
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
