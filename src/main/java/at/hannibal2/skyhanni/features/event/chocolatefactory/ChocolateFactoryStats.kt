package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ChocolateFactoryStats {

    private val config get() = ChocolateFactoryAPI.config

    private var displayList = listOf<String>()

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        if (!config.statsDisplay) return

        config.position.renderStrings(displayList, posLabel = "Chocolate Factory Stats")
    }

    fun updateDisplay() {
        val perSecond = ChocolateFactoryAPI.chocolatePerSecond
        val perMinute = perSecond * 60
        val perHour = perMinute * 60
        val perDay = perHour * 24
        val position = ChocolateFactoryAPI.leaderboardPosition?.addSeparators() ?: "???"
        val timeTowerInfo = if (ChocolateFactoryAPI.timeTowerActive) {
            "§d§lActive"
        } else {
            "§6${ChocolateFactoryTimeTowerManager.timeTowerCharges()}"
        }

        displayList = formatList(buildList {
            add("§6§lChocolate Factory Stats")

            add("§eCurrent Chocolate: §6${ChocolateFactoryAPI.chocolateCurrent.addSeparators()}")
            add("§eThis Prestige: §6${ChocolateFactoryAPI.chocolateThisPrestige.addSeparators()}")
            add("§eAll-time: §6${ChocolateFactoryAPI.chocolateAllTime.addSeparators()}")

            add("§ePer Second: §6${perSecond.addSeparators()}")
            add("§ePer Minute: §6${perMinute.addSeparators()}")
            add("§ePer Hour: §6${perHour.addSeparators()}")
            add("§ePer Day: §6${perDay.addSeparators()}")

            add("§eChocolate Multiplier: §6${ChocolateFactoryAPI.chocolateMultiplier}")
            add("§eBarn: §6${ChocolateFactoryBarnManager.barnStatus()}")

            add("§ePosition: §7#§b$position")

            add("")
            add("")
            add("")

            add("§eTime Tower: §6$timeTowerInfo")
        })
    }

    private fun formatList(list: List<String>): List<String> {
        return config.statsDisplayList
            .filter { it.shouldDisplay() }
            .map { list[it.ordinal] }
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
        LEADERBOARD_POS("§ePosition: §7#§b103"),
        EMPTY(""),
        EMPTY_2(""),
        EMPTY_3(""),
        TIME_TOWER("§eTime Tower: §62/3 Charges", { ChocolateFactoryTimeTowerManager.currentCharges() != -1 }),
        ;

        override fun toString(): String {
            return display
        }
    }
}
