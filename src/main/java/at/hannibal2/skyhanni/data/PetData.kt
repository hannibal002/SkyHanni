package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.CurrentPetApi.petDespawnMenuPattern
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetSkinJson
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.PetUtils.getSkinOrNull
import at.hannibal2.skyhanni.utils.PetUtils.levelToXp
import at.hannibal2.skyhanni.utils.PetUtils.xpToLevel
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.Gson
import com.google.gson.annotations.Expose
import net.minecraft.item.ItemStack

data class PetDataStorage(
    @Expose var petItem: NeuInternalName? = null, // The internal name of the pet, e.g., `RABBIT;5`
    @Expose var heldItem: NeuInternalName? = null, // The held item of the pet, e.g., `PET_ITEM_COMBAT_SKILL_BOOST_EPIC`
    @Expose var cleanName: String? = null, // The clean name of the pet, e.g., `Rabbit`
    @Expose var skinSymbolColor: LorenzColor? = null, // The color symbol of the skin of the pet, e.g., §d ✦ -> `LorenzColor.Pink`
    @Expose var rarity: LorenzRarity? = null, // The rarity of the pet, e.g., `COMMON`
    @Expose var level: Int? = null, // The current level of the pet as an integer, e.g., `100`
    @Expose var xp: Double? = null, // The total XP of the pet as a double, e.g., `0.0`
    @Expose var skinInternalNameOverride: NeuInternalName? = null, // If the skin is known (i.e., from stored data or Inventory)
) {
    fun toPetData(): PetData = PetData(
        petItem = petItem,
        heldItem = heldItem,
        cleanName = cleanName,
        skinSymbolColor = skinSymbolColor,
        rarity = rarity,
        level = level,
        xp = xp,
        skinInternalNameOverride = skinInternalNameOverride,
    )
}

