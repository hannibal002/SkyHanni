package at.hannibal2.skyhanni.features.event.carnival

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CarnivalReminder {

    private val config get() = SkyHanniMod.feature.event.carnival.reminderDailyTickets

    private var cooldown = SimpleTimeMark.farFuture()

    private var claimedToday = false

    private var storage
        get() = ProfileStorageData.profileSpecific?.carnival?.lastClaimedDay
        set(value) {
            ProfileStorageData.profileSpecific?.carnival?.lastClaimedDay = value
        }

    private val repoGroup = RepoPattern.group("carnival.tickets")

    /** REGEX-TEST: §aYou claimed §r§aCarnival Ticket §r§8x25§r§a!
     */
    private val ticketClaimedPattern by repoGroup.pattern("claimed", "§aYou claimed §r§aCarnival Ticket §r§8x25§r§a!")

    /** REGEX-TEST: §e[NPC] §aCarnival Leader§f: §rYou've already claimed your §aCarnival Tickets §ffor §btoday§f, but I'm happy to answer any questions you might have.
     */
    private val alreadyClaimedPattern by repoGroup.pattern(
        "already",
        "§e[NPC] §aCarnival Leader§f: §rYou've already claimed your §aCarnival Tickets §ffor §btoday§f, but I'm happy to answer any questions you might have.",
    )

    @SubscribeEvent
    fun onSecondPassedEvent(event: SecondPassedEvent) {
        if (!isEnabled() || !cooldown.isInPast()) return
        check()
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        claimedToday = false
        if (!isEnabled()) return
        cooldown = 30.0.seconds.fromNow()
        check()
    }

    @SubscribeEvent
    fun onLorenzChat(event: LorenzChatEvent) {
        if (!isEnabled() && !claimedToday) return
        if (!ticketClaimedPattern.matches(event.message) && !alreadyClaimedPattern.matches(event.message)) return
        claimedToday = true
        storage = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()
    }

    fun check() {
        if (claimedToday) {
            val currentDay = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()
            val stored = storage

            if (stored == null || currentDay.isAfter(stored)) {
                claimedToday = false
            }
        }
        if (!claimedToday) {
            ChatUtils.clickableChat(
                "Carnival Tickets are ready to be claimed!",
                {
                    HypixelCommands.warp("carnival")
                },
                "/warp carnival",
            )
            cooldown = 5.0.minutes.fromNow()
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config && Perk.CHIVALROUS_CARNIVAL.isActive
}
