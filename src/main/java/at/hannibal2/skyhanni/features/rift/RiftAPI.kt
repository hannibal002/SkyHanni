package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.RiftConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName_new
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import net.minecraft.item.ItemStack

object RiftAPI {
    fun inRift() = LorenzUtils.inIsland(IslandType.THE_RIFT)

    val config: RiftConfig get() = SkyHanniMod.feature.rift

    // internal name -> motes
    var motesPrice = emptyMap<NEUInternalName, Double>()

    fun ItemStack.motesNpcPrice(): Double? {
        val baseMotes = motesPrice[getInternalName_new()] ?: return null
        val burgerStacks = config.motes.burgerStacks
        val pricePer = baseMotes + (burgerStacks * 5) * baseMotes / 100
        return pricePer * stackSize
    }

    fun inLivingCave() = LorenzUtils.skyBlockArea == "Living Cave"
    fun inLivingStillness() = LorenzUtils.skyBlockArea == "Living Stillness"
    fun inStillgoreChateau() = LorenzUtils.skyBlockArea == "Stillgore Château" || LorenzUtils.skyBlockArea == "Oubliette"
    fun inDreadfarm() = LorenzUtils.skyBlockArea == "Dreadfarm"
}