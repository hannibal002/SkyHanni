package at.hannibal2.skyhanni.data.bazaar

import com.google.gson.annotations.Expose

class BazaarApiResponse(
    @Expose
    val success: Boolean,
    @Expose
    val cause: String,
    @Expose
    val lastUpdated: Long,
    @Expose
    val products: Map<String, BazaarProduct>,
)
