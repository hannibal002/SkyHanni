package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ChocolateFactoryStats {

    private val config get() = ChocolateFactoryApi.config

    private var displayList = listOf<String>()

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!ChocolateFactoryApi.inChocolateFactory) return
        if (!config.statsDisplay) return

        config.position.renderStrings(displayList, posLabel = "Chocolate Factory Stats")
    }

    fun updateDisplay() {
        val newList = mutableListOf<String>()
        val perSecond = ChocolateFactoryApi.chocolatePerSecond
        val perMinute = perSecond * 60
        val perHour = perMinute * 60
        val perDay = perHour * 24
        val position = ChocolateFactoryApi.leaderboardPosition?.addSeparators() ?: "???"

        newList.add("§6§lChocolate Factory Stats")

        newList.add("§eCurrent Chocolate: §6${ChocolateFactoryApi.chocolateCurrent.addSeparators()}")
        newList.add("§eThis Prestige: §6${ChocolateFactoryApi.chocolateThisPrestige.addSeparators()}")
        newList.add("§eAll-time: §6${ChocolateFactoryApi.chocolateAllTime.addSeparators()}")

        newList.add("§ePer Second: §6${perSecond.addSeparators()}")
        newList.add("§ePer Minute: §6${perMinute.addSeparators()}")
        newList.add("§ePer Hour: §6${perHour.addSeparators()}")
        newList.add("§ePer Day: §6${perDay.addSeparators()}")

        newList.add("§eChocolate Multiplier: §6${ChocolateFactoryApi.chocolateMultiplier}")
        newList.add("§eBarn: §6${ChocolateFactoryBarnManager.barnStatus()}")

        newList.add("§ePosition: §7#§b$position")

        newList.add("")
        newList.add("")
        newList.add("")

        displayList = formatList(newList)
    }

    private fun formatList(list: MutableList<String>): List<String> {
        val newList = mutableListOf<String>()
        for (index in config.statsDisplayList) {
            newList.add(list[index.ordinal])
        }

        return newList
    }

    enum class ChocolateFactoryStatsType(val display: String) {
        HEADER("§6§lChocolate Factory Stats"),
        CURRENT("§eCurrent Chocolate: §65,272,230"),
        THIS_PRESTIGE("§eThis Prestige: §6483,023,853"),
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
        ;

        override fun toString(): String {
            return display
        }
    }
}
