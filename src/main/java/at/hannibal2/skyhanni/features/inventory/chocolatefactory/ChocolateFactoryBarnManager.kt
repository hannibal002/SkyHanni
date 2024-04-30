package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggsManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object ChocolateFactoryBarnManager {

    private val config get() = ChocolateFactoryAPI.config
    private val profileStorage get() = ChocolateFactoryAPI.profileStorage

    private val newRabbitPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "rabbit.new",
        "§d§lNEW RABBIT! §6\\+\\d Chocolate §7and §6\\+0.\\d+x Chocolate §7per second!"
    )
    private val rabbitDuplicatePattern by ChocolateFactoryAPI.patternGroup.pattern(
        "rabbit.duplicate",
        "§7§lDUPLICATE RABBIT! §6\\+[\\d,]+ Chocolate"
    )

    var barnFull = false
    private var lastBarnFullWarning = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        newRabbitPattern.matchMatcher(event.message) {
            val profileStorage = profileStorage ?: return
            profileStorage.currentRabbits += 1
            trySendBarnFullMessage()
            HoppityEggsManager.shareWaypointPrompt()
        }

        rabbitDuplicatePattern.matchMatcher(event.message) {
            HoppityEggsManager.shareWaypointPrompt()
        }
    }

    fun trySendBarnFullMessage() {
        if (!ChocolateFactoryAPI.isEnabled()) return

        if (config.barnCapacityThreshold <= 0) {
            return
        }

        val profileStorage = profileStorage ?: return

        if (profileStorage.maxRabbits >= ChocolateFactoryAPI.maxRabbits) return

        val remainingSpace = profileStorage.maxRabbits - profileStorage.currentRabbits
        barnFull = remainingSpace <= config.barnCapacityThreshold
        if (!barnFull) return

        if (lastBarnFullWarning.passedSince() < 30.seconds) return

        if (profileStorage.maxRabbits == -1) {
            ChatUtils.clickableChat(
                "Open your chocolate factory to see your barn's capacity status!",
                onClick = {
                    HypixelCommands.chocolateFactory()
                }
            )
            return
        }

        ChatUtils.clickableChat(
            message = if (profileStorage.currentRabbits == profileStorage.maxRabbits) {
                "§cYour barn is full! §7(${barnStatus()}). §cUpgrade it so they don't get crushed"
            } else {
                "§cYour barn is almost full! §7(${barnStatus()}). §cUpgrade it so they don't get crushed"
            },
            onClick = {
                HypixelCommands.chocolateFactory()
            }
        )
        SoundUtils.playBeepSound()
        lastBarnFullWarning = SimpleTimeMark.now()
    }

    fun barnStatus(): String {
        val profileStorage = profileStorage ?: return "Unknown"
        return "${profileStorage.currentRabbits}/${profileStorage.maxRabbits} Rabbits"
    }
}
