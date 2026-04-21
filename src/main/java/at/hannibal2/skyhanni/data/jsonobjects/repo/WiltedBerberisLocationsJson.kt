package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.gson.annotations.Expose

data class WiltedBerberisLocationsJson(
    @Expose val fieldCenters: List<FieldCenter>,
)

data class FieldCenter(
    @Expose val position: LorenzVec,
    @Expose val count: Int,
)
