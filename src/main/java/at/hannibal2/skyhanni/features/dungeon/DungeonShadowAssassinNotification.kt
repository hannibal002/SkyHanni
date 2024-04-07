package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorWorldBoarderPacket
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraft.network.play.server.S44PacketWorldBorder
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class DungeonShadowAssassinNotification {
    private val config get() = SkyHanniMod.feature.dungeon

    @SubscribeEvent
    fun onWorldBoarderChange(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return
        if (DungeonAPI.dungeonFloor?.contains("3") == true && DungeonAPI.inBossRoom) return

        val packet = event.packet as? AccessorWorldBoarderPacket ?: return
        val action = packet.action
        val warningTime = packet.warningTime

        if (action == S44PacketWorldBorder.Action.INITIALIZE && warningTime == 10000) {
            TitleManager.sendTitle("§cShadow Assassin Jumping!", 2.seconds, 3.6, 7.0f)
            SoundUtils.playBeepSound()
        }
    }

    private fun isEnabled() = LorenzUtils.inDungeons && config.shadowAssassinJumpNotifier
}
