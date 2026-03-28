package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object NpcSellLimitAchievement {

    private val sellLimitPattern by AchievementManager.group.pattern(
        "npc-sell-limit",
        "You've reached the daily limit of coins you may earn from NPC shops\\.",
    )

    private const val NPC_SELL_LIMIT_ACHIEVEMENT = "npc sell limit"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Bankrolled by NPCs".asComponent(),
            "Steal 500mil coins from an NPC in one day".asComponent(),
            20f,
        )
        event.register(achievement, NPC_SELL_LIMIT_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (sellLimitPattern.matches(event.cleanMessage)) {
            AchievementManager.completeAchievement(NPC_SELL_LIMIT_ACHIEVEMENT)
        }
    }
}
