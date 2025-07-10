package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi.isAlternateDay
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.collection.CollectionUtils
import java.util.regex.Matcher
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

enum class HoppityEggType(
    val mealName: String,
    val mealColor: String,
    val resetsAt: Int,
    private var claimed: Boolean = false,
    private var lastReset: SkyBlockTime = SkyBlockTime.fromSBYear(0),
    val altDay: Boolean = false,
) {
    BREAKFAST("Breakfast", "§6", 7),
    LUNCH("Lunch", "§9", 14),
    DINNER("Dinner", "§a", 21),
    BRUNCH("Brunch", "§6", 7, altDay = true),
    DEJEUNER("Déjeuner", "§9", 14, altDay = true),
    SUPPER("Supper", "§a", 21, altDay = true),
    SIDE_DISH("Side Dish", "§6§l", -1),
    HITMAN("Hitman", "§c", -1),
    BOUGHT("Bought", "§a", -1),
    BOUGHT_ABIPHONE("✆ Bought", "§a", -1),
    CHOCOLATE_SHOP_MILESTONE("Shop Milestone", "§6§l", -1),
    CHOCOLATE_FACTORY_MILESTONE("Chocolate Milestone", "§6§l", -1),
    STRAY("Stray", "§a", -1)
    ;

    val isResetting get() = resettingEntries.contains(this)
    val formattedName get() = "${if (isClaimed()) "§7§m" else mealColor}$mealName:$mealColor"
    val coloredName get() = "$mealColor$mealName"

    val timeUntil: Duration get() = nextSpawn.timeUntil()
    private val nextSpawn: SimpleTimeMark
        get() = nextSpawnCache[this]?.takeIf { !it.isInPast() }
            ?: calculateNextSpawn()

    private fun spawnsToday(): Boolean {
        val sbTimeNow = SkyBlockTime.now()
        return altDay == sbTimeNow.isAlternateDay()
    }

    fun spawnedToday(): Boolean {
        val sbTimeNow = SkyBlockTime.now()
        return altDay == sbTimeNow.isAlternateDay() && sbTimeNow.hour >= resetsAt
    }

    fun alreadyResetToday(): Boolean {
        val sbTimeNow = SkyBlockTime.now()
        return lastReset.day == sbTimeNow.day && lastReset.month == sbTimeNow.month
    }

    private fun calculateNextSpawn(): SimpleTimeMark {
        if (resetsAt == -1) return SimpleTimeMark.farFuture()
        val sbTimeNow = SkyBlockTime.now()
        val isEggDayToday = spawnsToday()

        val daysToAdd = when {
            isEggDayToday && sbTimeNow.hour < resetsAt -> 0
            isEggDayToday && sbTimeNow.hour >= resetsAt -> 2
            else -> 1
        }

        return sbTimeNow.copy(
            day = sbTimeNow.day + daysToAdd,
            hour = resetsAt,
            minute = 0,
            second = 0,
        ).toTimeMark().also {
            nextSpawnCache[this] = it
        }
    }

    fun markClaimed(mark: SimpleTimeMark? = null) {
        claimed = true
        mark?.let { profileStorage?.mealLastFound?.set(this, it) }
    }

    fun markSpawned(setLastReset: Boolean = false) {
        claimed = false
        if (setLastReset) lastReset = SkyBlockTime.now()
    }

    fun isClaimed() = claimed || hasNotFirstSpawnedYet()

    fun hasRemainingSpawns(): Boolean {
        val hoppityEndMark = HoppityApi.getEventEndMark() ?: return false
        // If it's before the last two days of the event, we can assume there are more spawns
        if (hoppityEndMark.timeUntil() > SkyBlockTime.SKYBLOCK_DAY_MILLIS.milliseconds * 2) return true
        // Otherwise we have to check if the next spawn is after the end of the event
        return timeUntil < hoppityEndMark.timeUntil()
    }

    fun hasNotFirstSpawnedYet(): Boolean {
        val now = SkyBlockTime.now()
        if (now.month > 1 || (altDay && now.day > 2) || (!altDay && now.day > 1)) return false
        return (altDay && now.day < 2) || now.hour < resetsAt
    }

    companion object {
        private val profileStorage get() = ProfileStorageData.profileSpecific?.chocolateFactory
        private val nextSpawnCache = CollectionUtils.ObservableMutableMap<HoppityEggType, SimpleTimeMark>(
            postUpdate = { key, value ->
                val newMark = value ?: run {
                    profileStorage?.mealNextSpawn?.remove(key)
                    return@ObservableMutableMap
                }
                profileStorage?.mealNextSpawn?.set(key, newMark)
            },
        )
        val resettingEntries = entries.filter { it.resetsAt != -1 }.sortedBy { it.resetsAt }

        fun markAllFound() = resettingEntries.forEach { it.markClaimed() }
        fun anyEggsUnclaimed(): Boolean = resettingEntries.any { !it.claimed }
        fun allEggsUnclaimed(): Boolean = resettingEntries.all { !it.claimed }

        internal fun Matcher.getEggType(event: SkyHanniChatEvent): HoppityEggType =
            entries.find { it.mealName == group("meal") } ?: run {
                ErrorManager.skyHanniError(
                    "Unknown meal: ${group("meal")}",
                    "message" to event.message,
                )
            }
    }
}
