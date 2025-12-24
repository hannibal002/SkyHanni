package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class LividSolverJson(
    @Expose val lividSkins: Map<String, String>,
)
