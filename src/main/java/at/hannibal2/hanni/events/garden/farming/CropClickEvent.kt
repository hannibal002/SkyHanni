package at.hannibal2.hanni.events.garden.farming

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.ClickType
import at.hannibal2.hanni.features.garden.CropType
import at.hannibal2.hanni.utils.LorenzVec
import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack

/**
 * When the player clicks on a block that is linked to a CropType while in the garden.
 */
class CropClickEvent(
    val position: LorenzVec,
    val crop: CropType,
    val blockState: IBlockState,
    val clickType: ClickType,
    val itemInHand: ItemStack?,
) : HanniEvent()
