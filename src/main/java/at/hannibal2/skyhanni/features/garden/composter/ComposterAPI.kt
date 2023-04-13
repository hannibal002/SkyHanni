package at.hannibal2.skyhanni.features.garden.composter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.model.ComposterUpgrade
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

object ComposterAPI {
    var tabListData = mapOf<ComposterDisplay.DataType, String>()
    val composterUpgrades: MutableMap<ComposterUpgrade, Int> get() = SkyHanniMod.feature.hidden.gardenComposterUpgrades

    fun ComposterUpgrade.getLevel() = composterUpgrades[this] ?: 0

    fun getFuel() = tabListData[ComposterDisplay.DataType.FUEL]?.removeColor()?.formatNumber() ?: 0

    fun getOrganicMatter() = tabListData[ComposterDisplay.DataType.ORGANIC_MATTER]?.removeColor()?.formatNumber() ?: 0
}