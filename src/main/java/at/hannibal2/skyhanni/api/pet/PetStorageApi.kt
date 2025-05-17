package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetInfo
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.indexOfFirstOrNull
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import java.util.UUID

@SkyHanniModule
object PetStorageApi {

    private val patternGroup = RepoPattern.group("misc.pet.storage")

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: Pets
     * REGEX-TEST: Pets (1/3)
     */
    private val mainPetMenuNamePattern by patternGroup.pattern(
        "guiname.main",
        "Pets(?: \\((?<currentpage>\\d+)\\/(?<maxpage>\\d+)\\))? ?"
    )
    // </editor-fold>

    private fun Int.isPetStackLocation() = this > 9 && this < 44 &&
        this % 9 != 0 && (this + 1) % 9 != 0

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!mainPetMenuNamePattern.matches(event.inventoryName)) return
        val profilePets = ProfileStorageData.petProfiles?.pets ?: return

        val petItems = event.inventoryItems.filter { (slotNumber, stack) ->
            slotNumber.isPetStackLocation() && stack.getInternalNameOrNull() != null
        }

        ChatUtils.chat("Pet items count: ${petItems.size}")

        val qualifiedPets = petItems.mapNotNull { (_, item) ->
            val petInfo = item.getPetInfo() ?: return@mapNotNull null
            PetData(
                petInternalName = item.getInternalName(),
                skinInternalName = petInfo.skin,
                heldItemInternalName = petInfo.heldItem,
                exp = petInfo.exp,
                uuid = petInfo.uuid,
            )
        }

        ChatUtils.chat("Qualified pets count: ${qualifiedPets.size}")

        qualifiedPets.forEach { data ->
            val existingDataIndex = profilePets.indexOfFirstOrNull { it.uuid == data.uuid }
            if (existingDataIndex != null) {
                profilePets[existingDataIndex] = data
            } else profilePets.add(data)
        }

        SkyHanniMod.configManager.saveConfig(ConfigFileType.PETS, "saving-data")
        ChatUtils.chat("Saved data")
    }

}
