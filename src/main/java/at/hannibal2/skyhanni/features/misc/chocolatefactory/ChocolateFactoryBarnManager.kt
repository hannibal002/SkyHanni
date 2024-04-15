package at.hannibal2.skyhanni.features.misc.chocolatefactory

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object ChocolateFactoryBarnManager {

    private val config get() = ChocolateFactoryApi.config
    private val profileStorage get() = ChocolateFactoryApi.profileStorage

    private val newRabbitPattern by ChocolateFactoryApi.patternGroup.pattern(
        "rabbit.new",
        "§d§lNEW RABBIT! §6\\+\\d Chocolate §7and §6\\+0.\\d+x Chocolate §7per second!"
    )
    private val rabbitDuplicatePattern by ChocolateFactoryApi.patternGroup.pattern(
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
        }
    }

    fun trySendBarnFullMessage() {
        if (!ChocolateFactoryApi.isEnabled()) return
        val profileStorage = profileStorage ?: return

        val remainingSpace = profileStorage.maxRabbits - profileStorage.currentRabbits
        barnFull =
            remainingSpace <= config.barnCapacityThreshold && profileStorage.maxRabbits < ChocolateFactoryApi.maxRabbits
        if (!barnFull) return

        if (lastBarnFullWarning.passedSince() < 30.seconds) return

        if (profileStorage.maxRabbits == -1) {
            ChatUtils.clickableChat(
                "Open your chocolate factory to see your barn's capacity status!",
                "cf"
            )
            return
        }

        ChatUtils.clickableChat(
            "§cYour barn is almost full! " +
                "§7(${barnStatus()}). §cUpgrade it so they don't get crushed",
            "cf"
        )
        SoundUtils.playBeepSound()
        lastBarnFullWarning = SimpleTimeMark.now()
    }

    fun barnStatus(): String {
        val profileStorage = profileStorage ?: return "Unknown"
        return "${profileStorage.currentRabbits}/${profileStorage.maxRabbits} Rabbits"
    }
}
