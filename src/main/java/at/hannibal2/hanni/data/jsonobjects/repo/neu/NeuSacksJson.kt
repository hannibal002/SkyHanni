package at.hannibal2.hanni.data.jsonobjects.repo.neu

import at.hannibal2.hanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

data class NeuSacksJson(
    @Expose val sacks: Map<String, SackInfo>,
)

data class SackInfo(
    @Expose val item: NeuInternalName,
    @Expose val contents: List<NeuInternalName>,
)
