package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.Mayor.Companion.getMayorFromPerk
import at.hannibal2.skyhanni.data.Mayor.Companion.setAssumeMayor
import at.hannibal2.skyhanni.data.Mayor.Companion.setAssumeMayorJson
import at.hannibal2.skyhanni.data.Perk.Companion.getPerkFromName
import at.hannibal2.skyhanni.data.jsonobjects.other.MayorCandidate
import at.hannibal2.skyhanni.data.jsonobjects.other.MayorElection
import at.hannibal2.skyhanni.data.jsonobjects.other.MayorJson
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.CollectionUtils.put
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyBlockTime.Companion.SKYBLOCK_YEAR_MILLIS
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

object MayorAPI {

    private val group = RepoPattern.group("mayorapi")

    // TODO: Add Regex-test
    val foxyExtraEventPattern by group.pattern(
        "foxy.extraevent",
        "Schedules an extra §.(?<event>.*) §.event during the year\\."
    )

    /**
     * REGEX-TEST: The election room is now closed. Clerk Seraphine is doing a final count of the votes...
     */
    private val electionOverPattern by group.pattern(
        "election.over",
        "§eThe election room is now closed\\. Clerk Seraphine is doing a final count of the votes\\.\\.\\."
    )

    /**
     * REGEX-TEST: Calendar and Events
     */
    private val calendarGuiPattern by group.pattern(
        "calendar.gui",
        "Calendar and Events"
    )

    /**
     * REGEX-TEST: §dMayor Jerry
     */
    private val jerryHeadPattern by group.pattern(
        "jerry.head",
        "§dMayor Jerry"
    )

    /**
     * REGEX-TEST: §9Perkpocalypse Perks:
     */
    private val perkpocalypsePerksPattern by group.pattern(
        "perkpocalypse",
        "§9Perkpocalypse Perks:"
    )

    var currentMayor: Mayor? = null
        private set
    private var lastMayor: Mayor? = null
    var jerryExtraMayor: Pair<Mayor?, SimpleTimeMark> = null to SimpleTimeMark.farPast()
        private set
    var lastJerryExtraMayorReminder = SimpleTimeMark.farPast()

    private var lastUpdate = SimpleTimeMark.farPast()
    private var dispatcher = Dispatchers.IO

    private var rawMayorData: MayorJson? = null
    private var candidates = mapOf<Int, MayorCandidate>()

    var nextMayorTimestamp = SimpleTimeMark.farPast()
        private set

    private const val ELECTION_END_MONTH = 3 // Late Spring
    private const val ELECTION_END_DAY = 27

    /**
     * @param input: The name of the mayor
     * @return: The neu color of the mayor; If no mayor was found, it will return "§c"
     */
    fun mayorNameToColorCode(input: String): String = Mayor.getMayorFromName(input)?.color ?: "§c"

