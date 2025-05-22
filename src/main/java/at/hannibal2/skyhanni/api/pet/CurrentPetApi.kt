package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CurrentPetApi {
    val patternGroup = RepoPattern.Companion.group("misc.pet")

    var nonUuidPetOverride: PetData? = null
    val currentPet: PetData?
        get() = nonUuidPetOverride ?: ProfileStorageData.profileSpecific?.currentPetUuid?.let { currentUuid ->
            ProfileStorageData.petProfiles?.pets?.firstOrNull { it.uuid == currentUuid }
        }

    fun isCurrentPet(petName: String): Boolean = currentPet?.coloredName?.contains(petName) ?: false

    fun assertFoundCurrentData(petData: PetData) {
        if (petData.uuid == null) {
            nonUuidPetOverride = petData
            return
        }
        nonUuidPetOverride = null
        ProfileStorageData.profileSpecific?.currentPetUuid = petData.uuid
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("CurrentPetApi")
        if (currentPet == null) {
            event.addIrrelevant("no pet equipped")
            return
        }
        event.addIrrelevant {
            add("petName: '${currentPet?.petInternalName ?: ""}'")
            add("petSkin: '${currentPet?.skinInternalName ?: ""}'")
            add("petRarity: '${currentPet?.rarity?.rawName.orEmpty()}'")
            add("petItem: '${currentPet?.heldItemInternalName ?: ""}'")
            add("petLevel: '${currentPet?.level ?: 0}'")
            add("petXP: '${currentPet?.exp ?: 0.0}'")
        }
    }
}
