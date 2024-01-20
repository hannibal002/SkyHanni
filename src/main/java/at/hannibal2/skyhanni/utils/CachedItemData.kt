package at.hannibal2.skyhanni.utils

data class CachedItemData(
    // -1 = not loaded
    var petCandies: Int? = -1,

    // "" = not loaded
    var heldItem: String? = "",

    // -1 = not loaded
    var sackInASack: Int? = -1,

    // null = not loaded
    var riftTransferable: Boolean? = null,

    // null = not loaded
    var riftExportable: Boolean? = null,

    // null = not loaded
    var itemRarityLastCheck: Long = 0L, // Cant use SimpleTimeMark here

    // null = not loaded
    var itemRarity: LorenzRarity? = null,

    var itemCategory: ItemCategory? = null,
)
