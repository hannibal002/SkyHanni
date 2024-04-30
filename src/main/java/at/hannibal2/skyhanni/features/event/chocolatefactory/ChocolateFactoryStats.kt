package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import com.google.gson.JsonPrimitive
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ChocolateFactoryStats {

    private val config get() = ChocolateFactoryAPI.config
    private val profileStorage get() = ChocolateFactoryAPI.profileStorage

    private var display = listOf<Renderable>()

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!ChocolateFactoryAPI.chocolateFactoryPaused) return
        updateDisplay()
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory && !ChocolateFactoryAPI.chocolateFactoryPaused) return
        if (!config.statsDisplay) return

        config.position.renderRenderables(display, posLabel = "Chocolate Factory Stats")
    }

    fun updateDisplay() {
        val profileStorage = profileStorage ?: return

        val perSecond = ChocolateFactoryAPI.chocolatePerSecond
        val perMinute = perSecond * 60
        val perHour = perMinute * 60
        val perDay = perHour * 24
        val position = ChocolateFactoryAPI.leaderboardPosition?.addSeparators() ?: "???"
        val percentile = ChocolateFactoryAPI.leaderboardPercentile?.let { "§7Top §a$it%" } ?: ""
        val timeTowerInfo = if (ChocolateFactoryTimeTowerManager.timeTowerActive()) {
            "§d§lActive"
        } else {
            "§6${ChocolateFactoryTimeTowerManager.timeTowerCharges()}"
        }

        val prestigeEstimate = ChocolateAmount.PRESTIGE.formattedTimeUntilGoal(ChocolateFactoryAPI.chocolateForPrestige)

        val text = formatList(buildList {
            add("§6§lChocolate Factory Stats")

            add("§eCurrent Chocolate: §6${ChocolateAmount.CURRENT.formatted}")
            add("§eThis Prestige: §6${ChocolateAmount.PRESTIGE.formatted}")
            add("§eAll-time: §6${ChocolateAmount.ALL_TIME.formatted}")

            add("§ePer Second: §6${perSecond.addSeparators()}")
            add("§ePer Minute: §6${perMinute.addSeparators()}")
            add("§ePer Hour: §6${perHour.addSeparators()}")
            add("§ePer Day: §6${perDay.addSeparators()}")

            add("§eChocolate Multiplier: §6${profileStorage.chocolateMultiplier}")
            add("§eBarn: §6${ChocolateFactoryBarnManager.barnStatus()}")

            add("§ePosition: §7#§b$position $percentile")

            add("")
            add("")
            add("")

            add("§eTime Tower: §6$timeTowerInfo")
            add("§eTime To Prestige: $prestigeEstimate")
            add("§eRaw Per Second: §6${profileStorage.rawChocPerSecond.addSeparators()}")
        })

        display = listOf(Renderable.clickAndHover(
            Renderable.verticalContainer(text.map(Renderable::string)),
            tips = listOf("§bCopy to Clipboard!"),
            onClick = {
                val list = text.toMutableList()
                val titleHeader = list.indexOf("§6§lChocolate Factory Stats")
                if (titleHeader != -1) {
                    list[titleHeader] = "${LorenzUtils.getPlayerName()}'s Chocolate Factory Stats"
                } else {
                    list.add(0, "${LorenzUtils.getPlayerName()}'s Chocolate Factory Stats")
                }
                ClipboardUtils.copyToClipboard(list.joinToString("\n") { it.removeColor() })
            }
        ))
    }

    private fun formatList(list: List<String>): List<String> {
        return config.statsDisplayList
            .filter { it.shouldDisplay() }
            .map { list[it.ordinal] }
            .toMutableList()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(42, "event.chocolateFactory.statsDisplayList") { element ->
            val jsonArray = element.asJsonArray

            jsonArray.add(JsonPrimitive("TIME_TOWER"))
            jsonArray.add(JsonPrimitive("TIME_TO_PRESTIGE"))

            jsonArray
        }
    }

    enum class ChocolateFactoryStat(private val display: String, val shouldDisplay: () -> Boolean = { true }) {
        HEADER("§6§lChocolate Factory Stats"),
        CURRENT("§eCurrent Chocolate: §65,272,230"),
        THIS_PRESTIGE("§eThis Prestige: §6483,023,853", { ChocolateFactoryAPI.currentPrestige != 1 }),
        ALL_TIME("§eAll-time: §6641,119,115"),
        PER_SECOND("§ePer Second: §63,780.72"),
        PER_MINUTE("§ePer Minute: §6226,843.2"),
        PER_HOUR("§ePer Hour: §613,610,592"),
        PER_DAY("§ePer Day: §6326,654,208"),
        MULTIPLIER("§eChocolate Multiplier: §61.77"),
        BARN("§eBarn: §6171/190 Rabbits"),
        LEADERBOARD_POS("§ePosition: §7#§b103 §7Top §a0.87%"),
        EMPTY(""),
        EMPTY_2(""),
        EMPTY_3(""),
        TIME_TOWER("§eTime Tower: §62/3 Charges", { ChocolateFactoryTimeTowerManager.currentCharges() != -1 }),
        TIME_TO_PRESTIGE("§eTime To Prestige: §61d 13h 59m 4s", { ChocolateFactoryAPI.currentPrestige != 5 }),
        RAW_PER_SECOND("§eRaw Per Second: §62,136"),
        ;

        override fun toString(): String {
            return display
        }
    }
}
