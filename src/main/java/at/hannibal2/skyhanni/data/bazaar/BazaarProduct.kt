package at.hannibal2.skyhanni.data.bazaar

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class BazaarProduct(
    @Expose
    @SerializedName("product_id")
    val productId: String,
    @Expose
    @SerializedName("quick_status")
    val quickStatus: BazaarQuickStatus,
    @Expose
    @SerializedName("sell_summary")
    val sellSummary: List<BazaarSummary>,
    @Expose
    @SerializedName("buy_summary")
    val buySummary: List<BazaarSummary>,
)
