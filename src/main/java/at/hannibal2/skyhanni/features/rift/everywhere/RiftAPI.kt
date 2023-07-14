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

    fun ItemStack.motesNpcPriceBurgers(): Double {
        val base = motesNpcPrice() ?: 0.0
        return if (config.motes.burgerStacks == 0) {
            base
        } else {
            val burgerStacks = config.motes.burgerStacks
            val motesPerItem = base + (burgerStacks * 5) * base / 100
            if (stackSize > 1) {
                motesPerItem * stackSize
            } else {
                motesPerItem
            }
        }
    }


    fun inLivingCave() = LorenzUtils.skyBlockArea == "Living Cave"
    fun inLivingStillness() = LorenzUtils.skyBlockArea == "Living Stillness"
}