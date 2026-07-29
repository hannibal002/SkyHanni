package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.world.entity.Display
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

@SkyHanniModule
object ClearTreeLogs {

    private val treeBlocks = buildList<BlockState> {
        add(Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState())
        add(Blocks.MANGROVE_WOOD.defaultBlockState())
        add(Blocks.MANGROVE_LEAVES.defaultBlockState())
        add(Blocks.AZALEA_LEAVES.defaultBlockState())
        add(Blocks.STRIPPED_BIRCH_WOOD.defaultBlockState())
        add(Blocks.STRIPPED_MANGROVE_WOOD.defaultBlockState())
        add(Blocks.OAK_LEAVES.defaultBlockState())
    }

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    fun onRender(event: CheckRenderEntityEvent<Display.BlockDisplay>) {
        if (!SkyHanniMod.feature.foraging.trees.cleanView) return
        val block = event.entity.blockState
        if (block in treeBlocks) event.cancel()
    }
}
