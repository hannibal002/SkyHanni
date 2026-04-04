package at.hannibal2.skyhanni.data.achievements

import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import com.google.gson.annotations.Expose
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

data class Achievement(
    private val name_: Component? = null,
    private val description_: Component = Component.empty(),
    var userLuckAmount: Float = 0f,
    var secret: Boolean = false,
    var tiers: List<Int> = listOf(),
    @Expose
    var data: AchievementUserData = AchievementUserData(),
) {
    fun getName(): Component? {
        name_ ?: return null
        val tier = getCurrentTier() ?: return name_
        if (tier == 0 || tiers.size == 1) return name_
        return name_.copy().append(" $tier")
    }

    fun getDescription(): Component {
        if (tiers.isEmpty()) return description_

        if (tiers.size == 1) {
            return componentBuilder {
                append(description_)
                append(" \n")
                if (!data.achieved) append("${getProgressFormatted()}/${tiers.first().addSeparators()} to Unlock")
                else append("Current Progress: ${getProgressFormatted()} (you only needed ${tiers.first().addSeparators()} to unlock it)")
                withColor(ChatFormatting.YELLOW)
            }
        } else {
            return componentBuilder {
                append(description_)
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
