package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.data.Perk
import com.google.gson.annotations.Expose

data class ForcedRepoPerksJson(
    @Expose val perks: List<Perk>? = null,
)
