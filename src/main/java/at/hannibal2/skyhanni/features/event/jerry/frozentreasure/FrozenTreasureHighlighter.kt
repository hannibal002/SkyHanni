package at.hannibal2.skyhanni.features.event.jerry.frozentreasure

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.WinterApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.isSkull
import at.hannibal2.skyhanni.utils.blockhighlight.SkyHanniBlockHighlighter
import at.hannibal2.skyhanni.utils.blockhighlight.TimedHighlightBlock
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getEquipmentSlots
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getHelmet
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks

@SkyHanniModule
object FrozenTreasureHighlighter {
    private const val Y_OFFSET = 2

    private val config get() = SkyHanniMod.feature.event.winter.frozenTreasureHighlighter

    private val blockHighlighter = SkyHanniBlockHighlighter<TimedHighlightBlock>(
        highlightCondition = ::isEnabled,
        blockCondition = { it.block.equalsOneOf(Blocks.ICE, Blocks.PACKED_ICE) },
        colorProvider = config::treasureColor,
    )

    @HandleEvent
    private fun onTick() {
        if (!isEnabled()) return

        for (armorStand in EntityUtils.getEntitiesNearby<ArmorStand>(50.0)) {
            if (armorStand.getEquipmentSlots().values.count { it.isNotEmpty() } != 1) continue

            val standHelmet = armorStand.getHelmet().orNull() ?: continue
            if (standHelmet.isSkull() && standHelmet.cleanName.endsWith("Head")) continue

            val treasureLocation = armorStand.blockPosition().toLorenzVec().up(Y_OFFSET)
            blockHighlighter.addBlock(TimedHighlightBlock(treasureLocation))
        }
    }

    private fun isEnabled(): Boolean =
        config.enabled && IslandType.WINTER.isInIsland() && WinterApi.inGlacialCave()
}
