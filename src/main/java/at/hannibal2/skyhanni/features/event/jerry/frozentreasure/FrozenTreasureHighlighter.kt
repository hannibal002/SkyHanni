package at.hannibal2.skyhanni.features.event.jerry.frozentreasure

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.WinterApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.isSkull
import at.hannibal2.skyhanni.utils.VectorUtils.toVec3
import at.hannibal2.skyhanni.utils.VectorUtils.up
import at.hannibal2.skyhanni.utils.blockhighlight.SkyHanniBlockHighlighter
import at.hannibal2.skyhanni.utils.blockhighlight.TimedHighlightBlock
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.getInventoryItems
import at.hannibal2.skyhanni.utils.compat.getStandHelmet
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks

@SkyHanniModule
object FrozenTreasureHighlighter {

    private const val Y_OFFSET = 2.0

    private val config get() = SkyHanniMod.feature.event.winter.frozenTreasureHighlighter

    private val blockHighlighter = SkyHanniBlockHighlighter<TimedHighlightBlock>(
        highlightCondition = ::isEnabled,
        blockCondition = { it.block == Blocks.ICE || it.block == Blocks.PACKED_ICE },
        colorProvider = { config.treasureColor },
    )

    fun onTick() {
        if (!isEnabled()) return

        for (armorStand in EntityUtils.getEntitiesNearby<ArmorStand>(50.0)) {
            if (armorStand.getInventoryItems().count { it.isNotEmpty() } != 1) continue

            val standHelmet = armorStand.getStandHelmet().orNull() ?: continue
            if (standHelmet.isSkull() && standHelmet.hoverName.string.endsWith("Head")) continue

            val treasureLocation = armorStand.blockPosition().toVec3().up(Y_OFFSET)
            blockHighlighter.addBlock(TimedHighlightBlock(treasureLocation))
        }
    }

    private fun isEnabled() = config.enabled && WinterApi.inGlacialCave()
}
