package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FlightDurationAPI {

    private val group = RepoPattern.group("data.effects")
    private val flightGroup = group.group("flight")
    private val durationPattern by flightGroup.pattern(
        "duration",
        "§aYou can now fly for §r§c(?<time>[\\d,.]+)§r§a minutes."
    )
    private val durationExtendedPattern by flightGroup.pattern(
        "duration.extended",
        "§aYour flight has been extended for §r§c(?<time>[\\d,.]+)§r§a extra minutes."
    )
    private val expiresInPattern by flightGroup.pattern(
        "expires.duration",
        "§cMushroom Soup Flight expires in §r§e(?<time>\\d+)s§r§c."
    )
    private val expiredPattern by flightGroup.pattern(
        "expired",
        "§cMushroom Soup Flight has expired!"
    )

    private val storage get() = ProfileStorageData.profileSpecific

    var flightDuration: Duration
        get() = (storage?.flightDuration ?: 0).coerceAtLeast(0).seconds
        private set(value) {
            storage?.flightDuration = value.inWholeSeconds
        }

    private val flightIslands = listOf(
        IslandType.PRIVATE_ISLAND,
        IslandType.PRIVATE_ISLAND_GUEST,
        IslandType.GARDEN,
        IslandType.GARDEN_GUEST
    )

    private var inFlyingIsland = false

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val message = event.message
        durationPattern.matchMatcher(message) {
            val time = group("time").toInt()
            flightDuration = time.minutes
            return
        }
        durationExtendedPattern.matchMatcher(message) {
            val time = group("time").toInt()
            flightDuration += time.minutes
            return
        }
        expiresInPattern.matchMatcher(message) {
            val time = group("time").toInt()
            flightDuration = time.seconds
            return
        }
        if (expiredPattern.matches(message)) {
            flightDuration = 0.seconds
            return
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (isFlyingActive()) flightDuration -= 1.seconds
    }

    @SubscribeEvent
    fun onIslandSwitch(event: IslandChangeEvent) {
        inFlyingIsland = event.newIsland in flightIslands
    }

    fun isFlyingActive() = inFlyingIsland && flightDuration.isPositive() && !BitsAPI.hasCookieBuff()
}
