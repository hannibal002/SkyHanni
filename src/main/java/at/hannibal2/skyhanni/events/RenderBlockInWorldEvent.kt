package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraft.world.IBlockAccess

data class RenderBlockInWorldEvent(@JvmField var state: IBlockState?, var world: IBlockAccess, var pos: BlockPos?) : CancellableSkyHanniEvent()
