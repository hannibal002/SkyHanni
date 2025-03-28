package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.features.event.hoppity.MythicRabbitPetWarning
import at.hannibal2.skyhanni.features.misc.EnchantedClockHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ChocolateFactoryBlockOpen {
    private val config get() = SkyHanniMod.feature.inventory.chocolateFactory
    private val profileStorage get() = ProfileStorageData.profileSpecific?.bits

    /**
     * REGEX-TEST: /cf
     * REGEX-TEST: /cf test
     * REGEX-TEST: /chocolatefactory
     * REGEX-TEST: /chocolatefactory123456789
     * REGEX-TEST: /factory
     */
    private val commandPattern by RepoPattern.pattern(
        "inventory.chocolatefactory.opencommand",
        "\\/(?:cf|(?:chocolate)?factory)(?: .*)?",
    )

    /**
     * REGEX-TEST: §6Chocolate Factory
     * REGEX-TEST: §6Open Chocolate Factory
     */
    private val openCfItemPattern by RepoPattern.pattern(
        "inventory.chocolatefactory.openitem",
        "§6(?:Open )?Chocolate Factory",
    )

    private var commandSentTimer = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnSkyblock = true)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        val slotDisplayName = event.slot?.stack?.displayName ?: return
        if (!openCfItemPattern.matches(slotDisplayName)) return
        if (EnchantedClockHelper.enchantedClockPattern.matches(InventoryUtils.openInventoryName())) return

        tryBlock().takeIf { it != TryBlockResult.SUCCESS } ?: return
        event.cancel()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onCommandSend(event: MessageSendToServerEvent) {
        if (!commandPattern.matches(event.message)) return
        if (commandSentTimer.passedSince() < 5.seconds) return
        if (LorenzUtils.isBingoProfile) return

        tryBlock().takeIf { it != TryBlockResult.SUCCESS } ?: return
        commandSentTimer = SimpleTimeMark.now()
        event.cancel()
    }

    private enum class TryBlockResult(private val displayName: String) {
        SUCCESS(""),
        FAIL_NO_RABBIT("§7without a §dMythic Rabbit Pet §7equipped"),
        FAIL_NO_BOOSTER_COOKIE("§7without a §dBooster Cookie §7active"),
        ;

        override fun toString() = displayName
    }

    private fun tryBlock(): TryBlockResult {
        if (config.mythicRabbitRequirement && !MythicRabbitPetWarning.correctPet()) {
            ChatUtils.clickToActionOrDisable(
                "§cBlocked opening the Chocolate Factory without a §dMythic Rabbit Pet §cequipped!",
                config::mythicRabbitRequirement,
                actionName = "open pets menu",
                action = { HypixelCommands.pet() },
            )
            trySendFailureTitle(TryBlockResult.FAIL_NO_RABBIT)
            return TryBlockResult.FAIL_NO_RABBIT
        } else if (config.boosterCookieRequirement) {
            profileStorage?.boosterCookieExpiryTime?.let {
                if (it.timeUntil() > 0.seconds) return TryBlockResult.SUCCESS
                ChatUtils.clickToActionOrDisable(
                    "§cBlocked opening the Chocolate Factory without a §dBooster Cookie §cactive!",
                    config::boosterCookieRequirement,
                    actionName = "warp to hub",
                    action = {
                        HypixelCommands.warp("hub")
                        EntityMovementData.onNextTeleport(IslandType.HUB) {
                            IslandGraphs.pathFind(LorenzVec(-32.5, 71.0, -76.5), "§aBazaar", condition = { true })
                        }
                    },
                )
                trySendFailureTitle(TryBlockResult.FAIL_NO_BOOSTER_COOKIE)
                return TryBlockResult.FAIL_NO_BOOSTER_COOKIE
            }
        }
        return TryBlockResult.SUCCESS
    }

    private fun trySendFailureTitle(result: TryBlockResult) {
        if (result == TryBlockResult.SUCCESS) return
        val titleLocation = if (InventoryUtils.inInventory()) TitleManager.TitleLocation.INVENTORY else TitleManager.TitleLocation.GLOBAL
        TitleManager.sendTitle(
            "§cBlocked opening the Chocolate Factory",
            subtitleText = "$result",
            location = titleLocation,
        )
    }
}
