package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object PetAPI {
    private val petMenuPattern by RepoPattern.pattern(
        "misc.pet.menu.title",
        "Pets(?: \\(\\d+/\\d+\\) )?"
    )

    fun isPetMenu(inventoryTitle: String): Boolean = petMenuPattern.matches(inventoryTitle)

    // Contains color code + name and for older SkyHanni users maybe also the pet level
    var currentPet: String?
        get() = ProfileStorageData.profileSpecific?.currentPet
        set(value) {
            ProfileStorageData.profileSpecific?.currentPet = value
        }

    fun isCurrentPet(petName: String): Boolean = currentPet?.contains(petName) ?: false
}
