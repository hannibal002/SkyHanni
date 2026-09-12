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

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onRender(event: GuiRenderEvent) {
        if (!config.display) return
        val lines = buildLines()
        if (lines.isEmpty()) return
        config.displayPosition.renderRenderable(
            Renderable.vertical(lines, spacing = LINE_SPACING).withTitledFrame(buildTitle()),
            posLabel = "End Stone Protector",
        )
    }


    private fun buildTitle(): Renderable {
        val text = Renderable.text(
            componentBuilder {
                appendWithColor("End Stone Protector", TITLE_COLOR) { bold = true }
            },
        )
        val icon = Renderable.item(PROTECTOR_HEAD)
        return Renderable.horizontal(listOf(icon, text), spacing = TITLE_ICON_SPACING)
    }

    private fun buildLines(): List<Renderable> = buildList {
        if (config.showStage) {
            // Built as a component because the stage colours are plain RGB - the 16 legacy
            // colour codes have no real orange. Unknown wordings fall back to Hypixel's text.
            val stage = GolemStage.current
            // How long the awakening stage has been running, shown only during that stage.
            val elapsed = GolemStage.timeInAwakening()
                ?.format(biggestUnit = TimeUnit.MINUTE, showMilliSeconds = false)
            add(
                Renderable.text(
                    componentBuilder {
                        appendWithColor("Stage: ", LABEL_COLOR)
                        appendWithColor(stage?.display ?: GolemStage.stageName ?: "?", stage?.rgb ?: UNKNOWN_COLOR)
                        if (elapsed != null) appendWithColor(" ($elapsed)", LABEL_COLOR)
                    },
                ),
            )
        }
        if (config.showLocation) {
            // Before the protector starts rising there is no head block to read the spot from.
            val location = GolemLocation.currentLocationText()?.let { "§f$it" } ?: "§8not revealed"
            add(Renderable.text("§7Location: $location"))
        }
        if (config.showSpawnTimer) {
            val remaining = GolemSpawnTimer.timeUntilSpawn()
            // The line keeps its place while the protector is still awakening, so it does not
            // pop into existence only for the last 20 seconds.
            val countdown = if (remaining > 0.seconds) {
                "§a${(remaining.inWholeMilliseconds / 1000.0).roundTo(1)}s"
            } else "§8incoming"
            add(Renderable.text("§7Spawns in: $countdown"))
        }
    }
}
