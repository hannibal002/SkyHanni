package at.hannibal2.hanni.data.jsonobjects.repo

import at.hannibal2.hanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

data class SlayerProfitTrackerItemsJson(
    @Expose val slayers: Map<String, List<NeuInternalName>>,
)
