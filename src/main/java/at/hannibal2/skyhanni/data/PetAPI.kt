package at.hannibal2.skyhanni.data

object PetAPI {

    var currentPet: String?
        get() = ProfileStorageData.profileSpecific?.currentPet
        set(value) {
            ProfileStorageData.profileSpecific?.currentPet = value
        }
}
