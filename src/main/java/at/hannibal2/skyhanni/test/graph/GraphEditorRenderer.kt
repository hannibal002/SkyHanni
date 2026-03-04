package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.dev.GraphConfig
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.GraphUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawPyramid
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import java.awt.Color
import kotlin.math.min

@SkyHanniModule
object GraphEditorRenderer {

    val config: GraphConfig get() = SkyHanniMod.feature.dev.devTool.graph

    private val state get() = GraphEditor.state

    private val nodes get() = state.nodes
    private val edges get() = state.edges
    private val closestNode get() = state.closestNode
    private val inEditMode get() = state.inEditMode
    private val inTextMode get() = state.inTextMode
    private val activeNode get() = state.activeNode
    private val dissolvePossible get() = state.dissolvePossible
    private val selectedEdge get() = state.selectedEdge
    private val textBox get() = state.textBox
    private val seeThroughBlocks get() = state.seeThroughBlocks
    private val highlightedEdges get() = state.highlightedEdges
    private val highlightedNodes get() = state.highlightedNodes

    private val nodeColor = LorenzColor.BLUE.addOpacity(200)
    private val activeColor = LorenzColor.GREEN.addOpacity(200)
    private val closestColor = LorenzColor.YELLOW.addOpacity(200)
    private val dijkstraColor = LorenzColor.LIGHT_PURPLE.addOpacity(200)

    private val edgeColor = LorenzColor.GOLD.addOpacity(150)
    private val edgeDijkstraColor = LorenzColor.DARK_BLUE.addOpacity(150)
    private val edgeSelectedColor = LorenzColor.DARK_RED.addOpacity(150)

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        nodes.forEach { event.drawNode(it) }
        edges.forEach { event.drawEdge(it) }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRenderOverlay() {
        if (!isEnabled()) return
        config.infoDisplay.renderRenderables(buildDisplay(), posLabel = "Graph Info")
    }

    private fun buildDisplay(): List<Renderable> = buildList {
        if (GraphEditor.hideDisabled) {
            add("§cDisabled nodes are hidden!")
        }
        add("§eExit: §6${KeyboardManager.getKeyName(config.exitKey)}")
        if (!inEditMode && !inTextMode) {
            add("§ePlace: §6${KeyboardManager.getKeyName(config.placeKey)}")
            add("§eSelect: §6${KeyboardManager.getKeyName(config.selectKey)}")
            add("§eSelect (Look): §6${KeyboardManager.getKeyName(config.selectRaycastKey)}")
            add("§eConnect: §6${KeyboardManager.getKeyName(config.connectKey)}")
            add("§eTest Dijkstra: §6${KeyboardManager.getKeyName(config.dijkstraKey)}")
            add("§eVision: §6${KeyboardManager.getKeyName(config.throughBlocksKey)}")
            add("§eSave: §6${KeyboardManager.getKeyName(config.saveKey)}")
            add("§eLoad: §6${KeyboardManager.getKeyName(config.loadKey)}")
            add("§eClear: §6${KeyboardManager.getKeyName(config.clearKey)}")
            add("§eTutorial: §6${KeyboardManager.getKeyName(config.tutorialKey)}")
            GraphEditorHistory.addDisplayLines(this)
            add(" ")
            if (activeNode != null) {
                add("§eText: §6${KeyboardManager.getKeyName(config.textKey)}")
                if (dissolvePossible) add("§eDissolve: §6${KeyboardManager.getKeyName(config.dissolveKey)}")
                if (selectedEdge != null) {
                    add("§eSplit: §6${KeyboardManager.getKeyName(config.splitKey)}")
                    add("§eCycle Direction: §6${KeyboardManager.getKeyName(config.edgeCycle)}")
                }
            }
        }

        if (!inTextMode) {
            if (activeNode != null) {
                add("§eEdit active node: §6${KeyboardManager.getKeyName(config.editKey)}")
            }
        }

        if (inEditMode) {
            add("§ex+ §6${KeyboardManager.getKeyName(KeyboardManager.WasdInputMatrix.w.key.value)}")
            add("§ex- §6${KeyboardManager.getKeyName(KeyboardManager.WasdInputMatrix.s.key.value)}")
            add("§ez+ §6${KeyboardManager.getKeyName(KeyboardManager.WasdInputMatrix.a.key.value)}")
            add("§ez- §6${KeyboardManager.getKeyName(KeyboardManager.WasdInputMatrix.d.key.value)}")
            add("§ey+ §6${KeyboardManager.getKeyName(KeyboardManager.WasdInputMatrix.up.key.value)}")
            add("§ey- §6${KeyboardManager.getKeyName(KeyboardManager.WasdInputMatrix.down.key.value)}")
        }
        if (inTextMode) {
            add("§eFormat: ${textBox.finalText()}")
            add("§eRaw:     ${textBox.editText(textColor = LorenzColor.YELLOW)}")
        }
    }.map { StringRenderable.from(it) }

