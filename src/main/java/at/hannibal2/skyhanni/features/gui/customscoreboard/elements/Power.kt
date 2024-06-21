package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MaxwellAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.maxwellConfig
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators

object Power : Element() {
    override fun getDisplayPair() = listOf(
        (MaxwellAPI.currentPower?.let {
            val mp = if (maxwellConfig.showMagicalPower) "§7(§6${MaxwellAPI.magicalPower?.addSeparators()}§7)" else ""
            if (displayConfig.displayNumbersFirst) {
                "§a${it.replace(" Power", "")} Power $mp"
            } else "Power: §a$it $mp"
        } ?: "§cOpen \"Your Bags\"!"),
    )

    override fun showWhen() = !inAnyIsland(IslandType.THE_RIFT)

    override val configLine = "§Power: §aSighted §7(§61.263§7)"
}
