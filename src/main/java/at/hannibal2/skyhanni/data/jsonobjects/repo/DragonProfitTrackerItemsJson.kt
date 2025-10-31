package at.hannibal2.hanni.data.jsonobjects.repo

import at.hannibal2.hanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

data class DragonProfitTrackerItemsJson(
    @Expose val items: Map<NeuInternalName, DragonProfitTrackerItemDataJson>,
)

data class DragonProfitTrackerItemDataJson(
    @Expose val weight: Int,
)
