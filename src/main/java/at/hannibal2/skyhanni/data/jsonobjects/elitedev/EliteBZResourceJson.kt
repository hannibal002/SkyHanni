package at.hannibal2.skyhanni.data.jsonobjects.elitedev

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

data class EliteBazaarResponse(
    @Expose val products: Map<NeuInternalName, EliteBazaarProduct>,
)

data class EliteBazaarProduct(
    @Expose val name: String? = null,
    @Expose val npc: Double? = null,
    @Expose val sell: Double,
    @Expose val buy: Double,
    @Expose val sellOrder: Double,
    @Expose val buyOrder: Double,
    @Expose val averageSell: Double,
    @Expose val averageBuy: Double,
    @Expose val averageSellOrder: Double,
    @Expose val averageBuyOrder: Double,
)
