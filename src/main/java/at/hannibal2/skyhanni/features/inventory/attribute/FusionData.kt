package at.hannibal2.skyhanni.features.inventory.attribute

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object FusionData {

    private const val FIRST_SHARD_SLOT = 12
    private const val SECOND_SHARD_SLOT = 14
    private const val OUTPUT_SHARD_SLOT = 31

    var currentFusionData: CurrentFusionData? = null

    fun updateFusionData() {
        val firstShard = processShard(InventoryUtils.getItemAtSlotIndex(FIRST_SHARD_SLOT))
        val secondShard = processShard(InventoryUtils.getItemAtSlotIndex(SECOND_SHARD_SLOT))
        val outputShard = InventoryUtils.getItemAtSlotIndex(OUTPUT_SHARD_SLOT)?.getInternalNameOrNull()
        if (firstShard == null || secondShard == null || outputShard == null) return
        currentFusionData = CurrentFusionData(firstShard, secondShard, outputShard)
    }

    private fun processShard(stack: ItemStack?): FusionShard? {
        val internalName = stack?.getInternalNameOrNull() ?: return null
        val amount = AttributeShardsData.requiredToFusePattern.firstMatcher(stack.getLore()) {
            group("amount").toInt()
        } ?: return null
        return FusionShard(internalName, amount)
    }

    @HandleEvent
    fun onWorldChange() {
        currentFusionData = null
    }

}

data class FusionShard(val internalName: NeuInternalName, val amount: Int)

data class CurrentFusionData(
    val firstShard: FusionShard,
    val secondShard: FusionShard,
    val outputShard: NeuInternalName,
)
