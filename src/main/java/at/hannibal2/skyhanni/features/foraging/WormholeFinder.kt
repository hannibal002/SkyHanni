package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.IslandLeaveEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.playerLocation
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.world.entity.Display
import org.joml.Vector3f
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object WormholeFinder {

    private val config get() = SkyHanniMod.feature.fishing.wormholeFinder

    private const val DIRECTION_TOLERANCE = 0.98

    private var matchedWormholes: List<GraphNode> = emptyList()
    private var currentTarget: GraphNode? = null
    private var lastDepartureAlert = SimpleTimeMark.farPast()
    private var lastPlayerPos: LorenzVec? = null

    @HandleEvent(onlyOnIslands = [IslandType.LOTUS_ATOLL, IslandType.CRIMSON_ISLE])
    fun onTick(event: SkyHanniTickEvent) {
        if (!config.enabled) return
        if (!event.isMod(10)) return
        if (!wearingFroggles()) return

        val playerPos = playerLocation()
        val rawArrows = playerPos.getEntitiesNearby<Display.TextDisplay>(3.0)

        matchedWormholes = rawArrows.mapNotNull { matchArrow(it) }.distinct()

        if (matchedWormholes.isNotEmpty()) {
            val newTarget = matchedWormholes.minByOrNull { it.position.distanceSqToPlayer() }
            if (newTarget != null && newTarget != currentTarget) {
                currentTarget = newTarget
                newTarget.pathFind(
                    "§dWormhole",
                    LorenzColor.LIGHT_PURPLE.toColor(),
                    onFound = { currentTarget = null },
                    condition = { config.enabled },
                )
            }
        } else {
            val last = lastPlayerPos
            val isMoving = last != null && run {
                val d = playerPos - last
                d.x * d.x + d.z * d.z > 0.25
            }
            if (rawArrows.isNotEmpty() && !isMoving) currentTarget = null
        }
        lastPlayerPos = playerPos
    }

    @Suppress("UnnecessarySafeCall")
    private fun Display.TextDisplay.arrowForwardVec(): LorenzVec {
        //~ if < 26.1 'leftRotation()' -> 'leftRotation'
        val quat = renderState()?.transformation()?.get(0f)?.leftRotation() ?: return LorenzVec(0, 0, 1)
        val localY = Vector3f(0f, 1f, 0f)
        quat.transform(localY)
        return LorenzVec(localY.x.toDouble(), 0.0, localY.z.toDouble()).normalize()
    }

    private fun matchArrow(arrow: Display.TextDisplay): GraphNode? {
        val graph = IslandGraphs.currentIslandGraph ?: return null
        val origin = arrow.getLorenzVec()
        val forward = arrow.arrowForwardVec()
        return graph.asSequence()
            .filter { it.hasTag(GraphNodeTag.FISHING_WORMHOLE) }
            .map { node ->
                val delta = node.position - origin
                val horizontal = LorenzVec(delta.x, 0.0, delta.z).normalize()
                node to forward.dotProduct(horizontal)
            }
            .filter { (_, score) -> score >= DIRECTION_TOLERANCE }
            .maxByOrNull { (_, score) -> score }
            ?.first
    }

    @HandleEvent(onlyOnIslands = [IslandType.LOTUS_ATOLL, IslandType.CRIMSON_ISLE])
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) return
        if (!wearingFroggles()) return
        for (wormhole in matchedWormholes) {
            event.drawWaypointFilled(wormhole.position, LorenzColor.LIGHT_PURPLE.toColor(), seeThroughBlocks = true)
            event.drawDynamicText(wormhole.position.up(), "§dWormhole", 1.5)
        }
    }

    @HandleEvent(onlyOnIslands = [IslandType.LOTUS_ATOLL, IslandType.CRIMSON_ISLE])
    fun onPlaySound(event: PlaySoundEvent) {
        if (!config.enabled || !config.departureAlert) return
        if (!wearingFroggles()) return
        if (!(event.soundName == "entity.enderman.teleport" && event.pitch == 0.6984127f)) return
        if (lastDepartureAlert.passedSince() < 3.seconds) return
        lastDepartureAlert = SimpleTimeMark.now()
        TitleManager.sendTitle("§cWormhole closed!")
    }

    @HandleEvent(IslandLeaveEvent::class)
    fun onIslandLeave() {
        matchedWormholes = emptyList()
        currentTarget = null
        lastPlayerPos = null
    }

    val DIAMOND_FROGGLES = "FROGGLES_DIAMOND".toInternalName()
    val GOLD_FROGGLES = "FROGGLES_GOLD".toInternalName()

    fun wearingFroggles(): Boolean {
        val id = InventoryUtils.getHelmet()?.getInternalName() ?: return false
        return id == DIAMOND_FROGGLES || id == GOLD_FROGGLES
    }
}
