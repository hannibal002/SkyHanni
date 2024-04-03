package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.DungeonStartEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object DungeonMilestonesDisplay {

    private val config get() = SkyHanniMod.feature.dungeon

    val milestonePattern by RepoPattern.pattern(
        "dungeon.milestone",
        "§e§l.*Milestone §r§e.§r§7: You have (?:tanked and )?(?:dealt|healed) §r§.*§r§7.*so far! §r§a.*"
    )

    private var display = ""
    private var currentMilestone = 0
    private var timeReached = 0L
    var colour = ""

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.isMod(5)) return
        if (currentMilestone >= 3 && System.currentTimeMillis() > timeReached + 3_000 && display.isNotEmpty()) {
            display = display.substring(1)
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        if (milestonePattern.matches(event.message)) {
            event.blockedReason = "dungeon_milestone"
            currentMilestone++
            update()
        }
    }

    private fun update() {
        if (currentMilestone > 3) return
        if (currentMilestone == 3) {
            timeReached = System.currentTimeMillis()
        }

        colour = when (currentMilestone) {
            0, 1 -> "§c"
            2 -> "§e"
            else -> "§a"
        }
        display = "Current Milestone: $currentMilestone"
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        display = ""
        currentMilestone = 0
    }

    @SubscribeEvent
    fun onDungeonStart(event: DungeonStartEvent) {
        currentMilestone = 0
        update()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.showMileStonesDisplayPos.renderString(
            colour + display,
            posLabel = "Dungeon Milestone"
        )
    }

    private fun isEnabled() = LorenzUtils.inDungeons && config.showMilestonesDisplay
}
