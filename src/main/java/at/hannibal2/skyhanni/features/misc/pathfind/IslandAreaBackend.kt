package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.events.AreaNodesUpdatedEvent
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
object IslandAreaBackend {
    private val config get() = SkyHanniMod.feature.misc.navigation

    private val areaListConfig get() = config.areasList

    var areaNodes = listOf<AreaNode>()
    private var nodeUpdateJob: Job? = null
    private val nodeSaveMutex = Mutex()

    var currentArea = ""
        private set

    @HandleEvent
    fun onWorldChange() {
        areaNodes = emptyList()
        hasMoved = true
        updateArea(AreaNode.NO_AREA, onlyInternal = true)
    }

    fun nodeMoved() {
        if (nodeUpdateJob?.isActive == true) return
        nodeUpdateJob = SkyHanniMod.launchCoroutineWithMutex("island area node moved", nodeSaveMutex) {
            updateNodes()
        }
    }

    private fun updateNodes() {
        if (!isEnabled()) return
        val graph = IslandGraphs.currentIslandGraph ?: return
        val closestNode = IslandGraphs.closestNode ?: return

        val (_, map) = GraphUtils.findFastestPaths(graph, closestNode) { it.getAreaTag() != null }

        val seenAreas = mutableSetOf<String>()
        this.areaNodes = map.sorted().mapNotNull { (node, distance) ->
            val name = node.name?.takeIf { it !in seenAreas } ?: return@mapNotNull null
            val tag = node.getAreaTag() ?: return@mapNotNull null
            seenAreas += name
            AreaNode(node, name, tag, distance)
        }
        AreaNodesUpdatedEvent().post()
    }

    private var hasMoved = false

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled() || !event.isMod(2) || !hasMoved) return
        update()
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
            update()
        }
    }

    // Updates the current area position and redraws the area list.
    fun update() {
        areaNodes.firstOrNull()?.let { area ->
            updateArea(area.name, onlyInternal = !area.isConfigVisible)
        }

        IslandAreaFeatures.redrawList()
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

    private fun isEnabled() = IslandGraphs.currentIslandGraph != null

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(114, "misc.areaNavigation", "misc.navigation")
        event.move(115, "misc.navigation.pathfinder", "misc.navigation.areasList")
        event.move(115, "misc.navigation.inWorld", "misc.navigation.showInWorld")
    }
}
