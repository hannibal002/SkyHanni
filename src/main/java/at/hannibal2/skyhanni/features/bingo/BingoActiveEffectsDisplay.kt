package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.TablistFooterUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import kotlin.time.Duration

/**
 * While on a Bingo profile, shows a small top-right readout of the total active effects and when
 * they expire (the soonest-expiring buff), so the player knows when to get their next god splash.
 * Sourced from the tab list's "Active Effects" footer section.
 */
@SkyHanniModule
object BingoActiveEffectsDisplay {

    private val config get() = SkyHanniMod.feature.event.bingo

    private val patternGroup = RepoPattern.group("bingo.activeeffects")

    // A vanilla potion tinted red (an empty potion renders as a colorless water bottle); the
    // Healing potion's contents give it the classic red potion color.
    private val potionIcon by lazy {
        Renderable.item(
            SafeItemStack(Items.POTION) {
                set(DataComponents.POTION_CONTENTS, PotionContents(Potions.HEALING))
            },
        )
    }

    /**
     * REGEX-TEST: Active Effects
     */
    private val footerTitlePattern by patternGroup.pattern(
        "footer.title",
        "Active Effects",
    )

    /**
     * REGEX-TEST: You have 28 active effects. Use "/effects" to see them!
     * REGEX-TEST: You have 1 non-god effects.
     */
    private val effectCountPattern by patternGroup.pattern(
        "footer.count",
        "You have (?<count>\\d+) (?:active|non-god) effects?.*",
    )

    /**
     * REGEX-TEST: Adrenaline VIII 10m
     * REGEX-TEST: Wisp's Ice-Flavored Water I 1h 27m
     */
    private val effectLinePattern by patternGroup.pattern(
        "footer.effect-line",
        "(?<name>.+?)[:\\s]+(?<time>(?:\\d+[wdhms]\\s?)+)\\s*",
    )

    private var display: Renderable? = null

    @HandleEvent
    private fun onProfileJoin() {
        display = null
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onTabListFooterUpdate(event: TablistFooterUpdateEvent) {
        display = buildDisplay(event.footer)
    }

    private fun buildDisplay(footer: List<Component>): Renderable? {
        val startIndex = footer.indexOfFirst { footerTitlePattern.matches(it) }
        if (startIndex == -1) return null

        var totalEffects: Int? = null
        var soonest: Duration? = null
        var soonestRaw: String? = null

        for (index in startIndex until footer.size) {
            val line = footer[index].string
            // The Active Effects block ends at the next blank line (footer section boundary).
            if (index != startIndex && line.isBlank()) break

            effectCountPattern.matchMatcher(line) {
                totalEffects = group("count").toIntOrNull()
            }
            effectLinePattern.matchMatcher(line) {
                val raw = group("time").trim()
                val duration = TimeUtils.getDurationOrNull(raw) ?: return@matchMatcher
                val current = soonest
                if (current == null || duration < current) {
                    soonest = duration
                    soonestRaw = raw
                }
            }
        }

        val lines = buildList {
            totalEffects?.let { add("§aTotal Effects: §e$it") }
            soonestRaw?.let { add("§aEffect Duration: §e$it") }
        }
        if (lines.isEmpty()) return null

        return Renderable.horizontal(
            potionIcon,
            Renderable.vertical(lines.map { Renderable.text(it) }),
            spacing = 3,
            verticalAlign = VerticalAlignment.CENTER,
        )
    }

    @HandleEvent
    private fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val current = display ?: return
        config.activeEffectsPosition.renderRenderable(current, posLabel = "Bingo Active Effects")
    }

    private fun isEnabled() = SkyBlockUtils.isBingoProfile && config.activeEffectsDisplay
}
