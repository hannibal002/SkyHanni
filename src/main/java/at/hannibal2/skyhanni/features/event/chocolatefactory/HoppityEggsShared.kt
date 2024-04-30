package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.event.chocolatefactory.HoppityEggsManager.getEggType
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object HoppityEggsShared {

    private val config get() = ChocolateFactoryAPI.config.hoppityEggs

    /**
     * REGEX-TEST: CalMWolfs: [SkyHanni] Breakfast Chocolate Egg located at x: 142, y: 71, z: -453
     */
    private val sharedEggPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "egg.shared",
        ".*\\[SkyHanni] (?<meal>\\w+) Chocolate Egg located at x: (?<x>-?\\d+), y: (?<y>-?\\d+), z: (?<z>-?\\d+)"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        sharedEggPattern.matchMatcher(event.message.removeColor()) {
            val (x, y, z) = listOf(group("x"), group("y"), group("z")).map { it.formatInt() }
            val eggLocation = LorenzVec(x, y, z)

            val meal = getEggType(event)

            if (meal.isClaimed()) return
            if (!HoppityEggLocator.isValidEggLocation(eggLocation)) return

            HoppityEggLocator.sharedEggLocation = eggLocation
            HoppityEggLocator.currentEggType = meal
        }
    }

    fun shareNearbyEggLocation(playerLocation: LorenzVec, meal: HoppityEggType) {
        if (!isEnabled()) return
        val islandEggsLocations = HoppityEggLocator.getCurrentIslandEggLocations() ?: return
        val closestEgg = islandEggsLocations.minByOrNull { it.distance(playerLocation) } ?: return

        val x = closestEgg.x.toInt()
        val y = closestEgg.y.toInt()
        val z = closestEgg.z.toInt()

        val message = "[SkyHanni] ${meal.mealName} Chocolate Egg located at x: $x, y: $y, z: $z"
        ChatUtils.sendCommandToServer("ac $message")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.waypoints && config.sharedWaypoints
}
