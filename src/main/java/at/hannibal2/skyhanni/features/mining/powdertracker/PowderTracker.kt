package at.hannibal2.skyhanni.features.mining.powdertracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.HotmApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MiningEventsApi
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.mining.PowderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PowderTracker {

    private val config get() = SkyHanniMod.feature.mining.powderTracker

    private val patternGroup = RepoPattern.group("mining.powder.tracker")
    private val pickedPattern by patternGroup.pattern(
        "picked",
        "  §r§6§lCHEST LOCKPICKED ",
    )

    /**
     * REGEX-TEST: §b§lCOMPACT! §r§fYou found an §r§aEnchanted Hard Stone§r§f!
     */
    private val compactedPattern by patternGroup.pattern(
        "compacted",
        "§b§lCOMPACT! §r§fYou found an §r§aEnchanted Hard Stone§r§f!",
    )

    private var lastChestPicked = SimpleTimeMark.farPast()
    private var isGrinding = false
    private val gemstoneInfo = ResourceInfo(0L, 0L, 0, 0.0, mutableListOf())
    private val mithrilInfo = ResourceInfo(0L, 0L, 0, 0.0, mutableListOf())
    private val diamondEssenceInfo = ResourceInfo(0L, 0L, 0, 0.0, mutableListOf())
    private val goldEssenceInfo = ResourceInfo(0L, 0L, 0, 0.0, mutableListOf())
    private val chestInfo = ResourceInfo(0L, 0L, 0, 0.0, mutableListOf())
    private val hardStoneInfo = ResourceInfo(0L, 0L, 0, 0.0, mutableListOf())
    private val gemstones = listOf(
        "Ruby" to "§c",
        "Sapphire" to "§b",
        "Amber" to "§6",
        "Amethyst" to "§5",
        "Jade" to "§a",
        "Topaz" to "§e",
    )

    init {
        PowderChestReward.entries.forEach { it.chatPattern }
    }

    @HandleEvent
    fun onSecondPassed() {
        if (!isEnabled()) return
        calculateResourceHour(gemstoneInfo)
        calculateResourceHour(mithrilInfo)
        calculateResourceHour(diamondEssenceInfo)
        calculateResourceHour(goldEssenceInfo)
        calculateResourceHour(chestInfo)
        calculateResourceHour(hardStoneInfo)

        if (lastChestPicked.passedSince() > 1.minutes) {
            isGrinding = false
        }
        val event = MiningEventsApi.getActiveEvent(MiningEventsApi.MiningEventType.DOUBLE_POWDER)

        if (event != null) {
            tracker.update()
        }
    }

    private val tracker =
        SkyHanniTracker(
            "Powder Tracker",
            ::Data,
            { it.powderTracker },
            trackerConfig = { config.perTrackerConfig }
        ) { formatDisplay(drawDisplay(it)) }

    data class Data(
        @Expose var totalChestPicked: Long = 0,
        @Expose var totalHardStoneCompacted: Long = 0,
        // TODO remove this field and transform this into a ItemProfitTracker
        @Expose var rewards: MutableMap<PowderChestReward, Long> = mutableMapOf(),
    ) : TrackerData()

    init {
        tracker.initRenderer({ config.position }) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean {
        if (!isEnabled()) return false
        if (config.onlyWhenPowderGrinding && !isGrinding) return false

        return true
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        val msg = event.message

        pickedPattern.matchMatcher(msg) {
            tracker.modify {
                it.totalChestPicked += 1
            }
            isGrinding = true
            lastChestPicked = SimpleTimeMark.now()
        }

        compactedPattern.matchMatcher(msg) {
            tracker.modify {
                it.totalHardStoneCompacted += 1
            }
        }

        for (reward in PowderChestReward.entries) {
            if (reward == PowderChestReward.GEMSTONE_POWDER) continue
            reward.chatPattern.matchMatcher(msg) {
                tracker.modify {
                    val count = it.rewards[reward] ?: 0
                    val amount = groupOrNull("amount")?.formatLong() ?: 1
                    it.rewards[reward] = count + amount
                }
            }
        }
        tracker.update()
    }

    @HandleEvent(onlyOnIsland = IslandType.CRYSTAL_HOLLOWS)
    fun onPowderGain(event: PowderEvent.Gain) {
        if (lastChestPicked.passedSince() > 5.seconds) return
        tracker.modify {
            val reward = when (event.powder) {
                HotmApi.PowderType.GEMSTONE -> PowderChestReward.GEMSTONE_POWDER
                else -> return@modify
            }
            it.rewards.addOrPut(reward, event.amount)
        }
    }

    @HandleEvent
    fun onConfigLoad() {
        config.textFormat.afterChange {
            tracker.update()
        }
    }

    @HandleEvent
    fun onWorldChange() {
        if (!isEnabled()) return
        gemstoneInfo.perHour = 0.0
        gemstoneInfo.stoppedChecks = 0
        gemstoneInfo.perMin.clear()
        mithrilInfo.perHour = 0.0
        mithrilInfo.stoppedChecks = 0
        mithrilInfo.perMin.clear()
        diamondEssenceInfo.perHour = 0.0
        diamondEssenceInfo.stoppedChecks = 0
        diamondEssenceInfo.perMin.clear()
        goldEssenceInfo.perHour = 0.0
        goldEssenceInfo.stoppedChecks = 0
        goldEssenceInfo.perMin.clear()
        chestInfo.perHour = 0.0
        chestInfo.stoppedChecks = 0
        chestInfo.perMin.clear()
        hardStoneInfo.perHour = 0.0
        hardStoneInfo.stoppedChecks = 0
        hardStoneInfo.perMin.clear()
        tracker.update()
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.powderTrackerConfig", "mining.powderTracker")
        event.transform(8, "#profile.powderTracker") { old -> old.asJsonObject.get("0") }

        event.transform(20, "mining.powderTracker.textFormat") { element ->
            val newList = JsonArray()
            for (entry in element.asJsonArray) {
                if (entry is JsonNull) continue
                if (entry.asString.let { it != "TITLE" && it != "DISPLAY_MODE" }) {
                    newList.add(entry)
                }
            }
            newList
        }

        event.transform(61, "mining.powderTracker.textFormat") { element ->
            val newList = JsonArray()
            for (entry in element.asJsonArray) {
                if (entry is JsonNull) continue
                if (entry.asString.let { it != "MITHRIL_POWDER" }) {
                    newList.add(entry)
                }
            }
            newList
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.CRYSTAL_HOLLOWS) {
            tracker.firstUpdate()
        }
    }

    private fun formatDisplay(map: List<Searchable>) = buildList {
        if (map.isEmpty()) return@buildList

        addSearchString("§b§lPowder Tracker")

        for (index in config.textFormat.get()) {
            // TODO, change functionality to use enum rather than ordinals
            add(map[index.ordinal])
        }
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        calculate(data, gemstoneInfo, PowderChestReward.GEMSTONE_POWDER)
        calculate(data, diamondEssenceInfo, PowderChestReward.DIAMOND_ESSENCE)
        calculate(data, goldEssenceInfo, PowderChestReward.GOLD_ESSENCE)
        calculateChest(data)
        calculateHardStone(data)

        val chestPerHour = format(chestInfo.perHour)
        addSearchString("§d${data.totalChestPicked.addSeparators()} Total Chests Picked §7($chestPerHour/h)")

        val event = MiningEventsApi.getActiveEvent(MiningEventsApi.MiningEventType.DOUBLE_POWDER)

        addSearchString(
            if (event != null)
                "§bDouble Powder: §aActive! §7(${event.timeLeft})"
            else
                "§bDouble Powder: §cInactive!"
        )

        val rewards = data.rewards
        addPerHour(rewards, PowderChestReward.GEMSTONE_POWDER, gemstoneInfo)
        addSearchString("")
        addPerHour(rewards, PowderChestReward.DIAMOND_ESSENCE, diamondEssenceInfo)
        addPerHour(rewards, PowderChestReward.GOLD_ESSENCE, goldEssenceInfo)
        addSearchString("")
        val hardStonePerHour = format(hardStoneInfo.perHour)
        addSearchString(
            "§b${data.totalHardStoneCompacted.addSeparators()} §fHard Stone §bCompacted §7($hardStonePerHour/h)", "Hard Stone"
        )

        addSearchString("")
        for ((gem, color) in gemstones) {
            var totalGemstone = 0L

            for (quality in arrayOf("ROUGH", "FLAWED", "FINE", "FLAWLESS")) {
                val gemstoneType = PowderChestReward.valueOf("${quality}_${gem.uppercase()}_GEMSTONE")
                val count = rewards.getOrDefault(gemstoneType, 0)
                val multiplier = when (quality) {
                    "FLAWED" -> 80
                    "FINE" -> 6400
                    "FLAWLESS" -> 512000
                    else -> 1
                }
                totalGemstone += count * multiplier
            }

            val (flawless, fine, flawed, rough) = convert(totalGemstone)
            addSearchString("§5$flawless§7-§9$fine§7-§a$flawed§f-$rough $color$gem Gemstone", "Gemstone")
        }

        var totalParts = 0L
        for (reward in listOf(
            PowderChestReward.FTX_3070,
            PowderChestReward.ELECTRON_TRANSIMTTER,
            PowderChestReward.ROBOTRON_REFLECTOR,
            PowderChestReward.SUPERLITE_MOTOR,
            PowderChestReward.CONTROL_SWITCH,
            PowderChestReward.SYNTHETIC_HEART,
        )) {
            val count = rewards.getOrDefault(reward, 0)
            totalParts += count
            val name = reward.displayName
            addSearchString("§b${count.addSeparators()} $name", name)
        }
        addSearchString("§b${totalParts.addSeparators()} §9Total Robot Parts")

        val goblinEgg = rewards.getOrDefault(PowderChestReward.GOBLIN_EGG, 0)
        val greenEgg = rewards.getOrDefault(PowderChestReward.GREEN_GOBLIN_EGG, 0)
        val redEgg = rewards.getOrDefault(PowderChestReward.RED_GOBLIN_EGG, 0)
        val yellowEgg = rewards.getOrDefault(PowderChestReward.YELLOW_GOBLIN_EGG, 0)
        val blueEgg = rewards.getOrDefault(PowderChestReward.BLUE_GOBLIN_EGG, 0)
        addSearchString("§3$blueEgg§7-§c$redEgg§7-§e$yellowEgg§f-§a$greenEgg§f-§9$goblinEgg §fGoblin Egg")

        for (reward in listOf(
            PowderChestReward.WISHING_COMPASS,
            PowderChestReward.SLUDGE_JUICE,
            PowderChestReward.ASCENSION_ROPE,
            PowderChestReward.TREASURITE,
            PowderChestReward.JUNGLE_HEART,
            PowderChestReward.PICKONIMBUS_2000,
            PowderChestReward.YOGGIE,
            PowderChestReward.PREHISTORIC_EGG,
            PowderChestReward.OIL_BARREL,
        )) {
            val count = rewards.getOrDefault(reward, 0).addSeparators()
            val name = reward.displayName
            addSearchString("§b$count $name", name)
        }
    }

    private fun MutableList<Searchable>.addPerHour(
        map: Map<PowderChestReward, Long>,
        reward: PowderChestReward,
        info: ResourceInfo,
    ) {
        val mithrilCount = map.getOrDefault(reward, 0).addSeparators()
        val mithrilPerHour = format(info.perHour)
        addSearchString("§b$mithrilCount ${reward.displayName} §7($mithrilPerHour/h)")
    }

    private fun format(e: Double): String = if (e < 0) "0" else e.toInt().addSeparators()

    private fun calculateResourceHour(resourceInfo: ResourceInfo) {
        val difference = resourceInfo.estimated - resourceInfo.lastEstimated
        resourceInfo.lastEstimated = resourceInfo.estimated

        if (difference == resourceInfo.estimated) {
            return
        }

        resourceInfo.perHour = resourceInfo.perMin.average() * 3600
        resourceInfo.perMin.add(difference)

        if (difference == 0L) {
            resourceInfo.stoppedChecks += 1

            if (resourceInfo.stoppedChecks == 60) {
                resourceInfo.stoppedChecks = 0
                resourceInfo.perMin.clear()
                resourceInfo.perHour = 0.0
            }
            return
        }
        resourceInfo.stoppedChecks = 0
    }

    private fun calculate(display: Data, info: ResourceInfo, reward: PowderChestReward) {
        info.estimated = display.rewards.getOrDefault(reward, 0)
    }

    private fun calculateChest(data: Data) {
        chestInfo.estimated = data.totalChestPicked
    }

    private fun calculateHardStone(data: Data) {
        hardStoneInfo.estimated = data.totalHardStoneCompacted
    }

    private fun convert(roughCount: Long): Gem {
        val flawlessRatio = 512000
        val fineRatio = 6400
        val flawedRatio = 80

        val flawlessCount = roughCount / flawlessRatio
        val remainingAfterFlawless = roughCount % flawlessRatio

        val fineCount = remainingAfterFlawless / fineRatio
        val remainingAfterFine = remainingAfterFlawless % fineRatio

        val flawedCount = remainingAfterFine / flawedRatio
        val remainingRoughCount = remainingAfterFine % flawedRatio

        return Gem(flawlessCount, fineCount, flawedCount, remainingRoughCount)
    }

    data class Gem(val flawless: Long, val fine: Long, val flawed: Long, val rough: Long)

    private data class ResourceInfo(
        var estimated: Long,
        var lastEstimated: Long,
        var stoppedChecks: Int,
        var perHour: Double,
        val perMin: MutableList<Long>,
    )

    private fun isEnabled() = IslandType.CRYSTAL_HOLLOWS.isCurrent() && config.enabled

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetpowdertracker") {
            description = "Resets the Powder Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }
}
