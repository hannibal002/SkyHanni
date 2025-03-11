package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class KuudraPrestigeCostsJson(
    @Expose @SerializedName("kuudra_prestige_cost") val kuudraPrestigeCost: Map<String, PrestigeCost>,
)

data class PrestigeCost(
    @Expose @SerializedName("ESSENCE_CRIMSON") val crimsonEssence: Int,
    @Expose @SerializedName("KUUDRA_TEETH") val kuudraTeeth: Int,
    @Expose @SerializedName("SKYBLOCK_COIN") val coins: Int,
)

