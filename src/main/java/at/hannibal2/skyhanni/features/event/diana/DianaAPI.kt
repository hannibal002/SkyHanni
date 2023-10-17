package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MayorElection
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland

object DianaAPI {
    fun hasSpadeInHand() = InventoryUtils.itemInHandId.equals("ANCESTRAL_SPADE")

    private fun isRitualActive() = MayorElection.isPerkActive("Diana", "Mythological Ritual") ||
            MayorElection.isPerkActive("Jerry", "Perkpocalypse") || SkyHanniMod.feature.event.diana.alwaysDiana

    fun hasGriffinPet() = ProfileStorageData.profileSpecific?.currentPet?.contains("Griffin") ?: false

    fun featuresEnabled() = isRitualActive() && IslandType.HUB.isInIsland()
}