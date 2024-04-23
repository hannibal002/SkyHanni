package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class ContributorsJson(
    @Expose val contributors: Map<String, ContributorJsonEntry>
)

data class ContributorJsonEntry(
    @Expose val suffix: String = "§c:O",
    @Expose val spinny: Boolean = false
)
