package at.hannibal2.hanni.data.jsonobjects.repo

import at.hannibal2.hanni.utils.LorenzVec
import com.google.gson.annotations.Expose

data class ParkourJson(
    @Expose val locations: List<LorenzVec>,
    @Expose val shortCuts: List<ParkourShortCut> = listOf(),
)

data class ParkourShortCut(
    @Expose val from: Int,
    @Expose val to: Int,
)
