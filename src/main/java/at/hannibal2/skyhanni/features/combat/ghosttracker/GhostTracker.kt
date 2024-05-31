package at.hannibal2.skyhanni.features.combat.ghosttracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SkillExpGainEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GhostTracker {

    private val config get() = SkyHanniMod.feature.combat.ghostCounter

    val storage = ProfileStorageData.profileSpecific?.ghostStorage

    private var currentBestiaryKills = 0L
    private var isMaxBestiary = false

    private var allowedDrops = listOf<NEUInternalName>()

    private val MAX_BESTIARY_KILLS by lazy { getBestiaryKillsUntilLevel(25) }

    private val tracker = SkyHanniItemTracker(
        "Ghost Tracker",
        { Data() },
        { it.ghostStorage.ghostTracker }) { drawDisplay(it) }

    class Data : ItemTrackerData() {

        override fun resetItems() {
            kills = 0
            ghostsSinceSorrow = 0
            maxKillCombo = 0
            combatXpGained = 0
        }

        @Expose
        var kills = 0L

        @Expose
        var ghostsSinceSorrow = 0L

        @Expose
        var maxKillCombo = 0L

        @Expose
        var combatXpGained = 0L

        @Expose
        var totalMagicFind = 0L

        @Expose
        var totalMagicFindKills = 0L

        override fun getDescription(timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / kills
            val perKill = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))

            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop chance per kill: §c$perKill"
            )
        }

        override fun getCoinName(item: TrackedItem) = "§6Dropped Coins"

        override fun getCoinDescription(item: TrackedItem): List<String> {
            val coinsFormat = NumberUtil.format(item.totalAmount)
            return listOf(
                "§7Killing ghosts gives you coins (more with scavenger).",
                "§7You got §6$coinsFormat coins §7that way."
            )
        }
    }

    private val patternGroup = RepoPattern.group("combat.ghosttracker")

    private val itemDropPattern by patternGroup.pattern(
        "itemdrop",
        "§6§lRARE DROP! §r§9(?<item>[^§]*) §r§b\\([+](?:§.)*(?<mf>\\d*)% §r§b✯ Magic Find§r§b\\)"
    )
    private val killComboEndPattern by patternGroup.pattern(
        "killcombo.end",
        "§cYour Kill Combo has expired! You reached a (?<kill>\\d+) Kill Combo!"
    )
    private val bagOfCashPattern by patternGroup.pattern(
        "bagofcash",
        "§eThe ghost's death materialized §r§61,000,000 coins §r§efrom the mists!"
    )

    /**
     * REGEX-TEST:  Ghost 15§r§f: §r§b12,449/12,500
     */
    private val bestiaryTablistPattern by patternGroup.pattern(
        "tablist.bestiary",
        "\\s*Ghost (?<level>\\d+|[XVI]+)(?:§.)*: (?:§.)*(?<kills>[\\d,.]+)/(?<killsToNext>[\\d,.]+)"
    )
    private val maxBestiaryTablistPattern by patternGroup.pattern(
        "tablist.bestiarymax",
        "\\s*Ghost (?<level>\\d+|[XVI]+)(?:§.)*: (?:§.)*MAX"
    )


    private val SORROW by lazy { "SORROW".asInternalName() }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        config.ghostTrackerText.forEach { line ->
            val lines = getLine(line, data)
            lines.forEach { add(it) }
        }
    }

    private fun getLine(line: GhostTrackerLines, data: Data): List<List<Any>> {
        val itemsList = mutableListOf<List<Any>>()
        val profit = tracker.drawItems(data, { true }, itemsList)
        return when (line) {
            GhostTrackerLines.TITLE -> listOf(listOf("§e§lGhost Profit Tracker"))
            GhostTrackerLines.ITEMS -> itemsList
            GhostTrackerLines.KILLS -> listOf(listOf("§7Kills: §e${data.kills.addSeparators()}"))
            GhostTrackerLines.GHOSTS_SINCE_SORROW -> listOf(listOf("§7Ghosts Since Sorrow: §e${data.ghostsSinceSorrow.addSeparators()}"))
            GhostTrackerLines.MAX_KILL_COMBO -> listOf(listOf("§7Max Kill Combo: §e${data.maxKillCombo.addSeparators()}"))
            GhostTrackerLines.COMBAT_XP_GAINED -> listOf(listOf("§7Combat XP Gained: §e${data.combatXpGained.addSeparators()}"))
            GhostTrackerLines.AVERAGE_MAGIC_FIND -> listOf(
                listOf(
                    "§7Average Magic Find: §e${
                        getAverageMagicFind(
                            data.totalMagicFind,
                            data.totalMagicFindKills
                        )
                    }"
                )
            )

            GhostTrackerLines.BESTIARY_KILLS -> listOf(listOf("§7Bestiary Kills: §e" + if (currentBestiaryKills >= MAX_BESTIARY_KILLS) "MAX" else currentBestiaryKills.addSeparators()))
            GhostTrackerLines.TOTAL_PROFIT -> listOf(listOf("§7Total Profit: §6${profit.addSeparators()} coins"))
        }
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        currentBestiaryKills = storage?.bestiaryKills ?: 0
        isMaxBestiary = currentBestiaryKills >= MAX_BESTIARY_KILLS
    }

    @SubscribeEvent
    fun onSkillExp(event: SkillExpGainEvent) {
        if (!isEnabled()) return
        if (event.gained > 10_000) return
        tracker.modify {
            it.combatXpGained += event.gained.toLong()
        }
    }

    @SubscribeEvent
    fun onPurseChange(event: PurseChangeEvent) {
        if (!isEnabled()) return
        if (event.reason != PurseChangeCause.GAIN_MOB_KILL) return
        if (event.coins !in 200.0..2_000.0) return

        tracker.addCoins(event.coins.toInt())
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        itemDropPattern.matchMatcher(event.message) {
            val internalName = NEUInternalName.fromItemNameOrNull(group("item")) ?: return
            val mf = group("mf").formatInt()
            if (!isAllowedItem(internalName)) return

            tracker.addItem(internalName, 1)
            tracker.modify {
                it.totalMagicFind += mf
                it.totalMagicFindKills++

                if (internalName == SORROW) {
                    it.ghostsSinceSorrow = 0
                }
            }
            return
        }
        killComboEndPattern.matchMatcher(event.message) {
            val kill = group("kill").formatInt().toLong()
            tracker.modify {
                it.maxKillCombo = kill.coerceAtLeast(it.maxKillCombo)
            }
            return
        }
        if (bagOfCashPattern.matches(event.message)) {
            tracker.addCoins(1_000_000)
            return
        }
    }

    @SubscribeEvent
    fun onTablistUpdate(event: TabListUpdateEvent) {
        if (!isEnabled()) return
        if (isMaxBestiary) return
        event.tabList.forEach { line ->
            bestiaryTablistPattern.matchMatcher(line) {
                val level = group("level").romanToDecimalIfNecessary()
                val currentLevelKills = group("kills").formatInt().toLong()

                val kills = getTotalBestiaryKills(level, currentLevelKills)
                if (kills <= currentBestiaryKills) return
                val difference = kills - currentBestiaryKills

                if (difference > 50) {
                    currentBestiaryKills = kills
                    return
                }

                currentBestiaryKills = kills

                tracker.modify {
                    it.kills += difference
                    it.ghostsSinceSorrow += difference
                }
                return
            }
            if (maxBestiaryTablistPattern.matches(line)) {
                isMaxBestiary = true
                currentBestiaryKills = MAX_BESTIARY_KILLS.toLong()
                return
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        tracker.renderDisplay(config.trackerPosition)
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        //allowedDrops = event.getConstant<GhostDrops>("GhostDrops").ghost_drops
        allowedDrops = listOf(
            SORROW,
            "PLASMA".asInternalName(),
            "VOLTA".asInternalName(),
            "GHOST_BOOTS".asInternalName(),
        )
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.DWARVEN_MINES) {
            tracker.firstUpdate()
        }
    }

    private fun isAllowedItem(internalName: NEUInternalName): Boolean = internalName in allowedDrops

    private fun getAverageMagicFind(mf: Long, kills: Long) =
        if (mf == 0L || kills == 0L) 0.0 else mf / (kills).toDouble()


    private fun isEnabled() =
        IslandType.DWARVEN_MINES.isInIsland() && LorenzUtils.skyBlockArea == "The Mist" && config.enabled

    enum class GhostTrackerLines(private val display: String) {
        TITLE("§e§lGhost Profit Tracker\""),
        ITEMS(
            " §723x §9Sorrow§7: §67.47M\n" +
                " §713 §9Plasma§7: §6260k\n" +
                " §741 §9Volta§7: §6195k\n"
        ),
        KILLS("§7Kills: §e7,813"),
        GHOSTS_SINCE_SORROW("§7Ghosts Since Sorrow: §e71"),
        MAX_KILL_COMBO("§7Max Kill Combo: §e681"),
        COMBAT_XP_GAINED("§7Combat XP Gained: §e4,687,800"),
        AVERAGE_MAGIC_FIND("§7Average Magic Find: §b278.9"),
        BESTIARY_KILLS("§7Bestiary Kills: §e 71,893"),
        TOTAL_PROFIT("§7Total Profit: §67,928,713 coins"),
        ;

        override fun toString(): String {
            return display
        }
    }

    private fun getTotalBestiaryKills(level: Int, kills: Long) = getBestiaryKillsUntilLevel(level) + kills

    private fun getBestiaryKillsUntilLevel(level: Int): Int {
        var killsUntilLevel = 0
        for (i in 1..level) {
            killsUntilLevel += getBestiaryKillsInLevel(i)
        }
        return killsUntilLevel
    }

    private fun getBestiaryKillsInLevel(level: Int): Int {
        return when (level) {
            in 1..3 -> 5
            4 -> 10
            5 -> 25
            6 -> 50
            7 -> 100
            in 8..9 -> 150
            10 -> 250
            11 -> 750
            12 -> 1_500
            13 -> 2_000
            in 14..17 -> 2_500
            18 -> 3_000
            in 19..20 -> 3_500
            21 -> 25_000
            in 22..25 -> 50_000
            else -> 0
        }
    }

}
