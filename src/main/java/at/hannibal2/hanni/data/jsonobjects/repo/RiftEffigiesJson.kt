package at.hannibal2.hanni.data.jsonobjects.repo

import at.hannibal2.hanni.utils.LorenzVec
import com.google.gson.annotations.Expose

data class RiftEffigiesJson(
    @Expose val locations: List<LorenzVec>,
)
