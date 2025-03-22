package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.mining.GemstoneMoneyPerHourConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.ItemUtils.readableInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GemstoneMoneyPerHour {

    /**
     * REGEX-TEST: §d§lPRISTINE! §r§fYou found §r§a☘ Flawed Jade Gemstone §r§8x20§r§f!
     * REGEX-TEST: §d§lPRISTINE! §r§fYou found §r§a❈ Flawed Amethyst Gemstone §r§8x16§r§f!
     */
    private val pristineMessagePattern by RepoPattern.pattern(
        "mining.pristine",
        "§d§lPRISTINE! §r§fYou found §r§a. Flawed (?<gemstone>\\w+) Gemstone §r§8x(?<amount>\\d+)§r§f!"
    )

    private val roughGemstoneNamePattern by RepoPattern.pattern(
        "mining.roughgemstone",
        "rough (?<gemstone>\\w+) gem"
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
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        pristineMessagePattern.matchMatcher(event.message) {
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
    fun onSackChange(event: SackChangeEvent) {
        if (!isEnabled() || !useNextSackChange) return
        useNextSackChange = false

        for (change in event.sackChanges) {
            if (change.delta < 0) continue

            roughGemstoneNamePattern.matchMatcher(change.internalName.readableInternalName) {
                val gemstone = group("gemstone")
                val configGemstonePrice = getPrice(convertToInternalName(gemstone))
                val delta = change.delta.toDouble() * getFraction(1) * configGemstonePrice
                coins += delta.toInt()
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
        val gemstoneName = internalName.itemNameWithoutColor
        val gemstonePrice = getPrice(internalName).toInt()
        val pausedText = if (paused) " §c(PAUSED)"
        else ""

        add(Renderable.string("§d§lGemstone Money per Hour"))
        add(Renderable.string("§a($gemstoneName @ §b${gemstonePrice.shortFormat()})"))
        add(Renderable.string("§aCoins/hr: §b${moneyPerHour.toInt().shortFormat()}"))
        add(Renderable.string("§aCoins made: §b${coins.shortFormat()}"))
        add(Renderable.string("§aUptime: §b${uptime.format()}$pausedText"))
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        display.ifEmpty { updateDisplay() }
        if (display.isNotEmpty()) {
            config.position.renderRenderables(
                listOf(Renderable.verticalContainer(display, 2)),
                posLabel = "Gemstone Money per Hour Display",
            )
        }
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled() || lastMined.isFarPast()) display = listOf()
        else if (lastMined.passedSince() > config.timeoutTime.toInt().seconds) {
            if (config.pauseEnabled) paused = true
            else reset()
        } else uptime += 1.seconds
        display = createDisplay()
    }

    @HandleEvent
    fun onWorldChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.NONE || !paused) return
        if (!isEnabled() || !LorenzUtils.inMiningIsland()) return reset()
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

    private fun command() {
        reset()
        ChatUtils.chat("Reset gemstone money per hour display!")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetgemstone") {
            description = "Resets the gemstone money per hour display."
            category = CommandCategory.USERS_ACTIVE
            callback { command() }
        }
    }

    private fun isEnabled() = config.enabled
}
