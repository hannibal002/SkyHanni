package at.hannibal2.skyhanni.features.mining.powdertracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.mining.PowderTrackerConfig.PowderDisplayEntry
import at.hannibal2.skyhanni.data.BossbarData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes

object PowderTracker {

    private val config get() = SkyHanniMod.feature.mining.powderTracker

    private val patternGroup = RepoPattern.group("mining.powder.tracker")
    private val pickedPattern by patternGroup.pattern(
        "picked",
        "§6You have successfully picked the lock on this chest!"
    )
    private val uncoveredPattern by patternGroup.pattern(
        "uncovered",
        "§aYou uncovered a treasure chest!"
    )
    private val powderStartedPattern by patternGroup.pattern(
        "powder.started",
        ".*§r§b§l2X POWDER STARTED!.*"
    )
    private val powderEndedPattern by patternGroup.pattern(
        "powder.ended",
        ".*§r§b§l2X POWDER ENDED!.*"
    )
    private val powderBossBarPattern by patternGroup.pattern(
        "powder.bossbar",
        "§e§lPASSIVE EVENT §b§l2X POWDER §e§lRUNNING FOR §a§l(?<time>.*)§r"
    )

    private var lastChestPicked = SimpleTimeMark.farPast()
    private var isGrinding = false
    private val gemstoneInfo = ResourceInfo(0L, 0L, 0, 0.0, mutableListOf())
    private val mithrilInfo = ResourceInfo(0L, 0L, 0, 0.0, mutableListOf())
    private val diamondEssenceInfo = ResourceInfo(0L, 0L, 0, 0.0, mutableListOf())
    private val goldEssenceInfo = ResourceInfo(0L, 0L, 0, 0.0, mutableListOf())
    private val chestInfo = ResourceInfo(0L, 0L, 0, 0.0, mutableListOf())
    private var doublePowder = false
    private var powderTimer = ""
    private val gemstones = listOf(
        "Ruby" to "§c",
        "Sapphire" to "§b",
        "Amber" to "§6",
        "Amethyst" to "§5",
        "Jade" to "§a",
        "Topaz" to "§e"
    )

    init {
        PowderChestReward.entries.forEach { it.chatPattern }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        calculateResourceHour(gemstoneInfo)
        calculateResourceHour(mithrilInfo)
        calculateResourceHour(diamondEssenceInfo)
        calculateResourceHour(goldEssenceInfo)
        calculateResourceHour(chestInfo)
    }

    private val tracker = SkyHanniTracker("Powder Tracker", { Data() }, { it.powderTracker })
    { formatDisplay(drawDisplay(it)) }

    class Data : TrackerData() {

        override fun reset() {
            rewards.clear()
            totalChestPicked = 0
        }

        @Expose
        var totalChestPicked = 0

        @Expose
        var rewards: MutableMap<PowderChestReward, Long> = mutableMapOf()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        if (config.onlyWhenPowderGrinding && !isGrinding) return

        tracker.renderDisplay(config.position)
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val msg = event.message

        if (config.greatExplorerMaxed) {
            uncoveredPattern.matchMatcher(msg) {
                tracker.modify {
                    it.totalChestPicked += 1
                }
                isGrinding = true
                lastChestPicked = SimpleTimeMark.now()
            }
        }

        pickedPattern.matchMatcher(msg) {
            tracker.modify {
                it.totalChestPicked += 1
            }
            isGrinding = true
            lastChestPicked = SimpleTimeMark.now()
        }

        powderStartedPattern.matchMatcher(msg) { doublePowder = true }
        powderEndedPattern.matchMatcher(msg) { doublePowder = false }

        for (reward in PowderChestReward.entries) {
            reward.chatPattern.matchMatcher(msg) {
                tracker.modify {
                    val count = it.rewards[reward] ?: 0
                    var amount = group("amount").formatLong()
                    if ((reward == PowderChestReward.MITHRIL_POWDER || reward == PowderChestReward.GEMSTONE_POWDER) && doublePowder)
                        amount *= 2
                    it.rewards[reward] = count + amount
                }
            }
        }
        tracker.update()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (event.repeatSeconds(1)) {
            doublePowder = powderBossBarPattern.matcher(BossbarData.getBossbar()).find()
            powderBossBarPattern.matchMatcher(BossbarData.getBossbar()) {
                powderTimer = group("time")
                doublePowder = powderTimer != "00:00"

                tracker.update()
            }
        }
        if (lastChestPicked.passedSince() > 1.minutes) {
            isGrinding = false
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.textFormat.afterChange {
            tracker.update()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
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
        doublePowder = false
        tracker.update()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.powderTrackerConfig", "mining.powderTracker")
        event.transform(8, "#profile.powderTracker") { old -> old.asJsonObject.get("0") }
        event.transform(11, "mining.powderTracker.textFormat") { element ->
            ConfigUtils.migrateIntArrayListToEnumArrayList(element, PowderDisplayEntry::class.java)
        }

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
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.CRYSTAL_HOLLOWS) {
            tracker.firstUpdate()
        }
    }

