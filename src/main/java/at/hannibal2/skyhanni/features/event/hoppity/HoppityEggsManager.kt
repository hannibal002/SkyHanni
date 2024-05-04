package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.seconds

object HoppityEggsManager {

    val config get() = SkyHanniMod.feature.event.hoppityEggs

    /**
     * REGEX-TEST: §d§lHOPPITY'S HUNT §r§dYou found a §r§9Chocolate Lunch Egg §r§don a ledge next to the stairs up§r§d!
     * REGEX-TEST: §d§lHOPPITY'S HUNT §r§dYou found a §r§aChocolate Dinner Egg §r§dbehind Emissary Sisko§r§d!
     * REGEX-TEST: §d§lHOPPITY'S HUNT §r§dYou found a §r§9Chocolate Lunch Egg §r§dnear the Diamond Essence Shop§r§d!
     */
    private val eggFoundPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "egg.found",
        "§d§lHOPPITY'S HUNT §r§dYou found a §r§.Chocolate (?<meal>\\w+) Egg §r§d(?<note>.*)§r§d!"
    )
    private val noEggsLeftPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "egg.noneleft",
        "§cThere are no hidden Chocolate Rabbit Eggs nearby! Try again later!"
    )
    private val eggSpawnedPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "egg.spawned",
        "§d§lHOPPITY'S HUNT §r§dA §r§.Chocolate (?<meal>\\w+) Egg §r§dhas appeared!"
    )
    private val eggAlreadyCollectedPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "egg.alreadycollected",
        "§cYou have already collected this Chocolate (?<meal>\\w+) Egg§r§c! Try again when it respawns!"
    )
    private val hoppityEventNotOn by ChocolateFactoryAPI.patternGroup.pattern(
        "egg.notevent",
        "§cThis only works during Hoppity's Hunt!"
    )

    private var lastMeal: HoppityEggType? = null
    private var lastNote: String? = null

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        lastMeal = null
        lastNote = null
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        eggFoundPattern.matchMatcher(event.message) {
            HoppityEggLocator.eggFound()
            val meal = getEggType(event)
            val note = group("note").removeColor()
            meal.markClaimed()
            lastMeal = meal
            lastNote = note
            return
        }

        noEggsLeftPattern.matchMatcher(event.message) {
            HoppityEggType.allFound()

            if (config.timeInChat) {
                val nextEgg = HoppityEggType.entries.minByOrNull { it.timeUntil() } ?: return
                ChatUtils.chat("§eNext egg available in §b${nextEgg.timeUntil().format()}§e.")
                event.blockedReason = "hoppity_egg"
            }
            return
        }

        eggAlreadyCollectedPattern.matchMatcher(event.message) {
            getEggType(event).markClaimed()
            if (config.timeInChat) {
                val nextEgg = HoppityEggType.entries.minByOrNull { it.timeUntil() } ?: return
                ChatUtils.chat("§eNext egg available in §b${nextEgg.timeUntil().format()}§e.")
                event.blockedReason = "hoppity_egg"
            }
            return
        }

        eggSpawnedPattern.matchMatcher(event.message) {
            getEggType(event).markSpawned()
            return
        }

        hoppityEventNotOn.matchMatcher(event.message) {
            val currentYear = SkyBlockTime.now().year

            if (config.timeInChat) {
                val timeUntil = SkyBlockTime(currentYear + 1).asTimeMark().timeUntil()
                ChatUtils.chat("§eHoppity's Hunt not active. Next Hoppity's Hunt event in §b${timeUntil.format()}§e.")
                event.blockedReason = "hoppity_egg"
            }
            return
        }
    }

    internal fun Matcher.getEggType(event: LorenzChatEvent): HoppityEggType =
        HoppityEggType.getMealByName(group("meal")) ?: run {
            ErrorManager.skyHanniError(
                "Unknown meal: ${group("meal")}",
                "message" to event.message
            )
        }

    fun shareWaypointPrompt() {
        if (!config.sharedWaypoints) return
        val meal = lastMeal ?: return
        val note = lastNote ?: return
        lastMeal = null
        lastNote = null

        val currentLocation = LocationUtils.playerLocation()
        DelayedRun.runNextTick {
            ChatUtils.clickableChat(
                "Click here to share the location of this chocolate egg with the server!",
                onClick = { HoppityEggsShared.shareNearbyEggLocation(currentLocation, meal, note) },
                expireAt = 30.seconds.fromNow()
            )
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.showClaimedEggs) return
        if (ReminderUtils.isBusy(config.showDuringContest)) return
        if (!ChocolateFactoryAPI.isHoppityEvent()) return

        val displayList = HoppityEggType.entries
            .map { "§7 - ${it.formattedName} ${it.timeUntil().format()}" }
            .toMutableList()
        displayList.add(0, "§bUnfound Eggs:")
        if (displayList.size == 1) return

        config.position.renderStrings(displayList, posLabel = "Hoppity Eggs")
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        HoppityEggType.checkClaimed()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(
            44,
            "event.chocolateFactory.highlightHoppityShop",
            "event.chocolateFactory.hoppityEggs.highlightHoppityShop"
        )
        event.move(44, "event.chocolateFactory.hoppityEggs", "event.hoppityEggs")
    }

}
