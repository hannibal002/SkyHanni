package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.gson.annotations.Expose

class RiftEffigiesJson(
    @Expose val locations: List<LorenzVec>
)
