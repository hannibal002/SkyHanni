package at.hannibal2.skyhanni.features.misc.chocolatefactory

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class HoppityEggsManager {

    val config get() = ChocolateFactoryApi.config


    private val eggFoundPattern by ChocolateFactoryApi.patternGroup.pattern(
        "egg.found",
        "§d§lHOPPITY'S HUNT §r§dYou found a §r§.Chocolate (?<meal>\\w+) Egg.*"
    )
    private val sharedEggPattern by ChocolateFactoryApi.patternGroup.pattern(
        "egg.shared",
        ".*\\[SkyHanni] (?<meal>\\w+) Chocolate Egg located at x: (?<x>-?\\d+), y: (?<y>-?\\d+), z: (?<z>-?\\d+)"
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

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        eggFoundPattern.matchMatcher(event.message) {
            val currentLocation = LocationUtils.playerLocation()
            EggLocations.eggFound()
            val meal = CakeMealTime.getMealByName(group("meal")) ?: run {
                ErrorManager.skyHanniError(
                    "Unknown meal: ${group("meal")}",
                    "message" to event.message
                )
            }
            meal.markClaimed()

            DelayedRun.runDelayed(3.seconds) {
                ChatUtils.clickableChat(
                    "Click here to share the location of this chocolate egg with the server!",
                    onClick = { EggLocations.shareNearbyEggLocation(currentLocation, meal) },
                    SimpleTimeMark.now() + 30.seconds
                )
            }
            return
        }
        // todo later
//         sharedEggPattern.matchMatcher(event.message.removeColor()) {
//             val x = group("x").formatInt()
//             val y = group("y").formatInt()
//             val z = group("z").formatInt()
//             val eggLocation = LorenzVec(x, y, z)
//             val meal = CakeMealTime.getMealByName(group("meal")) ?: run {
//                 ErrorManager.skyHanniError(
//                     "Unknown meal: ${group("meal")}",
//                     "message" to event.message
//                 )
//             }
//             return
//         }
        noEggsLeftPattern.matchMatcher(event.message) {
            CakeMealTime.allFound()
            return
        }
        eggAlreadyCollectedPattern.matchMatcher(event.message) {
            val meal = CakeMealTime.getMealByName(group("meal")) ?: run {
                ErrorManager.skyHanniError(
                    "Unknown meal: ${group("meal")}",
                    "message" to event.message
                )
            }
            meal.markClaimed()
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

        for (meal in CakeMealTime.entries) {
            if (!meal.claimed) {
                displayList.add("§7 - ${meal.formattedName()}")
            }
        }
        if (displayList.size == 1) return

        config.position.renderStrings(displayList, posLabel = "Hoppity Eggs")
    }
}
