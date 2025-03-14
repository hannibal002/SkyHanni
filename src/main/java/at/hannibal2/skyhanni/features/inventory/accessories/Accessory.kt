package at.hannibal2.skyhanni.features.inventory.accessories

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi.HEGEMONY_ARTIFACT
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi.getBaseMagicalPower
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi.isAbiCasePattern
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi.isHatPattern
import at.hannibal2.skyhanni.features.inventory.accessories.AccessoryApi.repoAccessoryLineage
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.utils.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import com.google.gson.annotations.Expose

class Accessory(
    @Expose val internalName: NeuInternalName,
    @Expose val rarity: LorenzRarity? = null,
    @Expose val enrichment: SkyblockStat? = null,
    @Expose val totalStats: Map<SkyblockStat, Double> = enumMapOf(),
) {
    var index: Int = -1
    // String in each of these is the lore line that matched them
    var usageSlayerRequirement: Triple<SlayerType, Int, String>? = null
    var craftSlayerRequirement: Triple<SlayerType, Int, String>? = null

    override fun toString(): String = internalName.asString()

    val isAbiCase = isAbiCasePattern.matches(internalName.asString())
    val isHat = isHatPattern.matches(internalName.asString())
    val magicPower: Int get() = rarity?.let { getMagicalPower() } ?: 0
    val successor: Accessory?
        get() = repoAccessoryLineage.getRelatives(this, LineageType.SUCCESSOR, limit = 1).firstOrNull()
    val siblings: List<Accessory>
        get() = repoAccessoryLineage.getRelatives(this, LineageType.SIBLING, Int.MAX_VALUE)

    override fun equals(other: Any?): Boolean =
        (other as? Accessory)?.internalName == this.internalName

    override fun hashCode(): Int = internalName.hashCode()

    private fun magicalPowerOutlierHandler(basePower: Int): Int? = when {
        internalName == HEGEMONY_ARTIFACT -> basePower * 2
        isAbiCase -> {
            val contactCount = ProfileStorageData.profileSpecific?.abiphoneContactAmount ?: 0
            contactCount / 2
        }

        else -> null
    }

    private fun getMagicalPower(): Int {
        val basePower = rarity?.getBaseMagicalPower() ?: return 0
        return magicalPowerOutlierHandler(basePower) ?: basePower
    }

    fun getUpgradeCost(from: Accessory? = null): Double {
        val thisCost = this.internalName.getPrice()
        val fromCost = from?.internalName?.getPrice() ?: 0.0
        return thisCost - fromCost
    }
}
