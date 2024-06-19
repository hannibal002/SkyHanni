package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

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

    @SubscribeEvent
    fun onCommandSend(event: MessageSendToServerEvent) {
        if (!isEnabled()) return
        if (!commandPattern.matches(event.message)) return
        if (PetAPI.currentPet?.startsWith("§dRabbit") == true) return

        event.cancel()
        ChatUtils.chat("Blocked opening the Chocolate Factory without a mythic §dRabbit §epet equipped.")
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.mythicRabbitRequirement
}
