package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.events.IslandGraphReloadEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.CopyNearbyEntitiesCommand.getMobInfo
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils
import at.hannibal2.skyhanni.utils.render.LineDrawer
import java.util.TreeSet

@SkyHanniModule
object MatriarchHelper {

    private val config get() = SkyHanniMod.feature.crimsonIsle.matriarchHelper

    private val pearlList = TreeSet<Pair<Mob, GraphNode>> { first, second ->
        first.first.baseEntity.getLorenzVec().y.compareTo(second.first.baseEntity.getLorenzVec().y)
    }

    private const val EXIT_LABEL = "Heavy Pearls"
    private const val AREA_NAME = "Belly of the Beast"

    private val exitNodeLazy = { IslandGraphs.currentIslandGraph?.getName(EXIT_LABEL)?.firstOrNull()?.also { exitNode == it } }
    private var exitNode: GraphNode? = null

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onMobSpawn(event: MobEvent.Spawn.Special) {
        if (!isHeavyPearl(event)) return
        val node = IslandGraphs.findClosestNode(event.mob.baseEntity.getLorenzVec().up(1.2), { true })
        if (node == null) {
            ErrorManager.logErrorStateWithData(
                "Something went wrong with the Heavy Pearl detection",
                "No graphNode found for pearl",
                "pearList" to pearlList.map { getMobInfo(it.first) to it.second },
                "mob" to getMobInfo(event.mob),
            )
            return
        }
        pearlList.add(event.mob to node)
        if (pearlList.size > 3) {
            ErrorManager.logErrorStateWithData(
                "Something went wrong with the Heavy Pearl detection",
                "More then 3 pearls",
                "pearList" to pearlList.map { getMobInfo(it.first) to it.second },
                "mob" to getMobInfo(event.mob),
            )
            pearlList.clear()
        }
    }

    private fun isHeavyPearl(event: MobEvent) = isEnabled() && event.mob.name == "Heavy Pearl"

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onMobDespawn(event: MobEvent.DeSpawn.Special) {
        if (!isHeavyPearl(event)) return
        pearlList.removeIf { it.first == event.mob }
    }

    private val path = mutableListOf<LorenzVec>()

    private var tspCache: List<LorenzVec>? = null
    private var lastTspPearls = 0

    private fun accessPearls(): List<LorenzVec> {
        if (config.useShortestDistance) {
            val path = tspCache ?: NavigationUtils.getRoute(
                pearlList.map { it.second },
                maxIterations = 5,
            ).also {
                val pearls = path.size
                if (pearls != lastTspPearls) {
                    tspCache = path
                    lastTspPearls = pearls
                }
            }
            return path
        } else {
            return pearlList.map { it.first.baseEntity.getLorenzVec().up(1.2) }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onTick(event: SkyHanniTickEvent) {
        if (SkyBlockUtils.graphArea != AREA_NAME) return
        path.clear()
        path.addAll(accessPearls())
        val exitNode = exitNode ?: exitNodeLazy() ?: return
        val end = path.lastOrNull() ?: LocationUtils.playerLocation()
        val endNode = IslandGraphs.findClosestNode(end, { true }) ?: return
        path.addAll(GraphUtils.findShortestPath(endNode, exitNode).drop(1).map { it.blockCenter() })
    }

    @HandleEvent(GraphAreaChangeEvent::class, onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onGraphAreaChange() {
        if (SkyBlockUtils.graphArea != AREA_NAME) {
            tspCache = null
            lastTspPearls = 0
            path.clear()
            pearlList.clear()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (config.highlight) {
            val color = config.highlightColor.toSpecialColor()
            pearlList.forEach {
                event.drawFilledBoundingBox(it.first.boundingBox.expandBlock(), color, 1f)
            }
        }
        if (config.line) {
            val color = config.lineColor.toSpecialColor()
            var prePoint = event.exactPlayerEyeLocation()
            if (config.simpleLine) {
                accessPearls().forEach { point ->
                    event.draw3DLine(prePoint, point, color, 10, true)
                    prePoint = point
                }
            } else {
                LineDrawer.draw3D(event.partialTicks) {
                    drawPath(
                        listOf(prePoint) + path, color, 10, true, bezierPoint = -1.0,
                    )
                }
            }
        }
    }

    @HandleEvent(IslandGraphReloadEvent::class)
    fun onIslandGraphReload() {
        exitNode = null
    }

    fun isEnabled() = config.enabled
}
