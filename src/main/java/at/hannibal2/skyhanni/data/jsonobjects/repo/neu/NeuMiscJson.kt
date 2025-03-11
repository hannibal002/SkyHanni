package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class NeuMiscJson(
    @Expose @SerializedName("ignored_talisman") val ignoredTalismans: List<String>,
    @Expose @SerializedName("talisman_upgrades") val talismanUpgrades: Map<String, List<String>>,
)
