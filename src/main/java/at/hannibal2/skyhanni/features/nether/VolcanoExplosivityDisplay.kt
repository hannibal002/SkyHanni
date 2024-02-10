package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class VolcanoExplosivityDisplay {

    private val config get() = SkyHanniMod.feature.crimsonIsle
    private val patternGroup = RepoPattern.group("crimson.volcano")
    private val headerPattern by patternGroup.pattern(
        "header.tablistline",
        "(?:§.)*Volcano Explosivity:(?:[\\S ]+)*"
    )
    private val statusPattern by patternGroup.pattern(
        "status.tablistline",
        " *(?<status>(?:§.)*\\S+)"
    )
    private var display = ""

    @SubscribeEvent
    fun onTick(event: TabListUpdateEvent) {
        if (!isEnabled()) return
        val text = event.tabList.nextAfter({ headerPattern.matches(it) }) ?: return
        statusPattern.matchMatcher(text) {
            display = "§bVolcano Explosivity§7: ${group("status")}"
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.positionVolcano.renderString(display, posLabel = "Volcano Explosivity")
    }

    private fun isEnabled() = IslandType.CRIMSON_ISLE.isInIsland() && config.volcanoExplosivity
}
