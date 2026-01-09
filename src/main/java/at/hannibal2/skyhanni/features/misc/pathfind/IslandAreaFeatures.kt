package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.data.title.TitleContext
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.AreaNodesUpdatedEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.features.misc.pathfind.IslandAreaBackend.getAreaTag
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
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
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object IslandAreaFeatures {
    private val config get() = SkyHanniMod.feature.misc.navigation
    private val areaListConfig get() = config.areasList

    var display: Renderable? = null
    private var smallAreas = setOf<String>()
    private var targetNode: GraphNode? = null
    private val textInput = SearchTextInput()
    private val areaNodes get() = IslandAreaBackend.areaNodes
    private var visibleAreaNodes = listOf<AreaNode>()
    private var currentTitle: TitleContext? = null
    private var lastTitleTime = SimpleTimeMark.farPast()

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
                IslandAreaBackend.update()
            },
            allowRerouting = true,
            condition = ::isAreaListEnabled,
        )
        IslandAreaBackend.update()
    }

    @HandleEvent
    fun onAreaChange(event: GraphAreaChangeEvent) {
        val name = event.area
        val inAnArea = name != AreaNode.NO_AREA
        // when this is a small area move and small areas are disabled via config
        if (!config.includeSmallAreas && name in smallAreas) return

        currentTitle?.stop()
        if (event.onlyInternal) return
        if (!inAnArea || !config.enterTitle) return
        if (lastTitleTime.passedSince() < 2.seconds) return

        currentTitle = TitleManager.sendTitle("§aEntered $name!")
        lastTitleTime = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return

        if (!config.showInWorld) return
        visibleAreaNodes = areaNodes.filter { area ->
            !area.isInside &&
                !area.isNoArea &&
                area.isConfigVisible &&
                area.node.position.canBeSeen(40.0)
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (!config.showInWorld) return
        for (area in visibleAreaNodes) {
            event.drawDynamicText(area.node.position, area.coloredName, 1.5)
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRenderOverlay() {
        if (!isAreaListEnabled()) return
        if (!areaListConfig.showAlways) return
        val isInOwnInventory = Minecraft.getInstance().screen is InventoryScreen
        if (!isInOwnInventory) {
            doRender()
        }
    }

    @HandleEvent(GuiRenderEvent.ChestGuiOverlayRenderEvent::class)
    fun onBackgroundDraw() {
        if (!isAreaListEnabled()) return
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

    @HandleEvent
    fun onWorldChange() {
        display = null
        targetNode = null
        currentTitle?.stop()
    }

    private fun isAreaListEnabled(): Boolean = isEnabled() && areaListConfig.enabled.get()

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && IslandGraphs.currentIslandGraph != null

    @HandleEvent(AreaNodesUpdatedEvent::class)
    fun onAreaNodesUpdated() {
        smallAreas = areaNodes
            .filter { GraphNodeTag.SMALL_AREA in it.node.tags }
            .map { it.name }
            .toSet()
    }

    fun redrawList() {
        if (!isAreaListEnabled()) {
            display = null
            return
        }
        display = createDisplay().buildSearchBox(textInput)
    }


    fun createDisplay(): List<Searchable> = buildList {
        if (areaNodes.isEmpty()) {
            addSearchString("§cThere is no ${SkyBlockUtils.currentIsland.displayName} area data available yet!")
            return@buildList
        }

        val current = areaNodes.first()
        val nearby = areaNodes.drop(1)
        if (areaListConfig.includeCurrentArea.get()) {
            if (!current.isNoArea && current.isConfigVisible) {
                addSearchString("§eCurrent area: ${current.tag.color.getChatColor()}${current.name}")
            } else {
                addSearchString("§7Not in an area.")
            }
        }

        val visibleNearby = nearby.filter { !it.isNoArea && it.isConfigVisible }
        if (visibleNearby.isEmpty()) {
            addSearchString("§cThere is only one area in ${SkyBlockUtils.currentIsland.displayName}")
            addSearchString("§cnothing else to navigate to!")
            return@buildList
        }

        addSearchString("§eAreas nearby:")
        for (area in visibleNearby) {
            // Compare by name as multiple nodes can share names
            val isTarget = area.name == targetNode?.name
            val color = if (isTarget) LorenzColor.GOLD else area.tag.color
            val coloredName = "${color.getChatColor()}${area.name}"
            val distance = area.distance.roundTo(0).toInt()

            add(buildAreaEntry(coloredName, area, distance))
        }
    }

    private fun buildAreaEntry(displayText: String, area: AreaNode, distance: Int): Searchable = Renderable.clickable(
        "$displayText§7: §e$distance",
        tips = buildList {
            add("${area.tag.color.getChatColor()}${area.name}")
            add("§7Type: ${area.tag.displayName}")
            add("§7Distance: §e$distance blocks")
            add("")
            if (area.node == targetNode) {
                add("§aPath Finder points to this!")
                add("")
                add("§eClick to stop navigating!")
            } else {
                add("§eClick to find a path!")
            }
        },
        onLeftClick = {
            if (area.node == targetNode) {
                targetNode = null
                IslandGraphs.stop()
                IslandAreaBackend.update()
            } else {
                setTarget(area.node)
            }
        },
    ).toSearchable(area.name)
}
