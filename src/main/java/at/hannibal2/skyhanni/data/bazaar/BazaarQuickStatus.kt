package at.hannibal2.skyhanni.data.bazaar

import com.google.gson.annotations.Expose

class BazaarQuickStatus(
    @Expose
    val productId: String,
    @Expose
    val sellPrice: Double,
    @Expose
    val sellVolume: Long,
    @Expose
    val sellMovingWeek: Long,
    @Expose
    val sellOrders: Long,
    @Expose
    val buyPrice: Double,
    @Expose
    val buyVolume: Long,
    @Expose
    val buyMovingWeek: Long,
    @Expose
    val buyOrders: Long,
)
