package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CurrentPetApi {
    private val config get() = SkyHanniMod.feature.misc.pets
    val patternGroup = RepoPattern.Companion.group("misc.pet")

    val currentPet: PetData?
        get() = ProfileStorageData.profileSpecific?.currentPetUuid?.let { currentUuid ->
            ProfileStorageData.petProfiles?.pets?.firstOrNull { it.uuid == currentUuid }
        }

    fun isCurrentPet(petName: String): Boolean = currentPet?.coloredName?.contains(petName) ?: false

    // <editor-fold desc="Patterns">
    // </editor-fold>

    // <editor-fold desc="Helpers">
    // </editor-fold>

    // <editor-fold desc="Pet Data Extractors (Widget)">
    // </editor-fold>

    // <editor-fold desc="Pet Data Extractors (AutoPet)">
    // </editor-fold>

    // <editor-fold desc="Pet Data Extractors (Selected Pet)">
    // </editor-fold>

    // <editor-fold desc="Event Handlers">

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("CurrentPetApi")
        if (currentPet == null) {
            event.addIrrelevant("no pet equipped")
            return
        }
        event.addIrrelevant {
            add("petName: '${currentPet?.petInternalName ?: ""}'")
            add("petRarity: '${currentPet?.rarity?.rawName.orEmpty()}'")
            add("petItem: '${currentPet?.heldItemInternalName ?: ""}'")
            add("petLevel: '${currentPet?.level ?: 0}'")
            add("petXP: '${currentPet?.exp ?: 0.0}'")
        }
    }
    // </editor-fold>
}
