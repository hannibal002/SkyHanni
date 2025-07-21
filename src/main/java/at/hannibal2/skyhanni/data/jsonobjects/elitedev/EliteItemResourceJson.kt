package at.hannibal2.skyhanni.data.jsonobjects.elitedev

import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraTier
import at.hannibal2.skyhanni.features.nether.reputationhelper.FactionType
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils
import at.hannibal2.skyhanni.utils.TimeUnit
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.github.notenoughupdates.moulconfig.ChromaColour
import kotlin.time.Duration

@KSerializable
data class EliteItemResponse(
    @Expose val items: Map<NeuInternalName, EliteItem>,
)

// <editor-fold desc="Helpers to parse to SH formats">
enum class SoulboundType { NONE, SOLO, COOP }

data class EliteSkin(
    @Expose val value: String,
    @Expose val signature: String,
)

enum class SlotCostType { COINS, ITEM }

@KSerializable
data class EliteGemstoneSlotCost(
    @Expose private val type: String,
    @Expose val coins: Int? = null,
    @Expose @SerializedName("item_id") val itemId: NeuInternalName? = null,
    @Expose @SerializedName("amount") val amount: Int? = null,
) {
    val costType: SlotCostType = SlotCostType.valueOf(type.uppercase())
}

@KSerializable
data class EliteGemstoneSlot(
    @Expose @SerializedName("slot_type") private val slotTypeStr: String,
    @Expose @SerializedName("costs") private val eliteCosts: List<EliteGemstoneSlotCost> = emptyList(),
) {
    val slotType = SkyBlockItemModifierUtils.GemstoneSlotType.getByName(slotTypeStr)
    val costs: Map<NeuInternalName, Int> = eliteCosts.mapNotNull { cost ->
        when (cost.costType) {
            SlotCostType.COINS -> NeuInternalName.SKYBLOCK_COIN to (cost.coins ?: 0)
            SlotCostType.ITEM -> {
                val itemId = cost.itemId ?: return@mapNotNull null
                itemId to (cost.amount ?: 0)
            }
        }
    }.associate { it }
}

enum class EliteRequirementType {
    CHOCOLATE_FACTORY, COLLECTION,
    CRIMSON_ISLE_REPUTATION, DUNGEON_SKILL,
    DUNGEON_TIER, EASTER_RABBIT,
    GARDEN_LEVEL, HEART_OF_THE_MOUNTAIN,
    KUUDRA_COMPLETION, MELODY_HAIR,
    ONE_OF, PROFILE_AGE,
    SKILL, SLAYER,
    TARGET_PRACTICE, TROPHY_FISHING,
}

@KSerializable
data class EliteItemRequirement(
    @Expose val type: EliteRequirementType,
    // Slayer, Skill
    @Expose val level: Int? = null,
    // Collection, Dungeon
    @Expose val tier: Int? = null,

    // Kuudra
    @Expose @SerializedName("kuudra_tier") val kuudraTier: KuudraTier? = null,
    // Dungeon
    @Expose @SerializedName("dungeon_type") val dungeonType: String? = null,
    // Slayer
    @Expose @SerializedName("slayer_boss_type") private val slayerBossTypeStr: String? = null,
    // Skill
    @Expose val skill: SkyblockStat? = null,
    // Collection
    @Expose val collection: String? = null,
    // Target Practice
    @Expose @SerializedName("mode") private val eliteMode: String? = null,
    // CI Reputation
    @Expose @SerializedName("faction") private val ciFactionStr: String? = null,
    @Expose @SerializedName("reputation") val ciReputation: Int? = null,
    // Trophy Fishing
    @Expose @SerializedName("reward") private val trophyFishingTier: TrophyRarity? = null,
    // Hoppity
    @Expose val rabbit: String? = null,
    // Profile Age
    @Expose @SerializedName("minimum_age") private val minimumAgeInt: Int? = null,
    @Expose @SerializedName("minimum_age_unit") private val minimumAgeUnitStr: String? = null,

    // "One of" requirements
    @Expose @SerializedName("lore_index") val loreIndex: Int? = null,
    @Expose @SerializedName("requirements") val oneOfRequirements: List<EliteItemRequirement>? = null,
) {
    val minimumAge: Duration? = minimumAgeInt?.let { rawInt ->
        val ageUnit: TimeUnit = minimumAgeUnitStr?.let {
            TimeUnit.getByName(it.uppercase())
        } ?: return@let null
        ageUnit.asDuration(rawInt)
    }
    val slayerBossType: SlayerType? = slayerBossTypeStr?.let {
        SlayerType.getByClazzName(it)
    }
    val targetPracticeMode: Int? = eliteMode?.romanToDecimalIfNecessary()
    val ciFaction: FactionType? = ciFactionStr?.let {
        FactionType.valueOf(it.removeSuffix("S").uppercase())
    }
}

