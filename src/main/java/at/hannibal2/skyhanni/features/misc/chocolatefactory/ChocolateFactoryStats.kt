package at.hannibal2.skyhanni.features.misc.chocolatefactory

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ChocolateFactoryStats {

    private val config get() = ChocolateFactoryApi.config

    private var displayList = listOf<String>()

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!ChocolateFactoryApi.isEnabled()) return
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

        newList.add("§6§lChocolate Factory Stats")

        newList.add("§eCurrent Chocolate: §7${ChocolateFactoryApi.chocolateCurrent.addSeparators()}")
        newList.add("§eThis Prestige: §7${ChocolateFactoryApi.chocolateThisPrestige.addSeparators()}")
        newList.add("§eAll-time: §7${ChocolateFactoryApi.chocolateAllTime.addSeparators()}")

        newList.add("§ePer Second: §7${perSecond.addSeparators()}")
        newList.add("§ePer Minute: §7${perMinute.addSeparators()}")
        newList.add("§ePer Hour: §7${perHour.addSeparators()}")
        newList.add("§ePer Day: §7${perDay.addSeparators()}")

        newList.add("§eChocolate Multiplier: §7${ChocolateFactoryApi.chocolateMultiplier}")
        newList.add("§eBarn: §7${ChocolateFactoryBarnManager.barnStatus()}")

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
        HEADER("§eCurrent Chocolate: §7${ChocolateFactoryApi.chocolateCurrent.addSeparators()}"),
        CURRENT("Chocolate"),
        THIS_PRESTIGE("Chocolate this Prestige"),
        ALL_TIME("All-time Chocolate"),
        PER_SECOND("Chocolate Per Second"),
        PER_MINUTE("Chocolate Per Minute"),
        PER_HOUR("Chocolate Per Hour"),
        PER_DAY("Chocolate Per Day"),
        MULTIPLIER("Chocolate Multiplier"),
        BARN("Barn: 87/90 Rabbits"),
        EMPTY(""),
        EMPTY_2(""),
        EMPTY_3(""),
        ;

        override fun toString(): String {
            return display
        }
    }
}
