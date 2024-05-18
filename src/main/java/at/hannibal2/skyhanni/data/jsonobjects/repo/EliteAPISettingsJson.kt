package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class EliteAPISettingsJson(
    @Expose val refreshTime: Int = 10,
    @Expose val worldSwapRefresh: Boolean = true,
)
