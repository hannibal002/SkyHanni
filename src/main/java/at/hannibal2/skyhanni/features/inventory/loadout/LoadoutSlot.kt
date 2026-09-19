package at.hannibal2.skyhanni.features.inventory.loadout

class LoadoutSlot(
    val id: Int,
    val page: Int,
    val inventorySlot: Int,
) {
    fun getData() = LoadoutApi.storage?.data?.getOrPut(id) {
        LoadoutApi.LoadoutData(
            id,
            name = null,
            armor = LoadoutApi.emptyArmor(),
            equipment = LoadoutApi.emptyEquipment(),
            pet = null,
            powerstone = null,
            tunings = null,
            hotm = null,
            hotf = null,
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

    val name: String? get() = getData()?.name

    fun isEmpty(): Boolean = getData()?.let { data ->
        data.armor.all { it == null } &&
            data.equipment.all { it == null } &&
            data.pet == null &&
            data.powerstone == null &&
            data.tunings == null &&
            data.hotm == null &&
            data.hotf == null
    } ?: true

    fun isCurrentSlot() = getData()?.id == LoadoutApi.currentSlot

    fun isInCurrentPage() = (LoadoutApi.currentPage == null && page == 1) || (page == LoadoutApi.currentPage)
}
