package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import kotlin.time.Duration.Companion.minutes

data class CachedItemData(
    // -1 = not loaded
    var petCandies: Int? = -1,

    // NONE = not loaded
    var heldItem: NeuInternalName? = NeuInternalName.NONE,

    // -1 = not loaded
    var sackInASack: Int? = -1,

    // null = not loaded
    var riftTransferable: Boolean? = null,

    // null = not loaded
    var riftExportable: Boolean? = null,

    var itemRarityLastCheck: SimpleTimeMark = SimpleTimeMark.farPast(),

    // null = not loaded
    var itemRarity: LorenzRarity? = null,

    var itemCategory: ItemCategory? = null,

    var lastInternalName: NeuInternalName? = null,

    var lastInternalNameFetchTime: SimpleTimeMark = SimpleTimeMark.farPast(),

    var lastLore: List<String> = listOf(),

    var lastLoreFetchTime: SimpleTimeMark = SimpleTimeMark.farPast(),

    var lastExtraAttributes: NBTTagCompound? = null,

    var lastExtraAttributesFetchTime: SimpleTimeMark = SimpleTimeMark.farPast(),
) {
    companion object {
        private val cache = TimeLimitedCache<IdentityCharacteristics<Item>, CachedItemData>(expireAfterWrite = 5.minutes)
        val ItemStack.cachedData: CachedItemData get() = cache.getOrPut(IdentityCharacteristics(item)) { CachedItemData() }
    }
}
