package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigGuiManager
import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ChocolateFactoryBlockOpen {
    private val config get() = SkyHanniMod.feature.inventory.chocolateFactory

    /**
     * REGEX-TEST: /cf
     * REGEX-TEST: /cf test
     * REGEX-TEST: /chocolatefactory
     * REGEX-TEST: /chocolatefactory123456789
     */
    private val commandPattern by RepoPattern.pattern(
        "inventory.chocolatefactory.opencommand",
        "/(?:cf|chocolatefactory)(?: .*)?",
    )

    private var commandSentTimer = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onCommandSend(event: MessageSendToServerEvent) {
        if (!isEnabled()) return
        if (!commandPattern.matches(event.message)) return
        if (commandSentTimer.passedSince() < 5.seconds) return
        if (PetAPI.currentPet?.startsWith("§dRabbit") == true) return

        commandSentTimer = SimpleTimeMark.now()
        event.cancel()
        ChatUtils.clickableChat(
            "Blocked opening the Chocolate Factory without a mythic §dRabbit §epet equipped. Click here to disable!",
            { ConfigGuiManager.openConfigGui("mythic rabbit pet equipped") },
        )
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.mythicRabbitRequirement
}
