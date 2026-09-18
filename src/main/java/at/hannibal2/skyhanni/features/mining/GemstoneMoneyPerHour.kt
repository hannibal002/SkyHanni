package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.mining.GemstoneMoneyPerHourConfig
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.readableInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GemstoneMoneyPerHour {

    /**
     * REGEX-TEST: PRISTINE! You found ☘ Flawed Jade Gemstone x20!
     * REGEX-TEST: PRISTINE! You found ❈ Flawed Amethyst Gemstone x16!
     */
    private val pristineMessagePattern by RepoPattern.pattern(
        "mining.pristine.colorless",
        "PRISTINE! You found . Flawed (?<gemstone>\\w+) Gemstone x(?<amount>\\d+)!",
    )

    /**
     * REGEX-TEST: rough jade gem
     */
    private val roughGemstoneNamePattern by RepoPattern.pattern(
        "mining.roughgemstone",
        "rough (?<gemstone>\\w+) gem",
    )

    private val config get() = SkyHanniMod.feature.mining.gemstoneMoneyPerHour

    private var display: List<Renderable> = listOf()
    private var start = SimpleTimeMark.farPast()
    private var lastMined = SimpleTimeMark.farPast()
    private var coins = 0
    private var lastGemstone: String = ""
    private var useNextSackChange = false
    private var uptime: Duration = 0.seconds
    private var paused: Boolean = false

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SystemMessageEvent.Allow) {
        if (!isEnabled()) return
        pristineMessagePattern.matchMatcher(event.cleanMessage) {
            if (start.isFarPast()) start = SimpleTimeMark.now()
            if (paused) paused = false
            useNextSackChange = true
            lastMined = SimpleTimeMark.now()
            val gemstone = group("gemstone")
            lastGemstone = gemstone
            val configGemstonePrice = getPrice(convertToInternalName(lastGemstone))
            val delta = group("amount").toDouble() * getFraction(2) * configGemstonePrice
            coins += delta.toInt()
        }
    }

    @HandleEvent
    private fun onSackChange(event: SackChangeEvent) {
        if (!isEnabled() || !useNextSackChange) return
        useNextSackChange = false

        for ((delta, internalName) in event.sackChanges) {
            if (delta < 0) continue

            roughGemstoneNamePattern.matchMatcher(internalName.readableInternalName) {
                val gemstone = group("gemstone")
                val configGemstonePrice = getPrice(convertToInternalName(gemstone))
                val actualDelta = delta.toDouble() * getFraction(1) * configGemstonePrice
                coins += actualDelta.toInt()
            }
        }
    }

    private fun convertToInternalName(name: String): NeuInternalName {
        return "${config.gemstoneType.displayName}_${name}_GEM".toInternalName()
    }

    private fun getPrice(gemstone: NeuInternalName): Double {
        return if (config.forceNPC) gemstone.getNpcPrice()
        else maxOf(gemstone.getNpcPrice(), gemstone.getPrice())
    }

    // Finds number of gemstones needed to craft config.gemstoneType from type
    private fun getFraction(type: Int): Double {
        return (80.0).pow(type - toNum(config.gemstoneType))
    }

    private fun toNum(type: GemstoneMoneyPerHourConfig.GemstoneType): Int {
        return when (type) {
            GemstoneMoneyPerHourConfig.GemstoneType.ROUGH -> 1
            GemstoneMoneyPerHourConfig.GemstoneType.FLAWED -> 2
            GemstoneMoneyPerHourConfig.GemstoneType.FINE -> 3
            GemstoneMoneyPerHourConfig.GemstoneType.FLAWLESS -> 4
        }
    }

    private fun updateDisplay() {
        display = createDisplay()
    }

    private fun createDisplay() = buildList {
        if (start.isFarPast()) return@buildList
        if (lastGemstone.isEmpty()) return@buildList
        val moneyPerHour = coins / maxOf(uptime.inPartialSeconds, 1.0) * 3600
        val internalName = convertToInternalName(lastGemstone)
        val gemstoneName = internalName.repoItemName.removeSuffix(" Gemstone")
        val gemstonePrice = getPrice(internalName).toInt()
        val pausedText = if (paused) " §c(PAUSED)"
        else ""

        addString("§d§lGemstone Money per Hour")
        addString("§aSelling $gemstoneName §afor §6${gemstonePrice.shortFormat()} §aeach")
        addString("§aCoins/hr: §6${moneyPerHour.toInt().shortFormat()}")
        addString("§aCoins made: §6${coins.shortFormat()}")
        addString("§aUptime: §b${uptime.format()}$pausedText")
        addButtons()
    }

    private fun MutableList<Renderable>.addButtons() {
        if (!InventoryUtils.inAnyInventory()) return
        addRenderableButton<GemstoneMoneyPerHourConfig.GemstoneType>(
            label = "Gemstone Type",
            current = config.gemstoneType,
            getName = { it.displayName },
            onChange = {
                config.gemstoneType = it
                updateDisplay()
            },
        )

        addRenderableButton(
            label = "Use NPC Price",
            config = config::forceNPC,
            enabled = "Use NPC Price",
            disabled = "Use bazaar Price",
            onChange = {
                updateDisplay()
            },
        )

        addRenderableButton(
            label = "Pause Tracker when not mining",
            config = config::shouldPause,
            enabled = "Pause when not mining",
            disabled = "Reset when not mining",
            onChange = {
                updateDisplay()
            },
        )
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onGuiRenderTop() {
        if (!isEnabled()) return
        display.ifEmpty { updateDisplay() }
        if (display.isNotEmpty()) {
            config.position.renderRenderables(
                display,
                extraSpace = 2,
                posLabel = "Gemstone Money per Hour Display",
            )
        }
    }

    @HandleEvent
    private fun onSecondPassed() {
        if (!isEnabled() || lastMined.isFarPast()) display = listOf()
        else if (lastMined.passedSince() > config.timeoutTime.toInt().seconds) {
            if (config.shouldPause) paused = true
            else reset()
        } else uptime += 1.seconds
        display = createDisplay()
    }

    @HandleEvent
    private fun onIslandJoin() {
        if (!paused) return
        if (!isEnabled() || !IslandTypeTag.MINING.isInIsland()) return reset()
        paused = true
    }

    private fun reset() {
        start = SimpleTimeMark.farPast()
        lastMined = SimpleTimeMark.farPast()
        uptime = 0.seconds
        coins = 0
        paused = false
        lastGemstone = ""
        display = listOf()
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetgemstone") {
            description = "Resets the gemstone money per hour display."
            category = CommandCategory.USERS_RESET
            simpleCallback {
                reset()
                ChatUtils.chat("Reset gemstone money per hour display!")
            }
        }
    }

    private fun isEnabled() = config.enabled
}
