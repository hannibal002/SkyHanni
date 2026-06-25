package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.PetInfo
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetInfo
import java.util.UUID

object PetStorageExpShare {

    private val petStorage get() = ProfileStorageData.petProfiles
    private val expShareSlots = listOf(30, 31, 32)
    private const val EXP_SHARING_INVENTORY_NAME = "Exp Sharing"

    private val PetInfo.ownedUuid: UUID? get() = uniqueId ?: uuid

    fun readInventory(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != EXP_SHARING_INVENTORY_NAME) return
        val petStorage = petStorage ?: return
        petStorage.expSharePets.clear()
        petStorage.expSharePets.addAll(
            expShareSlots.map { expShareSlot ->
                val slotItem = event.inventoryItems[expShareSlot]?.takeIf {
                    it.hoverName.string != "No pet in slot"
                } ?: return@map null
                slotItem.getPetInfo()?.ownedUuid
            },
        )
    }

    fun getActivePets(): List<PetData> = petStorage?.let { petStorage ->
        petStorage.expSharePets.take(activeSlotCount()).mapNotNull { uuid ->
            uuid?.let { petUuid -> petStorage.pets.firstOrNull { it.uuid == petUuid } }
        }
    }.orEmpty()

    fun getActivePetUuids(): Set<UUID> =
        petStorage?.expSharePets?.take(activeSlotCount())?.filterNotNull()?.toSet().orEmpty()

    fun getDisabledPetUuids(): Set<UUID> =
        petStorage?.expSharePets?.drop(activeSlotCount())?.filterNotNull()?.toSet().orEmpty()

    fun isSlotDisabled(slot: Int) =
        slot in expShareSlots.drop(activeSlotCount())

    fun isInventory(inventoryName: String?) =
        inventoryName == EXP_SHARING_INVENTORY_NAME

    private fun activeSlotCount() =
        if (Perk.SHARING_IS_CARING.isActive) expShareSlots.size else 1
}
