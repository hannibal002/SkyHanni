package at.hannibal2.skyhanni.features.garden.composter

import at.hannibal2.skyhanni.data.model.ComposterUpgrade
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object ComposterAPI {
    var tabListData = mapOf<ComposterDisplay.DataType, String>()
    val composterUpgrades: MutableMap<ComposterUpgrade, Int>? get() = GardenAPI.storage?.composterUpgrades

    fun ComposterUpgrade.getLevel(addOne: ComposterUpgrade?) = (composterUpgrades?.get(this) ?: 0) + if (addOne == this) 1 else 0

    fun getFuel() = tabListData[ComposterDisplay.DataType.FUEL]?.removeColor()?.formatNumber() ?: 0

    fun getOrganicMatter() = tabListData[ComposterDisplay.DataType.ORGANIC_MATTER]?.removeColor()?.formatNumber() ?: 0

    fun maxOrganicMatter(addOne: ComposterUpgrade?) = 40_000 + ComposterUpgrade.ORGANIC_MATTER_CAP.getLevel(addOne) * 20_000

    fun multiDropChance(addOne: ComposterUpgrade?) = ComposterUpgrade.MULTI_DROP.getLevel(addOne) * 0.03

    fun maxFuel(addOne: ComposterUpgrade?) = 100_000 + ComposterUpgrade.FUEL_CAP.getLevel(addOne) * 30_000

    fun timePerCompost(addOne: ComposterUpgrade?): Duration {
        val speedUpgrade = ComposterUpgrade.COMPOSTER_SPEED.getLevel(addOne)
        val speedFactor = 1 + speedUpgrade * 0.2
        val baseDuration = 10.minutes
        return baseDuration / speedFactor
    }

    fun organicMatterRequiredPer(addOne: ComposterUpgrade?): Double {
        val costReduction = ComposterUpgrade.COST_REDUCTION.getLevel(addOne)
        val costFactor = 1.0 - costReduction.toDouble() / 100
        return 4_000.0 * costFactor
    }

    fun fuelRequiredPer(addOne: ComposterUpgrade?): Double {
        val costReduction = ComposterUpgrade.COST_REDUCTION.getLevel(addOne)
        val costFactor = 1.0 - costReduction.toDouble() / 100
        return 2_000.0 * costFactor
    }
}
