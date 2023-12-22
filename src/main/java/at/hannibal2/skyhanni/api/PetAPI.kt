package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.data.ProfileStorageData

object PetAPI {

    // Contains color code + name and for older SkyHanni users maybe also the pet level
    var currentPet: String?
        get() = ProfileStorageData.profileSpecific?.currentPet
        set(value) {
            ProfileStorageData.profileSpecific?.currentPet = value
        }

    fun isCurrentPet(petName: String): Boolean = currentPet?.contains(petName) ?: false
}
