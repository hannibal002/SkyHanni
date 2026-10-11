package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.LorenzVecArgumentType
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.onClick
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sorted
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings

@SkyHanniModule
object NavigationHelper {
    private val config get() = SkyHanniMod.feature.misc.navigation

    private val messageId = ChatUtils.getUniqueMessageId()

    private val commandCoroutine = CoroutineSettings("shnavigate command")

    val allowedSingleNavigationTags = setOf(
        GraphNodeTag.NPC,
        GraphNodeTag.AREA,
        GraphNodeTag.SMALL_AREA,
        GraphNodeTag.POI,
        GraphNodeTag.SLAYER,
        GraphNodeTag.GRIND_MOBS,
        GraphNodeTag.GRIND_ORES,
        GraphNodeTag.GRIND_CROPS,
        GraphNodeTag.CRIMSON_MINIBOSS,
    )

    private fun doCommandAsync(searchTerm: String, allowInstant: Boolean = true) {
        commandCoroutine.launch {
            runCommand(searchTerm, allowInstant)
        }
    }

    private fun runCommand(searchTerm: String, allowInstant: Boolean) {
        val distances = calculateDistances(searchTerm)
        val locations = calculateNames(distances)

        // going back always shows the list, otherwise an exact match would just navigate again
        val goBack = {
            IslandGraphs.stopNavigation()
            doCommandAsync(searchTerm, allowInstant = false)
        }
        val title = if (searchTerm.isBlank()) "SkyHanni Navigation Locations" else "SkyHanni Navigation Locations Matching: \"$searchTerm\""

        if (allowInstant && config.allowInstantNavigation) {
            val exactMatch = locations.firstOrNull { (name, _) ->
                name.substringBefore(" §7(").equals(searchTerm, ignoreCase = true)
            }

            val target = exactMatch ?: locations.takeIf { it.size == 1 }?.first()

            if (target != null) {
                val (name, node) = target
                node.pathFind(label = name, allowRerouting = true, condition = { true })

                val message = if (exactMatch != null) {
                    "§7Exact match found, navigating to §r$name"
                } else {
                    "§7Only one location found, navigating to §r$name"
                }
                sendNavigateMessageWithContent(message, goBack)
                return
            }
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
            val tag = node.tags.first { it in allowedSingleNavigationTags }
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
            val tag = node.tags.first { it in allowedSingleNavigationTags }
            val name = "${node.cleanName} §7(${tag.displayName}§7)"
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

        val shortestDistances = GraphUtils.findAllShortestDistances(closestNode).distances

        val distances = mutableMapOf<GraphNode, Double>()
        for (node in graph) {
            if (!node.enabled) continue
            val name = node.cleanName ?: continue
            val remainingTags = node.tags.filter { it in allowedSingleNavigationTags }
            if (remainingTags.isEmpty()) continue
            if (name.lowercase().contains(searchTerm)) {
                // unreachable nodes fall back to 0.0 and therefore sort to the front, same as before
                distances[node] = shortestDistances[node] ?: 0.0
            }
            if (remainingTags.size != 1) {
                println("found node with invalid amount of tags: ${node.name} (${remainingTags.map { it.cleanName }}")
            }
        }
        return distances
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shnavigate") {
            description = "Use the path finder to go to a specific location"
            aliases = listOf("shnav")
            argCallback("coords", LorenzVecArgumentType.double()) { location ->
                pathFind(location, "Custom Goal", condition = { true })
                ChatUtils.chat("Started Navigating to custom goal at §f${location.toLocalFormat()}", messageId = messageId)
            }
            argCallback("search", BrigadierArguments.greedyString(), BrigadierUtils.dynamicSuggestionProvider { getNames() }) {
                doCommandAsync(it.lowercase().removeColor())
            }
            literalCallback("stop") {
                IslandGraphs.manualCancel()
            }
            simpleCallback {
                doCommandAsync("")
            }
        }
    }

    private fun getNames(): List<String> {
        val graph = IslandGraphs.currentIslandGraph ?: return emptyList()
        return graph.filterByActive { it.isValidAreaNode() }.mapNotNull { it.cleanName }
    }

    private fun GraphNode.isValidAreaNode(): Boolean {
        val name = name ?: return false
        if (name == AreaNode.NO_AREA) return false
        if (name == SkyBlockUtils.graphArea) return false
        return tags.any { it in allowedSingleNavigationTags }
    }
}
