package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LorenzUtils

object RiftAPI {
    fun inRift() = LorenzUtils.inIsland(IslandType.THE_RIFT)
}