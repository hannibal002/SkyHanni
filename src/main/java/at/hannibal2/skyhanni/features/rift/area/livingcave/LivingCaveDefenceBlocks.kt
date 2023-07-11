package at.hannibal2.skyhanni.features.rift.area.livingcave

import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.features.rift.everywhere.RiftAPI
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLiving
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class LivingCaveDefenceBlocks {
    val config get() = RiftAPI.config.area.livingCaveConfig
    private var movingBlocks = mapOf<LorenzVec, Long>()
    private var staticBlocks = emptyList<LorenzVec>()

    class DefenceBlock(val entity: EntityLiving, val location: LorenzVec)

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return

        Minecraft.getMinecraft().thePlayer?.let {
            if (it.isSneaking) {
                staticBlocks = emptyList()
            }
        }

        movingBlocks = movingBlocks.editCopy {
            values.removeIf { System.currentTimeMillis() > it }
            keys.removeIf { staticBlocks.any { others -> others.distance(it) < 1.5 } }
        }

        val location = event.location.add(-0.5, 0.0, -0.5)
//        if (staticBlocks.any { it.distance(location) < 2.5 }) {
        if (staticBlocks.any { it.distance(location) < 3 }) {
            event.isCanceled = true
            return
        }

        if (event.type == EnumParticleTypes.CRIT_MAGIC) {
//            movingBlocks.keys.find { it.distance(location) < 0.3 }?.let {
            movingBlocks.keys.find { it.distance(location) < 0.5 }?.let {
                movingBlocks = movingBlocks.editCopy { remove(it) }
            }

            movingBlocks = movingBlocks.editCopy { this[location] = System.currentTimeMillis() + 500 }
            event.isCanceled = true
        }
    }

    // TODO move somewhere else
    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet

        if (packet is S23PacketBlockChange) {
            ServerBlockChangeEvent(packet.blockPosition, packet.blockState).postAndCatch()
        } else if (packet is S22PacketMultiBlockChange) {
            for (block in packet.changedBlocks) {
                ServerBlockChangeEvent(block.pos, block.blockState).postAndCatch()
            }
        }
    }

    @SubscribeEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!isEnabled()) return
        val location = event.location
        val old = event.old
        val new = event.new
        val distanceToPlayer = location.distanceToPlayer()

        if (old == "air" && (new == "stained_glass" || new == "diamond_block")) {
            println("detect spawn: ${distanceToPlayer.round(1)}")
            staticBlocks = staticBlocks.editCopy { add(location) }
        } else if (new == "air" && location in staticBlocks) {
            println("detect despawn: ${distanceToPlayer.round(1)}")
            staticBlocks = staticBlocks.editCopy { remove(location) }
        } else {
//            if (distanceToPlayer < 3) {
            if (distanceToPlayer < 10) {
//                if (old == "lapis_ore" || new == "lapis_ore") {
//                    println("block change: $old -> $new")
//                }
                if (old == "wool") return
                if (new == "wool") return
                if (old == "lapis_block") return
                if (new == "lapis_block") return
                if (old == "stained_glass" && new == "stone") return
                if (old == "stone" && new == "stained_glass") return
                if (old == "stained_glass" && new == "stained_hardened_clay") return
//                println("block change: $old -> $new")
            }
        }
//            if (old.contains("air") && new.contains("diamond_block")) {
//                println("detect big spawn: ${distanceToPlayer.round(1)}")
//            }
//            if (old.contains("diamond_block") && new.contains("air")) {
//                println("detect big despawn: ${distanceToPlayer.round(1)}")
//            }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!isEnabled()) return


        for (location in movingBlocks.keys) {
            event.drawWaypointFilled(location, LorenzColor.WHITE.toColor())
            event.drawDynamicText(location, "Defense Block", 1.5)
        }
        for (location in staticBlocks) {
            event.drawWaypointFilled(location, LorenzColor.WHITE.toColor())
            event.drawDynamicText(location, "Defense Block", 1.5)
        }
    }

    fun isEnabled() = RiftAPI.inRift() && config.defenceBlocks && RiftAPI.inLivingCave()
}
