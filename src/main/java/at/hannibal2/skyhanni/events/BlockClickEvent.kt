package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

@PrimaryFunction("onBlockClick")
class BlockClickEvent(clickType: ClickType, val position: Vec3, itemInHand: ItemStack?) :
    WorldClickEvent(itemInHand, clickType) {

    val getBlockState by lazy { position.getBlockStateAt() }
}
