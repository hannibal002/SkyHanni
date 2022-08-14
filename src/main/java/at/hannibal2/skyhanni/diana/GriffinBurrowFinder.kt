package at.hannibal2.skyhanni.diana

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.test.GriffinUtils.draw3DLine
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypoint
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import java.util.*

class GriffinBurrowFinder {

    private var ticks = 0
    val list = mutableListOf<UUID>()
    var lastArrowLine: Line? = null

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent?) {
        if (!LorenzUtils.inSkyblock) return
        ticks++
        if (ticks % 5 == 0) {
            checkEntities()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        lastArrowLine = null
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        val message = event.message
        if (message.startsWith("§eYou dug out a Griffin Burrow!") ||
            message == "§eYou finished the Griffin burrow chain! §r§7(4/4)"
        ) {
            lastArrowLine = null
        }
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (lastArrowLine != null) {
            var start = lastArrowLine!!.start
            val y = (LocationUtils.playerLocation().y - 1)
            start = LorenzVec(start.x, y, start.z)
            val direction = lastArrowLine!!.direction

            event.drawWaypoint(start, LorenzColor.WHITE)
            val nextPoint = start.add(direction.multiply(10))
//            event.drawWaypoint(nextPoint, LorenzColor.YELLOW)

            event.draw3DLine(start, start.add(direction.multiply(400)), LorenzColor.YELLOW, 3, true)
        }
    }

    var lastHarpTime = 0L
    var lastHarpPitch = 0f
    var lastHarpDistance = 0.0

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        if (packet is S2APacketParticles) {
            val x = packet.xCoordinate
            val y = packet.yCoordinate
            val z = packet.zCoordinate
            val distance = LorenzVec(x, y, z).distance(LocationUtils.playerLocation())
            if (distance < 20) {
//                LorenzDebug.log("")
//                LorenzDebug.log("S2APacketParticles close")
//                var particleType = packet.particleType
//                var particleID = particleType.particleID
//                LorenzDebug.log("particleType: $particleType")
//                LorenzDebug.log("particleID: $particleID")
//                LorenzDebug.log("distance: $distance")
//                LorenzDebug.log("")

            } else {
//                LorenzDebug.log("S2APacketParticles far")
            }
        }
        if (packet is S29PacketSoundEffect) {
            val x = packet.x
            val y = packet.y
            val z = packet.z
            val distance = LorenzVec(x, y, z).distance(LocationUtils.playerLocation())
            if (distance < 20) {
                val soundName = packet.soundName
                val pitch = packet.pitch
                val volume = packet.volume
                if (soundName == "game.player.hurt" && volume == 0f) return

                if (soundName == "note.harp") {

                    LorenzDebug.log("harp pitch: $pitch")
                    LorenzDebug.log("distance: $distance")
                    val now = System.currentTimeMillis()
                    if (lastHarpTime != 0L) {
                        LorenzDebug.log("")
                        val diffTime = now - lastHarpTime
                        LorenzDebug.log("diffTime: $diffTime")
                        val diffPitch = pitch - lastHarpPitch
                        LorenzDebug.log("diffPitch: $diffPitch")
                        val diffDistance = distance - lastHarpDistance
                        LorenzDebug.log("diffDistance: $diffDistance")
                    }
                    lastHarpTime = now
                    lastHarpPitch = pitch
                    lastHarpDistance = distance
                    LorenzDebug.log("")
                    return
                }

                LorenzDebug.log("")
                LorenzDebug.log("S29PacketSoundEffect close")

                LorenzDebug.log("soundName: $soundName")
                LorenzDebug.log("pitch: $pitch")
                LorenzDebug.log("volume: $volume")
                LorenzDebug.log("")
            } else {
//                LorenzDebug.log("S29PacketSoundEffect far")
            }
        }
    }

    private fun checkEntities() {
        val playerLocation = LocationUtils.playerLocation()
        for (entity in Minecraft.getMinecraft().theWorld.loadedEntityList) {
            if (list.contains(entity.uniqueID)) continue
            if (entity !is EntityArmorStand) continue
            val distance = entity.getLorenzVec().distance(playerLocation)
            if (distance > 10) continue


            val itemStack = entity.inventory[0] ?: continue
            if (itemStack.cleanName() != "Arrow") continue

            val rotationYaw = entity.rotationYaw
            val direction = LorenzVec.getFromYawPitch(rotationYaw.toDouble(), 0.0)

            lastArrowLine = Line(entity.getLorenzVec(), direction)
            list.add(entity.uniqueID)
            LorenzDebug.log("distance: $distance")
        }
    }

    class Line(val start: LorenzVec, val direction: LorenzVec)
}