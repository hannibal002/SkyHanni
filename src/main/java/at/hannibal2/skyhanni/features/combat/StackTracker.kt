package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class StackTracker {
    private val config get() = SkyHanniMod.feature.combat.stackTrackerConfig
    private var display = ""

    /**
     * REGEX-TEST: §66,171/4,422❤  §6§l10ᝐ§r     §a1,295§a❈ Defense     §b525/1,355✎ §3400ʬ
     * REGEX-TEST: §66,171/4,422❤  §65ᝐ     §b-150 Mana (§6Wither Impact§b)     §b1,016/1,355✎ §3400ʬ
     */
    private val armorStackPattern by RepoPattern.pattern(
        "combat.armorstack.actionbar",
        ".* (§6|§6§l)(?<stack>\\d+.).*"
    )

    @SubscribeEvent
    fun onActionBar(event: ActionBarUpdateEvent) {
        if (!isEnabled()) return
        val stacks = armorStackPattern.matchMatcher(event.actionBar) {
            "§6§l" + group("stack")
        } ?: ""
        display = stacks
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.stackTrackerPos.renderString(display, posLabel = "Stack Tracker Display")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}