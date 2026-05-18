package at.hannibal2.skyhanni.data.achievements

import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import com.google.gson.annotations.Expose
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

/**
 * An unlockable Achievement for doing something in game
 *
 * @param name Name show in Chat and /shachievements
 * @param description Description shown when hovering over the achievement in chat
 * @param userLuckAmount The amount of user luck gained when unlocking the achievement
 * @param secret Secret achievements only show their name once unlocked
 * @param tiers For achievements that require multiple completions (10, 20) would require 10 for tier 1 and 20 to fully unlock
 * @param hidden Hidden achievements are completely hidden until unlocked
 */
data class Achievement(
    private val name: Component? = null,
    private val description: Component = Component.empty(),
    var userLuckAmount: Float = 0f,
    var secret: Boolean = false, // Secret achievements hide the name
    var tiers: List<Int> = listOf(),
    var hidden: Boolean = false, // Hidden achievements are hidden until found
    @Expose
    var data: AchievementUserData = AchievementUserData(),
) {
    fun getNameOrNull(): Component? {
        name ?: return null
        val tier = getCurrentTier() ?: return name
        if (tier == 0 || tiers.size == 1) return name
        return name.copy().append(" $tier")
    }

    fun getName() = getNameOrNull() ?: "?".asComponent()

    fun getDescription(): Component {
        if (tiers.isEmpty()) return description

        if (tiers.size == 1) {
            return componentBuilder {
                append(description)
                append(" \n")
                if (!data.achieved) append("${getProgressFormatted()}/${tiers.first().addSeparators()} to Unlock")
                else append("Current Progress: ${getProgressFormatted()} (you only needed ${tiers.first().addSeparators()} to unlock it)")
                withColor(ChatFormatting.YELLOW)
            }
        } else {
            return componentBuilder {
                append(description)
                append("\n")
                if (!data.achieved) {
                    append("${getProgressFormatted()}/${getAmountForNextTier()} for the next tier.")
                    append("\n${getProgressFormatted()}/${tiers.last().addSeparators()} to fully unlock!")
                } else {
                    append(
                        "Current Progress: ${getProgressFormatted()} " +
                            "(you only needed ${tiers.last().addSeparators()} to fully unlock it)"
                    )
                }
                withColor(ChatFormatting.YELLOW)
            }
        }
    }

    fun getCurrentTier(): Int? {
        if (tiers.isEmpty()) return null
        var currentTier = 0
        for (tier in tiers) {
            if (data.progress >= tier) currentTier++
        }
        return currentTier
    }

    fun getAmountForNextTier(): String {
        var amount = 0
        for (tier in tiers) {
            if (data.progress >= tier) continue
            amount = tier
            break
        }
        return amount.addSeparators()
    }

    fun isTieredAchievement(): Boolean {
        return tiers.size > 1
    }

    fun getProgressFormatted(): String {
        return data.progress.addSeparators()
    }
}

data class AchievementUserData(
    @Expose
    var achieved: Boolean = false,
    @Expose
    var progress: Int = 0,
)
