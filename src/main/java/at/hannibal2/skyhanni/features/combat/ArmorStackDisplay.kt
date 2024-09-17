package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ArmorStackDisplay {
    private val config get() = SkyHanniMod.feature.combat.stackDisplayConfig
    private val stackPattern  by lazy { ActionBarStatsData.ARMOR_STACK.pattern }
    private val stackRemovePattern by lazy { Regex("\\$stackPattern?") }
    private var stackCount = 0
    private var stackSymbol = ""
    private var display = ""

    @SubscribeEvent
    fun onActionBar(event: ActionBarUpdateEvent) {
        if (!isEnabled()) return
        val actionBar = event.actionBar

        stackPattern.findMatcher(actionBar) {
            stackCount = group("stack").toInt()
            stackSymbol = group("symbol")
            display = "§6§l$stackCount$stackSymbol"
        } ?: run {
            stackCount = 0
            stackSymbol = ""
            display = ""
        }
        event.changeActionBar(actionBar.replace(stackRemovePattern, "").trim())
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.position.renderString(display, posLabel = "Armor Stack Display")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
