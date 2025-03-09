package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.MiningApi
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MetalDetectorAllToolsAlert {
    private val METAL_DETECTOR = "DWARVEN_METAL_DETECTOR".toInternalName()
    private val DWARVEN_LAPIS_SWORD = "DWARVEN_LAPIS_SWORD".toInternalName()
    private val DWARVEN_EMERALD_HAMMER = "DWARVEN_EMERALD_HAMMER".toInternalName()
    private val DWARVEN_GOLD_HAMMER = "DWARVEN_GOLD_HAMMER".toInternalName()
    private val DWARVEN_DIAMOND_AXE = "DWARVEN_DIAMOND_AXE".toInternalName()

    private val config get() = SkyHanniMod.feature.mining.metalDetector.metalDetectorAllToolsAlert


    @HandleEvent(onlyOnSkyblock = true)
    fun onActionBarUpdate(event: ActionBarUpdateEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.itemInHandId != METAL_DETECTOR) return

        var hasLapis = false
        var hasDiamond = false
        var hasEmerald = false
        var hasGold = false
        InventoryUtils.getItemsInOwnInventory().forEach {
            val internalName = it.getInternalName()
            if (internalName == DWARVEN_LAPIS_SWORD) hasLapis = true
            if (internalName == DWARVEN_DIAMOND_AXE) hasDiamond  = true
            if (internalName == DWARVEN_EMERALD_HAMMER) hasEmerald = true
            if (internalName == DWARVEN_GOLD_HAMMER) hasGold = true
        }
        if (hasLapis && hasDiamond && hasEmerald && hasGold) {
            LorenzUtils.sendTitle("§cALL TOOLS", 1.seconds)
        }
    }

    fun isEnabled() = MiningApi.inCustomMiningIsland() && config
}
