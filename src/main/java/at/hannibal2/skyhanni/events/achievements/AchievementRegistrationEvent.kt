package at.hannibal2.skyhanni.events.achievements

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.test.command.ErrorManager

class AchievementRegistrationEvent : SkyHanniEvent() {
    private val achievements = mutableMapOf<String, Achievement>()

    fun register(achievement: Achievement, id: String) {
        if (achievements.containsKey(id)) {
            ErrorManager.crashInDevEnv("multiple achievements with same id: $id")
        }

        achievements[id] = achievement
    }

    fun getAchievements(): Map<String, Achievement> {
        return achievements
    }
}
