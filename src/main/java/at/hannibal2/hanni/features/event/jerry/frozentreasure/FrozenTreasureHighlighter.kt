package at.hannibal2.hanni.features.event.jerry.frozentreasure

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.WinterApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.ItemUtils.isSkull
import at.hannibal2.hanni.utils.blockhighlight.HanniBlockHighlighter
import at.hannibal2.hanni.utils.blockhighlight.TimedHighlightBlock
import at.hannibal2.hanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.hanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.hanni.utils.compat.getInventoryItems
import at.hannibal2.hanni.utils.compat.getStandHelmet
import at.hannibal2.hanni.utils.system.PlatformUtils
import at.hannibal2.hanni.utils.toLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks

@HanniModule
object FrozenTreasureHighlighter {

    private val config get() = HanniMod.feature.event.winter.frozenTreasureHighlighter

    private val blockHighlighter = HanniBlockHighlighter<TimedHighlightBlock>(
        highlightCondition = { isEnabled() },
        blockCondition = { it.block == Blocks.ice || it.block == Blocks.packed_ice },
        colorProvider = { config.treasureColor },
    )

    private fun isEnabled(): Boolean {
        return IslandType.WINTER.isCurrent() && WinterApi.inGlacialCave() && config.enabled
    }

    // Why does modern versions make this not the same :(
    private val yOffset = if (PlatformUtils.IS_LEGACY) 1 else 2

    @HandleEvent(onlyOnIsland = IslandType.WINTER)
    fun onTick() {
        if (!isEnabled()) return

        for (armorStand in EntityUtils.getEntitiesNextToPlayer<EntityArmorStand>(50.0)) {
            if (armorStand.getInventoryItems().count { it.isNotEmpty() } != 1) continue

            val standHelmet = armorStand.getStandHelmet().orNull() ?: continue
            if (standHelmet.isSkull() && standHelmet.displayName.endsWith("Head")) continue

            val treasureLocation = armorStand.position.toLorenzVec().up(yOffset)
            blockHighlighter.addBlock(TimedHighlightBlock(treasureLocation))
        }
    }
}
