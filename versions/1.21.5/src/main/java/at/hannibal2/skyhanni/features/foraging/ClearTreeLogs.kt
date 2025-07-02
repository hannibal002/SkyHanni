package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.decoration.DisplayEntity

@SkyHanniModule
object ClearTreeLogs {

    private val treeBlocks = buildList<BlockState> {
        add(Blocks.STRIPPED_SPRUCE_WOOD.defaultState)
        add(Blocks.MANGROVE_WOOD.defaultState)
        add(Blocks.MANGROVE_LEAVES.defaultState)
        add(Blocks.AZALEA_LEAVES.defaultState)

    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun onRender(event: CheckRenderEntityEvent<DisplayEntity.BlockDisplayEntity>) {
        if (!SkyHanniMod.feature.foraging.trees.cleanView) return
        val block = event.entity.blockState
        if (block in treeBlocks) event.cancel()
    }
}
