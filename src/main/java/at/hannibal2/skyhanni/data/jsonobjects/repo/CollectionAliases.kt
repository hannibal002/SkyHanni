package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class CollectionAliases(
    // Map<Incorrect, Correct>
    @Expose val global: Map<String, String> = emptyMap(),
)
