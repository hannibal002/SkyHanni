package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetInfo
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.firstUniqueByOrNull
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.indexOfFirstOrNull
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.math.abs

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

        petItems.mapNotNull { (_, item) ->
            val petInfo = item.getPetInfo() ?: return@mapNotNull null
            PetData(
                petInternalName = item.getInternalName(),
                skinInternalName = petInfo.skin,
                heldItemInternalName = petInfo.heldItem,
                exp = petInfo.exp,
                uuid = petInfo.uuid,
            )
        }.forEach { data ->
            // Because this inventory is the "source of truth", if we come across the same UUID
            // we should always replace the data in-place
            profilePets.indexOfFirstOrNull { it.uuid == data.uuid }?.let {
                profilePets[it] = data
            } ?: profilePets.add(data)
        }

        // Strip away any pet data that don't have a UUID associated
        profilePets.removeIf { it.uuid == null }

        SkyHanniMod.configManager.saveConfig(ConfigFileType.PETS, "saving-data")
    }

    private fun resolvePetOrInsert(
        petName: String,
        rarity: LorenzRarity? = null,
        heldItem: NeuInternalName? = null,
        skinTag: String? = null,
        level: Int? = null,
        exp: Double? = null,
        // How far off the exp can be before it's excluded from resolution - 1% default
        expErrorFactor: Double = 0.01,
    ): PetData {
        val petData = resolvePetDataOrNull(
            uncoloredPetName = petName.removeColor(),
            rarity = rarity,
            skinTag = skinTag,
            level = level,
            exp = exp,
            expErrorFactor = expErrorFactor,
        )
        if (petData != null) return petData

        val profilePets = ProfileStorageData.petProfiles?.pets
            ?: ErrorManager.skyHanniError("Profile data not loaded in PetStorageApi")

        val petInternalName = if (rarity != null) {
            PetUtils.petNameAndRarityToInternalName(petName.removeColor(), rarity)
        } else PetUtils.petNameToInternalName(petName)

        if (petInternalName == null) {
            ErrorManager.skyHanniError("Internal name could not be found for $petName")
        }

        val petSkinInternalName: NeuInternalName? = skinTag?.let {
            val petBasicName = petInternalName.asString().replace(Regex(";\\d+"), "")
            val skins = PetUtils.petSkins[petBasicName]
            if (skins == null || skins.isEmpty()) return@let null

            val filteredSkins = skins.filter {
                it.displayName.substring(0, 2) + "✦" == skinTag
            }

            return@let if (filteredSkins.size != 1) null
            else filteredSkins.first().internalName
        }

        val newPetData = PetData(
            petInternalName = petInternalName,
            skinInternalName = petSkinInternalName,
            heldItemInternalName = heldItem,
            exp = exp,
        )
        profilePets.add(newPetData)
        return newPetData
    }

    private fun resolvePetDataOrNull(
        uncoloredPetName: String,
        rarity: LorenzRarity? = null,
        heldItem: NeuInternalName? = null,
        skinTag: String? = null,
        level: Int? = null,
        exp: Double? = null,
        expErrorFactor: Double = 0.01,
    ): PetData? = ProfileStorageData.petProfiles?.pets?.filter {
        it.uuid != null
    }?.takeIfNotEmpty()?.firstUniqueByOrNull(
        { it.cleanName == uncoloredPetName },
        { rarity == null || it.rarity == rarity },
        { heldItem == null || it.heldItemInternalName == heldItem },
        { skinTag == null || it.skinTag == skinTag },
        { level == null || it.level == level },
        { exp == null || abs((it.exp ?: 0.0) - exp) < exp * expErrorFactor }
    )
}
