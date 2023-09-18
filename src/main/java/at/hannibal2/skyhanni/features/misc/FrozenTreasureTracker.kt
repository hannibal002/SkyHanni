package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
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
    private val config get() = SkyHanniMod.feature.event.winter.frozenTreasureTracker
    private var display = emptyList<List<Any>>()
    private var estimatedIce = 0L
    private var lastEstimatedIce = 0L
    private val icePerMin = mutableListOf<Long>()
    private var icePerHour = 0
    private var stoppedChecks = 0
    private var compactPattern = "COMPACT! You found an Enchanted Ice!".toPattern()

    init {
        fixedRateTimer(name = "skyhanni-dungeon-milestone-display", period = 1000) {
            if (!onJerryWorkshop()) return@fixedRateTimer
            calculateIcePerHour()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        icePerHour = 0
        stoppedChecks = 0
        icePerMin.clear()
        saveAndUpdate()
    }

    private fun calculateIcePerHour() {
        val difference = estimatedIce - lastEstimatedIce
        lastEstimatedIce = estimatedIce

        if (difference == estimatedIce) {
            return
        }

        icePerHour = icePerMin.average().toInt() * 3600
        icePerMin.add(difference)

        if (difference == 0L) {
            stoppedChecks += 1
            if (stoppedChecks == 60) {
                stoppedChecks = 0
                icePerMin.clear()
                icePerHour = 0
            }
            return
        }
        stoppedChecks = 0
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
        val storage = ProfileStorageData.profileSpecific?.frozenTreasureTracker ?: return

        compactPattern.matchMatcher(message) {
            storage.compactProcs += 1
            saveAndUpdate()
            if (config.hideMessages) event.blockedReason = "frozen treasure tracker"
        }

        for (treasure in FrozenTreasure.entries) {
            if ("FROZEN TREASURE! You found ${treasure.displayName.removeColor()}!".toRegex().matches(message)) {
                storage.treasuresMined += 1
                val old = storage.treasureCount[treasure] ?: 0
                storage.treasureCount = storage.treasureCount.editCopy { this[treasure] = old + 1 }
                saveAndUpdate()
                if (config.hideMessages) event.blockedReason = "frozen treasure tracker"
            }
        }
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
    }

    private fun drawTreasureDisplay(storage: Storage.ProfileSpecific.FrozenTreasureTracker) = buildList<List<Any>> {
        addAsSingletonList("§1§lFrozen Treasure Tracker")
        addAsSingletonList("§6${formatNumber(storage.treasuresMined)} Treasures Mined")
        addAsSingletonList("§3${formatNumber(estimatedIce)} Total Ice")
        addAsSingletonList("§3${formatNumber(icePerHour)} Ice/hr")
        addAsSingletonList("§8${formatNumber(storage.treasuresMined)} Compact Procs")
        addAsSingletonList("")

        for (treasure in FrozenTreasure.entries) {
            val count = (storage.treasureCount[treasure] ?: 0) * if (config.showAsDrops) treasure.defaultAmount else 1
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
        val storage = ProfileStorageData.profileSpecific?.frozenTreasureTracker ?: return
        calculateIce(storage)
        display = formatDisplay(drawTreasureDisplay(storage))
    }

    private fun calculateIce(storage: Storage.ProfileSpecific.FrozenTreasureTracker) {
        estimatedIce = 0
        estimatedIce += storage.compactProcs * 160
        for (treasure in FrozenTreasure.entries) {
            val amount = storage.treasureCount[treasure] ?: 0
            estimatedIce += amount * treasure.defaultAmount * treasure.iceMultiplier
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (!onJerryWorkshop()) return
        if (config.onlyInCave && !inGlacialCave()) return
        config.position.renderStringsAndItems(display, posLabel = "Frozen Treasure Tracker")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.frozenTreasureTracker", "event.winter.frozenTreasureTracker")
    }

    private fun onJerryWorkshop() = LorenzUtils.inIsland(IslandType.WINTER)

    private fun inGlacialCave() = onJerryWorkshop() && ScoreboardData.sidebarLinesFormatted.contains(" §7⏣ §3Glacial Cave")
}