package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

data class SpecialCollectionItems(
    @Expose val global: Map<String, NeuInternalName> = emptyMap(),  // Collection name -> internal name of corresponding item
)
