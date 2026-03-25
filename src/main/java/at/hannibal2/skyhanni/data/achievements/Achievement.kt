package at.hannibal2.skyhanni.data.achievements

import com.google.gson.annotations.Expose
import net.minecraft.network.chat.Component

data class Achievement(
    val name: String? = null,
    val description: Component = Component.empty(),
    var userLuckAmount: Float = 0f,
    var secret: Boolean = false,
    @Expose
    var data: AchievementUserData = AchievementUserData(),
)

data class AchievementUserData(
    @Expose
    var achieved: Boolean = false,
)