    /**
     * @param input: The name of the mayor
     * @return: The neu color of the mayor + the name of the mayor; If no mayor was found, it will return "§c[input]"
     */
    fun mayorNameWithColorCode(input: String) = mayorNameToColorCode(input) + input

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!LorenzUtils.onHypixel) return
        if (event.repeatSeconds(2)) {
            checkHypixelAPI()
            getTimeTillNextMayor()
        }

        if (!LorenzUtils.inSkyBlock) return
        if (jerryExtraMayor.first != null && jerryExtraMayor.second.isInPast() && Mayor.JERRY.isActive()) {
            jerryExtraMayor = null to SimpleTimeMark.farPast()
            ChatUtils.clickableChat(
                "The Perkpocalypse Mayor has expired! Click here to update the new temporary Mayor.",
                onClick = { HypixelCommands.calendar() }
            )
        }
        if (Mayor.JERRY.isActive() && jerryExtraMayor.first == null) {
            if (lastJerryExtraMayorReminder.passedSince() < 5.minutes) return

            lastJerryExtraMayorReminder = SimpleTimeMark.now()
            ChatUtils.clickableChat(
                "The Perkpocalypse Mayor is not known! Click here to update the temporary Mayor.",
                onClick = { HypixelCommands.calendar() }
            )
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (electionOverPattern.matches(event.message)) {
            lastMayor = currentMayor
            currentMayor = Mayor.UNKNOWN
        }
    }

    @SubscribeEvent
    fun onInventory(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (!calendarGuiPattern.matches(event.inventoryName)) return

        val stack: ItemStack =
            event.inventoryItems.values.firstOrNull { jerryHeadPattern.matches(it.displayName) } ?: return

        stack.getLore().nextAfter(
            { perkpocalypsePerksPattern.matches(it) }
        )?.let { perk ->
            // This is one Perk of the Perkpocalypse Mayor
            val jerryMayor = getMayorFromPerk(getPerkFromName(perk.removeColor()) ?: return)?.addAllPerks() ?: return

            val lastMayorTimestamp = nextMayorTimestamp - SKYBLOCK_YEAR_MILLIS.milliseconds

            val expireTime = (1..21).map { lastMayorTimestamp + (6.hours * it) }.first { it.isInFuture() }

            ChatUtils.debug("Jerry Mayor found: ${jerryMayor.name} expiring at: ${expireTime.timeUntil()}")

            jerryExtraMayor = jerryMayor to expireTime
        }
    }

    private fun calculateNextMayorTime(): SimpleTimeMark {
        var mayorYear = SkyBlockTime.now().year

        // Check if either the month is already over or the day after 27th in the third month
        if (SkyBlockTime.now().month > ELECTION_END_MONTH || (SkyBlockTime.now().day >= ELECTION_END_DAY && SkyBlockTime.now().month == ELECTION_END_MONTH)) {
            // If so, the next mayor will be in the next year
            mayorYear++
        }

        return SkyBlockTime(mayorYear, ELECTION_END_MONTH, day = ELECTION_END_DAY).asTimeMark()
    }

    private fun getTimeTillNextMayor() {
        nextMayorTimestamp = calculateNextMayorTime()
    }

    private fun checkCurrentMayor() {
        val nextMayorTime = calculateNextMayorTime()

        // Check if it is still the mayor from the old SkyBlock year
        currentMayor = candidates[nextMayorTime.toSkyBlockTime().year - 1]?.let {
            if (it.name == lastMayor?.name) return

            // TODO: Once Jerry is active, add the sub mayor perks in here
            setAssumeMayorJson(it.name, it.perks)
        }
    }

    private fun checkHypixelAPI() {
        if (lastUpdate.passedSince() < 20.minutes || (currentMayor == Mayor.UNKNOWN && lastUpdate.passedSince() < 1.minutes)) return
        lastUpdate = SimpleTimeMark.now()

        SkyHanniMod.coroutineScope.launch {
            val url = "https://api.hypixel.net/v2/resources/skyblock/election"
            val jsonObject = withContext(dispatcher) { APIUtil.getJSONResponse(url) }
            rawMayorData = ConfigManager.gson.fromJson<MayorJson>(jsonObject)
            val data = rawMayorData ?: return@launch
            val map = mutableMapOf<Int, MayorCandidate>()
            map put data.mayor.election.getPairs()
            data.current?.let {
                map put data.current.getPairs()
            }
            candidates = map
            checkCurrentMayor()
        }
    }

    private fun MayorElection.getPairs() = year + 1 to candidates.bestCandidate()

    private fun List<MayorCandidate>.bestCandidate() = maxBy { it.votes }

    @SubscribeEvent
    fun onConfigReload(event: ConfigLoadEvent) {
        SkyHanniMod.feature.dev.debug.assumeMayor.onToggle {
            val mayor = SkyHanniMod.feature.dev.debug.assumeMayor.get()

            if (mayor == Mayor.DISABLED) {
                checkCurrentMayor()
            } else {
                mayor.setAssumeMayor(mayor.perks.toList())
                currentMayor = mayor
            }
        }
    }

    @SubscribeEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Mayor")
        event.addIrrelevant {
            add("Current Mayor: ${currentMayor?.name ?: "Unknown"}")
            add("Active Perks: ${currentMayor?.activePerks}")
            add("Last Update: $lastUpdate (${lastUpdate.passedSince()} ago)")
            add("Time Till Next Mayor: ${nextMayorTimestamp.timeUntil()}")
            if (jerryExtraMayor.first != null) {
                add("Jerry Mayor: ${jerryExtraMayor.first?.name} expiring at: ${jerryExtraMayor.second.timeUntil()}")
            }
        }
    }
}