data class PetData(
    val petItem: NeuInternalName? = null,
    val heldItem: NeuInternalName? = null,
    val cleanName: String? = null,
    val skinSymbolColor: LorenzColor? = null,
    val rarity: LorenzRarity? = null,
    val level: Int? = null,
    val xp: Double? = null,
    val skinInternalNameOverride: NeuInternalName? = null,
) {
    val displayName = "${rarity?.chatColorCode}$cleanName"
    val skin: NeuPetSkinJson? = getSkinOrNull()

    val levelProgressionPercentage: Double? = when {
        xp == null -> null
        level == null -> null
        petItem == null -> null
        PetUtils.isValidLevel(level + 1, petItem) -> {
            val currentLevelXp = levelToXp(level, petItem) ?: 0.0
            val nextLevelXp = levelToXp(level + 1, petItem) ?: 0.0
            val xpDifference = nextLevelXp - currentLevelXp
            val xpProgress = xp - currentLevelXp
            xpProgress / xpDifference * 100
        }
        else -> 100.0
    }

    @Expose var skinInternalName: NeuInternalName? = skinInternalNameOverride ?: skin?.internalName

    // Please god only use this for UI, not for comparisons
    fun getUserFriendlyName(
        includeLevel: Boolean = true,
        includeSkin: Boolean = true,
    ): String {
        val levelString = if (includeLevel) "§7[Lvl $level] §r" else ""
        val skinString = if (includeSkin) skinSymbolColor?.let { "${it.getChatColor()}✦" }.orEmpty() else ""
        return "§r$levelString$displayName$skinString"
    }

    fun getItemStackOrNull(): ItemStack? = skin?.itemStack ?: petItem?.getItemStackOrNull()

    override fun equals(other: Any?): Boolean {
        if (other !is PetData) return false
        return allButSkinEquivalent(other) && this.skinInternalName == other.skinInternalName
    }

    fun allButSkinEquivalent(other: Any?): Boolean {
        if (other !is PetData) return false
        return this.petItem == other.petItem &&
            this.heldItem == other.heldItem &&
            this.cleanName == other.cleanName &&
            this.rarity == other.rarity &&
            this.level == other.level &&
            this.xp == other.xp
    }

    override fun hashCode(): Int {
        var result = cleanName.hashCode()
        result = 31 * result + rarity.hashCode()
        result = 31 * result + (heldItem?.hashCode() ?: 0)
        result = 31 * result + (level ?: 0)
        return result
    }

    fun isInitialized(): Boolean {
        return petItem != null && cleanName != null && rarity != null && level != null && xp != null
    }

    fun asStorage(): PetDataStorage = PetDataStorage(
        petItem = petItem,
        heldItem = heldItem,
        cleanName = cleanName,
        skinSymbolColor = skinSymbolColor,
        rarity = rarity,
        level = level,
        xp = xp,
        skinInternalNameOverride = skinInternalNameOverride,
    )

    companion object {
        // <editor-fold desc="Pet Data Extractors (General)">
        fun parsePetData(
            lines: List<String>,
            itemHandler: (String) -> NeuInternalName?,
            xpHandler: (String) -> Double?,
            petHandler: (String) -> PetData?
        ): Pair<PetData, Double>? {
            return parsePetDataLists(
                lines,
                itemHandlerList = { it.firstNotNullOfOrNull(itemHandler) },
                xpHandlerList = { it.firstNotNullOfOrNull(xpHandler) },
                petHandlerList = { it.firstNotNullOfOrNull(petHandler) }
            )
        }

        fun parsePetDataLists(
            lines: List<String>,
            itemHandlerList: (List<String>) -> NeuInternalName?,
            xpHandlerList: (List<String>) -> Double?,
            petHandlerList: (List<String>) -> PetData?
        ): Pair<PetData, Double>? {
            val petItem = itemHandlerList(lines) ?: return null
            val overflowXP = xpHandlerList(lines) ?: 0.0

            val data = petHandlerList(lines) ?: return null
            val petData = PetData(
                petItem = data.petItem,
                cleanName = data.cleanName,
                rarity = data.rarity,
                heldItem = petItem,
                level = data.level,
                xp = data.xp,
            )

            return petData to overflowXP
        }

        private fun parseFromItem(item: ItemStack): PetData {
            val petInfo = Gson().fromJson(item.getExtraAttributes()?.getString("petInfo"), PetNBT::class.java)

            val petName = petInfo.type
            val petRarity = LorenzRarity.getByName(petInfo.tier) ?: ErrorManager.skyHanniError(
                "Couldn't parse pet rarity.",
                Pair("petNBT", petInfo),
                Pair("rarity", petInfo.tier),
            )
            val internalName = petNameToInternalName(petName, petRarity)
            val level = xpToLevel(petInfo.exp, internalName) ?: 0

            return PetData(
                petItem = internalName,
                cleanName = petName.firstLetterUppercase(),
                level = level,
                rarity = petRarity,
                heldItem = petInfo.heldItem?.toInternalName(),
                xp = petInfo.exp,
            )
        }

        fun petNameToInternalName(name: String, rarity: LorenzRarity): NeuInternalName =
            "${name.removeColor()};${rarity.id}".toInternalName()

        fun internalNameToPetName(internalName: NeuInternalName): Pair<String, LorenzRarity>? {
            val (name, rarityStr) = internalName.asString().split(";")
            val rarity = LorenzRarity.getById(rarityStr.toInt()) ?: return null
            return Pair(name, rarity)
        }

        fun parsePetAsItem(item: ItemStack): PetData? {
            val lore = item.getLore()
            if (petDespawnMenuPattern.anyMatches(lore)) return null
            return parseFromItem(item)
        }
        // </editor-fold>
    }
}

data class PetNBT(
    val type: String,
    val active: Boolean,
    val exp: Double,
    val tier: String,
    val hideInfo: Boolean,
    val heldItem: String?,
    val candyUsed: Int,
    val skin: String?,
    val uuid: String,
    val uniqueId: String,
    val hideRightClick: Boolean,
    val noMove: Boolean,
)
