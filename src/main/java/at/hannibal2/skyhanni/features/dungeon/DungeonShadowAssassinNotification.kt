package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraft.network.play.server.S44PacketWorldBorder
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.lang.reflect.Field
import kotlin.time.Duration.Companion.seconds

class DungeonShadowAssassinNotification {
    @SubscribeEvent
    fun onWorldBoarderChange(event: PacketEvent.ReceiveEvent) {
        if (!LorenzUtils.inSkyBlock || !LorenzUtils.inDungeons) return
        if (!SkyHanniMod.feature.dungeon.shadowAssassinJumpNotifier) return
        if (DungeonAPI.inBossRoom && !DungeonAPI.dungeonFloor?.contains("7")!!) return
        if (event.packet !is S44PacketWorldBorder) return
        val packet: S44PacketWorldBorder = event.packet
        val action: Field = packet.javaClass.getDeclaredField("field_179795_a")
        action.isAccessible = true
        val warningTime: Field = packet.javaClass.getDeclaredField("field_179796_h")
        warningTime.isAccessible = true
        if (action.get(packet) == S44PacketWorldBorder.Action.INITIALIZE && warningTime.getInt(packet) == 10000){
            TitleManager.sendTitle("§cShadow Assassin Jump!", 2.seconds, 3.6, 7.0)
            SoundUtils.playBeepSound()
        }
    }
}
