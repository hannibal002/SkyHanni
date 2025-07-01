package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketSentEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.isInt
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.ReflectionUtils.makeAccessible
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat.isLocalPlayer
import at.hannibal2.skyhanni.utils.compat.getLocation
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.entity.Entity
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C00PacketKeepAlive
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S00PacketKeepAlive
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S03PacketTimeUpdate
import net.minecraft.network.play.server.S04PacketEntityEquipment
import net.minecraft.network.play.server.S06PacketUpdateHealth
import net.minecraft.network.play.server.S0BPacketAnimation
import net.minecraft.network.play.server.S0CPacketSpawnPlayer
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S13PacketDestroyEntities
import net.minecraft.network.play.server.S14PacketEntity
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.network.play.server.S19PacketEntityHeadLook
import net.minecraft.network.play.server.S19PacketEntityStatus
import net.minecraft.network.play.server.S1BPacketEntityAttach
import net.minecraft.network.play.server.S1CPacketEntityMetadata
import net.minecraft.network.play.server.S1DPacketEntityEffect
import net.minecraft.network.play.server.S1FPacketSetExperience
import net.minecraft.network.play.server.S20PacketEntityProperties
import net.minecraft.network.play.server.S21PacketChunkData
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraft.network.play.server.S28PacketEffect
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.network.play.server.S33PacketUpdateSign
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraft.network.play.server.S3BPacketScoreboardObjective
import net.minecraft.network.play.server.S3CPacketUpdateScore
import net.minecraft.network.play.server.S3EPacketTeams
import net.minecraft.network.play.server.S14PacketEntity.S15PacketEntityRelMove as EntityRelMove
import net.minecraft.network.play.server.S14PacketEntity.S16PacketEntityLook as EntityLook
import net.minecraft.network.play.server.S14PacketEntity.S17PacketEntityLookMove as EntityLookMove
//#if MC < 1.21
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.network.play.server.S0EPacketSpawnObject
import net.minecraft.network.play.server.S0FPacketSpawnMob
//#endif


@SkyHanniModule
object PacketTest {

    private var enabled = false
    private var full = false

    private val entityMap = mutableMapOf<Int, MutableList<Packet<*>>>()

    private fun command(args: Array<String>) {
        if (args.size == 1 && args[0].isInt()) {
            sendEntityPacketData(args[0].toInt())
            return
        }
        if (args.size == 1 && (args[0] == "full" || args[0] == "all")) {
            full = !full
            ChatUtils.chat("Packet test full: $full")
            return
        }

        toggle()
    }

    private fun sendEntityPacketData(id: Int) {
        ChatUtils.chat("Packet Entity Data: $id")
        entityMap[id]?.forEach { it.print() }
        println("End of Data")
    }

    private fun toggle() {
        enabled = !enabled
        ChatUtils.chat("Packet test: $enabled")
    }

    @HandleEvent
    fun onSendPacket(event: PacketSentEvent) {
        if (!enabled) return

        val packet = event.packet
        val packetName = packet.javaClass.simpleName

        if (packetName == C00PacketKeepAlive::class.simpleName) return
        //#if MC < 1.21
        if (packetName == C0FPacketConfirmTransaction::class.simpleName) return
        //#endif
        if (packetName == C04PacketPlayerPosition::class.simpleName) return

        if (packetName == C09PacketHeldItemChange::class.simpleName) return
        if (packetName == C06PacketPlayerPosLook::class.simpleName) return
        if (packetName == C0BPacketEntityAction::class.simpleName) return
        if (packetName == C05PacketPlayerLook::class.simpleName) return
        //#if MC > 1.21
        //$$ if (packetName == net.minecraft.network.packet.c2s.common.CommonPongC2SPacket::class.simpleName) return
        //$$ if (packetName == net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket::class.simpleName) return
        //#endif
        if (packetName == C03PacketPlayer::class.simpleName) return

        println("Send: [$packetName]")
    }

    @HandleEvent(priority = HandleEvent.LOW, receiveCancelled = true)
    fun onPacketReceive(event: PacketReceivedEvent) {
        if (!enabled) return
        val packet = event.packet
        packet.print()
        if (packet is S13PacketDestroyEntities) {
            packet.entityIDs.forEach {
                entityMap.getOrDefault(it, mutableListOf()).add(packet)
            }
        } else {
            val id = packet.getEntityId() ?: return
            entityMap.getOrDefault(id, mutableListOf()).add(packet)
        }
    }

