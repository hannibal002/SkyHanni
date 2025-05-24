package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import com.google.gson.annotations.Expose
import net.minecraft.item.ItemStack
import java.util.UUID

// todo 1.21 impl needed
class PetDataStorage {
    @Expose
    val players: MutableMap<UUID, PlayerSpecific> = mutableMapOf()

    class PlayerSpecific {
        @Expose
        val profiles: MutableMap<String, ProfileSpecific> = mutableMapOf()
    }

    class ProfileSpecific {
        @Expose
        val pets: MutableList<PetData> = mutableListOf()
    }
}

@KSerializable
data class PetData(
    @Expose val petInternalName: NeuInternalName, // The internal name of the pet, e.g., `RABBIT;5`
    @Expose var skinInternalName: NeuInternalName? = null, // The skin of the pet, e.g., `PET_SKIN_WOLF_DOGE`
    @Expose var heldItemInternalName: NeuInternalName? = null, // The held item of the pet, e.g., `PET_ITEM_COMBAT_SKILL_BOOST_EPIC`
    @Expose var exp: Double? = null, // The total XP of the pet as a double, e.g., `0.0`
    @Expose val uuid: UUID? = null, // If this data is for a 'real' pet, this is the UUID of it
) {
    private val internalNameSplits: Pair<String, LorenzRarity> =
        PetUtils.internalNameToPetWithRarity(petInternalName)
            ?: ("???" to LorenzRarity.COMMON)

    val rarity = internalNameSplits.second
    val cleanInternalName = internalNameSplits.first
    val cleanName = cleanInternalName
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.firstLetterUppercase() }
    val coloredName = "${rarity.chatColorCode}$cleanName"

    val level: Int get() = PetUtils.xpToLevel(exp ?: 0.0, petInternalName)
    val skinTag: String? get() = skinInternalName?.getItemStack()?.getItemRarityOrNull()?.let { it.chatColorCode + "✦" }
    val levelProgressionPercentage: Double get() = when {
        exp == null || exp == 0.0 -> 0.0
        PetUtils.getMaxLevel(petInternalName) <= level -> 100.0
        else -> {
            val xpDifference = nextLevelXp - currentLevelXp
            val xpProgress = (exp ?: 0.0) - currentLevelXp
            xpProgress / xpDifference * 100
        }
    }
    val currentLevelXp get() = PetUtils.levelToXp(level, petInternalName) ?: 0.0
    val nextLevelXp get() = PetUtils.levelToXp(level + 1, petInternalName) ?: 0.0

    val overflowXp get() = when {
        level == PetUtils.getMaxLevel(petInternalName) -> {
            val currentTotalXp = exp ?: 0.0
            val levelXp = PetUtils.levelToXp(level, petInternalName) ?: 0.0
            (currentTotalXp - levelXp).takeIf { it >= 0.0 } ?: 0.0
        }
        else -> 0.0
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
