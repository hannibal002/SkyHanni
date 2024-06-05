package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FlightDurationAPI {

    private val group = RepoPattern.group("data.activeeffects")
    private val flightGroup = group.group("flight")
    private val durationPattern by flightGroup.pattern(
        "duration",
        "§aYou can now fly for §r§c(?<time>[\\d,.]+)§r§a minutes."
    )
    private val durationExtendedPattern by flightGroup.pattern(
        "duration.extended",
        "§aYour flight has been extended for §r§c(?<time>[\\d,.]+)§r§a extra minutes."
    )

    private val storage get() = ProfileStorageData.profileSpecific

    private var flightDuration: Duration
        get() = (storage?.flightDuration ?: 0).milliseconds
        set(value) {
            storage?.flightDuration = value.inWholeMilliseconds
        }

    private val flightIslands = listOf(
        IslandType.PRIVATE_ISLAND,
        IslandType.PRIVATE_ISLAND_GUEST,
        IslandType.GARDEN,
        IslandType.GARDEN_GUEST
    )

    private var inIsland = false

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val message = event.message
        durationPattern.matchMatcher(message) {
            val time = group("time").toLong()
            flightDuration = time.minutes
            ChatUtils.debug("§aFlight duration set to §c${flightDuration}§a minutes.")
        }
        durationExtendedPattern.matchMatcher(message) {
            val time = group("time").toLong()
            flightDuration += time.minutes
            ChatUtils.debug("§aFlight duration extended to §c${flightDuration}§a minutes.")
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (inIsland && flightDuration.isPositive() && !BitsAPI.hasCookieBuff()) {
            flightDuration -= 1.seconds
        }
    }

    @SubscribeEvent
    fun onIslandSwitch(event: IslandChangeEvent) {
        inIsland = event.newIsland in flightIslands
    }


}
