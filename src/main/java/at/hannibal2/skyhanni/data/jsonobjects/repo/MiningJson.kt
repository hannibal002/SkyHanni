package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class MiningJson(
    @Expose val blockStrengths: Map<String, Int>,
)
