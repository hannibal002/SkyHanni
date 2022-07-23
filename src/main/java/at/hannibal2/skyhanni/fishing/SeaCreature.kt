package at.hannibal2.skyhanni.fishing

data class SeaCreature(
    val displayName: String,
    val fishingExperience: Int,
    val chatColor: String,
    val rare: Boolean,
) {

    override fun toString(): String {
        return  chatColor + rare() + displayName
    }

    private fun rare(): String {
        return if (rare) "§l" else ""
    }
}