    private fun formatDisplay(map: List<List<Any>>) = buildList<List<Any>> {
        if (map.isEmpty()) return@buildList

        addAsSingletonList("§b§lPowder Tracker")

        for (index in config.textFormat.get()) {
            // TODO, change functionality to use enum rather than ordinals
            add(map[index.ordinal])
        }
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        calculate(data, gemstoneInfo, PowderChestReward.GEMSTONE_POWDER)
        calculate(data, mithrilInfo, PowderChestReward.MITHRIL_POWDER)
        calculate(data, diamondEssenceInfo, PowderChestReward.DIAMOND_ESSENCE)
        calculate(data, goldEssenceInfo, PowderChestReward.GOLD_ESSENCE)
        calculateChest(data)

        val chestPerHour = format(chestInfo.perHour)
        addAsSingletonList("§d${data.totalChestPicked.addSeparators()} Total Chests Picked §7($chestPerHour/h)")
        addAsSingletonList("§bDouble Powder: ${if (doublePowder) "§aActive! §7($powderTimer)" else "§cInactive!"}")

        val entries = PowderChestReward.entries
        val rewards = data.rewards
        addPerHour(rewards, entries[0], mithrilInfo)
        addPerHour(rewards, entries[1], gemstoneInfo)
        addAsSingletonList("")
        addPerHour(rewards, entries[46], diamondEssenceInfo)
        addPerHour(rewards, entries[47], goldEssenceInfo)


        addAsSingletonList("")


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
            addAsSingletonList("§5${flawless}§7-§9${fine}§7-§a${flawed}§f-${rough} $color$gem Gemstone")
        }

        var totalParts = 0L
        for (reward in entries.subList(26, 32)) { // robots part
            val count = rewards.getOrDefault(reward, 0)
            totalParts += count
            addAsSingletonList("§b${count.addSeparators()} ${reward.displayName}")
        }
        addAsSingletonList("§b${totalParts.addSeparators()} §9Total Robot Parts")

        val goblinEgg = rewards.getOrDefault(PowderChestReward.GOBLIN_EGG, 0)
        val greenEgg = rewards.getOrDefault(PowderChestReward.GREEN_GOBLIN_EGG, 0)
        val redEgg = rewards.getOrDefault(PowderChestReward.RED_GOBLIN_EGG, 0)
        val yellowEgg = rewards.getOrDefault(PowderChestReward.YELLOW_GOBLIN_EGG, 0)
        val blueEgg = rewards.getOrDefault(PowderChestReward.BLUE_GOBLIN_EGG, 0)
        addAsSingletonList("§9$goblinEgg§7-§a$greenEgg§7-§c$redEgg§f-§e$yellowEgg§f-§3$blueEgg §fGoblin Egg")

        for (reward in entries.subList(37, 46)) {
            val count = rewards.getOrDefault(reward, 0).addSeparators()
            addAsSingletonList("§b$count ${reward.displayName}")
        }
    }

    private fun MutableList<List<Any>>.addPerHour(
        map: MutableMap<PowderChestReward, Long>,
        reward: PowderChestReward,
        info: ResourceInfo,
    ) {
        val mithrilCount = map.getOrDefault(reward, 0).addSeparators()
        val mithrilPerHour = format(info.perHour)
        addAsSingletonList("§b$mithrilCount ${reward.displayName} §7($mithrilPerHour/h)")
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
        chestInfo.estimated = data.totalChestPicked.toLong()
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

    private fun isEnabled() = IslandType.CRYSTAL_HOLLOWS.isInIsland() && config.enabled

    fun resetCommand() {
        tracker.resetCommand()
    }
}
