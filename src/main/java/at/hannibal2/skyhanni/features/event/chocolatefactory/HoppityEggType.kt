package at.hannibal2.skyhanni.features.event.chocolatefactory

import io.github.moulberry.notenoughupdates.util.SkyBlockTime

enum class HoppityEggType(
    val mealName: String,
    val resetsAt: Int,
    private val mealColour: String,
    var lastResetDay: Int = -1,
    private var claimed: Boolean = false
) {
    BREAKFAST("Breakfast", 7, "§a"),
    LUNCH("Lunch", 14, "§9"),
    DINNER("Dinner", 21, "§6"),
    ;

    fun markClaimed() {
        claimed = true
    }

    fun isClaimed() = claimed
    fun formattedName() = "$mealColour$mealName"

    companion object {
        fun allFound() = entries.forEach { it.markClaimed() }

        fun getMealByName(mealName: String) = entries.find { it.mealName == mealName }

        fun checkClaimed() {
            val currentSbTime = SkyBlockTime.now()
            val currentSbDay = currentSbTime.day
            val currentSbHour = currentSbTime.hour

            entries.forEach {
                if (currentSbHour >= it.resetsAt && it.lastResetDay != currentSbDay) {
                    it.claimed = false
                    it.lastResetDay = currentSbDay
                    if (HoppityEggsLocations.currentEggType == it) {
                        HoppityEggsLocations.currentEggType = null
                        HoppityEggsLocations.sharedEggLocation = null
                    }
                }
            }
        }

        fun eggsRemaining(): Boolean {
            return entries.any { !it.claimed }
        }
    }
}
