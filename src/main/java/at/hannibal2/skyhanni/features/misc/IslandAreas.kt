package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.data.model.findShortestPathAsGraphWithDistance
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addString
import at.hannibal2.skyhanni.utils.CollectionUtils.sorted
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object IslandAreas {
    private val config get() = SkyHanniMod.feature.misc.areaOverview

    private var nodes = mapOf<GraphNode, Double>()
    private var paths = mapOf<GraphNode, Graph>()
    private var display = listOf<Renderable>()
    private var target = ""
    private var currentArea = ""

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        nodes = emptyMap()
        display = emptyList()
        target = ""
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
        if (!config.display) return
        if (nodes.isEmpty()) return

        if (event.isMod(2)) {
            display = buildDisplay()
        }
    }

    @SubscribeEvent
    // TODO add options
//     fun onOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
    fun onOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (!config.display) return

        config.position.renderRenderables(display, posLabel = "Island Areas")
    }

    private fun buildDisplay() = buildList<Renderable> {
        val closedNote = IslandGraphs.closedNote ?: return@buildList
        val playerDiff = closedNote.position.distanceToPlayer()

        // TODO impl
        val showSmallAreas = true

        var foundCurrentArea = false

        for ((node, diff) in nodes) {
            val difference = diff + playerDiff
            val tag = node.getAreaTag() ?: continue

            if (!showSmallAreas && tag == GraphNodeTag.SMALL_AREA) continue

            val name = node.name ?: continue
            val color = if (name == target) LorenzColor.GOLD else tag.color
            val coloredName = "${color.getChatColor()}${name}"

            var suffix = ""
            paths[node]?.let { path ->
                val passedAreas = path.graph.filter { it.getAreaTag() != null }.map { it.name }.distinct().toMutableList()
                passedAreas.remove(name)
                passedAreas.remove(null)
                passedAreas.remove("null")
                passedAreas.remove(currentArea)
                if (passedAreas.isNotEmpty()) {
                    // TODO option to show areas needed to pass thorough
//                     suffix = " §7${passedAreas.joinToString(", ")}"
                }
            }

            val text = "${coloredName}§7: §e${difference.round(1)}$suffix"

            if (!foundCurrentArea) {
                foundCurrentArea = true

                val inAnArea = name != "no_area"
                if (inAnArea) {
                    addString("Current area: $text")
                } else {
                    addString("Not in an area!")
                }
                if (name != currentArea) {
                    if (inAnArea) {
                        // TODO write feature
                        LorenzUtils.sendTitle("§aEntered $name!", 3.seconds)
                    } else {
//                         LorenzUtils.sendTitle("§cLeft ${currentArea}!", 3.seconds)
                    }
                    currentArea = name
                    // found the target
//                     if (target == name) {
//                         target = ""
//                     }

                }
                addString(" ")

                addString("Areas nearby:")
                continue
            }

            if (name == "no_area") continue

            add(Renderable.clickAndHover(text, tips = emptyList(), onClick = { setGoal(name) }))
        }
    }

    private val allowedTags = listOf(GraphNodeTag.AREA, GraphNodeTag.SMALL_AREA)

    private fun GraphNode.getAreaTag(): GraphNodeTag? = tags.firstOrNull { it in allowedTags }

    private fun setGoal(name: String) {
        target = name
        val graph = IslandGraphs.currentIslandGraph ?: return
        val closedNote = IslandGraphs.closedNote ?: return

        val map = mutableMapOf<GraphNode, Double>()
        for (graphNode in graph.graph) {
            if (graphNode.getAreaTag() == null) continue
            if (graphNode.name != name) continue
            val (_, distance) = graph.findShortestPathAsGraphWithDistance(closedNote, graphNode)
            map[graphNode] = distance
        }

        val note = map.minBy { it.value }.key
        IslandGraphs.find(note.position)
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.let { it.display || it.inWorld }
}
