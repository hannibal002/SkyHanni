package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MayorElection
import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName

object DianaAPI {

    val spade by lazy { "ANCESTRAL_SPADE".asInternalName() }

    fun hasSpadeInHand() = InventoryUtils.itemInHandId == spade

    private fun isRitualActive() = MayorElection.isPerkActive("Diana", "Mythological Ritual") ||
            MayorElection.isPerkActive("Jerry", "Perkpocalypse") || SkyHanniMod.feature.event.diana.alwaysDiana

    fun hasGriffinPet() = PetAPI.currentPet?.contains("Griffin") ?: false

    fun featuresEnabled() = IslandType.HUB.isInIsland() && isRitualActive()
}
