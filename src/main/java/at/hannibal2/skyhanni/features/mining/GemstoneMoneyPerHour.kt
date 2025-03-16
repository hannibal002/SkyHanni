package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.mining.GemstoneMoneyPerHourConfig
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.math.pow
import kotlin.time.Duration.Companion.convert
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GemstoneMoneyPerHour {

    /**
     * REGEX-TEST: §d§lPRISTINE! §r§fYou found §r§a☘ Flawed Jade Gemstone §r§8x20§r§f!
     */
    private val pristineMessagePattern by RepoPattern.pattern(
        "mining.pristine",
        "§d§lPRISTINE! §r§fYou found .* Flawed (?<gemstone>\\w+) Gemstone .*x(?<amount>\\d+).*!"
    )

    private val config get() = SkyHanniMod.feature.mining.gemstoneMoneyPerHour

    private var display: List<Renderable> = listOf()
    private var start = SimpleTimeMark.farFuture()
    private var lastMined = SimpleTimeMark.farFuture()
    private var coins = 0
    private var lastGemstone: String = ""

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        pristineMessagePattern.matchMatcher(event.message) {
            if (start.isFarFuture()) start = SimpleTimeMark.now()
            lastMined = SimpleTimeMark.now()
            val gemstone = group("gemstone")
            lastGemstone = gemstone
            val configGemstonePrice = getPrice(convertToInternalName(lastGemstone) ?: return)
            val delta = group("amount").toDouble() * getFraction() * configGemstonePrice
            coins += delta.toInt()
        }
    }

    private fun convertToInternalName(name: String): NeuInternalName {
        return "${config.gemstoneType.displayName}_${name}_GEM".toInternalName()
    }

    private fun getPrice(gemstone: NeuInternalName): Double {
        return if (config.forceNPC) gemstone.getNpcPrice()
        else maxOf(gemstone.getNpcPrice(), gemstone.getPrice())
    }

    private fun getFraction(): Double {
        return (80.0).pow(2 - toNum(config.gemstoneType))
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
        if (start.isFarFuture()) return@buildList
        if (lastGemstone.isEmpty()) return@buildList
        val uptime = start.passedSince()
        val moneyPerHour = coins / maxOf(uptime.inPartialSeconds, 1.0) * 3600
        val internalName = convertToInternalName(lastGemstone)
        val gemstoneName = internalName.itemNameWithoutColor
        val gemstonePrice = getPrice(internalName)

        add(Renderable.string("§d§lGemstone Coins/h"))
        add(Renderable.string("§a($gemstoneName @ §b${gemstonePrice.addSeparators()})"))
        add(Renderable.string("§a$/hr: §b${moneyPerHour.toInt().shortFormat()}"))
        add(Renderable.string("§a$ made: ${coins.shortFormat()}"))
        add(Renderable.string("§bUptime: ${uptime.format()}"))
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        display.ifEmpty { updateDisplay() }
        if (display.isNotEmpty()) {
            config.position.renderRenderables(
                listOf(Renderable.verticalContainer(display, 2)),
                posLabel = "Gemstone Coins/h Display",
            )
        }
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) display = listOf()
        if (lastMined.passedSince() > 10.seconds) reset()
        display = createDisplay()
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        reset()
    }

    private fun reset() {
        start = SimpleTimeMark.farFuture()
        lastMined = SimpleTimeMark.farFuture()
        coins = 0
    }

    private fun isEnabled() = config.enabled
}
