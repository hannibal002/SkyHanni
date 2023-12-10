package at.hannibal2.skyhanni.features.rift.area.dreadfarm

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox_nea
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

class RiftWiltedBerberisHelper {
    private val config get() = RiftAPI.config.area.dreadfarm.wiltedBerberis
    private var isOnFarmland = false
    private var hasFarmingToolInHand = false
    private var listBerberisParticle = listOf<WiltedBerberisParticle>()

    private var mapBerberisSequence = mutableMapOf<LorenzVec, WiltedBerberisSequence>()

    // Test purposes, Map center of field to amount of wilted berberis for each field
    // -29 71 -179: 12
    // -65 72 -184: 15
    // -79 72 -162: 19
    // -69 71 -134: 21
    // -33 70 -140: 36
    private val mapBerberis = mutableMapOf(
        LorenzVec(-29, 71, -179) to 12,
        LorenzVec(-65, 72, -184) to 15,
        LorenzVec(-79, 72, -162) to 19,
        LorenzVec(-69, 71, -134) to 21,
        LorenzVec(-33, 70, -140) to 36
    )
    //

    class WiltedBerberisParticle(var currentParticles: LorenzVec) {
        var previous: LorenzVec? = null
        var moving = true
        var y = 0.0
        var lastTime = System.currentTimeMillis()
    }

    class WiltedBerberisSequence {
        var listBerberisSequence = mutableListOf<LorenzVec>()
        var sequenceValid = false
        var away = false
        var renderSequence = false
        var timeSinceLastParticle = SimpleTimeMark(0)
        var currentPositionInSequence = 0

        fun next() {
            currentPositionInSequence++

            if (currentPositionInSequence >= listBerberisSequence.size) {
                reset()
            }
        }

        fun reset() {
            sequenceValid = false
            listBerberisSequence.clear()
            timeSinceLastParticle = SimpleTimeMark(0)
            currentPositionInSequence = 0
        }

        fun invalidate() {
            reset()
            LorenzUtils.chat("Current Berberis Sequence is invalid, falling back to using particles", prefixColor = "Â§c")
            renderSequence = false
        }

        fun locationEqualsCurrent(other: Any?): Boolean {
            if (other !is LorenzVec) return false
            return listBerberisSequence[currentPositionInSequence].roundLocationToBlock() == other.roundLocationToBlock()
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return

        listBerberisParticle = listBerberisParticle.editCopy { removeIf { System.currentTimeMillis() > it.lastTime + 500 } }

        hasFarmingToolInHand = InventoryUtils.getItemInHand()?.getInternalName() == RiftAPI.farmingTool

        if (Minecraft.getMinecraft().thePlayer.onGround) {
            val block = LocationUtils.playerLocation().add(y = -1).getBlockAt().toString()
            val currentY = LocationUtils.playerLocation().y
            isOnFarmland = block == "Block{minecraft:farmland}" && (currentY % 1 == 0.0)
        }

        val (nearestCenter, berberisSequence) = mapBerberisSequence.nearestBerberisSequenceOrNull(LocationUtils.playerLocation(), false) ?: return

        with(berberisSequence) {
            val distanceToPlayer = nearestCenter.distanceToPlayer()
            if ((!away && distanceToPlayer > 20) || (away && distanceToPlayer < 20)) {
                away = !away

                if (away) return

                var check = "Block{minecraft:air}"
                for ((index, berberis) in listBerberisSequence.withIndex()) {
                    if (index == currentPositionInSequence) check = "Block{minecraft:deadbush}"

                    if (berberis.getBlockAt().toString() != check) {
                        invalidate()
                        return
                    }
                }
            }
        }
    }

    private fun nearestBerberisParticle(location: LorenzVec): WiltedBerberisParticle? {
        return listBerberisParticle.filter { it.currentParticles.distanceSq(location) < 8 }
            .minByOrNull { it.currentParticles.distanceSq(location) }
    }

    private fun MutableMap<LorenzVec, Int>.nearestFieldCenterOrNull(location: LorenzVec): LorenzVec? {
        return keys.minByOrNull { it.distanceSq(location) }
    }

    private fun MutableMap<LorenzVec, WiltedBerberisSequence>.nearestBerberisSequenceOrNull(location: LorenzVec, filter: Boolean = true): Pair<LorenzVec, WiltedBerberisSequence>? {
        return filter{ (!it.value.away && it.value.sequenceValid) || !filter }
            .minByOrNull { it.key.distanceSq(location) }?.toPair()
    }

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (!hasFarmingToolInHand) return

        val location = event.location
        val berberis = nearestBerberisParticle(location)
        val block = location.add(y = -1).getBlockAt().toString()
        val nearestCenter = mapBerberis.nearestFieldCenterOrNull(location)

