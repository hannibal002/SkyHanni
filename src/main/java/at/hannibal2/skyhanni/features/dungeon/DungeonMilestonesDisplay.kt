package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DungeonStartEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DungeonMilestonesDisplay {

    private val config get() = SkyHanniMod.feature.dungeon

    private val milestonePattern by RepoPattern.pattern(
        "dungeon.milestone",
        "§e§l.*Milestone §r§e.§r§7: You have (?:tanked and )?(?:dealt|healed) §r§.*§r§7.*so far! §r§a.*"
    )

    private var display = ""
    private var currentMilestone = 0
    private var timeReached = SimpleTimeMark.farPast()
    var color = ""

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return
        if (currentMilestone >= 3 && timeReached.passedSince() > 3.seconds && display.isNotEmpty()) {
            display = display.substring(1)
        }
    }

    @HandleEvent(receiveCancelled = true)
    fun onChat(event: SkyHanniChatEvent) {
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
            timeReached = SimpleTimeMark.now()
        }

        color = when (currentMilestone) {
            0, 1 -> "§c"
            2 -> "§e"
            else -> "§a"
        }
        display = "Current Milestone: $currentMilestone"
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        display = ""
        currentMilestone = 0
    }

    @HandleEvent
    fun onDungeonStart(event: DungeonStartEvent) {
        currentMilestone = 0
        update()
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.showMileStonesDisplayPos.renderString(
            color + display,
            posLabel = "Dungeon Milestone"
        )
    }

    private fun isEnabled() = DungeonAPI.inDungeon() && config.showMilestonesDisplay
}
