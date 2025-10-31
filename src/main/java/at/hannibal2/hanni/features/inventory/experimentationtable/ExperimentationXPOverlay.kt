package at.hannibal2.hanni.features.inventory.experimentationtable

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.ExperimentationTableApi
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.GuiRenderItemEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.RenderUtils.drawSlotText
import at.hannibal2.hanni.utils.compat.DyeCompat.Companion.isDye
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft

@HanniModule
object ExperimentationXPOverlay {
    private val config get() = HanniMod.feature.inventory.experimentationTable.superpairs

    private val patternGroup = RepoPattern.group("enchanting.experiments")

    /**
     * REGEX-TEST: §331k Enchanting Exp
     * REGEX-TEST: §3143k Enchanting Exp
     * REGEX-TEST: §350k Enchanting Exp
     * REGEX-TEST: §341k Enchanting Exp
     * REGEX-TEST: §3137k Enchanting Exp
     * REGEX-TEST: §3142k Enchanting Exp
     * REGEX-TEST: §3130k Enchanting Exp
     * REGEX-TEST: §36.5k Enchanting Exp
     * REGEX-TEST: §35.5k Enchanting Exp
     * REGEX-TEST: §33.5k Enchanting Exp
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
        enchantingXPPattern.matchMatcher(event.stack.displayName) {
            val text = "${group("xp")}k"
            val stringWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text)
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
