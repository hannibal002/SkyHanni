package at.hannibal2.hanni.features.dungeon

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.SoundUtils
//#if MC < 1.16
import at.hannibal2.hanni.mixins.transformers.AccessorWorldBorderPacket
import net.minecraft.network.play.server.S44PacketWorldBorder
//#else
//$$ import net.minecraft.network.packet.s2c.play.WorldBorderInitializeS2CPacket
//#endif

@HanniModule
object DungeonShadowAssassinNotification {

    private val config get() = HanniMod.feature.dungeon

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onWorldBorderChange(event: PacketReceivedEvent) {
        if (!isEnabled()) return
        if (DungeonApi.dungeonFloor?.contains("3") == true && DungeonApi.inBossRoom) return

        //#if MC < 1.16
        val packet = event.packet as? AccessorWorldBorderPacket ?: return
        val action = packet.action
        if (action != S44PacketWorldBorder.Action.INITIALIZE) return
        //#else
        //$$ val packet = event.packet as? WorldBorderInitializeS2CPacket ?: return
        //#endif
        val warningTime = packet.warningTime

        if (warningTime == 10000) {
            TitleManager.sendTitle("§cShadow Assassin Jumping!")
            SoundUtils.playBeepSound()
        }
    }

    private fun isEnabled() = config.shadowAssassinJumpNotifier
}
