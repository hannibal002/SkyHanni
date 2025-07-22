package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.ElectionApi
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.utils.TimeUtils.format

// internal
// set 1s timer
object ScoreboardElementMayor : ScoreboardElement() {
    private val config get() = CustomScoreboard.displayConfig.mayor

    override fun getDisplay() = buildList {
        val currentMayorName = ElectionApi.currentMayor?.mayorName?.let {
            ElectionApi.mayorNameWithColorCode(it)
        } ?: return@buildList
        val timeTillNextMayor = if (config.showTimeTillNextMayor) {
            "§7 (§e${ElectionApi.nextMayorTimestamp.timeUntil().format(maxUnits = 2)}§7)"
        } else ""

        add(currentMayorName + timeTillNextMayor)

        if (config.showMayorPerks) {
            ElectionApi.currentMayor?.activePerks?.forEach { perk ->
                add(" §7- §e${perk.perkName}")
            }
        }

        if (!config.showExtraMayor) return@buildList
        addAll(addMinister())
        addAll(addJerryMayor())
    }

    override val configLine = "§2Diana §7(§e4d 12h§7)\n §7- §eLucky!\n §7- §eMythological Ritual\n §7- §ePet XP Buff"

    override fun showIsland() = !RiftApi.inRift()

    private fun addMinister() = buildList {
        val ministerName = ElectionApi.currentMinister?.mayorName?.let {
            ElectionApi.mayorNameWithColorCode(it)
        } ?: return@buildList
        add(ministerName)

        if (config.showMayorPerks) {
            ElectionApi.currentMinister?.activePerks?.forEach { perk ->
                add(" §7- §e${perk.perkName}")
            }
        }
    }

    private fun addJerryMayor() = buildList {
        val jerryExtraMayor = ElectionApi.jerryExtraMayor
        val extraMayor = jerryExtraMayor.first ?: return@buildList

        val extraMayorName = ElectionApi.mayorNameWithColorCode(extraMayor.mayorName)
        val extraTimeTillNextMayor = if (config.showTimeTillNextMayor) {
            " §7(§6${jerryExtraMayor.second.timeUntil().format(maxUnits = 2)}§7)"
        } else ""

        add(extraMayorName + extraTimeTillNextMayor)
    }
}

// click: open /calendar
// hover on perks: show description
