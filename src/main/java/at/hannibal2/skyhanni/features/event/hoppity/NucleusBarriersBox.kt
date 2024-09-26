package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

@SkyHanniModule
object NucleusBarriersBox {
    private val config = SkyHanniMod.feature.event.hoppityEggs

    private enum class Crystal(val color: Color) {
        AMBER(LorenzColor.GOLD.addOpacity(60)),
        AMETHYST(LorenzColor.DARK_PURPLE.addOpacity(60)),
        TOPAZ(LorenzColor.YELLOW.addOpacity(60)),
        JADE(LorenzColor.GREEN.addOpacity(60)),
        SAPPHIRE(LorenzColor.BLUE.addOpacity(60)),
    }

    private val crystalCoordinatePairs: MutableMap<Crystal, AxisAlignedBB> = mapOf(
        Crystal.AMBER to AxisAlignedBB(
            BlockPos(474.0, 124.0, 524.0),
            BlockPos(485.0, 111.0, 535.0),
        ).expandBlock(),
        Crystal.AMETHYST to AxisAlignedBB(
            BlockPos(474.0, 124.0, 492.0),
            BlockPos(485.0, 111.0, 503.0),
        ).expandBlock(),
        Crystal.TOPAZ to AxisAlignedBB(
            BlockPos(508.0, 124.0, 473.0),
            BlockPos(519.0, 111.0, 484.0),
        ).expandBlock(),
        Crystal.JADE to AxisAlignedBB(
            BlockPos(542.0, 124.0, 492.0),
            BlockPos(553.0, 111.0, 503.0),
        ).expandBlock(),
        Crystal.SAPPHIRE to AxisAlignedBB(
            BlockPos(542.0, 124.0, 524.0),
            BlockPos(553.0, 111.0, 535.0),
        ).expandBlock(),
    ).toMutableMap()

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        crystalCoordinatePairs.forEach { (crystal, axis) ->
            RenderUtils.drawFilledBoundingBox_nea(
                axis,
                crystal.color,
                partialTicks = event.partialTicks,
                renderRelativeToCamera = false
            )
        }
    }

    private fun isEnabled() = config.drawBarrierBoxes &&
        HoppityAPI.isHoppityEvent() &&
        IslandType.CRYSTAL_HOLLOWS.isInIsland()
}
