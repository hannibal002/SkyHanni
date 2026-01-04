package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class LividSolverJson(
    @Expose val livids: Map<String, LividData>,
)
data class LividData(
    @Expose val skin: String,
    @Expose val name: String,
)
