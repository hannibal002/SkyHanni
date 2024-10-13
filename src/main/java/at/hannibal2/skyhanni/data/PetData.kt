package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NEUInternalName

data class PetData(
    val name: NEUInternalName,
    val rarity: LorenzRarity,
    val petItem: NEUInternalName,
    val hasSkin: Boolean,
    val level: Int,
    val xp: Double,
    val rawPetName: String,
)
