package at.hannibal2.skyhanni.data.bazaar

import com.google.gson.annotations.Expose

data class BazaarSummary(
    @Expose
    val amount: Long,
    @Expose
    val pricePerUnit: Double,
    @Expose
    val orders: Long,
)
