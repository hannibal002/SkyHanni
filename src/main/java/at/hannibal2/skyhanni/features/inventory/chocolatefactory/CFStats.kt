package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi
import at.hannibal2.skyhanni.features.event.hoppity.summary.HoppityEventSummary
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.data.ChocolateAmount
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.hitman.HitmanApi.getHitmanTimeToAll
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.hitman.HitmanApi.getOpenSlots
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.hitman.HitmanApi.getTimeToFull
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.toRoman
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import kotlin.time.Duration

@SkyHanniModule
object CFStats {

    private val config get() = CFApi.config
    private val profileStorage get() = CFApi.profileStorage

    private var display: Renderable? = null

    @HandleEvent(SecondPassedEvent::class, onlyOnSkyblock = true)
    fun onSecondPassed() {
        if (!CFApi.chocolateFactoryPaused) return
        updateDisplay()
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!CFApi.inChocolateFactory && !CFApi.chocolateFactoryPaused) return
        if (!config.statsDisplay) return

        display?.let {
            config.position.renderRenderable(it, posLabel = "Chocolate Factory Stats")
        }
    }

    fun updateDisplay() {
        val profileStorage = profileStorage ?: return

        val map = buildMap {
            put(CFStat.HEADER, "§6§lChocolate Factory ${CFApi.currentPrestige.toRoman()}")

            val maxSuffix = if (CFApi.isMax()) " §cMax!" else ""
            put(CFStat.CURRENT, "§eCurrent Chocolate: §6${ChocolateAmount.CURRENT.formatted}$maxSuffix")
            put(CFStat.THIS_PRESTIGE, "§eThis Prestige: §6${ChocolateAmount.PRESTIGE.formatted}")
            put(CFStat.ALL_TIME, "§eAll-time: §6${ChocolateAmount.ALL_TIME.formatted}")

            addProduction()

            put(CFStat.MULTIPLIER, "§eChocolate Multiplier: §6${profileStorage.chocolateMultiplier}")
            put(CFStat.BARN, "§eBarn: §6${CFBarnManager.barnStatus()}")

            addLeaderboard()

            put(CFStat.EMPTY, "")
            put(CFStat.EMPTY_2, "")
            put(CFStat.EMPTY_3, "")
            put(CFStat.EMPTY_4, "")
            put(CFStat.EMPTY_5, "")

            addTimeTower()

            put(
                CFStat.RAW_PER_SECOND,
                "§eRaw Per Second: §6${profileStorage.rawChocPerSecond.addSeparators()}",
            )

            addPrestige()

            val upgradeAvailableAt = ChocolateAmount.CURRENT.formattedTimeUntilGoal(profileStorage.bestUpgradeCost)
            put(CFStat.TIME_TO_BEST_UPGRADE, "§eBest Upgrade: $upgradeAvailableAt")

            addHitman()
        }
        val text = config.statsDisplayList.filter {
            it.shouldDisplay()
        }.flatMap {
            map[it]?.let { text -> CFApi.partyModeReplace(text) }?.split("\n").orEmpty()
        }
        display = createDisplay(text)
    }

    private fun MutableMap<CFStat, String>.addLeaderboard() {
        val position = CFApi.leaderboardPosition
        val positionText = position?.addSeparators() ?: "???"
        val percentile = CFApi.leaderboardPercentile
        val percentileText = percentile?.let { "§7Top §a$it%" }.orEmpty()
        val leaderboard = "#$positionText $percentileText"
        CFApi.updatePosition(position, leaderboard)
        HoppityEventSummary.updateCfPosition(position, percentile)
        put(CFStat.LEADERBOARD_POS, "§ePosition: §b$leaderboard")
    }

    private fun SimpleTimeMark?.formatIfFuture(): String? = this?.takeIf { it.isInFuture() }?.timeUntil()?.format()

    private fun MutableMap<CFStat, String>.addHitman() {
        val profileStorage = CFStats.profileStorage ?: return

        val hitmanStats = profileStorage.hitmanStats
        val availableHitmanEggs = hitmanStats.availableHitmanEggs.takeIf { it > 0 }?.toString() ?: "§7None"
        val hitmanSingleSlotCd = hitmanStats.singleSlotCooldownMark.formatIfFuture() ?: "§aAll Ready"
        val hitmanAllSlotsCd = hitmanStats.allSlotsCooldownMark.formatIfFuture() ?: "§aAll Ready"
        val openSlotsNow = hitmanStats.getOpenSlots()
        val purchasedSlots = hitmanStats.purchasedHitmanSlots

        val (hitmanAllSlotsTime, allSlotsEventInhibited) = hitmanStats.getHitmanTimeToAll()
        val hitmanAllClaimString = hitmanAllSlotsTime.takeIf { it > Duration.ZERO }?.format() ?: "§aAll Ready"
        val hitmanAllClaimReady = "${if (allSlotsEventInhibited) "§c" else "§b"}$hitmanAllClaimString"

        val (hitmanFullTime, hitmanFullEventInhibited) = hitmanStats.getTimeToFull()
        val hitmanFullString = if (openSlotsNow == 0) "§7Cooldown..."
        else hitmanFullTime.takeIf { it > Duration.ZERO }?.format() ?: "§cFull Now"
        val hitmanSlotsFull = "${if (hitmanFullEventInhibited) "§c" else "§b"}$hitmanFullString"
        put(CFStat.HITMAN_HEADER, "§c§lRabbit Hitman")
        put(CFStat.AVAILABLE_HITMAN_EGGS, "§eAvailable Hitman Eggs: §6$availableHitmanEggs")
        put(CFStat.OPEN_HITMAN_SLOTS, "§eOpen Hitman Slots: §6$openSlotsNow")
        put(CFStat.HITMAN_SLOT_COOLDOWN, "§eHitman Slot Cooldown: §b$hitmanSingleSlotCd")
        put(CFStat.HITMAN_ALL_SLOTS, "§eAll Hitman Slots Cooldown: §b$hitmanAllSlotsCd")

        if (HoppityApi.isHoppityEvent()) {
            put(CFStat.HITMAN_FULL_SLOTS, "§eFull Hitman Slots: §b$hitmanSlotsFull")
            put(CFStat.HITMAN_28_SLOTS, "§e$purchasedSlots Hitman Claims: $hitmanAllClaimReady")
        }
    }

    private fun MutableMap<CFStat, String>.addPrestige() {
        val allTime = ChocolateAmount.ALL_TIME.chocolate()
        val nextChocolateMilestone = CFApi.getNextMilestoneChocolate(allTime)
        val amountUntilNextMilestone = nextChocolateMilestone - allTime
        val amountFormat = amountUntilNextMilestone.addSeparators()
        val maxMilestoneEstimate = ChocolateAmount.ALL_TIME.formattedTimeUntilGoal(nextChocolateMilestone)
        val prestigeEstimate = ChocolateAmount.PRESTIGE.formattedTimeUntilGoal(CFApi.chocolateForPrestige)
        val chocolateUntilPrestigeCalculation = CFApi.chocolateForPrestige - ChocolateAmount.PRESTIGE.chocolate()

        var chocolateUntilPrestige = "§6${chocolateUntilPrestigeCalculation.addSeparators()}"

        if (chocolateUntilPrestigeCalculation <= 0) {
            chocolateUntilPrestige = "§aPrestige Available"
        }
        val prestigeData = when {
            !CFApi.isMaxPrestige() -> mapOf(
                CFStat.TIME_TO_PRESTIGE to "§eTime To Prestige: $prestigeEstimate",
                CFStat.CHOCOLATE_UNTIL_PRESTIGE to "§eChocolate To Prestige: §6$chocolateUntilPrestige",
            )

            amountUntilNextMilestone >= 0 -> mapOf(
                CFStat.TIME_TO_PRESTIGE to "§eTime To Next Milestone: $maxMilestoneEstimate",
                CFStat.CHOCOLATE_UNTIL_PRESTIGE to "§eChocolate To Next Milestone: §6$amountFormat",
            )

            else -> emptyMap()
        }
        putAll(prestigeData)
    }

    private fun MutableMap<CFStat, String>.addTimeTower() {
        val timeTowerInfo = if (CFTimeTowerManager.timeTowerActive()) {
            "§d§lActive"
        } else {
            "§6${CFTimeTowerManager.timeTowerCharges()}"
        }
        put(CFStat.TIME_TOWER, "§eTime Tower: §6$timeTowerInfo")

        val timeTowerFull = CFTimeTowerManager.timeTowerFullTimeMark()
        put(
            CFStat.TIME_TOWER_FULL,
            if (CFTimeTowerManager.timeTowerFull()) {
                "§eFull Tower Charges: §a§lNow\n" + "§eHappens at: §a§lNow"
            } else {
                "§eFull Tower Charges: §b${
                    timeTowerFull.timeUntil().format()
                }\n" + "§eHappens at: §b${timeTowerFull.formattedDate("EEEE, MMM d h:mm a")}"
            },
        )
    }

    private fun createDisplay(text: List<String>) = Renderable.clickable(
        Renderable.vertical(text.map(StringRenderable::from)),
        tips = listOf("§bCopy to Clipboard!"),
        onLeftClick = {
            val list = text.toMutableList()
            list.add(0, "${PlayerUtils.getName()}'s Chocolate Factory Stats")

            ClipboardUtils.copyToClipboard(list.joinToString("\n") { it.removeColor() })
        },
    )

    private fun MutableMap<CFStat, String>.addProduction() {
        val perSecond = CFApi.chocolatePerSecond
        val perMinute = perSecond * 60
        val perHour = perMinute * 60
        val perDay = perHour * 24
        put(CFStat.PER_SECOND, "§ePer Second: §6${perSecond.addSeparators()}")
        put(CFStat.PER_MINUTE, "§ePer Minute: §6${perMinute.addSeparators()}")
        put(CFStat.PER_HOUR, "§ePer Hour: §6${perHour.addSeparators()}")
        put(CFStat.PER_DAY, "§ePer Day: §6${perDay.addSeparators()}")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(42, "event.chocolateFactory.statsDisplayList") { element ->
            addToDisplayList(element, "TIME_TOWER", "TIME_TO_PRESTIGE")
        }
        event.transform(45, "inventory.chocolateFactory.statsDisplayList") { element ->
            addToDisplayList(element, "TIME_TO_BEST_UPGRADE")
        }
    }

    private fun addToDisplayList(element: JsonElement, vararg toAdd: String): JsonElement {
        val jsonArray = element.asJsonArray
        toAdd.forEach { jsonArray.add(JsonPrimitive(it)) }
        return jsonArray
    }

    enum class CFStat(private val display: String, val shouldDisplay: () -> Boolean = { true }) {
        HEADER("§6§lChocolate Factory Stats"),
        CURRENT("§eCurrent Chocolate: §65,272,230"),
        THIS_PRESTIGE("§eThis Prestige: §6483,023,853", { CFApi.currentPrestige != 1 }),
        ALL_TIME("§eAll-time: §6641,119,115"),
        PER_SECOND("§ePer Second: §63,780.72"),
        PER_MINUTE("§ePer Minute: §6226,843.2"),
        PER_HOUR("§ePer Hour: §613,610,592"),
        PER_DAY("§ePer Day: §6326,654,208"),
        MULTIPLIER("§eChocolate Multiplier: §61.77"),
        BARN("§eBarn: §6171/190 Rabbits"),
        LEADERBOARD_POS("§ePosition: §b#103 §7Top §a0.87%"),
        EMPTY(""),
        EMPTY_2(""),
        EMPTY_3(""),
        EMPTY_4(""),
        EMPTY_5(""),
        TIME_TOWER("§eTime Tower: §62/3 Charges", { CFTimeTowerManager.currentCharges() != -1 }),
        TIME_TOWER_FULL(
            "§eTime Tower Full Charges: §b5h 13m 59s\n§bHappens at: Monday, May 13 5:32 AM",
            { CFTimeTowerManager.currentCharges() != -1 || CFTimeTowerManager.timeTowerFull() },
        ),
        TIME_TO_PRESTIGE("§eTime To Prestige: §b1d 13h 59m 4s"),
        RAW_PER_SECOND("§eRaw Per Second: §62,136"),
        CHOCOLATE_UNTIL_PRESTIGE("§eChocolate To Prestige: §65,851"),
        TIME_TO_BEST_UPGRADE(
            "§eBest Upgrade: §b 59m 4s",
            { CFApi.profileStorage?.bestUpgradeCost != 0L },
        ),
        HITMAN_HEADER("§c§lRabbit Hitman"),
        AVAILABLE_HITMAN_EGGS("§eAvailable Hitman Eggs: §63"),
        OPEN_HITMAN_SLOTS("§eOpen Hitman Slots: §63"),
        HITMAN_SLOT_COOLDOWN("§eHitman Slot Cooldown: §b8m 6s"),
        HITMAN_ALL_SLOTS("§eAll Hitman Slots Cooldown: §b8h 8m 6s"),
        HITMAN_FULL_SLOTS("§eFull Hitman Slots: §b2h 10m"),
        HITMAN_28_SLOTS("§e28 Hitman Claims: §b3h 20m"),
        ;

        override fun toString(): String {
            return display
        }
    }
}
