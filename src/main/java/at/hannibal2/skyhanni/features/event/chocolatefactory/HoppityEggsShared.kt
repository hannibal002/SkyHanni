package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object HoppityEggsShared {

    private val config get() = ChocolateFactoryApi.config.hoppityEggs

    private val sharedEggPattern by ChocolateFactoryApi.patternGroup.pattern(
        "egg.shared",
        ".*\\[SkyHanni] (?<meal>\\w+) Chocolate Egg located at x: (?<x>-?\\d+), y: (?<y>-?\\d+), z: (?<z>-?\\d+)"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        sharedEggPattern.matchMatcher(event.message.removeColor()) {
            val x = group("x").formatInt()
            val y = group("y").formatInt()
            val z = group("z").formatInt()
            val eggLocation = LorenzVec(x, y, z)

            val meal = HoppityEggType.getMealByName(group("meal")) ?: run {
                ErrorManager.skyHanniError(
                    "Unknown meal: ${group("meal")}",
                    "message" to event.message
                )
            }

            if (meal.isClaimed()) return
            if (!HoppityEggsLocations.isValidEggLocation(eggLocation)) return

            HoppityEggsLocations.sharedEggLocation = eggLocation
            HoppityEggsLocations.currentEggType = meal
        }
    }

    fun shareNearbyEggLocation(playerLocation: LorenzVec, meal: HoppityEggType) {
        if (!isEnabled()) return
        val islandEggsLocations = HoppityEggsLocations.getCurrentIslandEggLocations() ?: return
        val closestEgg = islandEggsLocations.minByOrNull { it.distance(playerLocation) } ?: return

        val x = closestEgg.x.toInt()
        val y = closestEgg.y.toInt()
        val z = closestEgg.z.toInt()

        val message = "[SkyHanni] ${meal.mealName} Chocolate Egg located at x: $x, y: $y, z: $z"
        ChatUtils.sendCommandToServer("ac $message")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.waypoints && config.sharedWaypoints
}
