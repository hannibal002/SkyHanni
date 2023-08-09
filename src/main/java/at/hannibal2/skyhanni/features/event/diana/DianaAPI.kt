package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.data.MayorElection
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.InventoryUtils

object DianaAPI {
    fun hasSpadeInHand() = InventoryUtils.itemInHandId.equals("ANCESTRAL_SPADE")

    fun isRitualActive() = MayorElection.isPerkActive("Diana", "Mythological Ritual")

    fun hasGriffinPet() = ProfileStorageData.profileSpecific?.currentPet?.contains("Griffin") ?: false
}