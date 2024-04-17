package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class NamedParkourJson(
    @Expose val data: Map<String, ParkourJson>
)