    private fun Packet<*>.print() {
        val packetName = javaClass.simpleName

        // Keep alive
        if (packetName == S00PacketKeepAlive::class.simpleName) return
        if (packetName == C00PacketKeepAlive::class.simpleName) return
        //#if MC < 1.21
        if (packetName == S32PacketConfirmTransaction::class.simpleName) return
        //#else
        //$$ if (packetName == net.minecraft.network.packet.s2c.common.CommonPingS2CPacket::class.simpleName) return
        //$$ if (packetName == net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket::class.simpleName) return
        //#endif

        // Gui
        if (packetName == S3BPacketScoreboardObjective::class.simpleName) return
        if (packetName == S3EPacketTeams::class.simpleName) return
        if (packetName == S38PacketPlayerListItem::class.simpleName) return
        if (packetName == S3CPacketUpdateScore::class.simpleName) return
        if (packetName == S1FPacketSetExperience::class.simpleName) return
        if (packetName == S06PacketUpdateHealth::class.simpleName) return

        // Block & World
        if (packetName == S33PacketUpdateSign::class.simpleName) return
        if (packetName == S03PacketTimeUpdate::class.simpleName) return
        if (packetName == S21PacketChunkData::class.simpleName) return
        if (packetName == S22PacketMultiBlockChange::class.simpleName) return
        if (packetName == S23PacketBlockChange::class.simpleName) return
        //#if MC > 1.21
        //$$ if (packetName == net.minecraft.network.packet.s2c.play.BlockEventS2CPacket::class.simpleName) return
        //$$ if (packetName == net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket::class.simpleName) return
        //$$ if (packetName == net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket::class.simpleName) return
        //#endif

        // Chat
        if (packetName == S02PacketChat::class.simpleName) return

        // Others
        if (packetName == S29PacketSoundEffect::class.simpleName) return
        if (!full && packetName == S2APacketParticles::class.simpleName) return
        //#if MC > 1.21
        //$$ if(packetName == net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket::class.simpleName) return
        //#endif

        // Entity
        if (this is S13PacketDestroyEntities) {
            println("Receive: [$packetName] with IDs: ${entityIDs.joinToString(", ")}")
            return
        }

        if (!full) {
            if (packetName == S18PacketEntityTeleport::class.simpleName) return
            if (packetName == S04PacketEntityEquipment::class.simpleName) return
            if (packetName == EntityRelMove::class.simpleName) return
            if (packetName == EntityLookMove::class.simpleName) return
            if (packetName == S19PacketEntityHeadLook::class.simpleName) return
            if (packetName == EntityLook::class.simpleName) return
            //#if MC > 1.21
            //$$ if(packetName == net.minecraft.network.packet.s2c.play.BossBarS2CPacket::class.simpleName) return
            //$$ if(packetName == net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket::class.simpleName) return
            //#endif
            if (packetName == S12PacketEntityVelocity::class.simpleName) return
            if (packetName == S1CPacketEntityMetadata::class.simpleName) return
            if (packetName == S20PacketEntityProperties::class.simpleName) return
            if (packetName == S0BPacketAnimation::class.simpleName) return
        }

//        if (packetName == S0EPacketSpawnObject::class.simpleName) return
//        if (packetName == S06PacketUpdateHealth::class.simpleName) return
//        if (packetName == S1DPacketEntityEffect::class.simpleName) return
//        if (packetName == S19PacketEntityStatus::class.simpleName) return
//        if (packetName == S1BPacketEntityAttach::class.simpleName) return

        buildString {
            append("Receive: [$packetName]")

            val id = getEntityId()
            if (id != null) {
                append(" ID: $id")
            }

            val entity = getEntity(this@print, id)
            val distance = getDistance(getLocation(this@print, entity))

            if (entity != null) {
                if (entity.isLocalPlayer) {
                    append(" own")
                    return@buildString
                } else {
                    append(" distance: $distance other")
                }
            } else {
                if (id == null) {
                    return@buildString
                }
                append(" entity is null.")
            }
        }.let { println(it) }
    }

    private fun getDistance(location: LorenzVec?): Double {
        return location?.distanceToPlayer()?.roundTo(1) ?: 0.0
    }

    private fun getLocation(packet: Packet<*>, entity: Entity?): LorenzVec? {
        if (packet is S2APacketParticles) {
            return LorenzVec(packet.xCoordinate, packet.yCoordinate, packet.zCoordinate)
        }

        if (packet is S0CPacketSpawnPlayer) {
            return LorenzVec(packet.x, packet.y, packet.z)
        }
        //#if MC < 1.21
        if (packet is S0EPacketSpawnObject) {
            return LorenzVec(packet.x, packet.y, packet.z)
        }
        if (packet is S0FPacketSpawnMob) {
            return LorenzVec(packet.x, packet.y, packet.z)
        }
        //#endif

        if (packet is C03PacketPlayer) {
            return packet.getLocation()
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
        val world = MinecraftCompat.localWorld
        if (packet is S14PacketEntity) {
            return packet.getEntity(world)
        }
        //#if MC < 1.21
        if (packet is S19PacketEntityHeadLook) {
            return packet.getEntity(world)
        }
        //#endif
        if (packet is S19PacketEntityStatus) {
            return packet.getEntity(world)
        }
        if (id != null) {
            return EntityUtils.getEntityByID(id)
        }

        return null
    }

    private fun Packet<*>.getEntityId() = when (this) {
        is S1CPacketEntityMetadata -> entityId
        is S20PacketEntityProperties -> entityId
        is S04PacketEntityEquipment -> entityID
        is S12PacketEntityVelocity -> entityID
        is S1BPacketEntityAttach -> entityId
        is S0BPacketAnimation -> entityID
        is S18PacketEntityTeleport -> entityId
        is S1DPacketEntityEffect -> entityId
        is S0CPacketSpawnPlayer -> entityID
        //#if MC < 1.21
        is S0FPacketSpawnMob -> entityID
        is S0EPacketSpawnObject -> entityID
        is S19PacketEntityHeadLook -> javaClass.getDeclaredField("entityId").makeAccessible().get(this) as Int
        //#endif
        is S19PacketEntityStatus -> javaClass.getDeclaredField("entityId").makeAccessible().get(this) as Int
        /* is S14PacketEntity.S15PacketEntityRelMove -> packet.javaClass.getDeclaredField("entityId").makeAccessible().get(packet) as Int
        is S14PacketEntity.S16PacketEntityLook -> packet.javaClass.getDeclaredField("entityId").makeAccessible().get(packet) as Int
        is S14PacketEntity.S17PacketEntityLookMove -> packet.javaClass.getDeclaredField("entityId").makeAccessible().get(packet) as Int */
        else -> null
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shtestpacket") {
            description = "Logs incoming and outgoing packets to the console"
            category = CommandCategory.DEVELOPER_TEST
            callback { command(it) }
        }
    }
}
