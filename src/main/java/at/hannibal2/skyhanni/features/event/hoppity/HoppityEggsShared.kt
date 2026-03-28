package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggType.Companion.getEggType
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.toVec3
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.VectorUtils.toChatFormat
import net.minecraft.world.phys.Vec3

@SkyHanniModule
object HoppityEggsShared {

    private val waypointsConfig get() = HoppityEggsManager.config.waypoints

    /**
     * REGEX-TEST: CalMWolfs: [SkyHanni] Breakfast Chocolate Egg located at x: 142, y: 71, z: -453
     * REGEX-TEST: CalMWolfs: [SkyHanni] Breakfast Chocolate Egg located at x: 142, y: 71, z: -453 (hidden note)
     */
    private val sharedEggPattern by CFApi.patternGroup.pattern(
        "egg.shared",
        ".*\\[SkyHanni] (?<meal>\\w+) Chocolate Egg located at x: (?<x>-?\\d+), y: (?<y>-?\\d+), z: (?<z>-?\\d+)(?: \\((?<note>.*)\\))?"
    )

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return

        sharedEggPattern.matchMatcher(event.message.removeColor()) {
            val eggLocation = toVec3() ?: return

            val meal = getEggType(event)
            val note = groupOrNull("note")

            if (meal.isClaimed()) return
            if (!HoppityEggLocator.isValidEggLocation(eggLocation)) return

            HoppityEggLocator.sharedEggLocation = eggLocation
            HoppityEggLocator.currentEggType = meal
            HoppityEggLocator.currentEggNote = note
        }
    }

    fun shareNearbyEggLocation(playerLocation: Vec3, meal: HoppityEggType, note: String) {
        if (!isEnabled()) return
        val islandEggsLocations = HoppityEggLocations.islandLocations
        val closestEgg = islandEggsLocations.minByOrNull { it.distanceTo(playerLocation) } ?: return

        val location = closestEgg.toChatFormat()

        HypixelCommands.allChat("[SkyHanni] ${meal.mealName} Chocolate Egg located at $location ($note)")
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && waypointsConfig.enabled && waypointsConfig.shared
}
