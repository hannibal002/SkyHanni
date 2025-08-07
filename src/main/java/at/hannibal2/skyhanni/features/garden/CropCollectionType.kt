package at.hannibal2.skyhanni.features.garden

enum class CropCollectionType(val displayName: String) {
    BREAKING_CROPS("Breaking Crops"),
    MOOSHROOM_COW("Mooshroom Cow"),
    DICER("Dicer Drops"),
    PEST_BASE("Pest Base Drop"),
    PEST_RNG("Pest RNG Drop"),
    UNKNOWN("Unknown")
    ;

    companion object {
        fun getByName(name: String) = CropCollectionType.entries.firstOrNull { it.displayName == name }
    }
}
