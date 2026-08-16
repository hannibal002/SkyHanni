package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DisplayEntityUtils.isInitialized
import at.hannibal2.skyhanni.utils.DisplayEntityUtils.isRotated
import at.hannibal2.skyhanni.utils.DisplayEntityUtils.transformation
import at.hannibal2.skyhanni.utils.DisplayEntityUtils.uniformScale
import com.google.gson.JsonObject
import com.mojang.math.Transformation
import net.minecraft.world.entity.Display
import net.minecraft.world.level.block.Blocks

@SkyHanniModule
object ClearTreeLogs {
    private val config get() = SkyHanniMod.feature.foraging.trees.cleanView
    private val SCALE_RANGE = 0.4f..1.0f

    private val treeBlocks = buildList {
        add(Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState())
        add(Blocks.MANGROVE_WOOD.defaultBlockState())
        add(Blocks.MANGROVE_LEAVES.defaultBlockState())
        add(Blocks.AZALEA_LEAVES.defaultBlockState())
        add(Blocks.STRIPPED_BIRCH_WOOD.defaultBlockState())
        add(Blocks.STRIPPED_MANGROVE_WOOD.defaultBlockState())
        add(Blocks.OAK_LEAVES.defaultBlockState())
    }

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    private fun onRender(event: CheckRenderEntityEvent<Display.BlockDisplay>) {
        if (!isEnabled()) return
        val entity = event.entity
        if (entity.blockState !in treeBlocks) return

        if ((config.hideTreeBlocks == config.hideRuneEffects) && config.hideTreeBlocks) {
            event.cancel()
            return
        }

        val transformation = entity.transformation ?: return
        val floatingTree = isFloatingTreeBlock(transformation)
        if ((config.hideTreeBlocks && floatingTree) ||
            (config.hideRuneEffects && !floatingTree)
        ) {
            event.cancel()
        }
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(144, "foraging.trees.cleanView") { element ->
            val obj = JsonObject()
            obj.addProperty("enabled", element.asBoolean)
            obj
        }
    }

    private fun isFloatingTreeBlock(transformation: Transformation): Boolean {
        if (transformation.isRotated) return false
        val scale = transformation.uniformScale ?: return false
        return scale in SCALE_RANGE
    }

    private fun isEnabled() = config.enabled
}
