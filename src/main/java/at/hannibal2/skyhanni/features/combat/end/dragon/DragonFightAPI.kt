package at.hannibal2.skyhanni.features.combat.end.dragon

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.EndBoss
import at.hannibal2.skyhanni.events.EndBossDeathEvent
import at.hannibal2.skyhanni.events.EndBossFightEndEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

/**
 * Single data layer for the End boss fights: owns every regex, reads chat, scoreboard and tab
 * list, keeps [DragonFightState] up to date and publishes the result as [EndBossFightEndEvent].
 * Features only consume this and never parse anything themselves.
 */
@SkyHanniModule
object DragonFightAPI {

    private val dragonNames = DragonType.entries
        .filter { it != DragonType.UNKNOWN }
        .joinToString("|") { it.name.firstLetterUppercase() }

    private val group = RepoPattern.group("combat.end-dragon-fight")
    private val chatGroup = group.group("chat")
    private val endGroup = group.group("chat.end")
    private val scoreboardGroup = group.group("scoreboard")



    /**
     * REGEX-TEST: ☬ The Protector Dragon has spawned!
     * REGEX-TEST: ☬ The Young Dragon has spawned!
     */
    private val dragonSpawnPattern by chatGroup.pattern(
        "spawn",
        "☬ The (?<dragon>$dragonNames) Dragon has spawned!",
    )

    /**
     * REGEX-TEST: ☬ You placed a Summoning Eye! (2/8)
     * REGEX-TEST: ☬ You placed a Summoning Eye! Brace yourselves! (8/8)
     */
    @Suppress("MaxLineLength")
    private val eyePlacedPattern by chatGroup.pattern(
        "eye.placed.you",
        "☬ You placed a Summoning Eye!(?: Brace yourselves!)? \\(\\d/8\\)",
    )

    /**
     * REGEX-TEST: You recovered a Summoning Eye!
     */
    private val eyeRemovedPattern by chatGroup.pattern("eye.removed.you", "You recovered a Summoning Eye!")

    /**
     * REGEX-TEST: ☬ The Dragon Egg has spawned!
     */
    private val eggSpawnedPattern by chatGroup.pattern("egg.spawn", "☬ The Dragon Egg has spawned!")

    /**
     * WRAPPED-REGEX-TEST: "                          PROTECTOR DRAGON DOWN!"
     * WRAPPED-REGEX-TEST: "                          YOUNG DRAGON DOWN!"
     */
    private val dragonDownPattern by endGroup.pattern(
        "down.dragon",
        "\\s+(?:PROTECTOR|OLD|UNSTABLE|YOUNG|STRONG|WISE|SUPERIOR) DRAGON DOWN!",
    )

    /**
     * WRAPPED-REGEX-TEST: "                    END STONE PROTECTOR DOWN!"
     */
    private val protectorDownPattern by endGroup.pattern(
        "down.protector",
        "\\s+END STONE PROTECTOR DOWN!",
    )

    /**
     * WRAPPED-REGEX-TEST: "             1st Damager - [MVP+] hordiniii - 5,057,018"
     * WRAPPED-REGEX-TEST: "          2nd Damager - Andromeda126785 - 3,372,454"
     * WRAPPED-REGEX-TEST: "             3rd Damager - [MVP+] AvitasG - 1,975,795"
     */
    @Suppress("MaxLineLength")
    private val leaderboardPattern by endGroup.pattern(
        "place",
        "\\s+(?<position>\\d+).. Damager - (?:\\[[^ ]+\\] )?(?<name>.*) - (?<damage>[\\d.,]+)",
    )

    /**
     * WRAPPED-REGEX-TEST: "                      Your Damage: 0 (Position #24)"
     * WRAPPED-REGEX-TEST: "                 Your Damage: 5,057,018 (Position #1)"
     */
    @Suppress("MaxLineLength")
    private val yourDamagePattern by endGroup.pattern(
        "position",
        "\\s+Your Damage: (?<damage>[\\d.,]+) (?:\\(NEW RECORD!\\) )?\\(Position #(?<position>\\d+)\\)",
    )

    /**
     * Hypixel sends a plain ❤ (U+2764) here, not the private-use health icon (U+E010).
     * Both are accepted so either format keeps working.
     *
     * WRAPPED-REGEX-TEST: " hordiniii: 3.6M❤"
     * WRAPPED-REGEX-TEST: " Andromeda126785: 816.9k❤"
     * WRAPPED-REGEX-TEST: " Paulinkaxcv: 17k❤"
     */
    private val tabDamagePattern by group.pattern(
        "tablist.player",
        "\\s(?<name>.+): (?<damage>[\\d.]+[kMB]?)[❤\\uE010].*",
    )

