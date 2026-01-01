package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.data.title.TitleContext
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.features.misc.IslandAreas.getAreaTag
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.SearchTextInput
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.buildSearchBox
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.InventoryScreen

@SkyHanniModule
object IslandAreaFeatures {
    private val config get() = SkyHanniMod.feature.misc.navigation
    var smallAreas = setOf<String>()

    private val areaListConfig get() = config.areasList
    private var nodes = mapOf<GraphNode, Double>()

    var display: Renderable? = null
    private var targetNode: GraphNode? = null
    private val textInput = SearchTextInput()

    private fun setTarget(node: GraphNode) {
        targetNode = node
        val tag = node.getAreaTag() ?: return
        val displayName = tag.color.getChatColor() + node.name
        val color = areaListConfig.color.get().toColor()
        node.pathFind(
            displayName,
            color,
            onFound = {
                targetNode = null
                update()
            },
            allowRerouting = true,
            condition = ::isPathfinderEnabled,
        )
        update()
    }

    fun update() {
        IslandAreas.update()
    }

    var oldTitle: TitleContext? = null

    @HandleEvent
    fun onAreaChange(event: GraphAreaChangeEvent) {
        val name = event.area
        val inAnArea = name != "no_area"
        // when this is a small area move and small areas are disabled via config
        if (!config.includeSmallAreas && (name in smallAreas || event.previousArea in smallAreas)) return

        oldTitle?.stop()
        if (event.onlyInternal) return
        if (inAnArea && config.enterTitle) {
            oldTitle = TitleManager.sendTitle("§aEntered $name!")
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (!config.showInWorld) return
        for ((node, _) in nodes) {
            val name = node.name ?: continue
            if (name == SkyBlockUtils.graphArea) continue
            if (name == "no_area") continue
            val position = node.position
            val areaTag = node.getAreaTag(useConfig = true) ?: continue
            val color = areaTag.color.getChatColor()
            if (!position.canBeSeen(40.0)) return
            event.drawDynamicText(position, color + name, 1.5)
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRenderOverlay() {
        if (!isEnabled()) return
        if (!isPathfinderEnabled()) return
        if (!areaListConfig.showAlways) return
        val isInOwnInventory = Minecraft.getInstance().screen is InventoryScreen
        if (!isInOwnInventory) {
            doRender()
        }
    }

    @HandleEvent(GuiRenderEvent.ChestGuiOverlayRenderEvent::class)
    fun onBackgroundDraw() {
        if (!isEnabled()) return
        if (!isPathfinderEnabled()) return
        val isInOwnInventory = Minecraft.getInstance().screen is InventoryScreen
        if (isInOwnInventory) {
            doRender()
        }
    }

    private fun doRender() {
        display?.let {
            areaListConfig.position.renderRenderable(it, posLabel = "Island Areas")
        }
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        with(areaListConfig) {
            ConditionalUtils.onToggle(color) {
                targetNode?.let { setTarget(it) }
            }
        }
    }

    fun reset() {
        display = null
        targetNode = null
    }

    private fun isPathfinderEnabled(): Boolean = areaListConfig.enabled.get()

    private fun isEnabled() = IslandGraphs.currentIslandGraph != null

    fun updateNodes(nodes: MutableMap<GraphNode, Double>) {
        this.nodes = nodes
        smallAreas = nodes.filter { GraphNodeTag.SMALL_AREA in it.key.tags }.mapNotNull { it.key.name }.toSet()
    }

    fun redraw() {
        var foundCurrentArea = false
        var foundAreas = 0
        val buildDisplay = mutableListOf<Searchable>()
        for ((node, difference) in nodes) {
            val tag = node.getAreaTag() ?: continue

            val name = node.name ?: continue
            // can not compare nodes directly. By using names, we also accept other nodes
            val isTarget = node.name == targetNode?.name
            val color = if (isTarget) LorenzColor.GOLD else tag.color

            val coloredName = "${color.getChatColor()}$name"

            val distance = difference.roundTo(0).toInt()
            val text = "$coloredName§7: §e$distance"

            val isConfigVisible = node.getAreaTag(useConfig = true) != null
            if (!foundCurrentArea) {
                foundCurrentArea = true

                val inAnArea = name != "no_area" && isConfigVisible
                if (areaListConfig.includeCurrentArea.get()) {
                    if (inAnArea) {
                        buildDisplay.addSearchString("§eCurrent area: $coloredName")
                    } else {
                        buildDisplay.addSearchString("§7Not in an area.")
                    }
                }

                buildDisplay.addSearchString("§eAreas nearby:")
                continue
            }

            if (name == "no_area") continue
            if (!isConfigVisible) continue
            foundAreas++

            buildDisplay.add(build(text, tag, node.name, distance, node, name))
        }
        if (foundAreas == 0) {
            val islandName = SkyBlockUtils.currentIsland.displayName
            if (foundCurrentArea) {
                buildDisplay.addSearchString("§cThere is only one area in $islandName,")
                buildDisplay.addSearchString("§cnothing else to navigate to!")
            } else {
                buildDisplay.addSearchString("§cThere is no $islandName area data available yet!")
            }
        }
        buildDisplay.let {
            display = it.buildSearchBox(textInput)
        }
    }

    private fun build(
        text: String,
        tag: GraphNodeTag,
        name: String,
        distance: Int,
        node: GraphNode,
        name2: String,
    ): Searchable = Renderable.clickable(
        text,
        tips = buildList {
            add(tag.color.getChatColor() + name)
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
    ).toSearchable(name2)
}
