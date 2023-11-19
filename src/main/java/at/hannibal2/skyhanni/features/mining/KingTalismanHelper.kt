package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUtils
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.Collections

class KingTalismanHelper {
    private val config get() = SkyHanniMod.feature.mining.kingTalisman

    companion object {
        private var currentOffset: Int? = null
        private var skyblockYear = 0

        private fun getCurrentOffset(): Int? {
            if (SkyBlockTime.now().year != skyblockYear) {
                return null
            }
            return currentOffset
        }

        fun kingFix() {
            currentOffset = null
            LorenzUtils.chat("Reset internal offset of King Talisman Helper.")
        }
    }

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

    private fun isNearby() = IslandType.DWARVEN_MINES.isInIsland() && LorenzUtils.skyBlockArea == "Royal Palace" &&
        kingLocation.distanceToPlayer() < 10

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.isMod(20)) return
        if (!isEnabled()) return
        val profileSpecific = ProfileStorageData.profileSpecific ?: return

        val nearby = isNearby()
        if (nearby && getCurrentOffset() == null) {
            checkOffset()
        }

        val kingsTalkedTo = profileSpecific.mining.kingsTalkedTo
        if (getCurrentOffset() == null) {
            val allKings = kingsTalkedTo.size == kingCircles.size
            display = if (allKings) emptyList() else listOf("§cVisit the king to sync up.")
            return
        }

        update(kingsTalkedTo)
        display = if (nearby) allKingsDisplay else Collections.singletonList(farDisplay)
    }

    private fun checkOffset() {
        val king = EntityUtils.getEntitiesNearby<EntityArmorStand>(LorenzVec(129.6, 196.0, 196.7), 2.0)
            .filter { it.name.startsWith("§6§lKing ") }.first()
        val foundKing = "§6§lKing (?<name>.*)".toPattern().matchMatcher(king.name) {
            group("name")
        } ?: return

        val currentId = kingCircles.indexOf(getCurrentKing())
        val foundId = kingCircles.indexOf(foundKing)
        currentOffset = currentId - foundId
        skyblockYear = SkyBlockTime.now().year
    }

    fun isEnabled() = config.enabled && LorenzUtils.inSkyBlock
        && (IslandType.DWARVEN_MINES.isInIsland() || config.outsideMines)

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Commissions") return
        if (!isEnabled()) return
        if (getCurrentOffset() == null) return
        if (!isNearby()) return
        val profileSpecific = ProfileStorageData.profileSpecific ?: return

        val currentKing = getCurrentKing()
        val kingsTalkedTo = profileSpecific.mining.kingsTalkedTo
        if (currentKing !in kingsTalkedTo) {
            LorenzUtils.debug("Found new king!")
            kingsTalkedTo.add(currentKing)
            update(kingsTalkedTo)
            display = allKingsDisplay
        }
    }

    private fun update(kingsTalkedTo: MutableList<String>) {
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
        val currentOffset = getCurrentOffset() ?: 0
        val oneSbDay = 1000 * 60 * 20
        val oneCircleTime = oneSbDay * kingCircles.size
        val kingTime = mutableMapOf<String, Long>()
        for ((index, king) in kingCircles.withIndex()) {
//             val startTime = SkyBlockTime(day = index + 2 - kingCircles.size)
//             val startTime = SkyBlockTime(day = index - kingCircles.size)
            val startTime = SkyBlockTime(day = index + currentOffset - kingCircles.size)
            var timeNext = startTime.toMillis()
            while (timeNext < System.currentTimeMillis()) {
                timeNext += oneCircleTime
            }
            val timeUntil = timeNext - System.currentTimeMillis()
            kingTime[king] = timeUntil
        }
        return kingTime
    }

    private fun getCurrentKing() = getKingTimes().sortedDesc().firstNotNullOf { it.key }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.position.renderStrings(display, posLabel = "King Talisman Helper")
    }
}
