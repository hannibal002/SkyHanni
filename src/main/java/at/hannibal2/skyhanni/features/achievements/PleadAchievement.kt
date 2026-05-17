package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.data.hypixel.chat.event.PlayerAllChatEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper
import net.minecraft.client.Minecraft

@SkyHanniModule
object PleadAchievement {

    private val pleadComponent = TextHelper.createAtlasSprite("plead")
    private const val PLEAD_ACHIEVEMENT = "Plead"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            pleadComponent,
            pleadComponent,
            secret = true
        )
        event.register(achievement, PLEAD_ACHIEVEMENT)
    }

    const val pleadDye = "ewogICJ0aW1lc3RhbXAiIDogMTc3ODk4MjYzMzU5NywKICAicHJvZmlsZUlkIiA6ICI1MzkyNGYxYTg3ZTY0NzA5OGU1M2YxYzdkMTNkYzIzOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaHJvd3BvIiwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzg5Y2MwN2U3YzI0MGQ0NjFmNDliMjM4ODdiYjNhNjRmOWFmMjM3OGE5MmJhZTU4MmQ1NmVkY2Y5MDQ4NTYxYjIiCiAgICB9CiAgfQp9"

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: PlayerAllChatEvent.Allow) {
        if (!event.author.contains(PlayerUtils.getName())) return
        if (!event.cleanMessage.contains("plead")) return
        AchievementManager.completeAchievement(PLEAD_ACHIEVEMENT)
        val pleadDye = ItemUtils.createSkull("Plead Dye", "53924f1a-87e6-4709-8e53-f1c7d13dc239", pleadDye)
        Minecraft.getInstance().gameRenderer.displayItemActivation(pleadDye)
    }
}
