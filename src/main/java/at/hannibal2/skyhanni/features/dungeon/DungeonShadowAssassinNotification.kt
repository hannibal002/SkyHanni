package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SoundUtils
//#if MC < 1.16
import at.hannibal2.skyhanni.mixins.transformers.AccessorWorldBorderPacket
import net.minecraft.network.play.server.S44PacketWorldBorder
//#else
//$$ import net.minecraft.network.packet.s2c.play.WorldBorderInitializeS2CPacket
//#endif

@SkyHanniModule
object DungeonShadowAssassinNotification {

    private val config get() = SkyHanniMod.feature.dungeon

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
