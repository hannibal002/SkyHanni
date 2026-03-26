package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

@SkyHanniModule
object CakeSoulAchievement {

    private val cakeSoulPattern by AchievementManager.group.pattern(
        "cake-soul",
        "You found a Cake Soul!",
    )

    private const val CAKE_SOUL_ACHIEVEMENT = "Found Cake Soul"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Hmmmmm cake... soul".asComponent(),
            Component.literal("Find a cake soul").withColor(ChatFormatting.LIGHT_PURPLE),
            3f,
        )
        event.register(achievement, CAKE_SOUL_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (cakeSoulPattern.matches(event.cleanMessage)) {
            AchievementManager.completeAchievement(CAKE_SOUL_ACHIEVEMENT)
        }
    }
}
