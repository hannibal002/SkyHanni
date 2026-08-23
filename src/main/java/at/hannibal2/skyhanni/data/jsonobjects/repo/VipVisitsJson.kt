package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class VipVisitsJson(
    @Expose @SerializedName(value = "vip_visits", alternate = ["vipVisits"]) val vipVisits: List<String>,
)
