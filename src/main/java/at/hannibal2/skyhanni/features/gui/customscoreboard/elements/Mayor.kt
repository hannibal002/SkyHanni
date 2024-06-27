package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.mayorConfig
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.TimeUtils.format

// internal
object Mayor : ScoreboardElement() {
    override fun getDisplay() = buildList {
        val currentMayorName = MayorAPI.currentMayor?.mayorName?.let { MayorAPI.mayorNameWithColorCode(it) } ?: return@buildList
        val timeTillNextMayor = if (mayorConfig.showTimeTillNextMayor) {
            "§7 (§e${MayorAPI.nextMayorTimestamp.timeUntil().format(maxUnits = 2)}§7)"
        } else ""

        add(currentMayorName + timeTillNextMayor)

        if (mayorConfig.showMayorPerks) {
            MayorAPI.currentMayor?.activePerks?.forEach { perk ->
                add(" §7- §e${perk.perkName}")
            }
        }

        if (!mayorConfig.showExtraMayor) return@buildList
        val jerryExtraMayor = MayorAPI.jerryExtraMayor
        val extraMayor = jerryExtraMayor.first ?: return@buildList

        val extraMayorName = MayorAPI.mayorNameWithColorCode(extraMayor.mayorName)
        val extraTimeTillNextMayor = if (mayorConfig.showTimeTillNextMayor) {
            " §7(§6${jerryExtraMayor.second.timeUntil().format(maxUnits = 2)}§7)"
        } else ""

        add(extraMayorName + extraTimeTillNextMayor)
    }

    override val configLine = "§2Diana:\n §7- §eLucky!\n §7- §eMythological Ritual\n §7- §ePet XP Buff"

    override fun showIsland() = !RiftAPI.inRift()
}
