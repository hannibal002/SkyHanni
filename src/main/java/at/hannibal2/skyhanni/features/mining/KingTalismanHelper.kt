package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.test.SkyHanniTestCommand
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.TimeUtils
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.Collections

class KingTalismanHelper {
    private val config get() = SkyHanniMod.feature.mining

    private val kingLocation = LorenzVec(129.6, 196.5, 194.1)
    private val kingCircles = listOf(
        "Brammor",
        "Emkam",
        "Redros",
        "Erren",
        "Thormyr",
        "Emmor",
        "Grandan",
    )

    private var allKingsDisplay = emptyList<String>()
    private var farDisplay = ""
    private var display = emptyList<String>()

    fun isNearby() = LorenzUtils.skyBlockArea == "Royal Palace" && kingLocation.distanceToPlayer() < 10

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.isMod(20)) return

        if (!isEnabled()) {
            display = emptyList()
            return
        }

        update()
        display = if (isNearby()) allKingsDisplay else Collections.singletonList(farDisplay)
    }

    fun isEnabled() = config.kingTalismanHelper && IslandType.DWARVEN_MINES.isInIsland()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Commissions") return
        if (!isEnabled()) return
        if (!isNearby()) return
        val profileSpecific = ProfileStorageData.profileSpecific ?: return

        val currentKing = getCurrentKing()
        val kingsTalkedTo = profileSpecific.mining.kingsTalkedTo
        if (currentKing !in kingsTalkedTo) {
            LorenzUtils.debug("Found new king!")
            kingsTalkedTo.add(currentKing)
            update()
            display = allKingsDisplay
        }
    }

    private fun update() {
        val profileSpecific = ProfileStorageData.profileSpecific ?: return
        val kingsTalkedTo = profileSpecific.mining.kingsTalkedTo
        if (kingsTalkedTo.size == kingCircles.size) {
            allKingsDisplay = Collections.singletonList("§eAll Kings found.")
            farDisplay = ""
            return
        }

        allKingsDisplay = buildList {
            var farDisplay_: String? = null

            val currentKing = getCurrentKing()
            for ((king, timeUntil) in getKingTimes()) {
                val missing = king !in kingsTalkedTo
                val missingString = if (missing) "" else " §aDone"

                val current = king == currentKing

                val missingTimeFormat = if (current) {
                    val time = TimeUtils.formatDuration(timeUntil - 1000 * 60 * 20 * (kingCircles.size - 1))
                    "§7(§b$time remaining§7)"
                } else {
                    val time = TimeUtils.formatDuration(timeUntil, maxUnits = 2)
                    "§7(§bin $time§7)"
                }

                val currentString = if (current) "§6King " else ""
                if (missing && current) {
                    farDisplay_ = "§cNext missing king: §7$king §eNow $missingTimeFormat"
                }

                val timeString = if (missing) " §cMissing $missingTimeFormat" else ""

                add("§7$currentString$king$missingString$timeString")
            }
            farDisplay = farDisplay_ ?: nextMissingText()
        }
    }

    private fun nextMissingText(): String {
        val profileSpecific = ProfileStorageData.profileSpecific ?: error("profileSpecific is null")
        val kingsTalkedTo = profileSpecific.mining.kingsTalkedTo
        val (nextKing, until) = getKingTimes().filter { it.key !in kingsTalkedTo }.sorted().firstNotNullOf { it }
        val time = TimeUtils.formatDuration(until, maxUnits = 2)

        return "§cNext missing king: §7$nextKing §7(§bin $time§7)"
    }

    private fun getKingTimes(): MutableMap<String, Long> {
        val currentTimeMillis = System.currentTimeMillis() + (SkyHanniTestCommand.a * 1000 * 60).toLong()
        val oneSbDay = 1000 * 60 * 20
        val oneCircleTime = oneSbDay * kingCircles.size
        val kingTime = mutableMapOf<String, Long>()
        for ((index, king) in kingCircles.withIndex()) {

            val startTime = SkyBlockTime(day = -kingCircles.size + index + 1)
            var timeNext = startTime.toMillis()
            while (timeNext < currentTimeMillis) {
                timeNext += oneCircleTime
            }
            val timeUntil = timeNext - currentTimeMillis
            kingTime[king] = timeUntil
        }
        return kingTime
    }

    private fun getCurrentKing() = getKingTimes().sortedDesc().firstNotNullOf { it.key }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!config.kingTalismanHelper) return

        config.kingTalismanHelperPos.renderStrings(display, posLabel = "King Talisman Helper")
    }
}