    private fun SkyHanniRenderWorldEvent.drawNode(node: GraphingNode) {
        if (!node.rendering) return
        if (GraphEditor.hideDisabled && !node.enabled) return
        this.drawWaypointFilled(
            node.position,
            node.getNodeColor(),
            seeThroughBlocks = seeThroughBlocks,
            minimumAlpha = 0.2f,
            inverseAlphaScale = true,
        )

        val showTextAlways = seeThroughBlocks || node.distanceSqToPlayer() < 100

        fun draw(text: String, yOff: Float) {
            this.drawDynamicText(
                node.position,
                text,
                scaleMultiplier = 0.8,
                seeThroughBlocks = showTextAlways,
                smallestDistanceVew = 12.0,
                ignoreY = true,
                yOff = yOff,
                maxDistance = 80,
            )
        }

        if (!node.enabled) {
            draw("§cDisabled", yOff = -30f)
        }

        val nodeName = if (inTextMode && node == activeNode) {
            textBox.finalText().ifEmpty { null }
        } else {
            node.name
        }
        if (nodeName != null) {
            draw(nodeName, yOff = -15f)
        }

        val tags = node.tags
        if (tags.isEmpty()) return
        val tagText = tags.joinToString(" §f+ ") { it.displayName }
        draw(tagText, yOff = 0f)
    }

    private fun SkyHanniRenderWorldEvent.drawEdge(edge: GraphingEdge) {
        if (!edge.node1.rendering && !edge.node2.rendering) return
        val color = when {
            selectedEdge == edge -> edgeSelectedColor
            edge in highlightedEdges -> edgeDijkstraColor
            else -> edge.networkColor ?: edgeColor
        }

        draw3DLine(
            edge.node1.position.add(0.5, 0.5, 0.5),
            edge.node2.position.add(0.5, 0.5, 0.5),
            color,
            7,
            !seeThroughBlocks,
        )
        if (edge.direction != EdgeDirection.BOTH) {
            drawDirection(edge, color)
        }
    }

    private fun SkyHanniRenderWorldEvent.drawDirection(edge: GraphingEdge, color: Color) {
        val lineVec = edge.node2.position - edge.node1.position
        val center = edge.node1.position + lineVec / 2.0
        val quad1 = edge.node1.position + lineVec / 4.0
        val quad2 = edge.node1.position + lineVec * (3.0 / 4.0)

        val pyramidSize =
            lineVec.normalize().times(min(lineVec.length() / 10.0, 1.0)) * (if (edge.direction == EdgeDirection.ONE_TO_TWO) 1.0 else -1.0)

        val lineOffsetVec = LorenzVec(0.5, 0.5, 0.5)

        fun pyramidDraw(
            pos: LorenzVec,
        ) {
            this.drawPyramid(
                pos + lineOffsetVec + pyramidSize,
                pos + lineOffsetVec,
                pos.crossProduct(lineVec).normalize().times(pyramidSize.length() / 2.5) + pos + lineOffsetVec,
                color,
            )
        }

        pyramidDraw(center)
        pyramidDraw(quad1)
        pyramidDraw(quad2)
    }

    private fun GraphingNode.getNodeColor() = when (this) {
        activeNode -> if (this == closestNode) ColorUtils.blendRGB(activeColor, closestColor, 0.5) else activeColor
        closestNode -> closestColor
        in highlightedNodes -> dijkstraColor
        else -> nodeColor
    }

    private fun isEnabled() = GraphEditor.isEnabled()

}