@KSerializable
data class EliteItemCatacombRequirement(
    @Expose val type: String,
    @Expose @SerializedName("dungeon_type") val dungeonType: String? = null,
    @Expose val level: Int,
)

enum class UpgradeCostType { ESSENCE, ITEM }

@KSerializable
@Suppress("unused")
data class EliteItemUpgradeCost(
    @Expose val type: UpgradeCostType,
    @Expose @SerializedName("essence_type") val eliteEssenceType: String? = null,
    @Expose @SerializedName("item_id") val itemId: NeuInternalName? = null,
    @Expose val amount: Int,
) {
    private val essenceType: NeuInternalName? = eliteEssenceType?.let {
        "ESSENCE_$it".toInternalName()
    }
    private val item: NeuInternalName? = itemId ?: essenceType
}

enum class EliteMuseumType { ARMOR_SETS, WEAPONS, RARITIES }
enum class EliteMuseumGameStage {
    STARTER,
    AMATEUR,
    INTERMEDIATE,
    SKILLED,
    EXPERT,
    PROFESSIONAL,
    MASTER,
}

@KSerializable
@Suppress("PrivatePropertyName")
data class EliteMuseumData(
    @Expose private val donation_xp: Int,
    @Expose private val parent: Map<String, String>? = null,
    @Expose val type: EliteMuseumType,
    @Expose @SerializedName("armor_set_donation_xp") val armorSetDonationXp: Map<String, Int>? = null,
    @Expose @SerializedName("game_stage") val gameStage: EliteMuseumGameStage,
    @Expose @SerializedName("mapped_item_ids") val mappedItemIds: List<NeuInternalName> = emptyList(),
) {
    val donationXp: Int = donation_xp
    val selfIdentifier: String? = parent?.entries?.first()?.key
    val parentIdentifier: String? = parent?.entries?.first()?.value
}

enum class EliteItemOrigin { RIFT, BINGO }
// </editor-fold>

