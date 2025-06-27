package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi
import at.hannibal2.skyhanni.features.event.hoppity.HoppityCollectionData
import at.hannibal2.skyhanni.features.event.hoppity.HoppityCollectionStats
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggsManager
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.data.ChocolateAmount
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CFBarnManager {

    private val config get() = CFApi.config
    private val hoppityChatConfig get() = HoppityEggsManager.config.chat
    private val profileStorage get() = CFApi.profileStorage
    private val virtualCountHolder = TimeLimitedCache<String, Int>(5.seconds)

    /**
     * REGEX-TEST: §c§lBARN FULL! §fOlivette §7got §ccrushed§7! §6+290,241 Chocolate
     */
    private val rabbitCrashedPattern by CFApi.patternGroup.pattern(
        "rabbit.crushed",
        "§c§lBARN FULL! §f\\D+ §7got §ccrushed§7! §6\\+(?<amount>[\\d,]+) Chocolate",
    )

    fun isBarnFull(): Boolean {
        val profileStorage = profileStorage ?: return false

        // when the unlocked barn space has already reached or surpassed the total amount of rabbits
        val alreadyBigEnough = profileStorage.maxRabbits >= HoppityCollectionData.knownRabbitCount

        val remainingSpace = profileStorage.maxRabbits - profileStorage.currentRabbits
        return remainingSpace <= config.barnCapacityThreshold && !alreadyBigEnough
    }

    private var sentBarnFullWarning = false
    private var lastRabbit = ""

    fun processDataSet(dataSet: HoppityApi.HoppityStateDataSet) {
        lastRabbit = dataSet.lastName
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent) {
        HoppityEggsManager.newRabbitFound.matchMatcher(event.message) {
            val profileStorage = profileStorage ?: return@matchMatcher
            profileStorage.currentRabbits += 1
            trySendBarnFullMessage(inventory = false)
            HoppityEggsManager.shareWaypointPrompt()
        }

        HoppityEggsManager.duplicateRabbitFound.matchMatcher(event.message) {
            HoppityEggsManager.shareWaypointPrompt()
            event.duplicateFoundMessage(group("amount"))
        }

        rabbitCrashedPattern.matchMatcher(event.message) {
            HoppityEggsManager.shareWaypointPrompt()
            ChocolateAmount.addToAll(group("amount").formatLong())
        }
    }

    private fun SkyHanniChatEvent.duplicateFoundMessage(rawAmount: String) {
        val amount = rawAmount.formatLong()
        if (config.showDuplicateTime && !hoppityChatConfig.compact) {
            val format = CFApi.timeUntilNeed(amount).format(maxUnits = 2)
            DelayedRun.runNextTick {
                ChatUtils.chat("§7(§a+§b$format §aof production§7)")
            }
        }
        ChocolateAmount.addToAll(amount)
        HoppityApi.attemptFireRabbitFound(this, lastDuplicateAmount = amount)

        var changedMessage = message

        // Add duplicate number to the duplicate rabbit message
        if (hoppityChatConfig.showDuplicateNumber && !hoppityChatConfig.compact) {
            val dupeNumber = when {
                virtualCountHolder[lastRabbit] != null -> (virtualCountHolder[lastRabbit] ?: 0) + 1
                else -> HoppityCollectionStats.getRabbitCount(lastRabbit).takeIf { it > 0 }?.also {
                    virtualCountHolder[lastRabbit] = it
                }
            }

            dupeNumber?.let {
                changedMessage = changedMessage.replace(
                    "§7§lDUPLICATE RABBIT!",
                    "§7§lDUPLICATE RABBIT! §7(Duplicate §b#$it§7)§r",
                )
            }
        }

        // Replace §6\+(?<amount>[\d,]+) Chocolate with §6\+§d(?<amount>[\d,]+) §6Chocolate
        if (hoppityChatConfig.recolorTTChocolate && CFTimeTowerManager.timeTowerActive()) {
            changedMessage = changedMessage.replace(
                "§6\\+(?<amount>[\\d,]+) Chocolate",
                "§6\\+§d$rawAmount §6Chocolate",
            )
        }

        if (message != changedMessage) chatComponent = changedMessage.asComponent()
    }

    @HandleEvent(InventoryCloseEvent::class)
    fun onInventoryClose() {
        sentBarnFullWarning = false
    }

    fun trySendBarnFullMessage(inventory: Boolean) {
        if (!CFApi.isEnabled()) return

        if (config.barnCapacityThreshold <= 0) {
            return
        }

        val profileStorage = profileStorage ?: return

        // TODO rename maxRabbits to maxUnlockedBarnSpace
        if (profileStorage.maxRabbits >= CFApi.maxRabbits) return

        if (!isBarnFull()) return

        if (inventory && sentBarnFullWarning) return

        sentBarnFullWarning = true

        if (profileStorage.maxRabbits == -1) {
            ChatUtils.clickableChat(
                "Open your chocolate factory to see your barn's capacity status!",
                onClick = { HypixelCommands.chocolateFactory() },
                "§eClick to run /cf!",
            )
            return
        }

        if (config.rabbitCrushOnlyDuringHoppity && !HoppityApi.isHoppityEvent()) return

        val fullLevel = if (profileStorage.currentRabbits == profileStorage.maxRabbits) "full" else "almost full"
        ChatUtils.clickableChat(
            "§cYour barn is $fullLevel §7(${barnStatus()}). §cUpgrade it so they don't get crushed!",
            onClick = { HypixelCommands.chocolateFactory() },
            "§eClick to run /cf!",
        )
        SoundUtils.playBeepSound()
    }

    fun barnStatus(): String {
        val profileStorage = profileStorage ?: return "Unknown"
        return "${profileStorage.currentRabbits}/${profileStorage.maxRabbits} Rabbits"
    }
}
