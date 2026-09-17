package at.hannibal2.skyhanni.features.combat.end.golem

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.bold
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.decorators.TitledFrameRenderable.Companion.withTitledFrame
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.seconds

/**
 * Single framed HUD element combining everything known about the End Stone Protector: its
 * awakening stage, the spawn point and the countdown until it becomes attackable. The data
 * itself is owned by [GolemStage], [GolemLocation] and [GolemSpawnTimer].
 */
@SkyHanniModule
object GolemDisplay {

    private val config get() = SkyHanniMod.feature.combat.endIsland.golem

    private const val LINE_SPACING = 3

    private const val LABEL_COLOR = 0xAAAAAA
    private const val UNKNOWN_COLOR = 0xFFFFFF

    private const val TITLE_COLOR = 0xE3D5A0

    private const val TITLE_ICON_SPACING = 4

    private val PROTECTOR_HEAD = "ENDSTONE_PROTECTOR_BOSS".toInternalName()

    /**
     * Everything the overlay shows. It is only rebuilt when this changes, instead of building new
     * renderables every frame for numbers that mostly stay the same.
     */
    private data class State(
        val showStage: Boolean,
        val showLocation: Boolean,
        val showSpawnTimer: Boolean,
        val stage: GolemStage.Stage?,
        val stageName: String?,
        val awakeningSeconds: Long?,
        val location: String?,
        val countdownTenths: Long,
    )

    private var state: State? = null
    private var display: Renderable? = null

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onTick() {
        if (!config.display) return
        val newState = currentState()
        if (newState == state) return
        state = newState
        val lines = buildLines(newState)
        display = if (lines.isEmpty()) null else Renderable.vertical(lines, spacing = LINE_SPACING).withTitledFrame(buildTitle())
    }

    /**
     * Shown on the whole island, even while nothing is known: for a lobby hopper "Stage: 0" and
     * "not revealed" are exactly the answer that there is nothing to wait for on this server.
     */
    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onRender(event: GuiRenderEvent) {
        if (!config.display) return
        val renderable = display ?: return
        config.displayPosition.renderRenderable(renderable, posLabel = "End Stone Protector")
    }

    private fun currentState() = State(
        showStage = config.showStage,
        showLocation = config.showLocation,
        showSpawnTimer = config.showSpawnTimer,
        stage = GolemStage.current,
        stageName = GolemStage.stageName,
        awakeningSeconds = GolemStage.timeInAwakening()?.inWholeSeconds,
        location = GolemLocation.currentLocationText(),
        countdownTenths = GolemSpawnTimer.timeUntilSpawn().inWholeMilliseconds / 100,
    )

    private fun buildTitle(): Renderable {
        val text = Renderable.text(
            componentBuilder {
                appendWithColor("End Stone Protector", TITLE_COLOR) { bold = true }
            },
        )
        val icon = Renderable.item(PROTECTOR_HEAD)
        return Renderable.horizontal(listOf(icon, text), spacing = TITLE_ICON_SPACING)
    }

    private fun buildLines(state: State): List<Renderable> = buildList {
        if (state.showStage) {
            // Built as a component because the stage colours are plain RGB - the 16 legacy
            // colour codes have no real orange. Unknown wordings fall back to Hypixel's text.
            val stage = state.stage
            // How long the awakening stage has been running, shown only during that stage.
            val elapsed = state.awakeningSeconds?.seconds
                ?.format(biggestUnit = TimeUnit.MINUTE, showMilliSeconds = false)
            add(
                Renderable.text(
                    componentBuilder {
                        appendWithColor("Stage: ", LABEL_COLOR)
                        appendWithColor(stage?.display ?: state.stageName ?: "?", stage?.rgb ?: UNKNOWN_COLOR)
                        if (elapsed != null) appendWithColor(" ($elapsed)", LABEL_COLOR)
                    },
                ),
            )
        }
        if (state.showLocation) {
            // Before the protector starts rising there is no head block to read the spot from.
            val location = state.location?.let { "§f$it" } ?: "§8not revealed"
            add(Renderable.text("§7Location: $location"))
        }
        if (state.showSpawnTimer) {
            // The line keeps its place while the protector is still awakening, so it does not
            // pop into existence only for the last 21 seconds.
            val countdown = if (state.countdownTenths > 0) {
                "§a${(state.countdownTenths / 10.0).roundTo(1)}s"
            } else "§8incoming"
            add(Renderable.text("§7Spawns in: $countdown"))
        }
    }
}
