package at.hannibal2.hanni.data.jsonobjects.repo

import at.hannibal2.hanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

data class FishingProfitItemsJson(
    @Expose val categories: Map<String, List<NeuInternalName>>,
)
