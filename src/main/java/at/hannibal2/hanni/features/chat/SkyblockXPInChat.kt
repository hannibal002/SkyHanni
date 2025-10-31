package at.hannibal2.hanni.features.chat

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.ActionBarStatsData
import at.hannibal2.hanni.events.ActionBarValueUpdateEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils

@HanniModule
object SkyblockXPInChat {

    val config get() = HanniMod.feature.chat.skyBlockXPInChat

    @HandleEvent
    fun onActionBarValueUpdate(event: ActionBarValueUpdateEvent) {
        if (event.updated != ActionBarStatsData.SKYBLOCK_XP) return
        if (!config) return
        ChatUtils.chat(event.updated.value)
    }
}
