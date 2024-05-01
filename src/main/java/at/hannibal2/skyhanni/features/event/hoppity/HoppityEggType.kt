package at.hannibal2.skyhanni.features.event.hoppity

import io.github.moulberry.notenoughupdates.util.SkyBlockTime

enum class HoppityEggType(
    val mealName: String,
    val resetsAt: Int,
    private val mealColour: String,
    var lastResetDay: Int = -1,
    private var claimed: Boolean = false,
) {
    BREAKFAST("Breakfast", 7, "§a"),
    LUNCH("Lunch", 14, "§9"),
    DINNER("Dinner", 21, "§6"),
    ;

    fun markClaimed() {
        claimed = true
    }

    fun markSpawned() {
        claimed = false
    }

    fun isClaimed() = claimed
    val formattedName by lazy { "$mealColour$mealName" }

    companion object {
        fun allFound() = entries.forEach { it.markClaimed() }

        fun getMealByName(mealName: String) = entries.find { it.mealName == mealName }

        fun checkClaimed() {
            val currentSbTime = SkyBlockTime.now()
            val currentSbDay = currentSbTime.day
            val currentSbHour = currentSbTime.hour

            for (eggType in entries) {
                if (currentSbHour < eggType.resetsAt || eggType.lastResetDay == currentSbDay) continue
                eggType.markSpawned()
                eggType.lastResetDay = currentSbDay
                if (HoppityEggLocator.currentEggType == eggType) {
                    HoppityEggLocator.currentEggType = null
                    HoppityEggLocator.sharedEggLocation = null
                }
            }
        }

        fun eggsRemaining(): Boolean {
            return entries.any { !it.claimed }
        }
    }
}
