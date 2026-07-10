package at.hannibal2.skyhanni.features.inventory.wardrobe

class WardrobeSlot(
    val id: Int,
    val page: Int,
    val inventorySlot: Int,
    val item1Slot: Int,
    val item2Slot: Int,
    val item3Slot: Int,
    val item4Slot: Int,
    val type: WardrobeType
) {
    // Armor aliases
    val helmetSlot get() = item1Slot
    val chestplateSlot get() = item2Slot
    val leggingsSlot get() = item3Slot
    val bootsSlot get() = item4Slot

    // Equipment aliases
    val necklaceSlot get() = item1Slot
    val cloakSlot get() = item2Slot
    val beltSlot get() = item3Slot
    val glovesSlot get() = item4Slot

    fun getData(): WardrobeApi.WardrobeData? {
        val storage = when (type) {
            EQUIPMENT -> WardrobeApi.storage?.equipmentData
            ARMOR -> WardrobeApi.storage?.data
        }
        return storage?.getOrPut(id) {
            WardrobeApi.WardrobeData(
                id,
                armor = WardrobeApi.emptyItems(),
                locked = true,
                favorite = false,
            )
        }
    }

    var locked: Boolean
        get() = getData()?.locked ?: true
        set(value) {
            getData()?.locked = value
        }

    var favorite: Boolean
        get() = getData()?.favorite ?: false
        set(value) {
            getData()?.favorite = value
        }

    val items get() = getData()?.armor ?: WardrobeApi.emptyItems()
    val armor get() = items // alias
    val equipment get() = items // alias

    val inventorySlots = listOf(item1Slot, item2Slot, item3Slot, item4Slot)

    fun isEmpty(): Boolean = items.all { it == null }

    fun isCurrentSlot() = getData()?.id == WardrobeApi.getCurrentWardrobeSlot(type)

    fun isInCurrentPage() = (WardrobeApi.currentPage == null && page == 1) || (page == WardrobeApi.currentPage)
}

enum class WardrobeType(val displayName: String, val slotNames: List<String>) {
    ARMOR("Armor", listOf("Helmet", "Chestplate", "Leggings", "Boots")),
    EQUIPMENT("Equipment", listOf("Necklace", "Cloak", "Belt", "Gloves")),
    ;
}