@KSerializable
data class EliteItem(
    @Expose private val id: NeuInternalName,
    @Expose val material: String,
    @Expose val durability: Int? = null,
    @Expose val skin: EliteSkin? = null,
    @Expose val name: String,
    @Expose @SerializedName("description") val dirtyDescription: String? = null,
    @Expose @SerializedName("category") private val itemCategory: String? = null,
    @Expose @SerializedName("tier") val rarity: LorenzRarity? = null,
    @Expose @SerializedName("has_uuid") val hasUuid: Boolean? = null,
    @Expose @SerializedName("item_model") val itemModel: String? = null,
    @Expose @SerializedName("soulbound") val soulboundType: SoulboundType? = null,
    @Expose @SerializedName("npc_sell_price") val npcSellPrice: Double? = null,
    @Expose val origin: EliteItemOrigin? = null,

    // Minecraft fields that hypixel uses
    @Expose val unstackable: Boolean? = null,
    @Expose val glowing: Boolean? = null,
    @Expose @SerializedName("can_interact") val canInteract: Boolean? = null,
    @Expose @SerializedName("can_interact_right_click") val canInteractRightClick: Boolean? = null,

    @Expose val furniture: NeuInternalName? = null, // Furniture item, if applicable
    @Expose @SerializedName("color") val eliteColor: String? = null, // Specific to dye-able items

    // There were way too many specific fields for item modifiers, so we just use a map of Any to Any
    // Create your own data class if you want to parse specific modifiers for an item
    @Expose @SerializedName("item_specific") val itemSpecific: Map<Any, Any>? = null,

    /** Museum Related */
    @Expose val museum: Boolean? = null,
    @Expose @SerializedName("museum_data") val museumData: EliteMuseumData? = null,

    /** Usage Related */
    @Expose val requirements: List<EliteItemRequirement>? = null,
    @Expose @SerializedName("stats") private val eliteStats: Map<String, Double>? = null,
    @Expose @SerializedName("tiered_stats") private val eliteTieredStats: Map<String, List<Double>>? = null,

    /** Minions */
    @Expose @SerializedName("generator_tier") val minionTier: Int? = null,
    @Expose @SerializedName("generator") val minionType: String? = null,

    /** Upgrade Related */
    @Expose @SerializedName("upgrade_costs") val upgradeCosts: List<List<EliteItemUpgradeCost>>? = null,
    @Expose @SerializedName("can_have_power_scroll") val canHavePowerScroll: Boolean? = null,
    @Expose @SerializedName("gemstone_slots") val gemstoneSlots: List<EliteGemstoneSlot>? = null,
    @Expose @SerializedName("can_have_attributes") val canHaveAttributes: Boolean? = null,

    /** Dungeon Specific */
    @Expose @SerializedName("dungeon_item") val isDungeonItem: Boolean? = null,
    @Expose @SerializedName("rarity_salvageable") val raritySalvageable: Boolean? = null,
    @Expose @SerializedName("catacombs_requirements") val catacombsRequirements: List<EliteItemCatacombRequirement>? = null,

    /** Rift Specific */
    @Expose @SerializedName("rift_transferrable") val riftTransferrable: Boolean? = null,
    @Expose @SerializedName("motes_sell_price") val motesSellPrice: Double? = null,
    @Expose @SerializedName("lose_motes_value_on_transfer") val loseMotesValueOnTransfer: Boolean? = null,
) {
    val internalName: NeuInternalName = id
    // Example of why we need this:
    // "description": "%%gray%%%%italic%%A perfectly fine tooth, besides its radiant brightness..."
    val description: String? = dirtyDescription?.let {
        // Match all groups of %%...%%
        val matcher = formattingMatcher.matcher(it)
        if (!matcher.find()) return@let it // No formatting found, return original description
        // Replace all matched groups with their corresponding replacements
        var formattedDescription = it
        do {
            val matchedGroup = matcher.group(1) ?: continue
            val replacementMap = getReplacementMap(matchedGroup)
            for ((key, value) in replacementMap) {
                formattedDescription = formattedDescription.replace(key, value)
            }
        } while (matcher.find())
        formattedDescription
    }

    val color: ChromaColour? = eliteColor?.let { colorStr ->
        val splits = colorStr.split(",").takeIf { it.size == 3 } ?: return@let null
        val red = splits.getOrNull(0)?.toIntOrNull() ?: return@let null
        val green = splits.getOrNull(1)?.toIntOrNull() ?: return@let null
        val blue = splits.getOrNull(2)?.toIntOrNull() ?: return@let null
        ChromaColour.fromStaticRGB(red, green, blue, 255)
    }
    val category: ItemCategory? = itemCategory?.let { ItemCategory.valueOf(it) }
    val stats: Map<SkyblockStat, Double>? = eliteStats?.mapNotNull { (statKey, value) ->
        val skyblockStat = SkyblockStat.getValueOrNull(statKey) ?: return@mapNotNull null
        skyblockStat to value
    }?.associate { it }
    val tieredStats: Map<SkyblockStat, List<Double>>? = eliteTieredStats?.mapNotNull { (statKey, values) ->
        val skyblockStat = SkyblockStat.getValueOrNull(statKey) ?: return@mapNotNull null
        skyblockStat to values
    }?.associate { it }

    companion object {
        private val formattingMatcher = Regex("%%(.*?)%%").toPattern()
        private val replacementCache: MutableMap<String, Map<String, String>> = mutableMapOf()
        private fun getReplacementMap(matchedGroup: String): Map<String, String> {
            if (replacementCache.containsKey(matchedGroup)) {
                return replacementCache[matchedGroup].orEmpty()
            }
            val formatter = matchedGroup.removePrefix("%%").removeSuffix("%%").lowercase()
            val replacement = when (formatter) {
                "italic" -> "§o"
                "bold" -> "§l"
                "underline" -> "§n"
                "strikethrough" -> "§m"
                "obfuscated" -> "§k"
                "reset" -> "§r"
                else -> {
                    try {
                        LorenzColor.valueOf(matchedGroup.uppercase()).getChatColor()
                    } catch (e: Exception) {
                        ""
                    }
                }
            }
            val replacementMap = mapOf("%%$formatter%%" to replacement)
            replacementCache[matchedGroup] = replacementMap
            return replacementMap
        }
    }
}
