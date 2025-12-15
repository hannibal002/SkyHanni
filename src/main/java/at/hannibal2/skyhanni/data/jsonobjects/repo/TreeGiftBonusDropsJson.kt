package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class TreeGiftBonusDropsJson(
    @Expose @SerializedName("uncommon_drops") val uncommonDrops: List<NeuInternalName>,
    @Expose @SerializedName("enchanted_books") val enchantedBooks: List<NeuInternalName>,
    @Expose @SerializedName("boosters") val boosters: List<NeuInternalName>,
    @Expose @SerializedName("shards") val shards: List<NeuInternalName>,
    @Expose @SerializedName("mobs") val mobs: List<String>,
    @Expose @SerializedName("runes") val runes: List<NeuInternalName>,
    @Expose @SerializedName("misc") val miscDrops: List<NeuInternalName>,
)
