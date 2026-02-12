package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.dev.GraphConfig
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.KeyMapping
import net.minecraft.client.player.LocalPlayer
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GraphEditor {

    val config: GraphConfig get() = SkyHanniMod.feature.dev.devTool.graph

    var state = GraphEditorState()
        set(value) {
            if (field.inTextMode) {
                field.inTextMode = false
            }
            field = value
            updateRender()
            GraphNodeEditor.updateNodeNames()
        }

    fun isEnabled(): Boolean = config.enabled

    private val nodes get() = state.nodes
    private val inTutorialMode get() = state.inTutorialMode
    private val inEditMode get() = state.inEditMode

    fun feedBackInTutorial(text: String) {
        if (inTutorialMode) {
            ChatUtils.chat(text)
        }
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        GraphEditorInput.input()
        if (event.isMod(5)) {
            updateRender()
        }
        if (nodes.isEmpty()) return

        // Update cache every second for normal movement
        if (state.lastCacheUpdate.passedSince() > 1.seconds) {
            updateCache()
        }

        state.closestNode = state.cachedNearbyNodes.minByOrNull { it.distanceSqToPlayer() }

        GraphEditorNodeFinder.handleAllNodeFind()
    }

    @HandleEvent
    fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        if (!isEnabled()) return
        if (!event.isLocalPlayer) return

        if (event.distance > 20) {
            updateCache()
        }
    }

    fun updateCache() {
        state.cachedNearbyNodes = nodes.sortedBy { it.distanceSqToPlayer() }.take(20)
        state.lastCacheUpdate = SimpleTimeMark.now()
    }

    fun updateRender() {
        val maxNodeDistance = config.maxNodeDistance * config.maxNodeDistance
        for (node in nodes) {
            node.rendering = node.distanceSqToPlayer() < maxNodeDistance
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shgraph") {
            description = "Enables the graph editor"
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback { toggleFeature() }
        }
        event.registerBrigadier("shgraphfindall") {
            description = "Navigate over the whole graph network"
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback { GraphEditorNodeFinder.toggleFindAll() }
        }
        event.registerBrigadier("shgraphloadthisisland") {
            description = "Loads the current island data into the graph editor."
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback { GraphEditorIO.loadThisIsland() }
        }
    }

    var bypassTempRemoveTimer = SimpleTimeMark.farPast()

    private fun toggleFeature() {
        config.enabled = !config.enabled
        if (config.enabled) {
            ChatUtils.chat("Graph Editor is now active.")
        } else {
            chatAtDisable()
        }
    }

    fun chatAtDisable() = ChatUtils.clickableChat(
        "Graph Editor is now inactive. §lClick to activate.",
        GraphEditor::toggleFeature,
    )

    fun onMinecraftInput(keyBinding: KeyMapping, cir: CallbackInfoReturnable<Boolean>) {
        if (!isEnabled()) return
        if (!inEditMode) return
        if (keyBinding !in KeyboardManager.WasdInputMatrix) return
        cir.returnValue = false
    }

    fun clear() {
        GraphEditorHistory.save("clear graph")
        state = GraphEditorState()
    }

    fun enable() {
        if (!config.enabled) {
            config.enabled = true
            ChatUtils.chat("Graph Editor is now active.")
        }
    }
}

