package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.DungeonStartEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.concurrent.fixedRateTimer

class DungeonMilestonesDisplay {

    companion object {
        private var display = ""
        var color = ""
        var currentMilestone = 0
        var timeReached = 0L

        fun isMilestoneMessage(message: String): Boolean = when {
            message.matchRegex("§e§l(.*) Milestone §r§e.§r§7: You have dealt §r§c(.*)§r§7 Total Damage so far! §r§a(.*)") -> true
            message.matchRegex("§e§lArcher Milestone §r§e.§r§7: You have dealt §r§c(.*)§r§7 Ranged Damage so far! §r§a(.*)") -> true
            message.matchRegex("§e§lHealer Milestone §r§e.§r§7: You have healed §r§a(.*)§r§7 Damage so far! §r§a(.*)") -> true
            message.matchRegex("§e§lTank Milestone §r§e.§r§7: You have tanked and dealt §r§c(.*)§r§7 Total Damage so far! §r§a(.*)s") -> true

            else -> false
        }
    }

    init {
        fixedRateTimer(name = "skyhanni-dungeon-milestone-display", period = 200) {
            if (!isEnabled()) return@fixedRateTimer
            checkVisibility()
        }
    }

    private fun checkVisibility() {
        if (currentMilestone >= 3 && System.currentTimeMillis() > timeReached + 3_000 && display != "") {
            display = display.substring(1)
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatPacket(event: LorenzChatEvent) {
        if (!isEnabled()) return

        if (isMilestoneMessage(event.message)) {
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

        color = when (currentMilestone) {
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

        SkyHanniMod.feature.dungeon.showMileStonesDisplayPos.renderString(color + display, posLabel = "Dungeon Milestone")
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inDungeons && SkyHanniMod.feature.dungeon.showMilestonesDisplay
    }
}