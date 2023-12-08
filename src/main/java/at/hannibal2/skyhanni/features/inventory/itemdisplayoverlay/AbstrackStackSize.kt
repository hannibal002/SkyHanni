package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay

import at.hannibal2.skyhanni.SkyHanniMod
import net.minecraft.item.ItemStack

abstract class AbstractStackSize {

    val configItemStackSize get() = SkyHanniMod.feature.inventory

    abstract fun getStackTip(item: ItemStack): String
}
