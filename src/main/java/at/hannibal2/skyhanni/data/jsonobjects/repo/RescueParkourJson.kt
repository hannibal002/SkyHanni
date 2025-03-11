package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.gson.annotations.Expose

class RescueParkourJson(
    @Expose val mage: Map<String, List<LorenzVec>>,
    @Expose val barb: Map<String, List<LorenzVec>>,
)
