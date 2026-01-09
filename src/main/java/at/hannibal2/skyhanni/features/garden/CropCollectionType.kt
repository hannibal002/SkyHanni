package at.hannibal2.skyhanni.features.garden

enum class CropCollectionType(val displayName: String) {
    BREAKING_CROPS("Breaking Crops"),
    MOOSHROOM_COW("Mooshroom Cow"),
    CROP_FEVER("Crop Fever Drops"),
    PEST_BASE("Pest Base Drop"),
    PEST_RNG("Pest RNG Drop"),
    GREENHOUSE("Greenhouse Drops"),
    UNKNOWN("Unknown")
    ;

    companion object {
        fun getByName(name: String) = CropCollectionType.entries.firstOrNull { it.displayName.lowercase() == name.lowercase() }
    }
}
