package at.hannibal2.skyhanni.features.rift.area.dreadfarm

import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.features.rift.everywhere.RiftAPI
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class RiftWiltedBerberisHelper {
    private val config get() = RiftAPI.config.area.dreadfarmConfig.wiltedBerberis

    private var currentParticles: LorenzVec? = null
    private var previous: LorenzVec? = null
    private var moving = true
    private var isOnFarmland = true
    private var playerY = 72.0

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (event.isMod(5)) {
            if (Minecraft.getMinecraft().thePlayer.onGround) {
                val block = LocationUtils.playerLocation().add(0, -1, 0).getBlockAt().toString()
                val currentY = LocationUtils.playerLocation().y
                isOnFarmland = block == "Block{minecraft:farmland}" && (currentY % 1 == 0.0)
                if (isOnFarmland) {
                    playerY = currentY
                }
            }
        }
    }

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return

        if (event.distanceToPlayer > 10) return

        if (event.type != EnumParticleTypes.FIREWORKS_SPARK) {
            if (config.hideparticles) {
                if (currentParticles != null) {
                    event.isCanceled = true
                }
            }
            return
        }

        currentParticles = if (hasFarmingWandInHand()) {
            null
        } else {
            if (config.hideparticles) {
                event.isCanceled = true
            }
            val isMoving = currentParticles != event.location
            if (isMoving) {
                currentParticles?.let {
                    if (it.distance(event.location) > 3) {
                        previous = null
                        moving = true
                    }
                }
                if (!moving) {
                    previous = currentParticles
                }
            }
            moving = isMoving

            event.location
        }
    }

    private fun hasFarmingWandInHand(): Boolean {
        val internalName = InventoryUtils.getItemInHand()?.getInternalName() ?: return false

        return internalName != "FARMING_WAND"
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!isEnabled()) return

        if (config.onlyOnFarmland && !isOnFarmland) return

        val location = currentParticles?.fixLocation() ?: return

        if (!moving) {
            event.drawWaypointFilled(location, LorenzColor.YELLOW.toColor())
            event.drawDynamicText(location, "§eWilted Berberis", 1.5)
        } else {
            event.drawWaypointFilled(location, LorenzColor.WHITE.toColor())
            previous?.let {
                event.drawWaypointFilled(location, LorenzColor.GRAY.toColor())
                event.draw3DLine(
                    it.fixLocation().add(0.5, 0.5, 0.5),
                    location.add(0.5, 0.5, 0.5),
                    Color.WHITE,
                    3,
                    false
                )
            }
        }
    }

    fun LorenzVec.fixLocation(): LorenzVec {
        val x = x - 0.5
        val y = playerY
        val z = z - 0.5
        return LorenzVec(x, y, z)
    }

    fun isEnabled() = RiftAPI.inRift() && RiftAPI.inDreadfarm() && config.enabled

}
