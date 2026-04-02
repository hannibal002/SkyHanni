package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonStartEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DungeonMilestonesDisplay {

    private val config get() = SkyHanniMod.feature.dungeon

    /**
     * REGEX-TEST: Mage Milestone ❷: You have dealt 300,000 Total Damage so far! 07s
     * REGEX-TEST: Tank Milestone ❷: You have tanked and dealt 180,000 Total Damage so far! 16s
     */
    private val milestonePattern by RepoPattern.pattern(
        "dungeon.milestone.colorless",
        ".*Milestone .: You have (?:tanked and )?(?:dealt|healed) *.*so far! .*",
    )

    private var displayString: String = ""
    private val display: Renderable? get() = displayString.takeIfNotEmpty()?.let { Renderable.text(color + it) }
    private var currentMilestone = 0
    private var timeReached = SimpleTimeMark.farPast()
    private var color = ""

    // Todo I like the ingenuity of the fade-out effect here, but this should probably be made
    //  into a reusable decorator of TextRenderable/StringRenderable
    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5) || displayString.isEmpty()) return
        if (currentMilestone < 3 || timeReached.passedSince() < 3.seconds) return
        displayString = displayString.substring(1)
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.showMilestonesDisplay) return
        if (milestonePattern.matches(event.chatComponent)) {
            event.blockedReason = "dungeon_milestone"
            currentMilestone++
            update()
        }
    }

    private fun update() {
        if (currentMilestone > 3) return
        else if (currentMilestone == 3) timeReached = SimpleTimeMark.now()

        color = when (currentMilestone) {
            0, 1 -> "§c"
            2 -> "§e"
            else -> "§a"
        }
        displayString = "Current Milestone: $currentMilestone"
    }

    @HandleEvent
    fun onWorldChange() {
        displayString = ""
        currentMilestone = 0
    }

    @HandleEvent
    fun onDungeonStart(event: DungeonStartEvent) {
        currentMilestone = 0
        update()
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.showMilestonesDisplay) return

        val display = display ?: return
        config.showMileStonesDisplayPos.renderRenderable(display, posLabel = "Dungeon Milestone")
    }
}
