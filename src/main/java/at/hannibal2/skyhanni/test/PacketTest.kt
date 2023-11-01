package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S04PacketEntityEquipment
import net.minecraft.network.play.server.S0BPacketAnimation
import net.minecraft.network.play.server.S0CPacketSpawnPlayer
import net.minecraft.network.play.server.S0EPacketSpawnObject
import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S14PacketEntity
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.network.play.server.S19PacketEntityHeadLook
import net.minecraft.network.play.server.S19PacketEntityStatus
import net.minecraft.network.play.server.S1BPacketEntityAttach
import net.minecraft.network.play.server.S1CPacketEntityMetadata
import net.minecraft.network.play.server.S1DPacketEntityEffect
import net.minecraft.network.play.server.S20PacketEntityProperties
import net.minecraft.network.play.server.S28PacketEffect
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PacketTest {
    companion object {
        private var enabled = false

        fun toggle() {
            enabled = !enabled
            LorenzUtils.chat("Packet test: $enabled", false)
        }
    }

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        if (!enabled) return

        val packet = event.packet
        val packetName = packet.javaClass.simpleName

        if (packetName == "C00PacketKeepAlive") return

        if (packetName == "C0FPacketConfirmTransaction") return
        if (packetName == "C03PacketPlayer") return
        if (packetName == "C04PacketPlayerPosition") return

        if (packetName == "C06PacketPlayerPosLook") return
        if (packetName == "C0BPacketEntityAction") return
        if (packetName == "C05PacketPlayerLook") return
        if (packetName == "C09PacketHeldItemChange") return

        println("Send: $packetName")
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        val packetName = packet.javaClass.simpleName

        if (!enabled) return

        // Keep alive
        if (packetName == "S00PacketKeepAlive") return
        if (packetName == "C00PacketKeepAlive") return
        if (packetName == "S32PacketConfirmTransaction") return

        // Gui
        if (packetName == "S3BPacketScoreboardObjective") return
        if (packetName == "S1FPacketSetExperience") return
        if (packetName == "S3EPacketTeams") return
        if (packetName == "S38PacketPlayerListItem") return
        if (packetName == "S3CPacketUpdateScore") return
        if (packetName == "S06PacketUpdateHealth") return

        // Block & World
        if (packetName == "S33PacketUpdateSign") return
        if (packetName == "S22PacketMultiBlockChange") return
        if (packetName == "S03PacketTimeUpdate") return
        if (packetName == "S21PacketChunkData") return
        if (packetName == "S23PacketBlockChange") return

        // Chat
        if (packetName == "S02PacketChat") return

        // Others
        if (packetName == "S29PacketSoundEffect") return
//        if (packetName == "S2APacketParticles") return

        // Entity
        if (packetName == "S13PacketDestroyEntities") return

        if (packetName == "S18PacketEntityTeleport") return
        if (packetName == "S15PacketEntityRelMove") return
        if (packetName == "S04PacketEntityEquipment") return

//        if (packetName == "S0EPacketSpawnObject") return
//        if (packetName == "S0BPacketAnimation") return
//        if (packetName == "S06PacketUpdateHealth") return
//        if (packetName == "S17PacketEntityLookMove") return
//        if (packetName == "S16PacketEntityLook") return
//        if (packetName == "S19PacketEntityHeadLook") return
//        if (packetName == "S1DPacketEntityEffect") return
//        if (packetName == "S12PacketEntityVelocity") return
//        if (packetName == "S19PacketEntityStatus") return
//        if (packetName == "S1CPacketEntityMetadata") return
//        if (packetName == "S20PacketEntityProperties") return
//        if (packetName == "S1BPacketEntityAttach") return


        val id = getEntityId(packet)
        val entity = getEntity(packet, id)
        val distance = getDistance(getLocation(packet, entity))
        if (distance > 10) return

        if (entity != null) {
            if (entity == Minecraft.getMinecraft().thePlayer) {
//                println("own: $distance $packetName")
                return
            } else {
                println("other: $distance")
            }
        } else {
            if (id != null) {
                return
            }


//            if (packetName.contains("")) {
//
//            }
            println("entity is null.")
        }

//        println("distance: $distance")
        println("Receive: $packetName")
        println(" ")
    }

    private fun getDistance(location: LorenzVec?): Double {
        return location?.distanceToPlayer()?.round(1) ?: 0.0
    }

    private fun getLocation(packet: Packet<*>, entity: Entity?): LorenzVec? {
        if (packet is S2APacketParticles) {
            return LorenzVec(packet.xCoordinate, packet.yCoordinate, packet.zCoordinate)
        }
        if (packet is S0EPacketSpawnObject) {
            return LorenzVec(packet.x, packet.y, packet.z)
        }
        if (packet is S0CPacketSpawnPlayer) {
            return LorenzVec(packet.x, packet.y, packet.z)
        }
        if (packet is C03PacketPlayer) {
            return LorenzVec(packet.positionX, packet.positionY, packet.positionZ)
        }

        if (packet is S0FPacketSpawnMob) {
            return LorenzVec(packet.x, packet.y, packet.z)
        }
        if (packet is S28PacketEffect) {
            return packet.soundPos.toLorenzVec()
        }

        if (entity != null) {
            return entity.getLorenzVec()
        }

        return null
    }

    private fun getEntity(packet: Packet<*>, id: Int?): Entity? {
        val world = Minecraft.getMinecraft().theWorld
        if (packet is S14PacketEntity) {
            return packet.getEntity(world)
        }
        if (packet is S19PacketEntityHeadLook) {
            return packet.getEntity(world)
        }
        if (packet is S19PacketEntityStatus) {
            return packet.getEntity(world)
        }
        if (id != null) {
            return EntityUtils.getEntityByID(id)
        }

        return null
    }

    private fun getEntityId(packet: Packet<*>) = when (packet) {
        is S1CPacketEntityMetadata -> packet.entityId
        is S20PacketEntityProperties -> packet.entityId
        is S04PacketEntityEquipment -> packet.entityID
        is S12PacketEntityVelocity -> packet.entityID
        is S1BPacketEntityAttach -> packet.entityId
        is S0BPacketAnimation -> packet.entityID
        is S18PacketEntityTeleport -> packet.entityId
        is S1DPacketEntityEffect -> packet.entityId

        else -> null
    }
}
