package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class BazaarJson(
    @Expose @SerializedName("daily_limit") val dailyLimit: Double,
    @Expose @SerializedName("cap_orders_at_integer_limit") val capOrdersAtIntLimit: Boolean,
)
