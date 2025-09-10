package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MiningApi
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sorted
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.Collections
import kotlin.collections.buildList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object KingTalismanHelper {

    private val config get() = SkyHanniMod.feature.mining.kingTalisman
    private val storage get() = ProfileStorageData.profileSpecific?.mining

    private val patternGroup = RepoPattern.group("mining.kingtalisman")

    /**
     * REGEX-TEST: §7You have received a §r§fKing Talisman§r§7!
     */
    private val talismanPattern by patternGroup.pattern(
        "talisman",
        "§7You have received a §r§fKing Talisman§r§7!",
    )

    private fun resetKings() {
        storage?.kingsTalkedTo = mutableListOf<String>()
        update()
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

    private fun isNearby() = IslandType.DWARVEN_MINES.isCurrent() &&
        SkyBlockUtils.graphArea == "Royal Palace" &&
        kingLocation.distanceToPlayer() < 10

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        update()
        display = if (isNearby()) allKingsDisplay else Collections.singletonList(farDisplay)
    }


    fun isEnabled() = config.enabled &&
        SkyBlockUtils.inSkyBlock &&
        (IslandType.DWARVEN_MINES.isCurrent() || config.outsideMines)

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Commissions") return
        if (!isEnabled()) return
        if (!isNearby()) return
        val storage = storage ?: return

        val currentKing = getCurrentKing()
        val kingsTalkedTo = storage.kingsTalkedTo
        if (currentKing !in kingsTalkedTo) {
            kingsTalkedTo.add(currentKing)
            update()
            display = allKingsDisplay
        }
    }

    private fun update() {
        val kingsTalkedTo = storage?.kingsTalkedTo ?: return
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
                    val changedTime = timeUntil.inWholeMilliseconds - 1000 * 60 * 20 * (kingCircles.size - 1)
                    val time = changedTime.milliseconds.format(maxUnits = 2)
                    "§7(§b$time remaining§7)"
                } else {
                    val time = timeUntil.format(maxUnits = 2)
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
        val storage = storage ?: error("profileSpecific is null")
        val kingsTalkedTo = storage.kingsTalkedTo
        val (nextKing, until) = getKingTimes().filter { it.key !in kingsTalkedTo }.sorted().firstNotNullOf { it }
        val time = until.format(maxUnits = 2)

        return "§cNext missing king: §7$nextKing §7(§bin $time§7)"
    }

    private fun getKingTimes(): Map<String, Duration> {
        val sbTime = SkyBlockTime.now()
        val kingIndex = (sbTime.day + 5) % kingCircles.size
        val absDay = (sbTime.year - 1) * 372 + (sbTime.month - 1) * 31 + sbTime.day
        val startTime = SkyBlockTime.fromAbsoluteDay(absDay - kingIndex)
        val oneSBDay = 20.minutes
        val oneCircleTime = oneSBDay * kingCircles.size

        return kingCircles.mapIndexed { index, king ->
            var timeNext = (startTime + oneSBDay * index).toTimeMark().timeUntil()
            while (timeNext.isNegative()) {
                timeNext += oneCircleTime
            }
            king to timeNext
        }.toMap()
    }

    private fun getCurrentKing() = getKingTimes().sortedDesc().firstNotNullOf { it.key }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.position.renderStrings(display, posLabel = "King Talisman Helper")
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        if (!MiningApi.inDwarvenMines) return

        if (talismanPattern.matches(event.message)) {
            storage?.kingsTalkedTo = kingCircles.toMutableList()
            update()
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetkinghelper") {
            description = "Resets the King Talisman Helper"
            category = CommandCategory.USERS_RESET
            callback { resetKings() }
        }
    }
}
