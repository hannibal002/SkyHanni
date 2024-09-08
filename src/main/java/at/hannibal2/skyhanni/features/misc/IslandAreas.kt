package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.data.model.findShortestPathAsGraphWithDistance
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addString
import at.hannibal2.skyhanni.utils.CollectionUtils.sorted
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object IslandAreas {
    private val config get() = SkyHanniMod.feature.misc.areaNavigation

    private var nodes = mapOf<GraphNode, Double>()
    private var paths = mapOf<GraphNode, Graph>()
    private var display = listOf<Renderable>()
    private var targetName = ""
    private var targetNode: GraphNode? = null
    private var currentAreaName = ""

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        nodes = emptyMap()
        display = emptyList()
        targetName = ""
    }

    fun noteMoved() {
        if (!isEnabled()) return

        val graph = IslandGraphs.currentIslandGraph ?: return
        val closedNote = IslandGraphs.closedNote ?: return

        val paths = mutableMapOf<GraphNode, Graph>()

        val map = mutableMapOf<GraphNode, Double>()
        for (graphNode in graph.graph) {
            if (graphNode.getAreaTag() == null) continue
            val (path, distance) = graph.findShortestPathAsGraphWithDistance(closedNote, graphNode)
            paths[graphNode] = path
            map[graphNode] = distance
        }
        this.paths = paths

        val finalNodes = mutableMapOf<GraphNode, Double>()

        val alreadyFoundAreas = mutableListOf<String>()
        for ((node, distance) in map.sorted()) {
            val areaName = node.name ?: continue
            if (areaName in alreadyFoundAreas) continue
            alreadyFoundAreas.add(areaName)

            finalNodes[node] = distance
        }

        nodes = finalNodes
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!IslandGraphs.existsForThisIsland) return

        if (event.isMod(2)) {
            update()
        }
    }

    private fun update() {
        display = buildDisplay()
    }

    @SubscribeEvent
    fun onOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!config.pathfinder.enabled) return
        if (!config.pathfinder.showAlways) return

        config.pathfinder.position.renderRenderables(display, posLabel = "Island Areas")
    }

    @SubscribeEvent
    fun onOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!config.pathfinder.enabled) return

        config.pathfinder.position.renderRenderables(display, posLabel = "Island Areas")
    }

    private fun buildDisplay() = buildList<Renderable> {
        val closedNote = IslandGraphs.closedNote ?: return@buildList
        val playerDiff = closedNote.position.distanceToPlayer()

        var foundCurrentArea = false

        for ((node, diff) in nodes) {
            val difference = diff + playerDiff
            val tag = node.getAreaTag() ?: continue

            val name = node.name ?: continue
            val isTarget = name == targetName
            val color = if (isTarget) LorenzColor.GOLD else tag.color

            // trying to find a faster path to the existing target
            if (isTarget && node != targetNode) {
                ChatUtils.debug("Found a faster node, rerouting...")
                setTarget(node)
            }
            val coloredName = "${color.getChatColor()}${name}"

            var suffix = ""
            paths[node]?.let { path ->
                val passedAreas = path.graph.filter { it.getAreaTag() != null }.map { it.name }.distinct().toMutableList()
                passedAreas.remove(name)
                passedAreas.remove(null)
                passedAreas.remove("null")
                passedAreas.remove(currentAreaName)
                // so show areas needed to pass thorough
                if (passedAreas.isNotEmpty()) {
//                     suffix = " §7${passedAreas.joinToString(", ")}"
                }
            }

            val text = "${coloredName}§7: §e${difference.round(1)}$suffix"

            if (!foundCurrentArea) {
                foundCurrentArea = true

                val inAnArea = name != "no_area"
                if (config.pathfinder.includeCurrentArea) {
                    if (inAnArea) {
                        addString("§eCurrent area: $text")
                    } else {
                        addString("§cNot in an area!")
                    }
                }
                if (name != currentAreaName) {
                    if (inAnArea && config.enterTitle) {
                        LorenzUtils.sendTitle("§aEntered $name!", 3.seconds)
                    }
                    currentAreaName = name
                }

                addString("§eAreas nearby:")
                continue
            }

            if (name == "no_area") continue

            add(
                Renderable.clickAndHover(
                    text, tips = emptyList(),
                    onClick = {
                        if (node.name == targetName) {
                            targetName = ""
                            targetNode = null
                            IslandGraphs.stop()
                        } else {
                            setTarget(node)
                        }
                    },
                ),
            )
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.inWorld) return
        for ((node, distance) in nodes) {
            val name = node.name ?: continue
            if (name == currentAreaName) continue
            if (name == "no_area") continue
            val position = node.position
            val color = node.getAreaTag()?.color?.getChatColor() ?: ""
            if (!position.canBeSeen(30.0)) return
            event.drawDynamicText(position, color + name, 1.5)
        }
    }

    private val allAreas = listOf(GraphNodeTag.AREA, GraphNodeTag.SMALL_AREA)
    private val onlyLargeAreas = listOf(GraphNodeTag.AREA)

    private fun GraphNode.getAreaTag(): GraphNodeTag? = tags.firstOrNull {
        it in (if (config.includeSmallAreas) allAreas else onlyLargeAreas)
    }

    private fun setTarget(node: GraphNode) {
        targetName = node.name ?: return
        targetNode = node
        val color = config.pathfinder.color.toChromaColor()
        IslandGraphs.find(
            node.position, color,
            onFound = {
                targetName = ""
                update()
            },
        )
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.let { it.pathfinder.enabled || it.enterTitle || it.inWorld }
}
