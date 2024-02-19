package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SocialFeatures {
    private val config get() = SkyHanniMod.feature.misc.socialFeatures
    private var players: List<String> = mutableListOf<String>()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock || LorenzUtils.inDungeons || !config.showLobbySize) return;
        config.showLobbySizePos?.renderString("${players.size} Players", posLabel = "Players in Lobby")
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        players = mutableListOf<String>()
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!LorenzUtils.inSkyBlock || LorenzUtils.inDungeons) return;
        if (!config.playerLeaveNotifications && !config.playerJoinNotifications) return;

        val playersInfo = TabListData.getTabList().filter { it.startsWith("§8[" ) }.map { it.removeColor().split("] ").last().split(" ").first() }.distinct()
        val initialLoad = players.isEmpty()

        for (name in playersInfo) {
            if (!players.contains(name)) {
                if (!initialLoad && config.playerJoinNotifications && config.playerNotificationLobbySize >=  playersInfo.size) {
                    LorenzUtils.chat("§7${name} has joined your lobby", false)
                }
            }
        }

        for (player in players) {
            if (!playersInfo.contains(player) && config.playerLeaveNotifications && config.playerNotificationLobbySize >=  playersInfo.size) {
                LorenzUtils.chat("§7${player} has left your lobby", false)
            }
        }

        players = playersInfo.toMutableList()
    }
}
