package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.GraphEditor.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.CollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.CollectionUtils.addString
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.buildSearchableScrollable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GraphNodeEditor {

    private val scrollValue = ScrollValue()
    private val textInput = TextInput()
    private var nodesDisplay = emptyList<Searchable>()
    private var lastUpdate = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onGuiRender(event: GuiRenderEvent) {
        if (!isEnabled()) return


        config.namedNodesList.renderRenderables(
            buildList {
                val list = getNodeNames()
                val size = list.size
                addString("§eGraph Nodes: $size")
                val height = (size * 10).coerceAtMost(250)
                if (list.isNotEmpty()) {
                    add(list.buildSearchableScrollable(height, textInput, scrollValue, velocity = 10.0))
                }
            },
            posLabel = "Graph Nodes List",
        )
    }

    private fun getNodeNames(): List<Searchable> {
        if (lastUpdate.passedSince() > 250.milliseconds) {
            updateNodeNames()
        }
        return nodesDisplay
    }

    private fun updateNodeNames() {
        lastUpdate = SimpleTimeMark.now()
        nodesDisplay = drawNodeNames()
    }

    private fun updateTagView(node: GraphingNode) {
        lastUpdate = SimpleTimeMark.now() + 60.seconds
        nodesDisplay = drawTagNames(node)
    }

    private fun drawTagNames(node: GraphingNode): List<Searchable> = buildList {
        addSearchString("§eChange tag for node '${node.name}§e'")
        addSearchString("")

        for (tag in GraphNodeTag.entries) {
            val state = if (tag in node.tags) "§aYES" else "§cNO"
            val name = state + " §r" + tag.displayName
            add(createTagName(name, tag, node))
        }
        addSearchString("")
        add(
            Renderable.clickAndHover(
                "§cGo Back!",
                tips = listOf("§eClick to go back to the node list!"),
                onClick = {
                    updateNodeNames()
                },
            ).toSearchable(),
        )
    }

    private fun createTagName(
        name: String,
        tag: GraphNodeTag,
        node: GraphingNode,
    ) = Renderable.clickAndHover(
        name,
        tips = listOf(
            "Tag ${tag.name}",
            "§7${tag.description}",
            "",
            "§eClick to set tag for ${node.name} to ${tag.name}!",
        ),
        onClick = {
            if (tag in node.tags) {
                node.tags.remove(tag)
            } else {
                node.tags.add(tag)
            }
            updateTagView(node)
        },
    ).toSearchable(name)

    private fun drawNodeNames(): List<Searchable> = buildList {
        for ((node, distance: Double) in GraphEditor.nodes.map { it to it.position.distanceSqToPlayer() }.sortedBy { it.second }) {
            val name = node.name?.takeIf { !it.isBlank() } ?: continue
            val color = if (node == GraphEditor.activeNode) "§a" else "§7"
            val distanceFormat = sqrt(distance).toInt().addSeparators()
            val tagText = node.tags.let {
                if (it.isEmpty()) {
                    " §cNo tag§r"
                } else {
                    val text = node.tags.map { it.internalName }.joinToString(", ")
                    " §f($text)"
                }
            }

            val text = "${color}Node §r$name$tagText §7[$distanceFormat]"
            add(createNodeTextLine(text, name, node))
        }
    }

    private fun MutableList<Searchable>.createNodeTextLine(
        text: String,
        name: String,
        node: GraphingNode,
    ): Searchable = Renderable.clickAndHover(
        text,
        tips = buildList {
            add("Node '$name'")
            add("")

            if (node.tags.isNotEmpty()) {
                add("Tags: ")
                for (tag in node.tags) {
                    add(" §8- §r${tag.displayName}")
                }
                add("")
            }

            add("§eClick to select/deselect this node!")
            add("§eControl-Click to edit the tags for this node!")

        },
        onClick = {
            if (KeyboardManager.isModifierKeyDown()) {
                updateTagView(node)
            } else {
                GraphEditor.activeNode = node
                updateNodeNames()
            }
        },
    ).toSearchable(name)

    fun isEnabled() = GraphEditor.isEnabled()
    private val config get() = GraphEditor.config

}
