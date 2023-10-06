package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.utils.LorenzRarity

data class SeaCreature(
    val displayName: String,
    val fishingExperience: Int,
    val chatColor: String,
    val rare: Boolean,
    val rarity: LorenzRarity,
) {

    override fun toString(): String {
        return chatColor + rare() + displayName
    }

    private fun rare(): String {
        return if (rare) "§l" else ""
    }
}

