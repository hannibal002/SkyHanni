package at.hannibal2.hanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class DungeonHubRacesJson(
    @Expose val data: Map<String, Map<String, ParkourJson>>,
)
