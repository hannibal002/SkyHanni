package at.hannibal2.skyhanni.features.event.jerry.frozentreasure

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.WinterApi
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.isSkull
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.getInventoryItems
import at.hannibal2.skyhanni.utils.compat.getStandHelmet
import at.hannibal2.skyhanni.utils.expand
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks

@SkyHanniModule
object FrozenTreasureHighlighter {

    private val config get() = SkyHanniMod.feature.event.winter.frozenTreasureHighlighter

    private val treasureLocations = mutableSetOf<LorenzVec>()

    // Why does modern versions make this not the same :(
    private val yOffset = if (PlatformUtils.IS_LEGACY) 1 else 2

    @HandleEvent(onlyOnIsland = IslandType.WINTER)
    fun onTick() {
        if (!config.enabled) return
        if (!WinterApi.inGlacialCave()) return
        treasureLocations.clear()

        @Suppress("LoopWithTooManyJumpStatements")
        for (armorStand in EntityUtils.getEntitiesNextToPlayer<EntityArmorStand>(50.0)) {
            if (armorStand.getInventoryItems().count { it.isNotEmpty() } != 1) continue

            val standHelmet = armorStand.getStandHelmet().orNull() ?: continue
            if (standHelmet.isSkull() && standHelmet.displayName.endsWith("Head")) continue

            val treasureLocation = armorStand.position.toLorenzVec().up(yOffset)
            if (!treasureLocation.isValidTreasureLocation()) continue
            treasureLocations.add(treasureLocation)
        }
    }

    private fun LorenzVec.isValidTreasureLocation(): Boolean {
        val blockAt = this.getBlockAt()
        return blockAt == Blocks.ice || blockAt == Blocks.packed_ice
    }

    @HandleEvent
    fun onWorldChange() {
        treasureLocations.clear()
    }

    @HandleEvent(onlyOnIsland = IslandType.WINTER)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) return
        if (!WinterApi.inGlacialCave()) return
        if (treasureLocations.isEmpty()) return

        for (location in treasureLocations) {
            val aabb = location.boundingToOffset(1.0, 1.0, 1.0).expand(0.001)
            event.drawFilledBoundingBox(aabb, config.treasureColor.getEffectiveColour(), renderRelativeToCamera = false)
        }
    }
}
