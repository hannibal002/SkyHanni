package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class TreeGiftBonusDropsJson(
    @Expose @SerializedName("uncommon_drops") val uncommonDrops: List<NeuInternalName>,
    @Expose @SerializedName("enchanted_books") val enchantedBooks: List<NeuInternalName>,
    @Expose val boosters: List<NeuInternalName>,
    @Expose val shards: List<NeuInternalName>,
    @Expose val mobs: List<String>,
    @Expose val runes: List<NeuInternalName>,
    @Expose @SerializedName("misc_drops") val miscDrops: List<NeuInternalName>,
)
