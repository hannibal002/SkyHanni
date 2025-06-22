package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.PetUtils.hasValidHigherTier
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils
import com.google.gson.annotations.Expose
import java.util.UUID

data class PetDataStorage(
    @Expose val players: MutableMap<UUID, PlayerSpecific> = mutableMapOf(),
) {
    data class PlayerSpecific(
        @Expose val profiles: MutableMap<String, ProfileSpecific> = mutableMapOf(),
    )
    data class ProfileSpecific(
        @Expose val pets: MutableList<PetData> = mutableListOf(),
        @Expose val expSharePets: MutableList<UUID?> = mutableListOf(),
    )
}

@KSerializable
@Suppress("DEPRECATION")
data class PetData(
    @Deprecated(
        "This does not reflect Tier Boost, use fauxInternalName instead.",
        replaceWith = ReplaceWith("fauxInternalName")
    )
    @Expose val petInternalName: NeuInternalName, // The internal name of the pet, e.g., `RABBIT;5`
    @Expose var skinInternalName: NeuInternalName? = null, // The skin of the pet, e.g., `PET_SKIN_WOLF_DOGE`
    @Expose var skinVariantIndex: Int? = null, // Used for pet skins that have variants, otherwise unused
    @Expose var heldItemInternalName: NeuInternalName? = null, // The held item of the pet, e.g., `PET_ITEM_COMBAT_SKILL_BOOST_EPIC`
    @Expose var exp: Double? = null, // The total XP of the pet as a double, e.g., `0.0`
    @Expose val uuid: UUID? = null, // If this data is for a 'real' pet, this is the UUID of it
) {
    constructor(petInfo: SkyBlockItemModifierUtils.PetInfo) : this(
        petInfo._internalName,
        petInfo.properSkinItem,
        petInfo.getSkinVariantIndex(),
        petInfo.heldItem,
        petInfo.exp,
        petInfo.uniqueId
    )

    private val tierBoosted get() = heldItemInternalName == TIER_BOOST && petInternalName.hasValidHigherTier()
    private val properPetName = PetUtils.getPetProperName(petInternalName)
    private val specifiedRarity = PetUtils.getPetRarity(petInternalName) ?: LorenzRarity.COMMON

    /**
     * Interpolated version of internal name that actually represents the state of the pet.
     * This is needed because of tier boosts.
     */
    val fauxInternalName: NeuInternalName get() = "$properPetName;${rarity.id}".toInternalName()
    val cleanName: String get() = PetUtils.getCleanPetName(fauxInternalName, colored = false)
    val coloredName: String get() = "${rarity.chatColorCode}$cleanName"
    val level: Int get() = PetUtils.xpToLevel(exp ?: 0.0, fauxInternalName)
    val skinTag: String? get() = skinInternalName?.getItemStack()?.getItemRarityOrNull()?.let { it.chatColorCode + "✦" }
    val rarity: LorenzRarity get() = if (tierBoosted) { specifiedRarity.oneAbove() ?: specifiedRarity } else specifiedRarity

    fun getUserFriendlyName(
        includeLevel: Boolean = true,
        includeSkinTag: Boolean = true,
    ) = buildString {
        if (includeLevel) append("§7[Lvl $level] ")
        append(coloredName)
        if (includeSkinTag && skinTag != null) append(" $skinTag")
    }

    companion object {
        private val TIER_BOOST = "PET_ITEM_TIER_BOOST".toInternalName()
    }

    override fun toString() = buildString {
        appendLine("  coloredName: '$coloredName'")
        appendLine("  petInternalName: '${petInternalName.asString()}'")
        appendLine("    fauxInternalName: '$fauxInternalName'")
        appendLine("    isPet: '${petInternalName.isPet}'")
        appendLine("    hasValidHigherTier: '${petInternalName.hasValidHigherTier()}'")
        appendLine("  skinInternalName: '${skinInternalName?.asString()}'")
        appendLine("  skinVariantIndex: '$skinVariantIndex'")
        appendLine("  heldItemInternalName: '${heldItemInternalName?.asString()}'")
        appendLine("  exp: '${exp?.addSeparators() ?: 0.0}'")
        appendLine("  uuid: '$uuid'")
        appendLine()
        appendLine("  isItemTierBoosted: '$tierBoosted'")
        appendLine("  rarity: '$rarity'")
        appendLine("  level: '$level'")
    }
}
