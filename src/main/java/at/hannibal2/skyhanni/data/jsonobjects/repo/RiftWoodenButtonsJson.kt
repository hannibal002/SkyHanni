package at.hannibal2.hanni.data.jsonobjects.repo

import at.hannibal2.hanni.utils.LorenzVec
import com.google.gson.annotations.Expose

data class RiftWoodenButtonsJson(
    @Expose val houses: Map<String, List<ButtonSpots>>
)

data class ButtonSpots(
    @Expose val position: LorenzVec,
    @Expose val buttons: List<LorenzVec>
)