        if (config.respawnSequence && location.distanceToPlayer() < 40 && event.type == EnumParticleTypes.VILLAGER_HAPPY && block == "Block{minecraft:farmland}" && nearestCenter != null) {
            val berberisSequence = mapBerberisSequence[nearestCenter] ?: WiltedBerberisSequence()

            with (berberisSequence) {
                if (timeSinceLastParticle.passedSince() > 3.seconds) reset()

                listBerberisSequence.add(location)
                timeSinceLastParticle = SimpleTimeMark.now()

                sequenceValid = listBerberisSequence.size == mapBerberis[nearestCenter]
                renderSequence = sequenceValid
            }

            mapBerberisSequence[nearestCenter] = berberisSequence
        }

        if (event.type != EnumParticleTypes.FIREWORKS_SPARK) {
            if (config.hideParticles && berberis != null) {
                event.isCanceled = true
            }
            return
        }

        if (config.hideParticles) {
            event.isCanceled = true
        }

        if (berberis == null) {
            listBerberisParticle = listBerberisParticle.editCopy { add(WiltedBerberisParticle(location)) }
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

            if (!moving && location.getBlockAt().toString() == "Block{minecraft:deadbush}" && nearestCenter != null) {
                val berberisSequence = mapBerberisSequence[nearestCenter] ?: return
                if (!berberisSequence.locationEqualsCurrent(location)) berberisSequence.invalidate()
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (!hasFarmingToolInHand) return
        if (config.onlyOnFarmland && !isOnFarmland) return

        val seqPair = mapBerberisSequence.nearestBerberisSequenceOrNull(LocationUtils.playerLocation(), false)

        if (config.respawnSequence && seqPair != null && seqPair.second.renderSequence) {
            val (nearestCenter, berberisSequence) = seqPair

            // TODO replace logic with ParkourHelper
            if (berberisSequence.away || !berberisSequence.sequenceValid) return

            with (berberisSequence) {
                if (listBerberisSequence[currentPositionInSequence].distanceToPlayer() > 20) return

                for ((index, location) in listBerberisSequence.withIndex().drop(currentPositionInSequence).take(2)) {
                    when (index) {
                        currentPositionInSequence -> {
                            event.drawFilledBoundingBox_nea(axisAlignedBB(location.roundLocationToBlock()), Color.GREEN, 0.7f)
                            event.drawDynamicText(location.add(x = -0.5, y = 1.0, z = -0.5), "Â§aWilted Berberis", 1.5, ignoreBlocks = false)
                        }
                        currentPositionInSequence + 1 -> {
                            event.drawFilledBoundingBox_nea(axisAlignedBB(location.roundLocationToBlock()), Color.YELLOW, 0.7f)
                            event.draw3DLine(location, listBerberisSequence[index - 1], Color.YELLOW, 3, false)
                        }
                        currentPositionInSequence + 2 -> {
                            event.drawFilledBoundingBox_nea(axisAlignedBB(location.roundLocationToBlock()), Color.RED, 0.7f)
                            event.draw3DLine(location, listBerberisSequence[index - 1], Color.RED, 3, false)
                        }
                    }
                }
            }
        } else {
            for (berberis in listBerberisParticle) {
                with(berberis) {
                    if (currentParticles.distanceToPlayer() > 20) continue
                    if (y == 0.0) continue

                    val location = currentParticles.fixLocation(berberis)
                    if (!moving) {
                        event.drawFilledBoundingBox_nea(axisAlignedBB(location), Color.YELLOW, 0.7f)
                        event.drawDynamicText(location.add(y = 1), "Â§eWilted Berberis", 1.5, ignoreBlocks = false)
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
    }

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return
        if (!hasFarmingToolInHand) return
        if (event.clickType != ClickType.LEFT_CLICK) return

        val location = event.position
        if (location.getBlockAt().toString() != "Block{minecraft:deadbush}") return

        val berberisSequence = mapBerberisSequence.nearestBerberisSequenceOrNull(location)?.second ?: return
        if (berberisSequence.locationEqualsCurrent(location)) berberisSequence.next()
    }

    @SubscribeEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!isEnabled()) return

        val location = event.location

        if (event.old != "deadbush" || event.new != "air") return

        val berberisSequence = mapBerberisSequence.nearestBerberisSequenceOrNull(location)?.second ?: return
        if (berberisSequence.locationEqualsCurrent(location)) berberisSequence.next()
    }

    private fun axisAlignedBB(loc: LorenzVec) = loc.add(0.1, -0.1, 0.1).boundingToOffset(0.8, 1.0, 0.8).expandBlock()

    private fun LorenzVec.fixLocation(wiltedBerberisParticle: WiltedBerberisParticle): LorenzVec {
        val x = x - 0.5
        val y = wiltedBerberisParticle.y
        val z = z - 0.5
        return LorenzVec(x, y, z)
    }

    private fun isEnabled() = RiftAPI.inRift() && RiftAPI.inDreadfarm() && config.enabled

}
