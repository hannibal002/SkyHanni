package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.concurrent.fixedRateTimer

class FrozenTreasureTracker {
    private val config get() = SkyHanniMod.feature.misc.frozenTreasureTracker
    private var display = listOf<List<Any>>()
    private var treasuresMined = 0
    private var compactProcs = 0
    private var estimatedIce = 0L
    private var lastEstimatedIce = 0L
    private val icePerMin = mutableListOf<Long>()
    private var icePerHour = 0
    private var missedMinutes = 0
    private var compactPattern = "COMPACT! You found an Enchanted Ice!".toPattern()

    private var treasureCount = mapOf<FrozenTreasures, Int>()

    init {
        fixedRateTimer(name = "skyhanni-dungeon-milestone-display", period = 60_000) {
            if (!onJerryWorkshop()) return@fixedRateTimer
            calculateIcePerHour()
        }
    }

    private fun calculateIcePerHour() {
        val difference = estimatedIce - lastEstimatedIce
        if (difference == 0L) {
            missedMinutes += 1
            if (missedMinutes == 3) {
                missedMinutes = 0
                icePerMin.clear()
                icePerHour = 0
            }
            return
        }
        missedMinutes = 0
        icePerMin.add(difference)
        icePerHour = icePerMin.average().toInt()
    }

    private fun formatDisplay(map: List<List<Any>>): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat) {
            newList.add(map[index])
        }
        return newList
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!ProfileStorageData.loaded) return
        if (!onJerryWorkshop()) return

        val message = event.message.removeColor().trim()

        compactPattern.matchMatcher(message) {
            compactProcs += 1
            saveAndUpdate()
            if (config.hideMessages) event.isCanceled
        }

        for (treasure in FrozenTreasures.values()) {
            if ("FROZEN TREASURE! You found ${treasure.displayName}!".toRegex().matches(message)) {
                val old = treasureCount[treasure] ?: 0
                treasureCount = treasureCount.editCopy { this[treasure] = old + 1 }
                saveAndUpdate()
                if (config.hideMessages) event.isCanceled
            }
        }
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val hidden = ProfileStorageData.profileSpecific?.frozenTreasureTracker ?: return
        treasuresMined = hidden.treasuresMined
        compactProcs = hidden.compactProcs
        treasureCount = hidden.treasureCount
        saveAndUpdate()
    }

    private fun drawTreasureDisplay() = buildList<List<Any>> {
        addAsSingletonList("§e§lFrozen Treasure Tracker")
        addAsSingletonList("§e${formatNumber(treasuresMined)} Treasures Mined")
        addAsSingletonList("§e${formatNumber(estimatedIce)} Total Ice")
        addAsSingletonList("§e${formatNumber(icePerHour)} Ice/hr")
        addAsSingletonList("§e${formatNumber(compactProcs)} Compact Procs")
        addAsSingletonList("")

        for (treasure in FrozenTreasures.values()) {
            val count = treasureCount[treasure] ?: 0
            addAsSingletonList("§b${formatNumber(count)} ${treasure.displayName}")
        }
        addAsSingletonList("")
    }

    fun formatNumber(amount: Number): String {
        if (amount is Int) return amount.addSeparators()
        if (amount is Long) return NumberUtil.format(amount)
        return "$amount"
    }
    private fun saveAndUpdate() {
        val hidden = ProfileStorageData.profileSpecific?.frozenTreasureTracker ?: return
        hidden.treasuresMined = treasuresMined
        hidden.compactProcs = compactProcs
        hidden.treasureCount = treasureCount
        calculateIce()
        display = formatDisplay(drawTreasureDisplay())
    }

    private fun calculateIce() {
        estimatedIce = 0
        if (config.countCompact) estimatedIce += compactProcs * 160
        for (treasure in FrozenTreasures.values()) {
            val amount = treasureCount[treasure] ?: 0
            estimatedIce += amount * treasure.defaultAmount * treasure.iceMultiplier
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!config.enabled) return
        if (!onJerryWorkshop()) return
        if (config.onlyInCave && !inGlacialCave()) return
        config.glacialDropPos.renderStringsAndItems(display, posLabel = "Visitor Stats")
    }

    private fun onJerryWorkshop() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.WINTER
    private fun inGlacialCave() = onJerryWorkshop() && ScoreboardData.sidebarLinesFormatted.contains("Glacial Cave")
}