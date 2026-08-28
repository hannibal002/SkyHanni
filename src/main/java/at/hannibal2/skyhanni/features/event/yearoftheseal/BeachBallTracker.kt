package at.hannibal2.skyhanni.features.event.yearoftheseal

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.inPartialHours
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object BeachBallTracker {

    private val config get() = SkyHanniMod.feature.event.yearOfTheSeal.beachBallTracker

    private val FISHY_TREAT = "FISHY_TREAT".toInternalName()
    private val ENCHANTED_RAW_FISH = "ENCHANTED_RAW_FISH".toInternalName()
    private val WATCH = "WATCH".toInternalName()

    private val patternGroup = RepoPattern.group("event.year-of-the-seal.beach-ball-tracker")

    /**
     * REGEX-TEST: AW MAN! You kept the Bouncy Beach Ball in the air for 0 bounces and earned 1 Fishy Treat!
     * REGEX-TEST: INSANE! You kept the Bouncy Beach Ball in the air for 187 bounces and earned 20 Fishy Treats!
     */
    private val normalResultPattern by patternGroup.pattern(
        "result.normal",
        ".*You kept the Bouncy Beach Ball in the air for \\d+ bounces and earned (?<treats>\\d+) Fishy Treat.*",
    )

    /**
     * REGEX-TEST: AW MAN! The Giant Bouncy Beach Ball was kept in the air for 0 bounces and 0 unique players have participated, rewarding you with 5 extra Fishy Treats!
     */
    private val giantResultPattern by patternGroup.pattern(
        "result.giant",
        ".*The Giant Bouncy Beach Ball was kept in the air for \\d+ bounces and " +
            "\\d+ unique players have participated, rewarding you with (?<treats>\\d+) extra Fishy Treats.*",
    )

    /**
     * REGEX-TEST: BOUNCE BONANZA! Keep the Bouncy Beach Ball in the air for as long as you can by bouncing it on your head!
     * REGEX-TEST: BOUNCE BONANZA EX! Keep the Giant Bouncy Beach Ball in the air for 60 seconds by bouncing it on your and other players heads!
     */
    private val startPattern by patternGroup.pattern(
        "start",
        ".*BOUNCE BONANZA(?: EX)?! Keep the (?:Giant )?Bouncy Beach Ball in the air .*",
    )

    private val tracker = SkyHanniTracker(
        "Beach Ball Tracker",
        ::Data,
        { it.beachBallTracker },
        trackerConfig = { config.perTrackerConfig },
    ) {
        drawDisplay(it)
    }

    private var lastBallActivity = SimpleTimeMark.farPast()

    data class Data(
        @Expose var ballsUsed: MutableMap<BallType, Int> = mutableMapOf(),
        @Expose var fishyTreats: Long = 0L,
    ) : TrackerData<SessionUptime.Normal>(SessionUptime.Normal::class)

    enum class BallType(
        val label: String,
        val item: NeuInternalName,
        val resultPattern: () -> Pattern,
    ) {
        NORMAL("Beach Balls", "BOUNCY_BEACH_BALL".toInternalName(), { normalResultPattern }),
        GIANT("Giant Beach Balls", "GIANT_BOUNCY_BEACH_BALL".toInternalName(), { giantResultPattern }),
    }

    private val ballItems = BallType.entries.map { it.item }.toSet()

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.enabled) return
        val message = event.cleanMessage

        // A ball was just thrown: show the overlay immediately (before any result comes in).
        if (startPattern.matches(message)) {
            markActivity()
            return
        }

        for (type in BallType.entries) {
            type.resultPattern().matchMatcher(message) {
                val treats = group("treats").formatInt()
                markActivity()
                tracker.modify {
                    it.ballsUsed.addOrPut(type, 1)
                    it.fishyTreats += treats
                }
                return
            }
        }
    }

    private fun markActivity() {
        lastBallActivity = SimpleTimeMark.now()
        tracker.firstUpdate()
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§6§lBeach Ball Tracker")

        for (type in BallType.entries) {
            val amount = data.ballsUsed[type] ?: continue
            addIconLine(type.item, "§e${amount.addSeparators()}x §f${type.label}", type.label)
        }

        addIconLine(FISHY_TREAT, "§e${data.fishyTreats.addSeparators()} §dFishy Treats", "Fishy Treats")

        val totalBalls = data.ballsUsed.values.sum()
        if (totalBalls > 0) {
            val avg = data.fishyTreats.toDouble() / totalBalls
            addIconLine(ENCHANTED_RAW_FISH, "§e${avg.roundTo(1).addSeparators()} §7Treats per Ball", "Treats per Ball")
        }

        val duration = data.getTotalUptime()
        if (duration != 0.seconds) {
            val perHour = data.fishyTreats / duration.inPartialHours
            addIconLine(WATCH, "§e${perHour.roundTo(0).addSeparators()} §7Treats per Hour", "Treats per Hour")
        }
    }

    private fun MutableList<Searchable>.addIconLine(icon: NeuInternalName, text: String, searchText: String) {
        add(
            Renderable.horizontal(
                listOf(
                    Renderable.item(icon),
                    Renderable.text(text),
                ),
                spacing = 2,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            ).toSearchable(searchText),
        )
    }

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            onlyOnIsland = IslandType.HUB,
            condition = { shouldShowDisplay() },
            onRender = {
                if (isHoldingBeachBall()) tracker.firstUpdate()
                tracker.renderDisplay(config.position)
            },
        )
    }

    private fun shouldShowDisplay(): Boolean {
        if (!config.enabled) return false
        val recentlyActive = lastBallActivity.passedSince() < config.hideAfterInactivity.minutes
        return recentlyActive || isHoldingBeachBall()
    }

    private fun isHoldingBeachBall(): Boolean {
        val held = InventoryUtils.getItemInHand()?.getInternalNameOrNull() ?: return false
        return held in ballItems
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetbeachballtracker") {
            description = "Resets the Beach Ball Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }
}
