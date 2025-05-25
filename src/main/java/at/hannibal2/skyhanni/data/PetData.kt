package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.AnimatedSkinJson
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.PetUtils.hasValidHigherTier
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackAnimationFrame
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
    @Expose var skinVariantIndex: Int? = null, // Used for pet skins that have variants, otherwise unused
    @Expose var heldItemInternalName: NeuInternalName? = null, // The held item of the pet, e.g., `PET_ITEM_COMBAT_SKILL_BOOST_EPIC`
    @Expose var exp: Double? = null, // The total XP of the pet as a double, e.g., `0.0`
    @Expose val uuid: UUID? = null, // If this data is for a 'real' pet, this is the UUID of it
) {
    private val isItemTierBoosted get() = heldItemInternalName == TIER_BOOST && petInternalName.hasValidHigherTier()
    private val internalNameSplits: Pair<String, LorenzRarity> =
        PetUtils.internalNameToPetWithRarity(petInternalName)
            ?: ("???" to LorenzRarity.COMMON)
    private val specifiedRarity = internalNameSplits.second
    val cleanInternalName = internalNameSplits.first
    val cleanName = cleanInternalName
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.firstLetterUppercase() }
    val coloredName = "${rarity.chatColorCode}$cleanName"

    val rarity: LorenzRarity get() = specifiedRarity.oneAbove().takeIf { isItemTierBoosted } ?: specifiedRarity
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
        if (includeLevel) appendLine("§7[Lvl $level] ")
        appendLine(coloredName)
        if (includeSkinTag && skinTag != null) appendLine(" $skinTag")
    }

    private fun String.buildTextureItemStack(): ItemStack {
        val (uuid, texture) = this.split(":")
        return ItemUtils.createSkull("Pet Skin", uuid, texture)
    }

    fun getAnimatedItemStackSequence(firstFrameOnly: Boolean = false): List<ItemStackAnimationFrame>? {
        val baseStack = getSkinItemStackOrNull(0) ?: run {
            return null
        }
        val firstFrame = ItemStackAnimationFrame(baseStack)
        val animationJson = getAnimatedJsonOrNull()
        if (firstFrameOnly || animationJson == null) {
            return listOf(firstFrame)
        }
        return animationJson.textures.map {
            ItemStackAnimationFrame(
                it.buildTextureItemStack(),
                ticks = animationJson.ticks,
            )
        }
    }

    private fun getAnimatedJsonOrNull(): AnimatedSkinJson? {
        val skinVariantIndex = skinVariantIndex ?: return null
        val skinInternalName = skinInternalName ?: return null
        val variantIdentifier = PetUtils.getSkinVariantIdentifier(skinInternalName, skinVariantIndex)
        val fullSkinIdentifier = "${skinInternalName.asString()}_$variantIdentifier"
        return PetUtils.animatedPetSkins[fullSkinIdentifier]
    }

    private fun getSkinItemStackOrNull(frameIndex: Int = 0): ItemStack? {
        val skinInternalName = skinInternalName ?: return null
        val baseItemStack = skinInternalName.getItemStackOrNull() ?: return null
        val animatedSkinJson = getAnimatedJsonOrNull()?.takeIf { it.textures.any() } ?: return baseItemStack
        val boundedFrameIndex = frameIndex.takeIf { it > 0 && it < animatedSkinJson.textures.size } ?: 0
        return animatedSkinJson.textures[boundedFrameIndex].buildTextureItemStack()
    }

    fun getItemStackOrNull(frameIndex: Int = 0): ItemStack? =
        getSkinItemStackOrNull(frameIndex) ?: petInternalName.getItemStackOrNull()

    companion object {
        private val TIER_BOOST = "PET_ITEM_TIER_BOOST".toInternalName()
    }

    override fun toString() = buildString {
        appendLine("  coloredName: $coloredName")
        appendLine("  petInternalName: ${petInternalName.asString()}")
        appendLine("    isPet: ${petInternalName.isPet}")
        appendLine("    hasValidHigherTier: ${petInternalName.hasValidHigherTier()}")
        appendLine("  skinInternalName: ${skinInternalName?.asString()}")
        appendLine("  skinVariantIndex: $skinVariantIndex")
        appendLine("    knownAnimationJson?: ${getAnimatedJsonOrNull() != null}")
        appendLine("  heldItemInternalName: ${heldItemInternalName?.asString()}")
        appendLine("  exp: ${exp?.addSeparators() ?: 0.0}")
        appendLine("  uuid: $uuid")
        appendLine("")
        appendLine("  isItemTierBoosted: $isItemTierBoosted")
        appendLine("  rarity: $rarity")
        appendLine("  level: $level")
    }
}
