package at.hannibal2.skyhanni.features.combat.end.dragon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.bold
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.decorators.TitledFrameRenderable.Companion.withTitledFrame
import at.hannibal2.skyhanni.utils.renderables.primitives.text

/**
 * Renders the per-player damage leaderboard of the running dragon fight, read from
 * [DragonFightAPI].
 */
@SkyHanniModule
object DragonDamageDisplay {

    private val config get() = SkyHanniMod.feature.combat.endIsland.dragon
    /** Breathing room between the entries. */
    private const val LINE_SPACING = 3

    private const val TITLE_COLOR = 0xE0B0FF

    private val waitingMessage = listOf(Renderable.text("§7Waiting for fight data..."))

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onRender(event: GuiRenderEvent) {
        if (!config.damageList) return
        // Only while a dragon is actually up: outside a fight the list has nothing to say, and
        // the tab list still carries the previous fight's numbers for a while.
        // Always visible on the End island, so the HUD can be placed outside of a fight.
        config.damageListPosition.renderRenderable(
            Renderable.vertical(buildLines(), spacing = LINE_SPACING).withTitledFrame(buildTitle()),
            posLabel = "Dragon Damage List",
        )
    }

    private fun buildTitle() = Renderable.text(
        componentBuilder {
            appendWithColor("Dragon Damage", TITLE_COLOR) { bold = true }
        },
    )

    private fun buildLines(): List<Renderable> {
        val entries = DragonFightAPI.damageEntries
        if (entries.isEmpty()) return waitingMessage

        val ownName = PlayerUtils.getName()
        // Hypixel already rounds these to one decimal in the tab list; shown as sent so the
        // numbers do not pretend to be more precise than they are.
        return entries.mapIndexed { index, entry ->
            val nameColor = if (entry.name == ownName) "§b" else "§7"
            Renderable.text("§8${index + 1}. $nameColor${entry.name}§7: §a${entry.rawDamage}")
        }
    }
}
