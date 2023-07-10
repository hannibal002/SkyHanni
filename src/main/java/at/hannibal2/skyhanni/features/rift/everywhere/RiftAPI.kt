package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.RiftConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.item.ItemStack

object RiftAPI {
    fun inRift() = LorenzUtils.inIsland(IslandType.THE_RIFT)

    val config: RiftConfig get() = SkyHanniMod.feature.rift

    // internal name -> motes
    var motesPrice = emptyMap<String, Double>()

    fun ItemStack.motesNpcPrice() = motesPrice[getInternalName()]
}