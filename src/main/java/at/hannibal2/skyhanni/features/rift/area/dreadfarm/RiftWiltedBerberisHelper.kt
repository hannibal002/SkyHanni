package at.hannibal2.skyhanni.features.rift.area.dreadfarm

import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class RiftWiltedBerberisHelper {
    private val config get() = RiftAPI.config.area.dreadfarm.wiltedBerberis
    private var isOnFarmland = false
    private var hasFarmingToolInHand = false
    private var list = listOf<WiltedBerberis>()

    class WiltedBerberis(var currentParticles: LorenzVec) {
        var previous: LorenzVec? = null
        var moving = true
        var y = 0.0
        var lastTime = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return

        list = list.editCopy { removeIf { System.currentTimeMillis() > it.lastTime + 500 } }

        hasFarmingToolInHand = InventoryUtils.getItemInHand()?.getInternalName() == RiftAPI.farmingTool

        if (Minecraft.getMinecraft().thePlayer.onGround) {
            val block = LocationUtils.playerLocation().add(y = -1).getBlockAt()
            val currentY = LocationUtils.playerLocation().y
            isOnFarmland = block == Blocks.farmland && (currentY % 1 == 0.0)
        }
    }

    private fun nearestBerberis(location: LorenzVec): WiltedBerberis? {
        return list.filter { it.currentParticles.distanceSq(location) < 8 }
            .minByOrNull { it.currentParticles.distanceSq(location) }
    }

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (!hasFarmingToolInHand) return

        val location = event.location
        val berberis = nearestBerberis(location)

        if (event.type != EnumParticleTypes.FIREWORKS_SPARK) {
            if (config.hideparticles && berberis != null) {
                event.isCanceled = true
            }
            return
        }

        if (config.hideparticles) {
            event.isCanceled = true
        }

        if (berberis == null) {
            list = list.editCopy { add(WiltedBerberis(location)) }
            return
        }

        with(berberis) {
            val isMoving = currentParticles != location
            if (isMoving) {
                if (currentParticles.distance(location) > 3) {
                    previous = null
                    moving = true
                }
                if (!moving) {
                    previous = currentParticles
                }
            }
            if (!isMoving) {
                y = location.y - 1
            }

            moving = isMoving
            currentParticles = location
            lastTime = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (!hasFarmingToolInHand) return

        if (config.onlyOnFarmland && !isOnFarmland) return

        for (berberis in list) {
            with(berberis) {
                if (currentParticles.distanceToPlayer() > 20) continue
                if (y == 0.0) continue

                val location = currentParticles.fixLocation(berberis)
                if (!moving) {
                    event.drawFilledBoundingBox_nea(axisAlignedBB(location), Color.YELLOW, 0.7f)
                    event.drawDynamicText(location.add(y = 1), "§eWilted Berberis", 1.5, ignoreBlocks = false)
                } else {
                    event.drawFilledBoundingBox_nea(axisAlignedBB(location), Color.WHITE, 0.5f)
                    previous?.fixLocation(berberis)?.let {
                        event.drawFilledBoundingBox_nea(axisAlignedBB(it), Color.LIGHT_GRAY, 0.2f)
                        event.draw3DLine(it.add(0.5, 0.0, 0.5), location.add(0.5, 0.0, 0.5), Color.WHITE, 3, false)
                    }
                }
            }
        }
    }

    private fun axisAlignedBB(loc: LorenzVec) = loc.add(0.1, -0.1, 0.1).boundingToOffset(0.8, 1.0, 0.8).expandBlock()

    private fun LorenzVec.fixLocation(wiltedBerberis: WiltedBerberis): LorenzVec {
        val x = x - 0.5
        val y = wiltedBerberis.y
        val z = z - 0.5
        return LorenzVec(x, y, z)
    }

    private fun isEnabled() = RiftAPI.inRift() && RiftAPI.inDreadfarm() && config.enabled

}
