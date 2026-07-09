package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class AreaTypeJson(
    @Expose val areas: Map<String, AreaJson>,
)

data class AreaJson(
    @Expose val name: String,
)
