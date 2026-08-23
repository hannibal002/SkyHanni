package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.data.achievements.Achievement
import com.google.gson.annotations.Expose

class AchievementStorage {
    @Expose
    val achievements: MutableMap<String, Achievement> = mutableMapOf()
}
