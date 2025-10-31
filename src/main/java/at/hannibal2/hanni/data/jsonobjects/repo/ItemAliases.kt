package at.hannibal2.hanni.data.jsonobjects.repo

import at.hannibal2.hanni.utils.NeuInternalName
import com.google.gson.annotations.Expose

data class ItemAliases(
    @Expose val global: Map<String, NeuInternalName> = emptyMap(),
)
