package at.hannibal2.skyhanni.features.mining.dwarves

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format

@SkyHanniModule
object FallenStarCultTimer {

    private val config get() = SkyHanniMod.feature.mining.fallenStarCult

    private val cultRoomLocation = LorenzVec(-45.5, 193.5, 45.6)
    private const val CULT_ROOM_RADIUS = 10.0

    private val fallenStarHelmet = "FALLEN_STAR_HAT".toInternalName()

    // Meetings start at midnight on these days of every SkyBlock month
    private val meetingDays = listOf(7, 14, 21, 28)
    private const val MEETING_END_HOUR = 6

    private var display = emptyList<String>()
    private var visible = false

    @HandleEvent
    private fun onSecondPassed() {
        if (!isEnabled()) {
            visible = false
            return
        }
        visible = shouldShow()
        if (visible) display = listOf(buildDisplay())
    }

    private fun buildDisplay(): String {
        val meetingEnd = currentMeetingEnd()
        if (meetingEnd != null) {
            val time = meetingEnd.timeUntil().format(maxUnits = 2)
            return "§aCult Meeting active! §7(§b$time left§7)"
        }
        val time = nextMeetingStart().timeUntil().format(maxUnits = 2)
        return "§eCult Meeting: §bin $time"
    }

    // Null while no meeting is running
    private fun currentMeetingEnd(): SimpleTimeMark? {
        val now = SkyBlockTime.now()
        if (now.day !in meetingDays || now.hour >= MEETING_END_HOUR) return null
        return SkyBlockTime(now.year, now.month, now.day, MEETING_END_HOUR).toTimeMark()
    }

    private fun nextMeetingStart(): SimpleTimeMark {
        val now = SkyBlockTime.now()
        meetingDays.firstOrNull { it > now.day }?.let {
            return SkyBlockTime(now.year, now.month, it).toTimeMark()
        }
        val nextYear = if (now.month == 12) now.year + 1 else now.year
        val nextMonth = if (now.month == 12) 1 else now.month + 1
        return SkyBlockTime(nextYear, nextMonth, meetingDays.first()).toTimeMark()
    }

    @HandleEvent
    private fun onGuiRenderOverlay() {
        if (shouldRender()) {
            config.position.renderStrings(display, posLabel = "Fallen Star Cult Timer")
        }
    }

    private fun inCultRoom() = IslandType.DWARVEN_MINES.isInIsland() &&
        cultRoomLocation.distanceToPlayer() < CULT_ROOM_RADIUS

    private fun wearingHelmet() = InventoryUtils.getHelmet()?.getInternalNameOrNull() == fallenStarHelmet

    private fun shouldShow(): Boolean {
        if (config.onlyShowWhen.isEmpty()) return true
        return config.onlyShowWhen.any {
            when (it) {
                IN_CULT_ROOM -> inCultRoom()
                WEARING_HELMET -> wearingHelmet()
            }
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled

    private fun shouldRender() = isEnabled() && visible
}
