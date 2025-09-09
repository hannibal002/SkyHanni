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
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket
import net.minecraft.network.packet.s2c.play.EntityS2CPacket
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket
import net.minecraft.network.packet.s2c.play.ExperienceBarUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import net.minecraft.network.packet.s2c.play.SignEditorOpenS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.TeamS2CPacket
import net.minecraft.network.packet.s2c.play.EntityS2CPacket.MoveRelative as EntityRelMove
import net.minecraft.network.packet.s2c.play.EntityS2CPacket.Rotate as EntityLook
import net.minecraft.network.packet.s2c.play.EntityS2CPacket.RotateAndMoveRelative as EntityLookMove
//#if MC < 1.21
//$$ import net.minecraft.network.packet.c2s.play.ConfirmScreenActionC2SPacket
//$$ import net.minecraft.network.packet.s2c.play.ConfirmScreenActionS2CPacket
//$$ import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
//$$ import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket
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

        if (packetName == KeepAliveC2SPacket::class.simpleName) return
        //#if MC < 1.21
        //$$ if (packetName == ConfirmScreenActionC2SPacket::class.simpleName) return
        //#endif
        if (packetName == PositionAndOnGround::class.simpleName) return

        if (packetName == UpdateSelectedSlotC2SPacket::class.simpleName) return
        if (packetName == Full::class.simpleName) return
        if (packetName == ClientCommandC2SPacket::class.simpleName) return
        if (packetName == LookAndOnGround::class.simpleName) return
        //#if MC > 1.21
        if (packetName == net.minecraft.network.packet.c2s.common.CommonPongC2SPacket::class.simpleName) return
        if (packetName == net.minecraft.network.packet.c2s.play.ClientTickEndC2SPacket::class.simpleName) return
        //#endif
        if (packetName == PlayerMoveC2SPacket::class.simpleName) return

        println("Send: [$packetName]")
    }

    @HandleEvent(priority = HandleEvent.LOW, receiveCancelled = true)
    fun onPacketReceive(event: PacketReceivedEvent) {
        if (!enabled) return
        val packet = event.packet
        packet.print()
        if (packet is EntitiesDestroyS2CPacket) {
            packet.entityIds.forEach {
                entityMap.getOrDefault(it, mutableListOf()).add(packet)
            }
        } else {
            val id = try {
                val field = packet.javaClass.getDeclaredField("entityId")
                field.makeAccessible()
                field.get(packet)
            } catch (e: NoSuchFieldException) {
                null
            } ?: return
            entityMap.getOrDefault(id, mutableListOf()).add(packet)
        }
    }

    private fun Packet<*>.print() {
        val packetName = javaClass.simpleName

        // Keep alive
        if (packetName == KeepAliveS2CPacket::class.simpleName) return
        if (packetName == KeepAliveC2SPacket::class.simpleName) return
        //#if MC < 1.21
        //$$ if (packetName == ConfirmScreenActionS2CPacket::class.simpleName) return
        //#else
        if (packetName == net.minecraft.network.packet.s2c.common.CommonPingS2CPacket::class.simpleName) return
        if (packetName == net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket::class.simpleName) return
        //#endif

        // Gui
        if (packetName == ScoreboardObjectiveUpdateS2CPacket::class.simpleName) return
        if (packetName == TeamS2CPacket::class.simpleName) return
        if (packetName == PlayerListS2CPacket::class.simpleName) return
        if (packetName == ScoreboardScoreUpdateS2CPacket::class.simpleName) return
        if (packetName == ExperienceBarUpdateS2CPacket::class.simpleName) return
        if (packetName == HealthUpdateS2CPacket::class.simpleName) return

        // Block & World
        if (packetName == SignEditorOpenS2CPacket::class.simpleName) return
        if (packetName == WorldTimeUpdateS2CPacket::class.simpleName) return
        if (packetName == ChunkDataS2CPacket::class.simpleName) return
        if (packetName == ChunkDeltaUpdateS2CPacket::class.simpleName) return
        if (packetName == BlockUpdateS2CPacket::class.simpleName) return
        //#if MC > 1.21
        if (packetName == net.minecraft.network.packet.s2c.play.BlockEventS2CPacket::class.simpleName) return
        if (packetName == net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket::class.simpleName) return
        if (packetName == net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket::class.simpleName) return
        //#endif

        // Chat
        if (packetName == GameMessageS2CPacket::class.simpleName) return

        // Others
        if (packetName == PlaySoundS2CPacket::class.simpleName) return
        if (!full && packetName == ParticleS2CPacket::class.simpleName) return
        //#if MC > 1.21
        if(packetName == net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket::class.simpleName) return
        //#endif

        // Entity
        if (this is EntitiesDestroyS2CPacket) {
            println("Receive: [$packetName] with IDs: ${entityIds.joinToString(", ")}")
            return
        }

        if (!full) {
            if (packetName == EntityPositionS2CPacket::class.simpleName) return
            if (packetName == EntityEquipmentUpdateS2CPacket::class.simpleName) return
            if (packetName == EntityRelMove::class.simpleName) return
            if (packetName == EntityLookMove::class.simpleName) return
            if (packetName == EntitySetHeadYawS2CPacket::class.simpleName) return
            if (packetName == EntityLook::class.simpleName) return
            //#if MC > 1.21
            if(packetName == net.minecraft.network.packet.s2c.play.BossBarS2CPacket::class.simpleName) return
            if(packetName == net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket::class.simpleName) return
            //#endif
            if (packetName == EntityVelocityUpdateS2CPacket::class.simpleName) return
            if (packetName == EntityTrackerUpdateS2CPacket::class.simpleName) return
            if (packetName == EntityAttributesS2CPacket::class.simpleName) return
            if (packetName == EntityAnimationS2CPacket::class.simpleName) return
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
        if (packet is ParticleS2CPacket) {
            return LorenzVec(packet.x, packet.y, packet.z)
        }

        if (packet is EntitySpawnS2CPacket) {
            return LorenzVec(packet.x, packet.y, packet.z)
        }
        //#if MC < 1.21
        //$$ if (packet is EntitySpawnS2CPacket) {
        //$$     return LorenzVec(packet.x, packet.y, packet.z)
        //$$ }
        //$$ if (packet is MobSpawnS2CPacket) {
        //$$     return LorenzVec(packet.x, packet.y, packet.z)
        //$$ }
        //#endif

        if (packet is PlayerMoveC2SPacket) {
            return packet.getLocation()
        }
        if (packet is WorldEventS2CPacket) {
            return packet.pos.toLorenzVec()
        }

        if (entity != null) {
            return entity.getLorenzVec()
        }

        return null
    }

    private fun getEntity(packet: Packet<*>, id: Int?): Entity? {
        val world = MinecraftCompat.localWorld
        if (packet is EntityS2CPacket) {
            return packet.getEntity(world)
        }
        //#if MC < 1.21
        //$$ if (packet is EntitySetHeadYawS2CPacket) {
        //$$     return packet.getEntity(world)
        //$$ }
        //#endif
        if (packet is EntityStatusS2CPacket) {
            return packet.getEntity(world)
        }
        if (id != null) {
            return EntityUtils.getEntityByID(id)
        }

        return null
    }

    private fun Packet<*>.getEntityId() = when (this) {
        is EntityTrackerUpdateS2CPacket -> id()
        is EntityAttributesS2CPacket -> entityId
        is EntityEquipmentUpdateS2CPacket -> entityId
        is EntityVelocityUpdateS2CPacket -> entityId
        is EntityAttachS2CPacket -> attachedEntityId
        is EntityAnimationS2CPacket -> entityId
        is EntityPositionS2CPacket -> entityId()
        is EntityStatusEffectS2CPacket -> entityId
        is EntitySpawnS2CPacket -> entityId
        //#if MC < 1.21
        //$$ is MobSpawnS2CPacket -> id
        //$$ is EntitySpawnS2CPacket -> id
        //$$ is EntitySetHeadYawS2CPacket -> javaClass.getDeclaredField("entityId").makeAccessible().get(this) as Int
        //#endif
        is EntityStatusS2CPacket -> javaClass.getDeclaredField("entityId").makeAccessible().get(this) as Int
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
