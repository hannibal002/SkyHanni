package at.hannibal2.skyhanni.data.jsonobjects.repo.neu

import com.google.gson.annotations.Expose

data class NeuRNGScore(
    @Expose
    val catacombs: Map<String, Map<String, Long>>,
    @Expose
    val slayer: Map<String, Map<String, Long>>
)
