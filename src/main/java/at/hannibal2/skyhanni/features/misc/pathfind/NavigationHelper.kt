package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.LorenzVecArgumentType
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.onClick
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sorted
import at.hannibal2.skyhanni.utils.compat.hover

@SkyHanniModule
object NavigationHelper {
    private val config get() = SkyHanniMod.feature.misc.navigation

    private val messageId = ChatUtils.getUniqueMessageId()

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

    private fun doCommandAsync(searchTerm: String) {
        val distances = calculateDistances(searchTerm)
        val locations = calculateNames(distances)

        val goBack = {
            doCommandAsync(searchTerm)
            IslandGraphs.stop()
        }
        val title = if (searchTerm.isBlank()) "SkyHanni Navigation Locations" else "SkyHanni Navigation Locations Matching: \"$searchTerm\""

        if (config.allowInstantNavigation && locations.size == 1) {
            val (name, node) = locations.first()
            node.pathFind(label = name, allowRerouting = true, condition = { true })
            sendNavigateMessageWithContent("§7Only one location found, navigating to §r$name", goBack)
            return
        }

        TextHelper.displayPaginatedList(
            title,
            locations,
            chatLineId = messageId,
            emptyMessage = "No locations found.",
        ) { (name, node) ->
            val distance = distances[node]!!.roundTo(1)
            val component = "$name §e$distance".asComponent()
            component.onClick {
                node.pathFind(label = name, allowRerouting = true, condition = { true })
                sendNavigateMessage(name, goBack)
            }
            val tag = node.tags.first { it in allowedTags }
            val hoverText = "Name: $name\n§7Type: §r${tag.displayName}\n§7Distance: §e$distance blocks\n§eClick to start navigating!"
            component.hover = hoverText.asComponent()
            component
        }
    }

    private fun sendNavigateMessageWithContent(content: String, goBack: () -> Unit) {
        val componentText = content.asComponent()
        componentText.onClick(onClick = goBack)
        componentText.hover = "§eClick to stop navigating and return to previous search".asComponent()
        componentText.send(messageId)
    }

    private fun sendNavigateMessage(name: String, goBack: () -> Unit) =
        sendNavigateMessageWithContent("§7Started navigating to §r$name§7. ", goBack)

    private fun calculateNames(distances: Map<GraphNode, Double>): List<Pair<String, GraphNode>> {
        val names = mutableMapOf<String, GraphNode>()
        for (node in distances.sorted().keys) {
            // hiding areas that are none
            if (node.name == AreaNode.NO_AREA) continue
            // no need to navigate to the current area
            if (node.name == SkyBlockUtils.graphArea) continue
            val tag = node.tags.first { it in allowedTags }
            val name = "${node.name} §7(${tag.displayName}§7)"
            if (name in names) continue
            names[name] = node
        }
        return names.toList()
    }

    private fun calculateDistances(
        searchTerm: String,
    ): Map<GraphNode, Double> {
        val graph = IslandGraphs.currentIslandGraph ?: return emptyMap()
        val closestNode = IslandGraphs.closestNode ?: return emptyMap()

        val distances = mutableMapOf<GraphNode, Double>()
        for (node in graph) {
            val name = node.name ?: continue
            val remainingTags = node.tags.filter { it in allowedTags }
            if (remainingTags.isEmpty()) continue
            if (name.lowercase().contains(searchTerm)) {
                distances[node] = GraphUtils.findShortestDistance(closestNode, node)
            }
            if (remainingTags.size != 1) {
                println("found node with invalid amount of tags: ${node.name} (${remainingTags.map { it.cleanName }}")
            }
        }
        return distances
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shnavigate") {
            description = "Using path finder to go to locations"
            aliases = listOf("shnav")
            argCallback("coords", LorenzVecArgumentType.double()) { location ->
                pathFind(location.add(-1, -1, -1), "Custom Goal", condition = { true })
                with(location) {
                    ChatUtils.chat("Started Navigating to custom goal at §f$x $y $z", messageId = messageId)
                }
            }
            argCallback("search", BrigadierArguments.greedyString(), BrigadierUtils.dynamicSuggestionProvider { getNames() }) {
                SkyHanniMod.launchCoroutine("shnavigate command") {
                    doCommandAsync(it.lowercase())
                }
            }
        }
    }

    private fun getNames(): List<String> {
        val graph = IslandGraphs.currentIslandGraph ?: return emptyList()
        return graph.filter { it.isValidAreaNode() }.mapNotNull { it.name }
    }

    private fun GraphNode.isValidAreaNode(): Boolean {
        val name = name ?: return false
        if (name == AreaNode.NO_AREA) return false
        if (name == SkyBlockUtils.graphArea) return false
        return tags.any { it in allowedTags }
    }
}
