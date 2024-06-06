package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

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

    private var flightDuration: Long?
        get() = ProfileStorageData.profileSpecific?.flightDuration
        set(value) {
            ProfileStorageData.profileSpecific?.flightDuration = value
        }

    var flightDurationEndTime: SimpleTimeMark? = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val message = event.message
        durationPattern.matchMatcher(message) {
            val time = group("time").toLong()
            flightDuration = time * 60000
            ChatUtils.debug("§aFlight duration set to §c${flightDuration}§a minutes.")
        }
        durationExtendedPattern.matchMatcher(message) {
            val time = group("time").toLong()
            flightDuration = (flightDuration ?: 0) + time * 60000
            ChatUtils.debug("§aFlight duration extended to §c${flightDuration}§a minutes.")
        }
    }

    @SubscribeEvent
    fun onIslandSwitch(event: IslandChangeEvent) {
        if (!LorenzUtils.inSkyBlock) return
        //if (flightDuration == null) return
        //if (flightDuration == 0L) return

        val flightIslands = listOf(
            IslandType.PRIVATE_ISLAND,
            IslandType.PRIVATE_ISLAND_GUEST,
            IslandType.GARDEN,
            IslandType.GARDEN_GUEST
        )

        ChatUtils.chat("Moved from ${event.oldIsland} to ${event.newIsland}.")

        if (event.newIsland in flightIslands && event.oldIsland !in flightIslands) {
            flightDurationEndTime = flightDuration?.let { SimpleTimeMark.now().plus(it.milliseconds) }
            ChatUtils.chat("§aFlight duration ends at §c${flightDurationEndTime?.timeUntil()}§a.")
        }

        if (event.newIsland !in flightIslands && event.oldIsland in flightIslands) {
            flightDuration?.let {
                flightDuration = flightDurationEndTime?.let { it - SimpleTimeMark.now() }?.inWholeSeconds
                ChatUtils.chat("§aFlight duration reduced to §c${flightDuration?.minutes}§a minutes.")
            }
        }
    }
}
