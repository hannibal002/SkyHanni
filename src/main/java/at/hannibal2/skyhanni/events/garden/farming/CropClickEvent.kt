package at.hannibal2.skyhanni.events.garden.farming

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

/**
 * When the player clicks on a block that is linked to a [CropType] while in the Garden.
 */
@PrimaryFunction("onCropClick")
class CropClickEvent(
    val position: Vec3,
    val crop: CropType,
    val blockState: BlockState,
    val clickType: ClickType,
    val itemInHand: ItemStack?,
) : SkyHanniEvent()
