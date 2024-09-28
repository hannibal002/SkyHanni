package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.JsonObject
import com.google.gson.annotations.Expose

data class NEUPetsJson(
    @Expose val pet_levels: List<Int>,
    @Expose val custom_pet_leveling: JsonObject,
)
