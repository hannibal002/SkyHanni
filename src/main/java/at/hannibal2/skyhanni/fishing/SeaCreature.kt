package at.hannibal2.skyhanni.fishing

data class SeaCreature(
    val displayName: String,
    val fishingExperience: Int,
    val chatColor: String,
    val special: Boolean,
) {

    override fun toString(): String {
        return chatColor + displayName
    }
}

