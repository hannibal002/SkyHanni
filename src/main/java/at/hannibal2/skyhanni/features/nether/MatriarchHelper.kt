package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesInBoundingBox
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.navigation.NavigationUtils
import at.hannibal2.skyhanni.utils.render.LineDrawer
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.expandBlock
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.cubemob.Slime

@SkyHanniModule
object MatriarchHelper {
    private val config get() = SkyHanniMod.feature.crimsonIsle.matriarchHelper

    private val HEAVY_PEARL_TEXTURE by SkullTextureHolder.texture("HEAVY_PEARL")

    private const val EXIT_LABEL = "Heavy Pearls"
    private const val AREA_NAME = "Belly of the Beast"

    private class Pearl(val armorStand: ArmorStand, val slime: Slime, val node: GraphNode) {
        val location get() = slime.getLorenzVec().up(1.2)
    }

    // Armor stands wearing the pearl skull that have no associated slime (yet).
    // Decorative midair pearls have no slime and stay in here.
    private val candidates = mutableSetOf<ArmorStand>()
    private val pearls = mutableListOf<Pearl>()

    private val path = mutableListOf<LorenzVec>()
    private val pearlWaypoints = mutableListOf<LorenzVec>()

    private var tspCache = emptyList<LorenzVec>()
    private var lastTspPearls = -1

    private var exitNode: GraphNode? = null

    @HandleEvent(onlyOnIsland = CRIMSON_ISLE)
    private fun onEntityEquipmentChange(event: EntityEquipmentChangeEvent<ArmorStand>) {
        if (!event.isHead) return
        val entity = event.entity
        if (!entity.wearingSkullTexture(HEAVY_PEARL_TEXTURE)) return
        if (pearls.none { it.armorStand == entity }) candidates.add(entity)
    }

    @HandleEvent(onlyOnIsland = CRIMSON_ISLE)
    private fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<LivingEntity>) {
        when (val entity = event.entity) {
            is ArmorStand -> candidates.remove(entity)
            // The slime despawning means the pearl got collected
            is Slime -> pearls.removeIf { it.slime == entity }
            else -> return
        }
    }

    @HandleEvent(onlyOnIsland = CRIMSON_ISLE)
    private fun onTick() {
        if (!isEnabled() || SkyBlockUtils.graphArea != AREA_NAME) return
        checkCandidates()
        if (!config.line) return
        pearlWaypoints.clear()
        pearlWaypoints.addAll(accessPearls())
        if (config.simpleLine) return
        path.clear()
        path.addAll(pearlWaypoints)
        val exitNode = getExitNode() ?: return
        val end = path.lastOrNull() ?: LocationUtils.playerLocation()
        val endNode = IslandGraphs.findClosestNode(end, { true }) ?: return
        path.addAll(GraphUtils.findShortestPath(endNode, exitNode).drop(1).map { it.blockCenter() })
    }

    private fun checkCandidates() {
        if (candidates.isEmpty()) return
        val iterator = candidates.iterator()
        while (iterator.hasNext()) {
            val armorStand = iterator.next()
            val slime = getEntitiesInBoundingBox<Slime>(armorStand.boundingBox).firstOrNull() ?: continue
            // Each pearl consists of multiple stacked armor stands sharing one slime
            if (pearls.any { it.slime == slime }) {
                iterator.remove()
                continue
            }
            val location = slime.getLorenzVec().up(1.2)
            val node = IslandGraphs.findClosestNode(location, { true })
            if (node == null) {
                ErrorManager.logErrorStateWithData(
                    "Something went wrong with the Heavy Pearl detection",
                    "No graphNode found for pearl",
                    "location" to location,
                )
                iterator.remove()
                continue
            }
            iterator.remove()
            pearls.add(Pearl(armorStand, slime, node))
        }
        if (pearls.size > 3) {
            ErrorManager.logErrorStateWithData(
                "Something went wrong with the Heavy Pearl detection",
                "More than 3 pearls",
                "pearls" to pearls.map { it.location },
            )
            reset()
        }
    }

    private fun accessPearls(): List<LorenzVec> = if (config.useShortestDistance) {
        if (pearls.size != lastTspPearls) {
            lastTspPearls = pearls.size
            tspCache = NavigationUtils.getRouteLocations(pearls.map { it.node }, maxIterations = 5)
        }
        tspCache
    } else {
        pearls.sortedBy { it.location.y }.map { it.location }
    }

    private fun getExitNode(): GraphNode? = exitNode
        ?: IslandGraphs.currentIslandGraph?.getNodesWithName(EXIT_LABEL)?.firstOrNull()?.also { exitNode = it }

    @HandleEvent(onlyOnIsland = CRIMSON_ISLE)
    private fun onAreaChange() {
        if (SkyBlockUtils.graphArea != AREA_NAME) reset()
    }

    @HandleEvent
    private fun onWorldChange() {
        reset()
        candidates.clear()
    }

    // Known pearls become candidates again and get revalidated by the next tick in the area
    private fun reset() {
        tspCache = emptyList()
        lastTspPearls = -1
        path.clear()
        pearlWaypoints.clear()
        pearls.forEach { candidates.add(it.armorStand) }
        pearls.clear()
    }

    @HandleEvent(onlyOnIsland = CRIMSON_ISLE)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (config.highlight) {
            val color = config.highlightColor
            pearls.forEach {
                event.drawFilledBoundingBox(it.slime.boundingBox.expandBlock(), color, 1f)
            }
        }
        if (config.line) {
            val color = config.lineColor.toColor()
            var prePoint = event.exactPlayerEyeLocation()
            if (config.simpleLine) {
                pearlWaypoints.forEach { point ->
                    event.draw3DLine(prePoint, point, color, 10, true)
                    prePoint = point
                }
            } else if (path.isNotEmpty()) {
                LineDrawer.draw3D(event, lineWidth = 10, depth = true) {
                    drawPath(
                        listOf(prePoint) + path, color, bezierPoint = -1.0,
                    )
                }
            }
        }
    }

    @HandleEvent
    private fun onIslandGraphReload() {
        exitNode = null
        reset()
    }

    private fun isEnabled() = config.enabled
}
