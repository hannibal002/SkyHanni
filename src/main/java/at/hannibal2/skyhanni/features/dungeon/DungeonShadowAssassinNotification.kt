package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket

@SkyHanniModule
object DungeonShadowAssassinNotification {

    private val config get() = SkyHanniMod.feature.dungeon

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onWorldBorderChange(event: PacketReceivedEvent) {
        if (!isEnabled()) return
        if (DungeonApi.dungeonFloor?.contains("3") == true && DungeonApi.inBossRoom) return

        val packet = event.packet as? ClientboundInitializeBorderPacket ?: return
        val warningTime = packet.warningTime

        if (warningTime == 10000) {
            TitleManager.sendTitle("§cShadow Assassin Jumping!")
            SoundUtils.playBeepSound()
        }
    }

    private fun isEnabled() = config.shadowAssassinJumpNotifier
}
