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

    // <editor-fold desc="Patterns">

    /**
     * REGEX-TEST: §5☬ §r§d§lThe §r§5§c§lProtector Dragon§r§d§l has spawned!
     * REGEX-TEST: §5☬ §r§d§lThe §r§5§c§lYoung Dragon§r§d§l has spawned!
     */
    private val dragonSpawnPattern by chatGroup.pattern(
        "spawn",
        "§5☬ §r§d§lThe §r§5§c§l(?<dragon>$dragonNames) Dragon§r§d§l has spawned!",
    )

    /**
     * REGEX-TEST: §5☬ §r§dYou placed a Summoning Eye! §r§7(§r§e2§r§7/§r§a8§r§7)
     * REGEX-TEST: §5☬ §r§dYou placed a Summoning Eye! Brace yourselves! §r§7(§r§a8§r§7/§r§a8§r§7)
     */
    @Suppress("MaxLineLength")
    private val eyePlacedPattern by chatGroup.pattern(
        "eye.placed.you",
        "§5☬ §r§dYou placed a Summoning Eye! §r§7\\(§r§e\\d§r§7\\/§r§a8§r§7\\)|§5☬ §r§dYou placed a Summoning Eye! Brace yourselves! §r§7\\(§r§a8§r§7\\/§r§a8§r§7\\)",
    )

    /**
     * REGEX-TEST: §5You recovered a Summoning Eye!
     */
    private val eyeRemovedPattern by chatGroup.pattern("eye.removed.you", "§5You recovered a Summoning Eye!")

    /**
     * REGEX-TEST: §5☬ §r§dThe Dragon Egg has spawned!
     */
    private val eggSpawnedPattern by chatGroup.pattern("egg.spawn", "§5☬ §r§dThe Dragon Egg has spawned!")

    /**
     * WRAPPED-REGEX-TEST: "                          §r§6§lPROTECTOR DRAGON DOWN!"
     * WRAPPED-REGEX-TEST: "                          §r§6§lYOUNG DRAGON DOWN!"
     */
    private val dragonDownPattern by endGroup.pattern(
        "down.dragon",
        "\\s+§r§6§l(?:PROTECTOR|OLD|UNSTABLE|YOUNG|STRONG|WISE|SUPERIOR) DRAGON DOWN!",
    )

    /**
     * WRAPPED-REGEX-TEST: "                    §r§6§lEND STONE PROTECTOR DOWN!"
     */
    private val protectorDownPattern by endGroup.pattern(
        "down.protector",
        "\\s+§r§6§lEND ?STONE PROTECTOR DOWN!",
    )

    /**
     * WRAPPED-REGEX-TEST: "             §r§e§l1st Damager §r§7- §r§b[MVP§r§c+§r§b] hordiniii§r§f §r§7- §r§e5,057,018"
     * WRAPPED-REGEX-TEST: "          §r§6§l2nd Damager §r§7- §r§7Andromeda126785§r§7 §r§7- §r§e3,372,454"
     * WRAPPED-REGEX-TEST: "             §r§c§l3rd Damager §r§7- §r§b[MVP§r§c+§r§b] AvitasG§r§f §r§7- §r§e1,975,795"
     */
    @Suppress("MaxLineLength")
    private val leaderboardPattern by endGroup.pattern(
        "place",
        "\\s+§r§.§l(?<position>\\d+).. Damager §r§7- §r§.(?:\\[[^ ]+\\] )?(?<name>.*)§r§. §r§7- §r§e(?<damage>[\\d.,]+)",
    )

    /**
     * WRAPPED-REGEX-TEST: "                      §r§eYour Damage: §r§a0 §r§7(Position #24)"
     * WRAPPED-REGEX-TEST: "                 §r§eYour Damage: §r§a5,057,018 §r§7(Position #1)"
     */
    @Suppress("MaxLineLength")
    private val yourDamagePattern by endGroup.pattern(
        "position",
        "\\s+§r§eYour Damage: §r§a(?<damage>[\\d.,]+) (?:§r§d§l\\(NEW RECORD!\\) )?§r§7\\(Position #(?<position>\\d+)\\)",
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
     * REGEX-TEST: Dragon HP: §a14,659,354 §c❤
     * REGEX-TEST: Protector HP: §a2,317,156 §c❤
     */
    private val scoreboardHpPattern by scoreboardGroup.pattern(
        "hp",
        "(?:Protector|Dragon) HP: §a(?<hp>[\\d,.]+) .*",
    )

    /**
     * REGEX-TEST: Your Damage: §c2,003.2
     */
    private val scoreboardDamagePattern by scoreboardGroup.pattern(
        "your-damage",
        "Your Damage: §c(?<damage>[\\w,.]+)",
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
        val message = event.message

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
        val index = event.new.indexOfFirst { scoreboardHpPattern.matches(it) }
        if (index == -1) return

        if (DragonFightState.eggSpawned) DragonFightState.dragonSpawned = true
        scoreboardHpPattern.matchMatcher(event.new[index]) {
            currentHp = group("hp").removeColor().formatIntOrNull()
        }
        scoreboardDamagePattern.matchMatcher(event.new[index + 1]) {
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
