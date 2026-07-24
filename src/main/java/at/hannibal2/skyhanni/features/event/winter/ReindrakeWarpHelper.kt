package at.hannibal2.skyhanni.features.event.winter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.WinterApi
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands

@SkyHanniModule
object ReindrakeWarpHelper {

    private val config get() = SkyHanniMod.feature.event.winter

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        if (!WinterApi.isReindrakeSpawnMessage(event.cleanMessage)) return
        ChatUtils.clickToActionOrDisable(
            "A Reindrake was detected. Click to warp to the Winter Island spawn!",
            config::reindrakeWarpHelper,
            actionName = "warp to winter island spawn",
            action = { HypixelCommands.warp("winter") },
        )
    }

    fun isEnabled() = IslandType.WINTER.isInIsland() && config.reindrakeWarpHelper
}
