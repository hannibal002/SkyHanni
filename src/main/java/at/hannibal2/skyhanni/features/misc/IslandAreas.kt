package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandGraphReloadEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sorted
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.SearchTextInput
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.buildSearchBox
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.inventory.GuiInventory
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object IslandAreas {
    private val config get() = SkyHanniMod.feature.misc.areaNavigation

    private var nodes = mapOf<GraphNode, Double>()
    private var paths = mapOf<GraphNode, Graph>()
    var display: Renderable? = null
    private var targetNode: GraphNode? = null

    @Deprecated("moved", ReplaceWith("SkyBlockUtils.graphArea"))
    val currentAreaName get() = currentArea

    var currentArea = ""
    private val textInput = SearchTextInput()

    @HandleEvent
    fun onWorldChange() {
        nodes = emptyMap()
        display = null
        targetNode = null
        hasMoved = true
        updateArea("no_area", onlyInternal = true)
    }

    fun nodeMoved() {
        SkyHanniMod.coroutineScope.launch {
            updateNodes()
        }
    }

    private fun updateNodes() {
        if (!isEnabled()) return
        val graph = IslandGraphs.currentIslandGraph ?: return
        val closestNode = IslandGraphs.closestNode ?: return

        val (paths, map) = GraphUtils.findFastestPaths(graph, closestNode) { it.getAreaTag() != null }
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

    private var hasMoved = false

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled() || !event.isMod(2) || !hasMoved) return
        update(shouldBuildDisplay = isPathfinderEnabled())
        hasMoved = false
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlayerMove(event: EntityMoveEvent<EntityPlayerSP>) {
        if (isEnabled() && event.isLocalPlayer) {
            hasMoved = true
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRenderOverlay() {
        if (!isEnabled()) return
        if (!isPathfinderEnabled()) return
        if (!config.pathfinder.showAlways) return
        val isInOwnInventory = Minecraft.getMinecraft().currentScreen is GuiInventory
        if (isInOwnInventory) return

        display?.let {
            config.pathfinder.position.renderRenderable(it, posLabel = "Island Areas")
        }
    }

    @HandleEvent(GuiRenderEvent.ChestGuiOverlayRenderEvent::class)
    fun onBackgroundDraw() {
        if (!isEnabled()) return
        if (!isPathfinderEnabled()) return
        val isInOwnInventory = Minecraft.getMinecraft().currentScreen is GuiInventory
        if (!isInOwnInventory) return

        display?.let {
            config.pathfinder.position.renderRenderable(it, posLabel = "Island Areas")
        }
    }

    @HandleEvent(IslandGraphReloadEvent::class)
    fun onIslandGraphReload() {
        nodeMoved()

        DelayedRun.runDelayed(150.milliseconds) {
            update(shouldBuildDisplay = isPathfinderEnabled())
        }
    }

    // updateing the position (mandatory for all other features), and builds the display (optionally)
    // TODO split the position update logic outside the display creation logic, without reducing performance.
    private fun update(shouldBuildDisplay: Boolean = true) {
        var foundCurrentArea = false
        var foundAreas = 0
        val buildDisplay: MutableList<Searchable>? = if (shouldBuildDisplay) {
            mutableListOf()
        } else null
        for ((node, difference) in nodes) {
            val tag = node.getAreaTag() ?: continue

            val name = node.name ?: continue
            // can not compare nodes directly. By using names, we also accept other nodes
            val isTarget = node.name == targetNode?.name
            val color = if (isTarget) LorenzColor.GOLD else tag.color

            val coloredName = "${color.getChatColor()}$name"

            var suffix = ""
            paths[node]?.let { path ->
                val passedAreas = path.nodes.filter { it.getAreaTag() != null }.map { it.name }.distinct().toMutableList()
                passedAreas.remove(name)
                passedAreas.remove(null)
                passedAreas.remove("null")
                passedAreas.remove(currentArea)
                // so show areas needed to pass thorough
                // TODO show this pass through in the /shnavigate command
                if (passedAreas.isNotEmpty()) {
//                     suffix = " §7${passedAreas.joinToString(", ")}"
                }
            }

            val distance = difference.roundTo(0).toInt()
            val text = "$coloredName§7: §e$distance$suffix"

            val isConfigVisible = node.getAreaTag(useConfig = true) != null
            if (!foundCurrentArea) {
                foundCurrentArea = true

                val inAnArea = name != "no_area" && isConfigVisible
                if (config.pathfinder.includeCurrentArea.get()) {
                    if (inAnArea) {
                        buildDisplay?.addSearchString("§eCurrent area: $coloredName")
                    } else {
                        buildDisplay?.addSearchString("§7Not in an area.")
                    }
                }
                updateArea(name, onlyInternal = !isConfigVisible)

                buildDisplay?.addSearchString("§eAreas nearby:")
                continue
            }

            if (name == "no_area") continue
            if (!isConfigVisible) continue
            foundAreas++

            buildDisplay?.add(
                Renderable.clickable(
                    text,
                    tips = buildList {
                        add(tag.color.getChatColor() + node.name)
                        add("§7Type: ${tag.displayName}")
                        add("§7Distance: §e$distance blocks")
                        add("")
                        if (node == targetNode) {
                            add("§aPath Finder points to this!")
                            add("")
                            add("§eClick to disable!")
                        } else {
                            add("§eClick to find a path!")
                        }
                    },
                    onLeftClick = {
                        if (node == targetNode) {
                            targetNode = null
                            IslandGraphs.stop()
                            update()
                        } else {
                            setTarget(node)
                        }
                    },
                ).toSearchable(name),
            )
        }
        if (foundAreas == 0) {
            val islandName = SkyBlockUtils.currentIsland.displayName
            if (foundCurrentArea) {
                buildDisplay?.addSearchString("§cThere is only one area in $islandName,")
                buildDisplay?.addSearchString("§cnothing else to navigate to!")
            } else {
                buildDisplay?.addSearchString("§cThere is no $islandName area data available yet!")
            }
        }
        buildDisplay?.let {
            display = it.buildSearchBox(textInput)
        }
    }

    private fun updateArea(name: String, onlyInternal: Boolean) {
        if (name != currentArea) {
            val oldArea = currentArea
            currentArea = name
            GraphAreaChangeEvent(name, oldArea, onlyInternal).post()
        }
    }

    @HandleEvent
    fun onAreaChange(event: GraphAreaChangeEvent) {
        val name = event.area
        val inAnArea = name != "no_area"
        // when this is a small area and small areas are disabled via config
        if (event.onlyInternal) return
        if (inAnArea && config.enterTitle) {
            TitleManager.sendTitle("§aEntered $name!")
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (!config.inWorld) return
        for ((node, distance) in nodes) {
            val name = node.name ?: continue
            if (name == currentArea) continue
            if (name == "no_area") continue
            val position = node.position
            val areaTag = node.getAreaTag(useConfig = true) ?: continue
            val color = areaTag.color.getChatColor()
            if (!position.canBeSeen(40.0)) return
            event.drawDynamicText(position, color + name, 1.5)
        }
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        with(config.pathfinder) {
            ConditionalUtils.onToggle(color) {
                targetNode?.let {
                    setTarget(it)
                }
            }
            ConditionalUtils.onToggle(
                color,
                includeCurrentArea,
                enabled,
            ) {
                updateNodes()
                update()
            }
        }
    }

    private val allAreas = listOf(GraphNodeTag.AREA, GraphNodeTag.SMALL_AREA)
    private val onlyLargeAreas = listOf(GraphNodeTag.AREA)

    fun GraphNode.getAreaTag(useConfig: Boolean = false): GraphNodeTag? = tags.firstOrNull {
        it in (if (config.includeSmallAreas || !useConfig) allAreas else onlyLargeAreas)
    }

    private fun setTarget(node: GraphNode) {
        targetNode = node
        val tag = node.getAreaTag() ?: return
        val displayName = tag.color.getChatColor() + node.name
        val color = config.pathfinder.color.get().toSpecialColor()
        node.pathFind(
            displayName,
            color,
            onFound = {
                targetNode = null
                update()
            },
            allowRerouting = true,
            condition = { isPathfinderEnabled() },
        )
        update()
    }

    private fun isPathfinderEnabled(): Boolean = config.pathfinder.enabled.get()

    private fun isEnabled() = IslandGraphs.currentIslandGraph != null
}
