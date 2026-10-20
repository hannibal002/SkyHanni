package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetInfo
import java.util.UUID

object PetStorageExpShare {

    private val petStorage get() = ProfileStorageData.petProfiles
    private val expShareSlots = listOf(30, 31, 32)
    private const val EXP_SHARING_INVENTORY_NAME = "Exp Sharing"

    fun readInventory(inventoryName: String, inventoryItems: Map<Int, SafeItemStack>) {
        if (!isInventory(inventoryName)) return
        val petStorage = petStorage ?: return
        petStorage.expSharePets.clear()
        petStorage.expSharePets.addAll(
            expShareSlots.map { expShareSlot ->
                val slotItem = inventoryItems[expShareSlot]?.takeIf {
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

    fun isSlotDisabled(slot: Int): Boolean =
        slot in expShareSlots.drop(activeSlotCount())

    fun isInventory(inventoryName: String?): Boolean =
        inventoryName == EXP_SHARING_INVENTORY_NAME

    private fun activeSlotCount(): Int =
        if (Perk.SHARING_IS_CARING.isActive) expShareSlots.size else 1
}
