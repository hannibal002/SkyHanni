package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

data class DragonProfitTrackerItemsJson(
    @Expose val items: Map<NeuInternalName, DragonProfitTrackerItemDataJson>,
)

data class DragonProfitTrackerItemDataJson(
    @Expose val weight: Int,
)
