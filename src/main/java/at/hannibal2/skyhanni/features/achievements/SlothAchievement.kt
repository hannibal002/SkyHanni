package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting

@SkyHanniModule
object SlothAchievement {

    private const val SECURITY_SLOTH_ACHIEVEMENT = "Security Sloth"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Good Advice".asComponent(),
            componentBuilder {
                append("Talk to the Security Sloth. ")
                append("They have good advice, you know") {
                    withColor(ChatFormatting.RED)
                }
            }
        )
        event.register(achievement, SECURITY_SLOTH_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (event.cleanMessage.startsWith("[SECURITY] Sloth")) {
            AchievementManager.completeAchievement(SECURITY_SLOTH_ACHIEVEMENT)
        }
    }
}
