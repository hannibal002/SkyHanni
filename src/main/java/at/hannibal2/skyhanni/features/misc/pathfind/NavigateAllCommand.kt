package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.StringUtils

@SkyHanniModule
object NavigateAllCommand {

    private val allowedMultiNavigationTags = setOf(
        GraphNodeTag.HOPPITY,
        GraphNodeTag.RIFT_EFFIGY,
        GraphNodeTag.RIFT_MONTEZUMA,
        GraphNodeTag.CRIMSON_MINIBOSS,
        GraphNodeTag.SPIDER_RELIC,
        GraphNodeTag.END_GOLEM,
        GraphNodeTag.FISHING_HOTSPOT,
        GraphNodeTag.FISHING_WORMHOLE,
        GraphNodeTag.FAIRY_SOUL,
        GraphNodeTag.HIDEONLEAF,
        GraphNodeTag.HIDEONSUN,
        GraphNodeTag.TREE_PROTECTION_ORDER,
        GraphNodeTag.HONEYHIVE,
        GraphNodeTag.SAFARI_BELL,
        GraphNodeTag.HIDEYHO_LOCATION,
        GraphNodeTag.PANGOLIN,
        GraphNodeTag.SANGER,
        GraphNodeTag.FLOOR_DROPS,
    )

    /**
     * Navigate to all nodes with the selected [GraphNodeTag]
     */
    private fun navigateAllCommand(nodeType: GraphNodeTag) {
        if (nodeType !in getValidTagNames()) {
            ChatUtils.userError("${nodeType.displayName} §cis invalid for navigation on this island!")
            return
        }

        val graph = IslandGraphs.currentIslandGraph ?: return
        val targetNodes = graph.getNodesWithTags(nodeType)

        NavigateAllApi.navigateAll(
            targetNodes,
            nodeType.displayName,
            nodeType.color.toColor(),
            onFinish = {
                ChatUtils.chat("Reached all ${StringUtils.pluralize(targetNodes.size, nodeType.displayName, withNumber = true)}§e.")
            },
            continueNavigationCondition = NavigationCondition.None,
            condition = { true },
        )
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shnavigateall") {
            description = "Use the path finder to go to all locations of a type"
            aliases = listOf("shnavall")

            argCallback(
                "nodeType",
                EnumArgumentType.filtered<GraphNodeTag>(
                    { it.cleanName.replace(" ", "_") },
                    isGreedy = true,
                ) { it in allowedMultiNavigationTags },
                BrigadierUtils.dynamicSuggestionProvider { getValidTagNames().map { it.cleanName } },
            ) { nodeType ->
                navigateAllCommand(nodeType)
            }
            literalCallback("skip") {
                NavigateAllApi.handleSkip()
            }
            literalCallback("stop") {
                NavigateAllApi.handleStop(manual = true)
            }
            simpleCallback {
                ChatUtils.userError("Usage: /shnavigateall <location type>")
            }
        }
    }

    private fun getValidTagNames(): Set<GraphNodeTag> {
        val activeTags = IslandGraphs.currentIslandGraph?.getActiveNodeTags() ?: return emptySet()
        return activeTags.filter { it in allowedMultiNavigationTags }.toSet()
    }
}
