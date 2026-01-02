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
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.entity.Entity
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Pos as EntityRelMove
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.PosRot as EntityLookMove
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket.Rot as EntityLook

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

        if (packetName == ServerboundKeepAlivePacket::class.simpleName) return
        if (packetName == Pos::class.simpleName) return

        if (packetName == ServerboundSetCarriedItemPacket::class.simpleName) return
        if (packetName == PosRot::class.simpleName) return
        if (packetName == ServerboundPlayerCommandPacket::class.simpleName) return
        if (packetName == Rot::class.simpleName) return
        if (packetName == net.minecraft.network.protocol.common.ServerboundPongPacket::class.simpleName) return
        if (packetName == net.minecraft.network.protocol.game.ServerboundClientTickEndPacket::class.simpleName) return
        if (packetName == ServerboundMovePlayerPacket::class.simpleName) return

        println("Send: [$packetName]")
    }

    @HandleEvent(priority = HandleEvent.LOW, receiveCancelled = true)
    fun onPacketReceive(event: PacketReceivedEvent) {
        if (!enabled) return
        val packet = event.packet
        packet.print()
        if (packet is ClientboundRemoveEntitiesPacket) {
            packet.entityIds.forEach {
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
        if (packetName == ClientboundKeepAlivePacket::class.simpleName) return
        if (packetName == ServerboundKeepAlivePacket::class.simpleName) return
        if (packetName == net.minecraft.network.protocol.common.ClientboundPingPacket::class.simpleName) return
        if (packetName == net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket::class.simpleName) return

        // Gui
        if (packetName == ClientboundSetObjectivePacket::class.simpleName) return
        if (packetName == ClientboundSetPlayerTeamPacket::class.simpleName) return
        if (packetName == ClientboundPlayerInfoUpdatePacket::class.simpleName) return
        if (packetName == ClientboundSetScorePacket::class.simpleName) return
        if (packetName == ClientboundSetExperiencePacket::class.simpleName) return
        if (packetName == ClientboundSetHealthPacket::class.simpleName) return

        // Block & World
        if (packetName == ClientboundOpenSignEditorPacket::class.simpleName) return
        if (packetName == ClientboundSetTimePacket::class.simpleName) return
        if (packetName == ClientboundLevelChunkWithLightPacket::class.simpleName) return
        if (packetName == ClientboundSectionBlocksUpdatePacket::class.simpleName) return
        if (packetName == ClientboundBlockUpdatePacket::class.simpleName) return
        if (packetName == net.minecraft.network.protocol.game.ClientboundBlockEventPacket::class.simpleName) return
        if (packetName == net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket::class.simpleName) return
        if (packetName == net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket::class.simpleName) return

        // Chat
        if (packetName == ClientboundSystemChatPacket::class.simpleName) return

        // Others
        if (packetName == ClientboundSoundPacket::class.simpleName) return
        if (!full && packetName == ClientboundLevelParticlesPacket::class.simpleName) return
        if(packetName == net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket::class.simpleName) return

        // Entity
        if (this is ClientboundRemoveEntitiesPacket) {
            println("Receive: [$packetName] with IDs: ${entityIds.joinToString(", ")}")
            return
        }

        if (!full) {
            if (packetName == ClientboundTeleportEntityPacket::class.simpleName) return
            if (packetName == ClientboundSetEquipmentPacket::class.simpleName) return
            if (packetName == EntityRelMove::class.simpleName) return
            if (packetName == EntityLookMove::class.simpleName) return
            if (packetName == ClientboundRotateHeadPacket::class.simpleName) return
            if (packetName == EntityLook::class.simpleName) return
            if(packetName == net.minecraft.network.protocol.game.ClientboundBossEventPacket::class.simpleName) return
            if(packetName == net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket::class.simpleName) return
            if (packetName == ClientboundSetEntityMotionPacket::class.simpleName) return
            if (packetName == ClientboundSetEntityDataPacket::class.simpleName) return
            if (packetName == ClientboundUpdateAttributesPacket::class.simpleName) return
            if (packetName == ClientboundAnimatePacket::class.simpleName) return
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
        if (packet is ClientboundLevelParticlesPacket) {
            return LorenzVec(packet.x, packet.y, packet.z)
        }

        if (packet is ClientboundAddEntityPacket) {
            return LorenzVec(packet.x, packet.y, packet.z)
        }

        if (packet is ServerboundMovePlayerPacket) {
            return LorenzVec(packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0))
        }
        if (packet is ClientboundLevelEventPacket) {
            return packet.pos.toLorenzVec()
        }

        if (entity != null) {
            return entity.getLorenzVec()
        }

        return null
    }

    private fun getEntity(packet: Packet<*>, id: Int?): Entity? {
        val world = MinecraftCompat.localWorld
        if (packet is ClientboundMoveEntityPacket) {
            return packet.getEntity(world)
        }
        if (packet is ClientboundEntityEventPacket) {
            return packet.getEntity(world)
        }
        if (id != null) {
            return EntityUtils.getEntityByID(id)
        }

        return null
    }

    private fun Packet<*>.getEntityId() = try {
        when (this) {
            is ClientboundSetEntityDataPacket -> id()
            is ClientboundUpdateAttributesPacket -> entityId
            is ClientboundSetEquipmentPacket -> entity
            is ClientboundSetEntityMotionPacket -> id
            is ClientboundSetEntityLinkPacket -> sourceId
            is ClientboundAnimatePacket -> id
            is ClientboundTeleportEntityPacket -> id()
            is ClientboundUpdateMobEffectPacket -> entityId
            is ClientboundAddEntityPacket -> id
            is ClientboundEntityEventPacket ->
                javaClass.getDeclaredField("entityId").makeAccessible().get(this) as Int
            /* is S14PacketEntity.S15PacketEntityRelMove ->
                packet.javaClass.getDeclaredField("entityId").makeAccessible().get(packet) as Int
            is S14PacketEntity.S16PacketEntityLook ->
                packet.javaClass.getDeclaredField("entityId").makeAccessible().get(packet) as Int
            is S14PacketEntity.S17PacketEntityLookMove ->
                packet.javaClass.getDeclaredField("entityId").makeAccessible().get(packet) as Int */
            else -> null
        }
    } catch (e: NoSuchFieldException) {
        null
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
