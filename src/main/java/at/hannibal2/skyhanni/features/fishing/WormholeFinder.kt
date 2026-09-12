package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.fishing.WormholeFinderConfig.LineMode
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.ParticleEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.DisplayEntityUtils.arrowForwardVec
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToCrosshair
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.entity.Display
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object WormholeFinder {

    private val config get() = SkyHanniMod.feature.fishing.wormholeFinder

    private const val WORMHOLE_LABEL = "§dWormhole"
    private const val WORMHOLE_TEXT = "Wormhole"
    private const val DIRECTION_TOLERANCE = 0.98
    private const val DIRECTION_SCORE_TIE_TOLERANCE = 0.005
    private const val MIN_HORIZONTAL_VECTOR_LENGTH_SQ = 1.0E-6
    private const val DEPARTURE_SOUND_PITCH = 0.6984127f
    private const val SOUND_PITCH_TOLERANCE = 0.0001f
    private const val PARTICLE_SPEED_TOLERANCE = 0.0001f
    private const val MAX_ACTIVE_WORMHOLES = 2
    private const val PARTICLE_CONFIRM_DISTANCE_SQ = 25.0
    private const val VERIFY_GUESS_DISTANCE_SQ = 400.0
    private const val REACHED_TARGET_SUPPRESS_DISTANCE_SQ = 25.0
    private const val CONFIRMED_MARKER_ALPHA = 255
    private const val UNCONFIRMED_MARKER_ALPHA = 128

    private val PARTICLE_PAIR_TIMEOUT = 500.milliseconds
    private val PARTICLE_SIGNAL_TIMEOUT = 2.seconds
    private val UNCONFIRMED_NEAR_TIMEOUT = 2.seconds
    private val CLOSE_EVENT_DEDUPE_TIMEOUT = 1.seconds

    private val activeWormholes = linkedMapOf<GraphNode, ActiveWormhole>()
    private val particleSignals = mutableMapOf<GraphNode, ParticleSignal>()
    private var visibleWormholes: List<GraphNode> = emptyList()
    private var currentTarget: GraphNode? = null
    private var reachedTarget: GraphNode? = null
    private var lastDepartureAlert = SimpleTimeMark.farPast()
    private var lastCloseRemoval = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnIslands = [IslandType.LOTUS_ATOLL, IslandType.CRIMSON_ISLE])
    private fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(10)) return
        if (!isEnabled()) {
            clearTarget()
            return
        }

        val playerPos = LocationUtils.playerLocation()
        val rawArrows = playerPos.getEntitiesNearby<Display.TextDisplay>(3.0)
        val newMatches = rawArrows.mapNotNull { matchArrow(it) }.distinct()

        if (newMatches.isNotEmpty()) {
            newMatches.forEach { updateFromArrow(it) }
        }
        pruneUnconfirmedGuesses()
        pruneStaleParticleSignals()
        updateTarget()
    }

    @HandleEvent(onlyOnIslands = [IslandType.LOTUS_ATOLL, IslandType.CRIMSON_ISLE])
    private fun onParticle(event: ParticleEvent) {
        if (!isEnabled()) return
        if (!isWormholeParticle(event.type, event.count, event.speed)) return
        val node = findNearestWormhole(event.location) ?: return
        val signal = particleSignals.getOrPut(node) { ParticleSignal() }
        signal.update(event.type)
        if (signal.isConfirmed()) updateActiveWormhole(node, seenByParticle = true)
    }

    private fun matchArrow(arrow: Display.TextDisplay): GraphNode? {
        val graph = IslandGraphs.currentIslandGraph ?: return null
        val origin = arrow.getLorenzVec()
        val forward = arrow.arrowForwardVec() ?: return null
        return findBestWormhole(graph.asSequence().filter { it.hasTag(GraphNodeTag.FISHING_WORMHOLE) }, origin, forward)
    }

    internal fun findBestWormhole(wormholes: Sequence<GraphNode>, origin: LorenzVec, forward: LorenzVec): GraphNode? {
        val direction = forward.normalizeOrNull() ?: return null
        val candidates = wormholes.mapNotNull { node ->
            val horizontal = horizontalDirection(origin, node.position) ?: return@mapNotNull null
            val score = direction.dotProduct(horizontal)
            if (score < DIRECTION_TOLERANCE) return@mapNotNull null
            WormholeCandidate(node, score, node.position.distanceSqIgnoreY(origin))
        }.toList()

        val bestScore = candidates.maxOfOrNull { it.score } ?: return null
        return candidates.asSequence()
            .filter { bestScore - it.score <= DIRECTION_SCORE_TIE_TOLERANCE }
            .minByOrNull { it.distanceSq }
            ?.node
    }

    private data class WormholeCandidate(
        val node: GraphNode,
        val score: Double,
        val distanceSq: Double,
    )

    private data class ActiveWormhole(
        val node: GraphNode,
        var lastArrow: SimpleTimeMark = SimpleTimeMark.farPast(),
        var lastParticle: SimpleTimeMark = SimpleTimeMark.farPast(),
    ) {
        val lastSeen get() = maxOf(lastArrow, lastParticle)
        val confirmed get() = lastParticle.takeIfInitialized() != null
        var nearSince = SimpleTimeMark.farPast()
    }

    private class ParticleSignal {
        private var lastEnchant = SimpleTimeMark.farPast()
        private var lastPortal = SimpleTimeMark.farPast()
        private val lastSeen get() = maxOf(lastEnchant, lastPortal)

        fun update(type: ParticleType<*>) {
            val now = SimpleTimeMark.now()
            when (type) {
                ParticleTypes.ENCHANT -> lastEnchant = now
                ParticleTypes.PORTAL -> lastPortal = now
            }
        }

        fun isConfirmed() =
            lastSeen.passedSince() <= PARTICLE_PAIR_TIMEOUT &&
                lastEnchant.absoluteDifference(lastPortal) <= PARTICLE_PAIR_TIMEOUT

        fun isStale() = lastSeen.passedSince() > PARTICLE_SIGNAL_TIMEOUT
    }

    private fun horizontalDirection(origin: LorenzVec, target: LorenzVec): LorenzVec? {
        val delta = target - origin
        return LorenzVec(delta.x, 0.0, delta.z).normalizeOrNull()
    }

    private fun LorenzVec.normalizeOrNull(): LorenzVec? =
        takeIf { it.lengthSquared() > MIN_HORIZONTAL_VECTOR_LENGTH_SQ }?.normalize()

    internal fun isWormholeParticle(type: ParticleType<*>, count: Int, speed: Float): Boolean = when (type) {
        ParticleTypes.ENCHANT -> count == 4 && abs(speed + 1.2f) <= PARTICLE_SPEED_TOLERANCE
        ParticleTypes.PORTAL -> count == 5 && abs(speed - 0.25f) <= PARTICLE_SPEED_TOLERANCE
        else -> false
    }

    private fun findNearestWormhole(location: LorenzVec): GraphNode? {
        val graph = IslandGraphs.currentIslandGraph ?: return null
        return graph.asSequence()
            .filter { it.hasTag(GraphNodeTag.FISHING_WORMHOLE) }
            .map { it to it.position.distanceSqIgnoreY(location) }
            .filter { (_, distanceSq) -> distanceSq <= PARTICLE_CONFIRM_DISTANCE_SQ }
            .minByOrNull { (_, distanceSq) -> distanceSq }
            ?.first
    }

    private fun updateFromArrow(node: GraphNode) {
        if (node in activeWormholes) {
            updateActiveWormhole(node, seenByArrow = true)
            return
        }
        if (shouldIgnoreArrowGuess(node)) return
        updateActiveWormhole(node, seenByArrow = true)
    }

    private fun updateActiveWormhole(
        node: GraphNode,
        seenByArrow: Boolean = false,
        seenByParticle: Boolean = false,
    ) {
        val active = activeWormholes.getOrPut(node) { ActiveWormhole(node) }
        val now = SimpleTimeMark.now()
        if (seenByArrow) active.lastArrow = now
        if (seenByParticle) {
            active.lastParticle = now
            active.nearSince = SimpleTimeMark.farPast()
        }
        trimActiveWormholes()
        updateVisibleWormholes()
    }

    private fun shouldIgnoreArrowGuess(node: GraphNode): Boolean {
        if (node in activeWormholes) return false
        return activeWormholes.values.count { it.confirmed } >= MAX_ACTIVE_WORMHOLES
    }

    private fun trimActiveWormholes() {
        if (activeWormholes.size <= MAX_ACTIVE_WORMHOLES) return
        val keep = activeWormholes.values
            .sortedWith(compareByDescending<ActiveWormhole> { it.confirmed }.thenByDescending { it.lastSeen })
            .take(MAX_ACTIVE_WORMHOLES)
            .mapTo(mutableSetOf()) { it.node }

        activeWormholes.keys.removeIf { it !in keep }
    }

    private fun pruneStaleParticleSignals() {
        particleSignals.values.removeIf { it.isStale() }
    }

    private fun pruneUnconfirmedGuesses() {
        activeWormholes.values.removeIf {
            if (it.confirmed) return@removeIf false
            if (it.node.position.distanceSqToPlayer() > VERIFY_GUESS_DISTANCE_SQ) {
                it.nearSince = SimpleTimeMark.farPast()
                return@removeIf false
            }
            if (it.nearSince.isFarPast()) it.nearSince = SimpleTimeMark.now()
            it.nearSince.passedSince() > UNCONFIRMED_NEAR_TIMEOUT
        }
        updateVisibleWormholes()
    }

    private fun updateVisibleWormholes() {
        visibleWormholes = activeWormholes.values
            .sortedBy { it.node.position.distanceSqToPlayer() }
            .map { it.node }
    }

    private fun updateTarget() {
        val reached = reachedTarget?.takeIf { reached ->
            reached in activeWormholes && reached.position.distanceSqToPlayer() <= REACHED_TARGET_SUPPRESS_DISTANCE_SQ
        }
        if (reachedTarget != null && reached == null) reachedTarget = null

        val oldTarget = currentTarget
        val newTarget = guideTarget(visibleWormholes.filter { it != reached })
        currentTarget = newTarget
        if (newTarget == null || config.lineMode != LineMode.NAVIGATION) {
            stopNavigation(oldTarget)
            return
        }
        if (!IslandGraphs.isActive(newTarget.position, WORMHOLE_LABEL)) {
            newTarget.pathFind(
                WORMHOLE_LABEL,
                LorenzColor.LIGHT_PURPLE.toColor(),
                onFound = { markTargetReached(newTarget) },
                condition = { isEnabled() && config.lineMode == LineMode.NAVIGATION && currentTarget == newTarget },
            )
        }
    }

    private fun markTargetReached(target: GraphNode) {
        reachedTarget = target
        if (currentTarget == target) currentTarget = null
    }

    private fun clearTarget(stopNavigation: Boolean = true) {
        val target = currentTarget
        activeWormholes.clear()
        particleSignals.clear()
        visibleWormholes = emptyList()
        currentTarget = null
        reachedTarget = null

        if (stopNavigation && target != null && IslandGraphs.isActive(target.position, WORMHOLE_LABEL)) {
            IslandGraphs.stopNavigation()
        }
    }

    @HandleEvent(onlyOnIslands = [IslandType.LOTUS_ATOLL, IslandType.CRIMSON_ISLE])
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        for (wormhole in visibleWormholes) {
            event.drawWaypointFilled(wormhole.position, wormhole.waypointColor(), seeThroughBlocks = true)
            event.drawDynamicText(
                wormhole.position.up(),
                wormhole.renderText(),
                1.5,
                color = wormhole.textColor(),
            )
        }
        if (config.lineMode == LineMode.DIRECT) directLineTarget()?.let {
            event.drawLineToCrosshair(it.position, LorenzColor.LIGHT_PURPLE.toChromaColor(), 3, false)
        }
    }

    private fun GraphNode.renderText() = WORMHOLE_TEXT + if (isConfirmed()) "!" else "?"

    private fun GraphNode.isConfirmed() = activeWormholes[this]?.confirmed == true

    private fun GraphNode.waypointColor() =
        LorenzColor.LIGHT_PURPLE.toColor()
            .addAlpha(markerAlpha())

    private fun GraphNode.textColor() =
        LorenzColor.LIGHT_PURPLE.toColor()
            .addAlpha(markerAlpha())

    private fun GraphNode.markerAlpha() = if (isConfirmed()) CONFIRMED_MARKER_ALPHA else UNCONFIRMED_MARKER_ALPHA

    private fun directLineTarget(): GraphNode? {
        val candidates = visibleWormholes.filter {
            it.position.distanceSqToPlayer() > REACHED_TARGET_SUPPRESS_DISTANCE_SQ
        }
        return guideTarget(candidates)
    }

    private fun guideTarget(candidates: Iterable<GraphNode>): GraphNode? =
        candidates.minWithOrNull(
            compareByDescending<GraphNode> { it.isConfirmed() }
                .thenBy { it.position.distanceSqToPlayer() },
        )

    @HandleEvent(onlyOnIslands = [IslandType.LOTUS_ATOLL, IslandType.CRIMSON_ISLE])
    private fun onPlaySound(event: PlaySoundEvent) {
        if (!config.enabled) return
        if (!wearingFroggles()) return
        if (event.soundName != "entity.enderman.teleport") return
        if (abs(event.pitch - DEPARTURE_SOUND_PITCH) > SOUND_PITCH_TOLERANCE) return
        removeClosedWormhole(event.location)
        if (!config.departureAlert) return
        if (lastDepartureAlert.passedSince() < 3.seconds) return
        lastDepartureAlert = SimpleTimeMark.now()
        TitleManager.sendTitle("§cWormhole closed!")
    }

    @HandleEvent(onlyOnIslands = [IslandType.LOTUS_ATOLL, IslandType.CRIMSON_ISLE])
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        if (!event.message.removeColor().contains("Your Wormhole closed up")) return
        removeClosedWormhole()
    }

    private fun removeClosedWormhole(referenceLocation: LorenzVec? = null) {
        if (lastCloseRemoval.passedSince() < CLOSE_EVENT_DEDUPE_TIMEOUT) return
        val target = if (referenceLocation != null) {
            activeWormholes.keys.minByOrNull { it.position.distanceSq(referenceLocation) }
        } else {
            activeWormholes.keys.minByOrNull { it.position.distanceSqToPlayer() }
        } ?: return
        lastCloseRemoval = SimpleTimeMark.now()
        removeWormhole(target)
        updateTarget()
    }

    private fun removeWormhole(target: GraphNode, stopNavigation: Boolean = true) {
        activeWormholes.remove(target)
        particleSignals.remove(target)
        if (stopNavigation) stopNavigation(target)
        if (currentTarget == target) currentTarget = null
        if (reachedTarget == target) reachedTarget = null
        updateVisibleWormholes()
    }

    private fun stopNavigation(target: GraphNode?) {
        val activeTarget = target?.takeIf { IslandGraphs.isActive(it.position, WORMHOLE_LABEL) }
            ?: activeWormholes.keys.firstOrNull { IslandGraphs.isActive(it.position, WORMHOLE_LABEL) }
        if (activeTarget != null) IslandGraphs.stopNavigation()
    }

    @HandleEvent
    private fun onIslandLeave() {
        clearTarget(stopNavigation = false)
    }

    val DIAMOND_FROGGLES = "FROGGLES_DIAMOND".toInternalName()
    val GOLD_FROGGLES = "FROGGLES_GOLD".toInternalName()

    fun wearingFroggles(): Boolean {
        val id = InventoryUtils.getHelmet()?.getInternalName() ?: return false
        return id == DIAMOND_FROGGLES || id == GOLD_FROGGLES
    }

    private fun isEnabled() = config.enabled && wearingFroggles()
}
