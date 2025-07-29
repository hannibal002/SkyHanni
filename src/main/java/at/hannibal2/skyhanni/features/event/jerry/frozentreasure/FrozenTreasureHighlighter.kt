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
import at.hannibal2.skyhanni.utils.compat.getInventoryItems
import at.hannibal2.skyhanni.utils.compat.getStandHelmet
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks

@SkyHanniModule
object FrozenTreasureHighlighter {

    private val config get() = SkyHanniMod.feature.event.winter.frozenTreasureHighlighter

    private val blockHighlighter = SkyHanniBlockHighlighter<TimedHighlightBlock>(
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
