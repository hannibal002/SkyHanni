package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.PetUtils.levelToXp
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import com.google.gson.annotations.Expose
import net.minecraft.item.ItemStack
import java.util.UUID

class PetDataStorage {
    @Expose
    var players: MutableMap<UUID, PlayerSpecific> = mutableMapOf()

    class PlayerSpecific {
        @Expose
        var profiles: MutableMap<String, ProfileSpecific> = mutableMapOf()
    }

    class ProfileSpecific {
        @Expose
        val pets: MutableList<PetData> = mutableListOf()
    }
}

data class PetData(
    @Expose val petInternalName: NeuInternalName, // The internal name of the pet, e.g., `RABBIT;5`
    @Expose var skinInternalName: NeuInternalName? = null, // The skin of the pet, e.g., `PET_SKIN_WOLF_DOGE`
    @Expose var heldItemInternalName: NeuInternalName? = null, // The held item of the pet, e.g., `PET_ITEM_COMBAT_SKILL_BOOST_EPIC`
    @Expose var exp: Double? = null, // The total XP of the pet as a double, e.g., `0.0`
    @Expose val uuid: UUID? = null, // If this data is for a 'real' pet, this is the UUID of it
) {
    val internalNameSplits: Pair<String, LorenzRarity> =
        PetUtils.internalNameToPetWithRarity(petInternalName) ?: ("???" to LorenzRarity.COMMON)

    val rarity = internalNameSplits.second
    val cleanInternalName = internalNameSplits.first
    val cleanName = cleanInternalName
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.firstLetterUppercase() }
    val coloredName: String = "${rarity.chatColorCode}$cleanName"

    val level: Int get() = PetUtils.xpToLevel(exp ?: 0.0, petInternalName)
    val skinTag: String? get() = skinInternalName?.getItemStack()?.getItemRarityOrNull()?.let { it.chatColorCode + "✦" }
    val levelProgressionPercentage: Double? = when {
        exp == null -> null
        level <= PetUtils.getMaxLevel(cleanInternalName) -> {
            val currentLevelXp = levelToXp(level, rarity, cleanInternalName) ?: 0.0
            val nextLevelXp = levelToXp(level + 1, rarity, cleanName) ?: 0.0
            val xpDifference = nextLevelXp - currentLevelXp
            val xpProgress = (exp ?: 0.0) - currentLevelXp
            xpProgress / xpDifference * 100
        }
        else -> 100.0
    }

    fun getUserFriendlyName(
        includeLevel: Boolean = true,
        includeSkinTag: Boolean = true,
    ) = buildString {
        if (includeLevel) append("§7[Lvl $level] ")
        append(coloredName)
        if (includeSkinTag && skinTag != null) append(" $skinTag")
    }

    fun getItemStackOrNull(): ItemStack? = skinInternalName?.getItemStack()
        ?: petInternalName.getItemStackOrNull()
}