    /**
     * REGEX-TEST: Dragon HP: 14,659,354 ❤
     * REGEX-TEST: Protector HP: 2,317,156 ❤
     */
    private val scoreboardHpPattern by scoreboardGroup.pattern(
        "hp",
        "(?:Protector|Dragon) HP: (?<hp>[\\d,.]+) .*",
    )

    /**
     * REGEX-TEST: Your Damage: 2,003.2
     */
    private val scoreboardDamagePattern by scoreboardGroup.pattern(
        "your-damage",
        "Your Damage: (?<damage>[\\w,.]+)",
    )

    private val nestAreaPattern by group.pattern("area.nest", "Dragon's Nest")
    // </editor-fold>

    fun inNestArea() = IslandType.THE_END.isInIsland() && nestAreaPattern.matches(SkyBlockUtils.graphArea)

    data class DamageEntry(val name: String, val rawDamage: String, val damage: Double)

    /** Live damage leaderboard of the running fight, highest first. */
    var damageEntries: List<DamageEntry> = emptyList()
        private set

    /** Damage of the current first place, 0.0 while unknown. */
    val topDamage: Double get() = damageEntries.firstOrNull()?.damage ?: 0.0

    /** Own placement (1-based) among the listed entries, null while not listed. */
    val ownPlace: Int?
        get() = damageEntries.indexOfFirst { it.name == PlayerUtils.getName() }.takeIf { it >= 0 }?.plus(1)

    // Consumed by the damage indicator, which shows the boss name and its health.
    var currentType: String? = null
        private set
    var currentHp: Int? = null
        private set

    private var endingBoss: EndBoss? = null
    private var endTopDamage = 0.0

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage

        when {
            eyePlacedPattern.matches(message) -> DragonFightState.eyesPlaced++
            eyeRemovedPattern.matches(message) -> DragonFightState.eyesPlaced--
            eggSpawnedPattern.matches(message) -> DragonFightState.eggSpawned = true
            dragonDownPattern.matches(message) -> onBossDown(EndBoss.DRAGON)
            protectorDownPattern.matches(message) -> onBossDown(EndBoss.END_STONE_PROTECTOR)
            else -> {
                dragonSpawnPattern.matchMatcher(message) {
                    val type = DragonType.getByName(group("dragon").uppercase())
                    currentType = type.displayName
                    DragonFightState.onDragonSpawn(type)
                    return
                }
                handleFightEnd(message)
            }
        }
    }

    /**
     * The loot already lies on the ground while the summary is still printing, so the death
     * message - not the summary - is what opens the scan window.
     */
    private fun onBossDown(boss: EndBoss) {
        endingBoss = boss
        EndBossDeathEvent(boss).post()
    }

    /** Reads the end-of-fight summary that dragons and the protector share. */
    private fun handleFightEnd(message: String) {
        val boss = endingBoss ?: return

        leaderboardPattern.matchMatcher(message) {
            if (group("position") == "1") endTopDamage = group("damage").formatDouble()
            return
        }

        // The own damage line closes the summary, so the result is complete here.
        yourDamagePattern.matchMatcher(message) {
            EndBossFightEndEvent(
                boss = boss,
                place = group("position").formatInt(),
                yourDamage = group("damage").formatDouble(),
                topDamage = endTopDamage,
            ).post()
            endingBoss = null
            endTopDamage = 0.0
            currentType = null
            currentHp = null
            if (boss == EndBoss.DRAGON) DragonFightState.reset()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onTabList(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.DRAGON)) return
        // The first line is the widget header, damage entries follow once players deal damage.
        damageEntries = if (event.isClear()) emptyList() else event.cleanLines.drop(1).mapNotNull { line ->
            tabDamagePattern.matchMatcher(line) {
                val raw = group("damage")
                DamageEntry(group("name"), raw, raw.formatDouble())
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onScoreboard(event: ScoreboardUpdateEvent) {
        // The scoreboard event carries the raw lines, so they are stripped here - the patterns
        // themselves stay colorless like everywhere else.
        val lines = event.new.map { it.removeColor() }
        val index = lines.indexOfFirst { scoreboardHpPattern.matches(it) }
        if (index == -1) return

        if (DragonFightState.eggSpawned) DragonFightState.dragonSpawned = true
        scoreboardHpPattern.matchMatcher(lines[index]) {
            currentHp = group("hp").formatIntOrNull()
        }
        scoreboardDamagePattern.matchMatcher(lines[index + 1]) {
            DragonFightState.yourDamage = group("damage").formatDouble()
        }
    }

    @HandleEvent
    private fun onIslandChange(event: IslandChangeEvent) {
        damageEntries = emptyList()
        endingBoss = null
        endTopDamage = 0.0
        currentType = null
        currentHp = null
        DragonFightState.reset()
        DragonFightState.eggSpawned = true
    }
}
