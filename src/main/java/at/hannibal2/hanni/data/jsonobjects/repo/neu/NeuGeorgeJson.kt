package at.hannibal2.hanni.data.jsonobjects.repo.neu

import at.hannibal2.hanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

data class NeuGeorgeJson(
    @Expose val prices: Map<NeuInternalName, Double>?
)
