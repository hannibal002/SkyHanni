package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import at.hannibal2.skyhanni.utils.compat.DyeCompat.Companion.isDye
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft

@SkyHanniModule
object ExperimentationXPOverlay {
    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.superpairs

    private val patternGroup = RepoPattern.group("enchanting.experiments")

    /**
     * @regexTest §331k Enchanting Exp
     * @regexTest §3143k Enchanting Exp
     * @regexTest §350k Enchanting Exp
     * @regexTest §341k Enchanting Exp
     * @regexTest §3137k Enchanting Exp
     * @regexTest §3142k Enchanting Exp
     * @regexTest §3130k Enchanting Exp
     * @regexTest §36.5k Enchanting Exp
     * @regexTest §35.5k Enchanting Exp
     * @regexTest §33.5k Enchanting Exp
     */
    private val enchantingXPPattern by patternGroup.pattern(
        "enchantingxp",
        "§3(?<xp>[\\d.]+)k Enchanting Exp",
    )

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        if (!isEnabled()) return
        event.stack ?: return
        if (!event.stack.isDye()) return
        enchantingXPPattern.matchMatcher(event.stack.hoverName.formattedTextCompatLeadingWhiteLessResets()) {
            val text = "${group("xp")}k"
            val stringWidth = Minecraft.getInstance().font.width(text)
            event.drawSlotText(event.x + 2 + stringWidth, event.y + 10, text, .6f)
        }
    }

    private fun isEnabled() = ExperimentationTableApi.inSuperpairs && config.xpOverlay

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val pathBase = "inventory.experimentationTable"
        event.move(93, "$pathBase.superpairsXPOverlay", "$pathBase.superpairs.xpOverlay")
    }
}
