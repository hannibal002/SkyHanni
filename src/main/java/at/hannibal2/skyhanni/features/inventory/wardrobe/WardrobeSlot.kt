package at.hannibal2.skyhanni.features.inventory.wardrobe

class WardrobeSlot(
    private val api: WardrobeApi,
    val id: Int,
    val page: Int,
    val inventorySlot: Int,
    val helmetSlot: Int,
    val chestplateSlot: Int,
    val leggingsSlot: Int,
    val bootsSlot: Int,
) {
    fun getData() = api.storage?.data?.getOrPut(id) {
        WardrobeApi.WardrobeData(
            id,
            armor = WardrobeApi.emptyArmor(),
            locked = true,
            favorite = false,
        )
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

    val armor get() = getData()?.armor ?: WardrobeApi.emptyArmor()

    val inventorySlots = listOf(helmetSlot, chestplateSlot, leggingsSlot, bootsSlot)

    fun isEmpty(): Boolean = armor.all { it == null }

    fun isCurrentSlot() = getData()?.id == api.currentSlot

    fun isInCurrentPage() = (api.currentPage == null && page == 1) || (page == api.currentPage)
}
