package at.hannibal2.skyhanni.features.achievements

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.UserLuckCalculateEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.command
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items

@SkyHanniModule
object AchievementManager {

    private val config get() = SkyHanniMod.achievementStorage.achievements
    private val shouldShowMessages get() = SkyHanniMod.feature.misc.achievementMessages
    val group = RepoPattern.group("achievements")
    private val achievementSound = SoundUtils.createSound("ui.toast.challenge_complete", 1f, .8f)

    @HandleEvent
    fun onInitFinished() {
        val event = AchievementRegistrationEvent()
        event.post()
        for ((id, achievement) in event.getAchievements()) {
            val oldAchievement = config[id]
            if (oldAchievement != null) {
                achievement.data = oldAchievement.data
            }
            config[id] = achievement
        }

        SkyHanniMod.configManager.saveConfig(ConfigFileType.ACHIEVEMENTS, "achievements loaded")
    }

    fun getAchievement(id: String): Achievement {
        return config[id] ?: ErrorManager.skyHanniError("Achievement with unknown id", "id" to id)
    }

    fun setAchievement(id: String, achievement: Achievement) {
        if (HypixelData.hypixelAlpha) return
        config[id] = achievement
        SkyHanniMod.configManager.saveConfig(ConfigFileType.ACHIEVEMENTS, "achievement set")
    }

    fun updateTieredAchievement(id: String, newProgress: Int) {
        if (HypixelData.hypixelAlpha) return
        val achievement = config[id] ?: ErrorManager.skyHanniError("Achievement with unknown id", "id" to id)
        val currentTier = achievement.getCurrentTier() ?: 0
        achievement.data.progress = newProgress
        val newTier = achievement.getCurrentTier() ?: 0
        if (newTier > currentTier) {
            if (newTier == achievement.tiers.size) achievement.data.achieved = true
            if (shouldShowMessages) {
                ChatUtils.chat(
                    componentBuilder {
                        append("Achievement Get! ") {
                            withColor(ChatFormatting.GOLD)
                        }
                        append(achievement.getName() ?: "?".asComponent()) {
                            withColor(ChatFormatting.GREEN)
                        }
                        if (!achievement.data.achieved) {
                            append(" $newProgress/${achievement.getAmountForNextTier()} to unlock the next tier")
                        }
                        append("!")
                        hover = achievement.getDescription()
                        command = "/shachievements"
                    }
                )
                achievementSound.playSound()
            }
        }

        config[id] = achievement
        SkyHanniMod.configManager.saveConfig(ConfigFileType.ACHIEVEMENTS, "achievement progress update")
    }

    fun completeAchievement(id: String) {
        if (HypixelData.hypixelAlpha) return
        val achievement = config[id] ?: ErrorManager.skyHanniError("Achievement with unknown id", "id" to id)
        if (achievement.data.achieved) return
        achievement.data.achieved = true
        config[id] = achievement
        if (shouldShowMessages) {
            ChatUtils.chat(
                componentBuilder {
                    append("Achievement Get! ") {
                        withColor(ChatFormatting.GOLD)
                    }
                    append(achievement.getName() ?: "?".asComponent()) {
                        withColor(ChatFormatting.GREEN)
                    }
                    append("!")
                    hover = achievement.getDescription()
                    command = "/shachievements"
                }
            )
            achievementSound.playSound()
        }

        SkyHanniMod.configManager.saveConfig(ConfigFileType.ACHIEVEMENTS, "achievement completed")
    }

    const val TEST_ACHIEVEMENT = "Test Achievement"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Test Achievement".asComponent(),
            componentBuilder {
                append("Run /shtestachievement to test the achievement system!") {
                    withColor(ChatFormatting.DARK_PURPLE)
                }
            },
            1f,
        )
        event.register(achievement, TEST_ACHIEVEMENT)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestachievement") {
            description = "Tests achievement granting and revoking"
            category = CommandCategory.DEVELOPER_TEST
            literalCallback("unlockall") {
                ChatUtils.chat("you didn't think this would really work? did you...")
            }
            simpleCallback {
                val achievement = getAchievement(TEST_ACHIEVEMENT)
                if (achievement.data.achieved) {
                    achievement.data.achieved = false
                    setAchievement(TEST_ACHIEVEMENT, achievement)
                    ChatUtils.chat("Lost the test achievement :(")
                } else {
                    completeAchievement(TEST_ACHIEVEMENT)
                }
            }
        }
        event.registerBrigadier("shachievements") {
            description = "Shows your current achievement progress"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback {
                val achievementList = config.map { it.value }.sortedBy { it.data.achieved }.filter { it.getName() != null }
                TextHelper.displayPaginatedList(
                    "SkyHanni Achievements!",
                    achievementList,
                    ChatUtils.getUniqueMessageId(),
                    "No Achievements Found"
                ) { achievement ->
                    componentBuilder {
                        if (achievement.secret && !achievement.data.achieved) {
                            append("???") {
                                withColor(ChatFormatting.DARK_GRAY)
                            }
                        } else {
                            append(achievement.getName() ?: "?".asComponent()) {
                                withColor(ChatFormatting.WHITE)
                            }
                        }
                        if (achievement.data.achieved) {
                            append(" ✔") {
                                withColor(ChatFormatting.GREEN)
                            }
                        } else if (!achievement.isTieredAchievement()) {
                            append(" ❌") {
                                withColor(ChatFormatting.RED)
                            }
                        } else {
                            append("/${achievement.tiers.size}")
                        }
                        hover = achievement.getDescription()
                    }
                }
            }
        }
    }

    @HandleEvent
    fun onUserLuck(event: UserLuckCalculateEvent) {
        var luck = 0f
        var hasDoneAllAchievements = true
        for ((_, achievement) in config) {
            if (!achievement.data.achieved) hasDoneAllAchievements = false
            else luck += achievement.userLuckAmount
        }
        if (hasDoneAllAchievements) luck += 100
        if (luck == 0f) return
        event.addLuck(luck)

        val stack = ItemUtils.createItemStack(
            Items.KNOWLEDGE_BOOK,
            Component.literal("✴ Achievements").withColor(ChatFormatting.GREEN),
            listOf(
                Component.literal("Innate").withColor(ChatFormatting.DARK_GRAY),
                Component.empty(),
                componentBuilder {
                    appendWithColor("Value: ", ChatFormatting.GRAY)
                    appendWithColor("$luck✴", ChatFormatting.GREEN)
                },
                Component.empty(),
                Component.literal("Gain more by completing achievements!").withColor(ChatFormatting.DARK_GRAY),
                Component.literal("Do /shachievements to see them all!").withColor(ChatFormatting.DARK_GRAY),
            ),
        )
        event.addItem(stack)
    }
}
