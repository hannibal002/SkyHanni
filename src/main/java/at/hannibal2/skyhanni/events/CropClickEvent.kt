package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.features.garden.CropType
import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack

class CropClickEvent(
    val crop: CropType,
    val blockState: IBlockState,
    val clickType: ClickType,
    val itemInHand: ItemStack?
) : LorenzEvent()