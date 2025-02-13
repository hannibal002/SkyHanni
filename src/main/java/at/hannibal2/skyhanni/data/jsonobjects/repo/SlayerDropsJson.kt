package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class SlayerDropsJson(
    @Expose val drops: Map<String, Map<String, Map<String, DropDetails>>>
)

data class DropDetails(
    @Expose val xpNeeded: Int,
    @Expose val weight: Int,
    @Expose val magicFind: Boolean,
)
