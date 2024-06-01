package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.TabWidgetUpdate
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchFirst
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FerocityDisplay {
    private val config get() = SkyHanniMod.feature.combat.ferocityDisplay

    /**
     * REGEX-TEST:  Ferocity: §r§c⫽14
     */
    private val ferocityPattern by RepoPattern.pattern(
        "combat.ferocity.tab",
        " Ferocity: §r§c⫽(?<stat>.*)"
    )

    private var display = ""

    @SubscribeEvent
    fun onTabListUpdate(event: TabWidgetUpdate) {
        if (!isEnabled()) return
        if (event.isEventFor(TabWidget.STATS) || event.isEventFor(TabWidget.DUNGEON_SKILLS_AND_STATS))
            display = ""
        if (event !is TabWidgetUpdate.NewValues) return
        val stat = event.lines.matchFirst(ferocityPattern) {
            group("stat")
        } ?: return

        display = "§c⫽$stat"

    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        config.position.renderString(display, posLabel = "Ferocity Display")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
