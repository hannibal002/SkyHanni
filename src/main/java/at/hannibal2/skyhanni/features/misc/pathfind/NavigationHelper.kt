package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.data.model.findShortestDistance
import at.hannibal2.skyhanni.utils.CollectionUtils.sorted
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.chat.Text
import at.hannibal2.skyhanni.utils.chat.Text.asComponent
import at.hannibal2.skyhanni.utils.chat.Text.center
import at.hannibal2.skyhanni.utils.chat.Text.hover
import at.hannibal2.skyhanni.utils.chat.Text.onClick
import at.hannibal2.skyhanni.utils.chat.Text.send
import kotlinx.coroutines.launch
import net.minecraft.util.IChatComponent

object NavigationHelper {
    private val NAVIGATION_CHAT_ID = -6457562

    val allowedTags = listOf(
        GraphNodeTag.NPC,
        GraphNodeTag.AREA,
        GraphNodeTag.SMALL_AREA,
        GraphNodeTag.POI,
        GraphNodeTag.SLAYER,
        GraphNodeTag.GRIND_MOBS,
        GraphNodeTag.GRIND_ORES,
        GraphNodeTag.GRIND_CROPS,
        GraphNodeTag.MINES_EMISSARY,
        GraphNodeTag.CRIMSON_MINIBOSS,
    )

    fun onCommand(args: Array<String>) {
        SkyHanniMod.coroutineScope.launch {
            asyncTest(args)
        }
    }

    private fun asyncTest(args: Array<String>) {
        val searchTerm = args.joinToString(" ").lowercase()
        val distances = calculateDistances(searchTerm)
        val names = calculateNames(distances)

        val text = mutableListOf<IChatComponent>()
        text.add(Text.createDivider())
        text.add("§7Found ${names.size} locations ($searchTerm)".asComponent().center())
        val goBack = {
            onCommand(searchTerm.split(" ").toTypedArray())
            IslandGraphs.stop()
        }
        // TODO dont show a too long list, add pages
        for ((name, node) in names) {
            val distance = distances[node]!!.round(1)
            val component = "$name §e$distance".asComponent()
            component.onClick {
                IslandGraphs.pathFind(node.position)
                sendNavigateMessage(name, goBack)
            }
            val tag = node.tags.first { it in allowedTags }
            component.hover = ("§eClick to start navigating to\n" + "Type: ${tag.displayName}\n" + "Distance: $distance").asComponent()
            text.add(component)
        }
        text.add(Text.createDivider())
        Text.multiline(text).send(NAVIGATION_CHAT_ID)
    }

    private fun sendNavigateMessage(name: String, goBack: () -> Unit) {
        val componentText = "§7Navigating to §r$name".asComponent()
        componentText.onClick(onClick = goBack)
        componentText.send(NAVIGATION_CHAT_ID)
    }

    private fun calculateNames(distances: Map<GraphNode, Double>): MutableMap<String, GraphNode> {
        val names = mutableMapOf<String, GraphNode>()
        for (node in distances.sorted().keys) {
            val tag = node.tags.first { it in allowedTags }
            val name = "${node.name} §7(${tag.displayName}§7)"
            if (name in names) continue
            names[name] = node
        }
        return names
    }

    private fun calculateDistances(
        searchTerm: String,
    ): Map<GraphNode, Double> {
        val grapth = IslandGraphs.currentIslandGraph ?: return emptyMap()
        val closedNote = IslandGraphs.closedNote ?: return emptyMap()

        val nodes = grapth.nodes
        val distances = mutableMapOf<GraphNode, Double>()
        for (node in nodes) {
            val name = node.name ?: continue
            if (!node.tags.any { it in allowedTags }) continue
            if (name.lowercase().contains(searchTerm)) {
                distances[node] = grapth.findShortestDistance(closedNote, node)
            }
            val remainingTags = node.tags.filter { it in allowedTags }
            if (remainingTags.size != 1) {
                println("found node with invalid amount of tags: ${node.name} (${remainingTags.map { it.cleanName }}")
            }
        }
        return distances
    }

}
