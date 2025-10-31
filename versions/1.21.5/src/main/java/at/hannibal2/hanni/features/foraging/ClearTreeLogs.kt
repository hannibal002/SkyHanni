package at.hannibal2.hanni.features.foraging

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.CheckRenderEntityEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.decoration.DisplayEntity

@HanniModule
object ClearTreeLogs {

    private val treeBlocks = buildList<BlockState> {
        add(Blocks.STRIPPED_SPRUCE_WOOD.defaultState)
        add(Blocks.MANGROVE_WOOD.defaultState)
        add(Blocks.MANGROVE_LEAVES.defaultState)
        add(Blocks.AZALEA_LEAVES.defaultState)

    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onRender(event: CheckRenderEntityEvent<DisplayEntity.BlockDisplayEntity>) {
        if (!HanniMod.feature.foraging.trees.cleanView) return
        val block = event.entity.blockState
        if (block in treeBlocks) event.cancel()
    }
}
