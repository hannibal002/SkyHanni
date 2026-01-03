package at.hannibal2.skyhanni.features.event.jerry.frozentreasure

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.WinterApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.isSkull
import at.hannibal2.skyhanni.utils.blockhighlight.SkyHanniBlockHighlighter
import at.hannibal2.skyhanni.utils.blockhighlight.TimedHighlightBlock
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.getInventoryItems
import at.hannibal2.skyhanni.utils.compat.getStandHelmet
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks

@SkyHanniModule
object FrozenTreasureHighlighter {

    private val config get() = SkyHanniMod.feature.event.winter.frozenTreasureHighlighter

    private val blockHighlighter = SkyHanniBlockHighlighter<TimedHighlightBlock>(
        highlightCondition = ::isEnabled,
        blockCondition = { it.block == Blocks.ICE || it.block == Blocks.PACKED_ICE },
        colorProvider = { config.treasureColor },
    )

    private fun isEnabled(): Boolean {
        return IslandType.WINTER.isCurrent() && WinterApi.inGlacialCave() && config.enabled
    }

    private const val yOffset = 2

    @HandleEvent(onlyOnIsland = IslandType.WINTER)
    fun onTick() {
        if (!isEnabled()) return

        for (armorStand in EntityUtils.getEntitiesNextToPlayer<ArmorStand>(50.0)) {
            if (armorStand.getInventoryItems().count { it.isNotEmpty() } != 1) continue

            val standHelmet = armorStand.getStandHelmet().orNull() ?: continue
            if (standHelmet.isSkull() && standHelmet.hoverName.string.endsWith("Head")) continue

            val treasureLocation = armorStand.blockPosition().toLorenzVec().up(yOffset)
            blockHighlighter.addBlock(TimedHighlightBlock(treasureLocation))
        }
    }
}
