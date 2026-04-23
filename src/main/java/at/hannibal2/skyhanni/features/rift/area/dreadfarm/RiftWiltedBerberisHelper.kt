package at.hannibal2.skyhanni.features.rift.area.dreadfarm

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.WiltedBerberisLocationsJson
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.expandBlock
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object RiftWiltedBerberisHelper {

    private val config get() = RiftApi.config.area.dreadfarm.wiltedBerberis

    private val berberisSounds = setOf("entity.donkey.death", "entity.donkey.hurt")

    // NOTE: Do not make this a set, it breaks identity checks
    private val list = mutableListOf<WiltedBerberis>()

    private var isOnFarmland = false
    private var hasFarmingToolInHand = false

    // Maps each field's center position to the number of Wilted Berberis in that field.
    private var fieldCenters = mapOf<LorenzVec, Int>()
    private var fieldSequences = mapOf<LorenzVec, BerberisSequence>()

    data class WiltedBerberis(var currentParticles: LorenzVec) {
        var previous: LorenzVec? = null
        var moving = true
        var y = 0.0
        var lastTime = SimpleTimeMark.now()
    }

    private class BerberisSequence(fieldCenter: LorenzVec) {
        val expectedCount: Int = fieldCenters.getValue(fieldCenter)
        val sequence = mutableListOf<LorenzVec>()
        var isValid = false
        var isRendering = false
        var isAway = false
        var currentIndex = 0
        var previousTarget: LorenzVec? = null
        var lastParticleTime = SimpleTimeMark.farPast()

        fun reset() {
            sequence.clear()
            isValid = false
            isRendering = false
            currentIndex = 0
            previousTarget = null
            lastParticleTime = SimpleTimeMark.farPast()
        }

        fun invalidate() {
            reset()
            ChatUtils.chat("Berberis sequence invalid, falling back to particle mode", prefixColor = "§c")
        }

        fun advance() {
            val current = currentTarget
            currentIndex++
            if (currentIndex >= sequence.size) {
                if (isValid) {
                    // Full cycle
                    val wasRendering = isRendering
                    reset()
                    isRendering = wasRendering
                } else {
                    // Partial sequence
                    reset()
                }
            } else {
                previousTarget = current
            }
        }

        val currentTarget get() = sequence.getOrNull(currentIndex)
        val nextTarget get() = sequence.getOrNull(currentIndex + 1)
        val thirdTarget get() = sequence.getOrNull(currentIndex + 2)

        fun isAtCurrent(location: LorenzVec) =
            currentTarget?.roundToBlock() == location.roundToBlock()
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        fieldCenters = event.getConstant<WiltedBerberisLocationsJson>("rift/WiltedBerberisLocations")
            .fieldCenters.associate { it.position to it.count }
        fieldSequences = fieldCenters.keys.associateWith { BerberisSequence(it) }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return

        list.removeIf { it.lastTime.passedSince() > 500.milliseconds }

        hasFarmingToolInHand = InventoryUtils.getItemInHand()?.getInternalName() == RiftApi.farmingTool

        if (PlayerUtils.onGround()) {
            isOnFarmland = LocationUtils.getBlockBelowPlayer().getBlockAt() == Blocks.FARMLAND
        }

        // TODO: when warping back into the lobby after only seeing a partial sequence and the first being broken, the sequence is stuck
        //  with invalid renders, could not recreate
        if (!config.respawnSequence) return
        for ((center, seq) in fieldSequences) {
            if (seq.sequence.isEmpty()) continue
            updateFieldProximity(center, seq)
        }
    }

    private fun updateFieldProximity(center: LorenzVec, seq: BerberisSequence) {
        val dist = center.distanceIgnoreY(LocationUtils.playerLocation())
        if (dist > 20.0) {
            seq.isAway = true
            return
        }
        if (seq.isAway) {
            seq.isAway = false
            if (seq.isValid) validateSequence(seq) else validatePartialSequence(seq)
        }
    }

    private fun nearestBerberis(location: LorenzVec): WiltedBerberis? =
        list.filter { it.currentParticles.distanceSq(location) < 8 }
            .minByOrNull { it.currentParticles.distanceSq(location) }

    private fun nearestFieldCenter(location: LorenzVec, maxDistance: Double = 50.0): LorenzVec? =
        fieldCenters.keys
            .filter { it.distanceIgnoreY(location) < maxDistance }
            .minByOrNull { it.distanceIgnoreY(location) }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (!isEnabled()) return
        if (!hasFarmingToolInHand) return

        val location = event.location
        val berberis = nearestBerberis(location)

        if (event.type != ParticleTypes.FIREWORK) {
            if (config.hideParticles && berberis != null) {
                event.cancel()
            }
            if (event.type == ParticleTypes.HAPPY_VILLAGER) {
                DelayedRun.runOrNextTick {
                    handleRespawnParticle(location)
                }
            }
            return
        }

        if (config.hideParticles) {
            event.cancel()
        }

        if (berberis == null) {
            list.add(WiltedBerberis(location))
            return
        }

        berberis.updateParticlePosition(location)
    }

    private fun WiltedBerberis.updateParticlePosition(location: LorenzVec) {
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
        lastTime = SimpleTimeMark.now()
    }

    private fun handleRespawnParticle(location: LorenzVec) {
        if (!config.respawnSequence) return

        val roundedLocation = location.roundToBlock()
        if (roundedLocation.add(y = -1).getBlockAt() != Blocks.FARMLAND) return

        val center = nearestFieldCenter(roundedLocation) ?: return
        val seq = fieldSequences[center] ?: return
        if (seq.isValid) return

        // Only clear partial buffer
        if (seq.lastParticleTime.passedSince() > 3.seconds) seq.sequence.clear()
        seq.lastParticleTime = SimpleTimeMark.now()

        if (seq.sequence.lastOrNull() == roundedLocation) return
        seq.sequence.add(roundedLocation)

        if (seq.sequence.size == seq.expectedCount) {
            seq.isValid = true
            seq.isRendering = true
            ChatUtils.chat("§aBerberis respawn sequence learned!")
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!isMuteOthersSoundsEnabled()) return

        if (event.soundName in berberisSounds) {
            event.cancel()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled() || !config.respawnSequence) return
        if (event.clickType != ClickType.LEFT_CLICK) return
        if (event.blockState.block != Blocks.DEAD_BUSH) return

        for (seq in fieldSequences.values) {
            if (!seq.isRendering || seq.isAway) continue
            if (seq.isAtCurrent(event.position)) {
                seq.advance()
                break
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!isEnabled() || !config.respawnSequence) return
        if (event.old != "dead_bush" || event.new != "air") return

        for (seq in fieldSequences.values) {
            if (!seq.isRendering || seq.isAway) continue
            if (seq.isAtCurrent(event.location)) {
                seq.advance()
                break
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (!hasFarmingToolInHand) return

        if (config.onlyOnFarmland && !isOnFarmland) return

        if (config.respawnSequence) {
            for (seq in fieldSequences.values) {
                if (!seq.isRendering || seq.isAway) continue
                val current = seq.currentTarget ?: continue
                event.renderSequenceWaypoints(seq, current)
            }
        }

        // Suppress particles whenever the sequence renderer is active
        // TODO: for some reason not working on partial sequences even if condition true while debugging
        val sequenceIsGuiding = config.respawnSequence &&
            fieldSequences.values.any { it.isRendering && !it.isAway && it.currentTarget != null }
        if (sequenceIsGuiding) return

        list.forEach { it.renderParticleBerberis(event) }
    }

    private fun WiltedBerberis.renderParticleBerberis(event: SkyHanniRenderWorldEvent) {
        if (currentParticles.distanceToPlayer() > 20) return
        if (y == 0.0) return

        val location = currentParticles.fixLocation(this)
        // TODO add chroma color support via config
        if (!moving) {
            event.drawFilledBoundingBox(axisAlignedBB(location), Color.YELLOW.toChromaColor(), 0.7f)
            event.drawDynamicText(location.up(), "§eWilted Berberis", 1.5, seeThroughBlocks = false)
        } else {
            event.drawFilledBoundingBox(axisAlignedBB(location), Color.WHITE.toChromaColor(), 0.5f)
            previous?.fixLocation(this)?.let {
                event.drawFilledBoundingBox(axisAlignedBB(it), Color.LIGHT_GRAY.toChromaColor(), 0.2f)
                event.draw3DLine(it.add(0.5, 0.0, 0.5), location.add(0.5, 0.0, 0.5), Color.WHITE.toChromaColor(), 3, false)
            }
        }
    }

    private fun SkyHanniRenderWorldEvent.renderSequenceWaypoints(seq: BerberisSequence, current: LorenzVec) {
        val next = seq.nextTarget
        val third = seq.thirdTarget
        val previous = seq.previousTarget

        drawFilledBoundingBox(axisAlignedBB(current), Color.GREEN.toChromaColor(), 0.7f)
        drawDynamicText(current.up(), "§aWilted Berberis", 1.5, seeThroughBlocks = false)

        drawPreviousTargetLine(previous, current)
        drawNextTargets(next, current, third)
    }

    // Line from the last harvested position to the current target
    private fun SkyHanniRenderWorldEvent.drawPreviousTargetLine(previous: LorenzVec?, current: LorenzVec) {
        if (previous == null) return
        draw3DLine(
            previous.add(0.5, 0.5, 0.5),
            current.add(0.5, 0.5, 0.5),
            Color.GREEN.toChromaColor(),
            3,
            false,
        )
    }

    private fun SkyHanniRenderWorldEvent.drawNextTargets(
        next: LorenzVec?,
        current: LorenzVec,
        third: LorenzVec?,
    ) {
        if (next == null) return
        drawFilledBoundingBox(axisAlignedBB(next), Color.YELLOW.toChromaColor(), 0.5f)
        draw3DLine(
            current.add(0.5, 0.5, 0.5),
            next.add(0.5, 0.5, 0.5),
            Color.YELLOW.toChromaColor(),
            3,
            false,
        )

        if (third != null) {
            drawFilledBoundingBox(axisAlignedBB(third), Color.RED.toChromaColor(), 0.3f)
            draw3DLine(
                next.add(0.5, 0.5, 0.5),
                third.add(0.5, 0.5, 0.5),
                Color.RED.toChromaColor(),
                3,
                false,
            )
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(60, "rift.area.dreadfarm.wiltedBerberis.hideparticles", "rift.area.dreadfarm.wiltedBerberis.hideParticles")
    }

    // Clears a partial recording buffer if any of its positions no longer contain a dead bush
    // (e.g. broken by other players, or stale from a previous lobby).
    private fun validatePartialSequence(seq: BerberisSequence) {
        for (location in seq.sequence) {
            if (location.getBlockAt() != Blocks.DEAD_BUSH) {
                seq.sequence.clear()
                return
            }
        }
    }

    // TODO: It might be possible to not instantly invalidate but instead check if there is a start point in the sequence
    //  which works and skip forward to that
    private fun validateSequence(seq: BerberisSequence) {
        for ((index, location) in seq.sequence.withIndex()) {
            val block = location.getBlockAt()
            val expectDeadBush = index >= seq.currentIndex

            if (expectDeadBush && block != Blocks.DEAD_BUSH) {
                seq.invalidate()
                return
            }
            if (!expectDeadBush && block == Blocks.DEAD_BUSH) {
                seq.invalidate()
                return
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

    private fun isEnabled() = RiftApi.inDreadfarm() && config.enabled

    private fun isMuteOthersSoundsEnabled() = config.muteOthersSounds &&
        (RiftApi.inDreadfarm() || RiftApi.inWestVillage()) &&
        !(hasFarmingToolInHand && isOnFarmland)
}
