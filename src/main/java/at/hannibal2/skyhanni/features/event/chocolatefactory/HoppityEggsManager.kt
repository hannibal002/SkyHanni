package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object HoppityEggsManager {

    private val config get() = ChocolateFactoryApi.config.hoppityEggs

    private val eggFoundPattern by ChocolateFactoryApi.patternGroup.pattern(
        "egg.found",
        "§d§lHOPPITY'S HUNT §r§dYou found a §r§.Chocolate (?<meal>\\w+) Egg.*"
    )
    private val noEggsLeftPattern by ChocolateFactoryApi.patternGroup.pattern(
        "egg.noneleft",
        "§cThere are no hidden Chocolate Rabbit Eggs nearby! Try again later!"
    )
    private val eggSpawnedPattern by ChocolateFactoryApi.patternGroup.pattern(
        "egg.spawned",
        "§d§lHOPPITY'S HUNT §r§dA §r§.Chocolate (?<meal>\\w+) Egg §r§dhas appeared!"
    )
    private val eggAlreadyCollectedPattern by ChocolateFactoryApi.patternGroup.pattern(
        "egg.alreadycollected",
        "§cYou have already collected this Chocolate (?<meal>\\w+) Egg§r§c! Try again when it respawns!"
    )

    private var lastMeal: HoppityEggType? = null

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        lastMeal = null
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        eggFoundPattern.matchMatcher(event.message) {
            HoppityEggsLocations.eggFound()

            val meal = HoppityEggType.getMealByName(group("meal")) ?: run {
                ErrorManager.skyHanniError(
                    "Unknown meal: ${group("meal")}",
                    "message" to event.message
                )
            }
            meal.markClaimed()
            lastMeal = meal
        }

        noEggsLeftPattern.matchMatcher(event.message) {
            HoppityEggType.allFound()
            return
        }

        eggAlreadyCollectedPattern.matchMatcher(event.message) {
            val meal = HoppityEggType.getMealByName(group("meal")) ?: run {
                ErrorManager.skyHanniError(
                    "Unknown meal: ${group("meal")}",
                    "message" to event.message
                )
            }
            meal.markClaimed()
        }
    }

    fun shareWaypointPrompt() {
        if (!config.sharedWaypoints) return
        val currentLocation = LocationUtils.playerLocation()
        val meal = lastMeal ?: return
        lastMeal = null

        DelayedRun.runDelayed(1.seconds) {
            ChatUtils.clickableChat(
                "Click here to share the location of this chocolate egg with the server!",
                onClick = { HoppityEggsShared.shareNearbyEggLocation(currentLocation, meal) },
                30.seconds.fromNow()
            )
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.showClaimedEggs) return
        if (ReminderUtils.isBusy()) return
        if (!ChocolateFactoryApi.isHoppityEvent()) return

        val displayList = mutableListOf<String>()
        displayList.add("§bUnfound Eggs:")

        for (meal in HoppityEggType.entries) {
            if (!meal.isClaimed()) {
                displayList.add("§7 - ${meal.formattedName()}")
            }
        }
        if (displayList.size == 1) return

        config.position.renderStrings(displayList, posLabel = "Hoppity Eggs")
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        HoppityEggType.checkClaimed()
    }
}
