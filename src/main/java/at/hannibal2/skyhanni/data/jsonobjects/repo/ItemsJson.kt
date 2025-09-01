package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ItemsJson(
    @Expose @SerializedName("crimson_armors") val crimsonArmors: List<String>,
    @Expose @SerializedName("crimson_tiers") val crimsonTiers: Map<String, Int>,
    @Expose @SerializedName("crimson_prestige_costs") val crimsonPrestigeCosts: Map<String, Map<NeuInternalName, Int>>,
    @Expose @SerializedName("enchant_multiplier") val enchantMultiplier: Map<String, Float>,
    @Expose @SerializedName("lava_fishing_rods") val lavaFishingRods: List<NeuInternalName>,
    @Expose @SerializedName("water_fishing_rods") val waterFishingRods: List<NeuInternalName>,
    @Expose @SerializedName("dungeon_secret_items") val dungeonSecretItems: Set<NeuInternalName>,
    @Expose @SerializedName("book_bundle_amount") val bookBundleAmount: Map<String, Int>,
    @Expose @SerializedName("value_calculation_data") val valueCalculationData: ItemValueCalculationDataJson,
    @Expose @SerializedName("compact_names") val compactNames: Map<String, String>,
    @Expose @SerializedName("renamed_enchantments") val renamedEnchantments: Map<String, String>,
    @Expose @SerializedName("shard_name_overrides") val shardNameOverrides: Map<String, String>,
)

data class ItemValueCalculationDataJson(
    @Expose @SerializedName("always_active_enchants") val alwaysActiveEnchants: Map<String, AlwaysActiveEnchantJson>,
    @Expose @SerializedName("only_tier_one_prices") val onlyTierOnePrices: List<String>,
    @Expose @SerializedName("only_tier_five_prices") val onlyTierFivePrices: List<String>,
    @Expose @SerializedName("endcap_enchants") val endcapEnchants: Map<String, EndCapData>? = mapOf(),
)

data class AlwaysActiveEnchantJson(
    @Expose val level: Int,
    @Expose val internalNames: List<NeuInternalName>,
)

data class EndCapData(
    @Expose @SerializedName("required_level") val requiredLevel: Int,
    @Expose @SerializedName("endcap_item") val endcapItem: NeuInternalName,
)
