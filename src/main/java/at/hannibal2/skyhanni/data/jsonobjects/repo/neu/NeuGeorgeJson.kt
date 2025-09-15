package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

data class NeuGeorgeJson(
    @Expose val prices: Map<NeuInternalName, Double>?
)
