package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.mayorConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland
import at.hannibal2.skyhanni.utils.TimeUtils.format

object Mayor : Element() {
    override fun getDisplay() = buildList {
        val currentMayorName = MayorAPI.currentMayor?.mayorName?.let { MayorAPI.mayorNameWithColorCode(it) } ?: HIDDEN
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

        val extraMayorName = extraMayor.mayorName.let { MayorAPI.mayorNameWithColorCode(it) }
        val extraTimeTillNextMayor = if (mayorConfig.showTimeTillNextMayor) {
            "§7 (§6${jerryExtraMayor.second.timeUntil().format(maxUnits = 2)}§7)"
        } else ""

        add(extraMayorName + extraTimeTillNextMayor)
    }

    override fun showWhen() = !inAnyIsland(IslandType.THE_RIFT) && MayorAPI.currentMayor != null

    override val configLine = "§2Diana:\n §7- §eLucky!\n §7- §eMythological Ritual\n §7- §ePet XP Buff"
}
