package at.hannibal2.hanni.features.event.hoppity

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.features.event.hoppity.HoppityEggType.Companion.getEggType
import at.hannibal2.hanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.NumberUtil.formatInt
import at.hannibal2.hanni.utils.RegexUtils.groupOrNull
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor

@HanniModule
object HoppityEggsShared {

    private val waypointsConfig get() = HoppityEggsManager.config.waypoints

    /**
     * REGEX-TEST: CalMWolfs: [Hanni] Breakfast Chocolate Egg located at x: 142, y: 71, z: -453
     * REGEX-TEST: CalMWolfs: [Hanni] Breakfast Chocolate Egg located at x: 142, y: 71, z: -453 (hidden note)
     */
    private val sharedEggPattern by CFApi.patternGroup.pattern(
        "egg.shared",
        ".*\\[Hanni] (?<meal>\\w+) Chocolate Egg located at x: (?<x>-?\\d+), y: (?<y>-?\\d+), z: (?<z>-?\\d+)(?: \\((?<note>.*)\\))?"
    )

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return

        sharedEggPattern.matchMatcher(event.message.removeColor()) {
            val (x, y, z) = listOf(group("x"), group("y"), group("z")).map { it.formatInt() }
            val eggLocation = LorenzVec(x, y, z)

            val meal = getEggType(event)
            val note = groupOrNull("note")

            if (meal.isClaimed()) return
            if (!HoppityEggLocator.isValidEggLocation(eggLocation)) return

            HoppityEggLocator.sharedEggLocation = eggLocation
            HoppityEggLocator.currentEggType = meal
            HoppityEggLocator.currentEggNote = note
        }
    }

    fun shareNearbyEggLocation(playerLocation: LorenzVec, meal: HoppityEggType, note: String) {
        if (!isEnabled()) return
        val islandEggsLocations = HoppityEggLocations.islandLocations
        val closestEgg = islandEggsLocations.minByOrNull { it.distance(playerLocation) } ?: return

        val x = closestEgg.x.toInt()
        val y = closestEgg.y.toInt()
        val z = closestEgg.z.toInt()

        HypixelCommands.allChat("[Hanni] ${meal.mealName} Chocolate Egg located at x: $x, y: $y, z: $z ($note)")
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && waypointsConfig.enabled && waypointsConfig.shared
}
