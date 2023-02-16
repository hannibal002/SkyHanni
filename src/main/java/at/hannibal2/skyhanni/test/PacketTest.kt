package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PacketTest {
    companion object {
        private var enabled = false

        fun toggle() {
            enabled = !enabled
            LorenzUtils.chat("Packet test: $enabled")
        }
    }

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        if (!enabled) return

        val packet = event.packet
        val name = packet.javaClass.simpleName

        if (name == "C00PacketKeepAlive") return

        if (name == "C0FPacketConfirmTransaction") return
        if (name == "C03PacketPlayer") return
        if (name == "C04PacketPlayerPosition") return

        println("Send: $name")
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!enabled) return

        val packet = event.packet
        val name = packet.javaClass.simpleName

        if (name == "S00PacketKeepAlive") return
        if (name == "C00PacketKeepAlive") return

        if (name == "S32PacketConfirmTransaction") return
        if (name == "S3BPacketScoreboardObjective") return
        if (name == "S17PacketEntityLookMove") return
        if (name == "S15PacketEntityRelMove") return
        if (name == "S33PacketUpdateSign") return
        if (name == "S18PacketEntityTeleport") return
        if (name == "S16PacketEntityLook") return
        if (name == "S0BPacketAnimation") return
        if (name == "S1FPacketSetExperience") return
        if (name == "S06PacketUpdateHealth") return
        if (name == "S02PacketChat") return
        if (name == "S03PacketTimeUpdate") return
        if (name == "S3EPacketTeams") return
        if (name == "S38PacketPlayerListItem") return
        if (name == "S04PacketEntityEquipment") return
        if (name == "S1DPacketEntityEffect") return

        if (name == "S19PacketEntityHeadLook") return
        if (name == "S12PacketEntityVelocity") return
        if (name == "S29PacketSoundEffect") return
        if (name == "S22PacketMultiBlockChange") return
        if (name == "S19PacketEntityStatus") return
        if (name == "S1CPacketEntityMetadata") return
        if (name == "S3CPacketUpdateScore") return
        if (name == "S20PacketEntityProperties") return

        if (name == "S1BPacketEntityAttach") return
        if (name == "S13PacketDestroyEntities") return
        if (name == "S0EPacketSpawnObject") return

        if (name == "S2APacketParticles") return

        println("Receive: $name")
    }
}