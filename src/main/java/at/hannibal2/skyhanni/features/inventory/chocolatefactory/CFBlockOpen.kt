package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.effects.EffectDurationChangeEvent
import at.hannibal2.skyhanni.events.effects.EffectDurationChangeType
import at.hannibal2.skyhanni.features.event.hoppity.MythicRabbitPetWarning
import at.hannibal2.skyhanni.features.misc.EnchantedClockHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CFBlockOpen {
    private val config get() = SkyHanniMod.feature.inventory.chocolateFactory
    private val profileStorage get() = ProfileStorageData.profileSpecific

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: /cf
     * REGEX-TEST: /cf test
     * REGEX-TEST: /chocolatefactory
     * REGEX-TEST: /chocolatefactory123456789
     * REGEX-TEST: /factory
     * REGEX-TEST: /CF
     */
    private val commandPattern by RepoPattern.pattern(
        "inventory.chocolatefactory.opencommand",
        "(?i)\\/(?:cf|(?:chocolate)?factory)(?: .*)?",
    )

    /**
     * REGEX-TEST: §6Chocolate Factory
     * REGEX-TEST: §6Open Chocolate Factory
     */
    private val openCfItemPattern by RepoPattern.pattern(
        "inventory.chocolatefactory.openitem",
        "§6(?:Open )?Chocolate Factory",
    )
    // </editor-fold>

    private var commandSentTimer = SimpleTimeMark.farPast()

    @HandleEvent
    fun onEffectUpdate(event: EffectDurationChangeEvent) {
        if (event.effect != NonGodPotEffect.HOT_CHOCOLATE || event.duration == null) return
        val chocolateFactory = profileStorage?.chocolateFactory ?: return

        chocolateFactory.hotChocolateMixinExpiry = when (event.durationChangeType) {
            EffectDurationChangeType.ADD -> chocolateFactory.hotChocolateMixinExpiry + event.duration
            EffectDurationChangeType.REMOVE -> SimpleTimeMark.farPast()
            EffectDurationChangeType.SET -> SimpleTimeMark.now() + event.duration
        }
    }

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
        if (SkyBlockUtils.isBingoProfile) return

        tryBlock().takeIf { it != TryBlockResult.SUCCESS } ?: return
        commandSentTimer = SimpleTimeMark.now()
        event.cancel()
    }

    private enum class TryBlockResult(private val displayName: String) {
        SUCCESS(""),
        FAIL_NO_RABBIT("§7without a §dMythic Rabbit Pet §7equipped"),
        FAIL_NO_BOOSTER_COOKIE("§7without a §dBooster Cookie §7active"),
        FAIL_NO_MIXIN("§7without a §6Hot Chocolate Mixin §7active"),
        ;

        override fun toString() = displayName
    }

    private fun tryBlock(): TryBlockResult {
        return if (config.mythicRabbitRequirement && !MythicRabbitPetWarning.correctPet()) {
            ChatUtils.clickToActionOrDisable(
                "§cBlocked opening the Chocolate Factory without a §dMythic Rabbit Pet §cequipped!",
                config::mythicRabbitRequirement,
                actionName = "open pets menu",
                action = { HypixelCommands.pet() },
            )
            trySendFailureTitle(TryBlockResult.FAIL_NO_RABBIT)
            TryBlockResult.FAIL_NO_RABBIT
        } else if (config.boosterCookieRequirement) {
            profileStorage?.bits?.boosterCookieExpiryTime?.let {
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
                TryBlockResult.FAIL_NO_BOOSTER_COOKIE
            } ?: TryBlockResult.SUCCESS
        } else if (config.hotChocolateMixinRequirement) {
            val mixinExpiryTime = profileStorage?.chocolateFactory?.hotChocolateMixinExpiry ?: SimpleTimeMark.farPast()
            val godPotExpiryTime = profileStorage?.godPotExpiry ?: SimpleTimeMark.farPast()
            if (mixinExpiryTime.isInPast()) {
                ChatUtils.clickToActionOrDisable(
                    "§cBlocked opening the Chocolate Factory without a §dHot Chocolate Mixin §cactive! " +
                        "§7You may need to open §c/effects §7to refresh mixin status.",
                    config::hotChocolateMixinRequirement,
                    actionName = "search AH for mixin",
                    action = { HypixelCommands.auctionSearch("hot chocolate mixin") },
                )
                trySendFailureTitle(TryBlockResult.FAIL_NO_MIXIN)
                TryBlockResult.FAIL_NO_MIXIN
            } else if (godPotExpiryTime.isInPast()) {
                ChatUtils.clickToActionOrDisable(
                    "§cBlocked opening the Chocolate Factory without a §dGod Potion §cactive! " +
                        "§7You may need to open §c/effects §7and cycle the §aFilter §7to §bGod Potion Effects §7to refresh potion status.",
                    config::hotChocolateMixinRequirement,
                    actionName = "search AH for god potion",
                    action = { HypixelCommands.auctionSearch("god potion") },
                )
                trySendFailureTitle(TryBlockResult.FAIL_NO_MIXIN)
                TryBlockResult.FAIL_NO_MIXIN
            } else TryBlockResult.SUCCESS
        } else TryBlockResult.SUCCESS
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
