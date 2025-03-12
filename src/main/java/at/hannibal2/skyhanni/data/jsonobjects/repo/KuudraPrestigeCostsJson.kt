package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class KuudraPrestigeCostsJson(
    @Expose @SerializedName("kuudra_prestige_cost") val kuudraPrestigeCost: Map<String, Map<NeuInternalName, Int>>,
)

