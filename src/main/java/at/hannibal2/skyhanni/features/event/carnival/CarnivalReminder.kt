package at.hannibal2.hanni.features.event.carnival

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.EntityMovementData
import at.hannibal2.hanni.data.IslandGraphs
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.Perk
import at.hannibal2.hanni.data.ProfileStorageData
import at.hannibal2.hanni.events.ProfileJoinEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.features.fame.ReminderUtils
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HanniModule
object CarnivalReminder {

    private val config get() = HanniMod.feature.event.carnival
    private val storage get() = ProfileStorageData.profileSpecific?.carnival

    private var nextCheckTime = SimpleTimeMark.farFuture()

    private var claimedToday = false

    private var lastClaimedDay
        get() = storage?.lastClaimedDay
        set(value) {
            storage?.lastClaimedDay = value
        }

    private val patternGroup = RepoPattern.group("carnival.tickets")

    /** REGEX-TEST: §aYou claimed §r§aCarnival Ticket §r§8x25§r§a!
     */
    private val ticketClaimedPattern by patternGroup.pattern("claimed", "§aYou claimed §r§aCarnival Ticket §r§8x25§r§a!")

    /** REGEX-TEST: §e[NPC] §aCarnival Leader§f: §rYou've already claimed your §aCarnival Tickets §ffor §btoday§f, but I'm happy to answer any questions you might have.
     */
    @Suppress("MaxLineLength")
    private val alreadyClaimedPattern by patternGroup.pattern(
        "already",
        "§e\\[NPC\\] §aCarnival Leader§f: §rYou've already claimed your §aCarnival Tickets §ffor §btoday§f, but I'm happy to answer any questions you might have.",
    )

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled() || nextCheckTime.isInFuture()) return
        check()
    }

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        claimedToday = false
        if (!isEnabled()) return
        nextCheckTime = 30.0.seconds.fromNow()
        checkDate()
        check()
    }

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled() && !claimedToday) return
        if (!ticketClaimedPattern.matches(event.message) && !alreadyClaimedPattern.matches(event.message)) return
        claimedToday = true
        lastClaimedDay = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()
    }

    private fun checkDate() {
        val currentDay = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()
        val lastClaimedDay = lastClaimedDay

        claimedToday = !(lastClaimedDay == null || currentDay.isAfter(lastClaimedDay))
    }

    fun check() {
        if (claimedToday) {
            checkDate()
            return
        }
        if (ReminderUtils.isBusy()) return

        ChatUtils.clickToActionOrDisable(
            "Carnival Tickets are ready to be claimed!",
            config::reminderDailyTickets,
            "warp to The Carnival",
            action = {
                HypixelCommands.warp("carnival")
                EntityMovementData.onNextTeleport(IslandType.HUB) {
                    IslandGraphs.pathFind(LorenzVec(-89.5, 71.0, -18.7), "§aCarnival Tickets", condition = { config.reminderDailyTickets })
                }
            },
        )
        nextCheckTime = 5.0.minutes.fromNow()
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.reminderDailyTickets && Perk.CHIVALROUS_CARNIVAL.isActive
}
