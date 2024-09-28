package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class PetsJson(
    @Expose val xpLeveling: List<Int>,
    @Expose val xpLevelingGoldenDragon: List<Int>,
)
