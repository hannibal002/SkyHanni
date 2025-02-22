package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.CurrentPetApi.petDespawnMenuPattern
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuPetSkinJson
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.PetUtils.getSkinOrNull
import at.hannibal2.skyhanni.utils.PetUtils.xpToLevel
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.Gson
import com.google.gson.annotations.Expose
import net.minecraft.item.ItemStack

data class PetData(
    @Expose val petItem: NeuInternalName? = null, // The internal name of the pet, e.g., `RABBIT;5`
    @Expose val heldItem: NeuInternalName? = null, // The held item of the pet, e.g., `PET_ITEM_COMBAT_SKILL_BOOST_EPIC`
    @Expose val cleanName: String? = null, // The clean name of the pet, e.g., `Rabbit`
    @Expose val skinSymbolColor: LorenzColor? = null, // The color symbol of the skin of the pet, e.g., §d ✦ -> `LorenzColor.Pink`
    @Expose val rarity: LorenzRarity? = null, // The rarity of the pet, e.g., `COMMON`
    @Expose val level: Int? = null, // The current level of the pet as an integer, e.g., `100`
    @Expose val xp: Double? = null, // The total XP of the pet as a double, e.g., `0.0`
    @Expose val skinInternalNameOverride: NeuInternalName? = null, // If the skin is known (i.e., from stored data or Inventory)
) {
    val displayName = petItem?.itemName
    val formattedName = "${rarity?.chatColorCode}$cleanName"
    val skin: NeuPetSkinJson? = getSkinOrNull()

    @Expose var skinInternalName: NeuInternalName? = skinInternalNameOverride ?: skin?.internalName

    // Please god only use this for UI, not for comparisons
    fun getUserFriendlyName(
        includeLevel: Boolean = true,
        includeSkin: Boolean = true,
    ): String {
        val levelString = if (includeLevel) "§7[Lvl $level] §r" else ""
        val skinString = if (includeSkin) skinSymbolColor?.let { "${it.getChatColor()}✦" }.orEmpty() else ""
        return "§r$levelString$formattedName$skinString"
    }

    fun getItemStackOrNull(): ItemStack? = skin?.itemStack ?: petItem?.getItemStackOrNull()

    override fun equals(other: Any?): Boolean {
        if (other !is PetData) return false
        return this.petItem == other.petItem &&
            this.heldItem == other.heldItem &&
            this.cleanName == other.cleanName &&
            this.skinSymbolColor == other.skinSymbolColor &&
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
