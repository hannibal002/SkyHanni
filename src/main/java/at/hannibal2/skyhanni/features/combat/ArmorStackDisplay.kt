package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ArmorStackDisplay {
    private val config get() = SkyHanniMod.feature.combat.stackDisplayConfig
    private var stackCount = 0
    private var stackSymbol = ""

    @SubscribeEvent
    fun onActionBar(event: ActionBarUpdateEvent) {
        if (!isEnabled()) return
        val actionBarText = event.actionBar
        val stackPattern = ActionBarStatsData.ARMOR_STACK.pattern

        stackSymbol = stackPattern.findMatcher(actionBarText) { group("symbol") } ?: ""
        stackCount = (stackPattern.findMatcher(actionBarText) { group("stack") } ?: "0").toInt()

        val updatedActionBarText = actionBarText.replace(Regex("\\$stackPattern?"), "").trim()
        event.changeActionBar(updatedActionBarText)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled() || stackCount == 0) return
        config.position.renderString("§6§l$stackCount$stackSymbol", posLabel = "Armor Stack Display")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
