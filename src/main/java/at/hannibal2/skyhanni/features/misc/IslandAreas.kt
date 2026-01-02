package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.IslandGraphReloadEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sorted
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import net.minecraft.client.player.LocalPlayer
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object IslandAreas {
    private val config get() = SkyHanniMod.feature.misc.navigation

    private val areaListConfig get() = config.areasList

    var nodes = mapOf<GraphNode, Double>()
    private var paths = mapOf<GraphNode, Graph>()
    private var nodeSaveJob: Job? = null
    private val nodeSaveMutex = Mutex()

    var currentArea = ""
        private set

    @HandleEvent
    fun onWorldChange() {
        nodes = emptyMap()
        IslandAreaFeatures.reset()
        hasMoved = true
        updateArea("no_area", onlyInternal = true)
    }

    fun nodeMoved() {
        if (nodeSaveJob?.isActive == true) return
        nodeSaveJob = SkyHanniMod.launchCoroutineWithMutex("§island area node moved", nodeSaveMutex) {
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
        IslandAreaFeatures.updateNodes(finalNodes)
    }

    private var hasMoved = false

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled() || !event.isMod(2) || !hasMoved) return
        update(shouldBuildDisplay = isPathfinderEnabled())
        hasMoved = false
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        if (isEnabled() && event.isLocalPlayer) {
            hasMoved = true
        }
    }

    @HandleEvent(IslandGraphReloadEvent::class)
    fun onIslandGraphReload() {
        nodeMoved()

        DelayedRun.runDelayed(150.milliseconds) {
            update(shouldBuildDisplay = isPathfinderEnabled())
        }
    }

    // updating the position (mandatory for all other features), and builds the display (optionally)
    fun update(shouldBuildDisplay: Boolean = true) {
        nodes.keys
            .firstOrNull { it.name != null }
            ?.let { node ->
                val isConfigVisible = node.getAreaTag(useConfig = true) != null
                updateArea(node.name!!, onlyInternal = !isConfigVisible)
            }

        if (shouldBuildDisplay) {
            IslandAreaFeatures.redraw()
        }
    }

    private fun updateArea(name: String, onlyInternal: Boolean) {
        if (name != currentArea) {
            val oldArea = currentArea
            currentArea = name
            GraphAreaChangeEvent(name, oldArea, onlyInternal).post()
        }
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        with(areaListConfig) {
            ConditionalUtils.onToggle(color, includeCurrentArea, enabled) {
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

    private fun isPathfinderEnabled(): Boolean = areaListConfig.enabled.get()

    private fun isEnabled() = IslandGraphs.currentIslandGraph != null

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(114, "misc.areaNavigation", "misc.navigation")
        event.move(115, "misc.navigation.pathfinder", "misc.navigation.areasList")
        event.move(115, "misc.navigation.inWorld", "misc.navigation.showInWorld")
    }
}
