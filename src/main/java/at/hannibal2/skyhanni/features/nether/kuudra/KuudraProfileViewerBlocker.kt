package at.hannibal2.skyhanni.features.nether.kuudra

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.minecraft.packet.PacketSentEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C02PacketUseEntity

@SkyHanniModule
object KuudraProfileViewerBlocker {

    private val config get() = SkyHanniMod.feature.crimsonIsle

    @HandleEvent(onlyOnIsland = IslandType.KUUDRA_ARENA)
    fun onClickEntity(event: PacketSentEvent) {
        if (!config.kuudraProfileViewer) return

        val world = MinecraftCompat.localWorldOrNull ?: return
        val packet = event.packet as? C02PacketUseEntity ?: return
        val entity = packet.getEntityFromWorld(world) ?: return
        if (entity !is EntityPlayer) return
        if (packet.action != C02PacketUseEntity.Action.INTERACT) return

        event.cancel()
    }
}
