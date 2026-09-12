package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftDetection
import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.gson.annotations.Expose

data class MineshaftCorpsesJson(
    @Expose val locations: Map<MineshaftDetection.MineshaftType, List<LorenzVec>>,
)
