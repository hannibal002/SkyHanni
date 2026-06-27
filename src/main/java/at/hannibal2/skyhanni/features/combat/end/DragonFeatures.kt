package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatchers
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.indexOfFirstOrNull
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DragonFeatures {

    private val config get() = SkyHanniMod.feature.combat.endIsland.dragon
    private val trackerConfig get() = SkyHanniMod.feature.combat.endIsland.dragon.dragonProfitTracker
    private val configProtector get() = SkyHanniMod.feature.combat.endIsland.endstoneProtectorChat

    private val dragonNames: List<String> = DragonType.entries
        .filter { it != DragonType.UNKNOWN }
        .map { it.name.firstLetterUppercase() }

    private val dragonNamesAsRegex = dragonNames.joinToString("|")
    private val dragonNamesAsRegexUppercase = dragonNames.joinToString("|") { it.uppercase() }

    private val protectorRepoGroup = RepoPattern.group("combat.boss.protector.1")
    private val repoGroup = RepoPattern.group("combat.boss.dragon.1")
    private val chatGroup = repoGroup.group("chat")
    private val scoreBoardGroup = repoGroup.group("scoreboard")
    private val tabListGroup = repoGroup.group("tablist-nocolor")

    /**
     * REGEX-TEST: ☬ You placed a Summoning Eye! (2/8)
     * REGEX-TEST: ☬ You placed a Summoning Eye! Brace yourselves! (8/8)
     */
    private val eyePlacedPattern by chatGroup.list(
        "eye.placed.you.list",
        "☬ You placed a Summoning Eye! \\(\\d/8\\)",
        "☬ You placed a Summoning Eye! Brace yourselves! \\(8/8\\)",
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
     * WRAPPED-REGEX-TEST: "                      PROTECTOR DRAGON DOWN!"
     */
    private val endStartLineDragonPattern by chatGroup.pattern(
        "end.boss",
        " +(?<dragon>$dragonNamesAsRegexUppercase) DRAGON DOWN!",
    )

    /**
     * WRAPPED-REGEX-TEST: "                    ENDSTONE PROTECTOR DOWN!"
     */
    private val endStartLineProtectorPattern by protectorRepoGroup.pattern(
        "chat.end.boss",
        " +ENDSTONE PROTECTOR DOWN!",
    )

    /**
     * WRAPPED-REGEX-TEST: "                   Your Damage: 88,966 (Position #5)"
     * WRAPPED-REGEX-TEST: "                 Your Damage: 3,198,068 (Position #1)"
     */
    private val endPositionPattern by chatGroup.pattern(
        "end.position",
        " +Your Damage: (?<damage>[\\d.,]+) (?:\\(NEW RECORD!\\) )?\\(Position #(?<position>\\d+)\\)",
    )

    /**
     * WRAPPED-REGEX-TEST: "             1st Damager - [VIP] Jarre07 - 9,659,033"
     * WRAPPED-REGEX-TEST: "          2nd Damager - [MVP+] FlamingZoom - 1,459,691"
     * WRAPPED-REGEX-TEST: "          3rd Damager - [MVP+] Dustbringer - 1,091,163"
     * WRAPPED-REGEX-TEST: "              1st Damager - [VIP] filip_zd - 3,965,533"
     */
    @Suppress("MaxLineLength")
    private val endLeaderboardPattern by chatGroup.pattern(
        "end.place",
        " +(?<position>\\d+)\\w{2} Damager - (?:\\[[^ ]+\\] )?(?<name>.*) - (?<damage>[\\d.,]+)",
    )

    /**
     * WRAPPED-REGEX-TEST: "                       Zealots Contributed: 27/100"
     */
    private val endZealotsPattern by protectorRepoGroup.pattern(
        "chat.end.zealot",
        " +Zealots Contributed: (?<amount>\\d+)/100",
    )

    /**
     * REGEX-TEST: ☬ The Protector Dragon has spawned!
     * REGEX-TEST: ☬ The Young Dragon has spawned!
     */
    private val dragonSpawnPattern by chatGroup.pattern(
        "spawn",
        "☬ The (?<dragon>$dragonNamesAsRegex) Dragon has spawned!",
    )

    /**
     * REGEX-TEST: Your Damage: 2,003.2
     */
    private val scoreDamagePattern by scoreBoardGroup.pattern("damage", "Your Damage: (?<damage>[\\d,.]+)")

    /**
     * REGEX-TEST: Dragon HP: 14,659,354 ❤
     */
    private val scoreDragonPattern by scoreBoardGroup.pattern("dragon", "Dragon HP: [\\d,.]+ ❤")

    /**
     * WRAPPED-REGEX-TEST: " JamBeastie: 7.4M❤"
     * WRAPPED-REGEX-TEST: " 42069HzMonitor: 3M❤"
     * WRAPPED-REGEX-TEST: " ItsJxxxxx2001: 457k❤"
     * WRAPPED-REGEX-TEST: " Thunderblade73: 12.3k❤"
     */
    private val tabDamagePattern by tabListGroup.pattern(
        "fight.player",
        " (?<name>.+): (?<damage>[\\d.]+[kM]?)❤",
    )

    private var yourEyes = 0

    private var dragonSpawned = false
        set(value) {
            field = value
            if (value) eggSpawned = false
        }

    private enum class EndType {
        GOLEM,
        DRAGON
    }

    private var endType: EndType? = null
    private var endTopDamage = 0.0
    private var endDamage = 0.0
    private var endPlace = 0

    private var dirty = false

    private fun <T> dirtyTracking(initial: T): kotlin.properties.ReadWriteProperty<Any?, T> =
        Delegates.observable(initial) { _, old, new ->
            if (old != new) dirty = true
        }

    private var currentDamage by dirtyTracking(0.0)
    private var currentTopDamage by dirtyTracking(0.0)
    private var currentPlace by dirtyTracking<Int?>(null)

    private var widgetActive = false
    var eggSpawned = true
    var weight = 0.0
    private var currentDragonType: DragonType? = null

    private fun resetEnd() {
        endType = null
        endTopDamage = 0.0
        endDamage = 0.0
        endPlace = 0
    }

    private fun reset() {
        resetEnd()
        dragonSpawned = false
        currentTopDamage = 0.0
        currentDamage = 0.0
        currentPlace = null
        widgetActive = false
        yourEyes = 0
        currentDragonType = null
        display = emptyList()
    }

    private fun getWeightForPlacement(place: Int) = when (place) {
        -1 -> 10
        1 -> 200
        2 -> 175
        3 -> 150
        4 -> 125
        5 -> 110
        6, 7, 8 -> 100
        9, 10 -> 90
        11, 12 -> 80
        else -> 70
    }

    private fun calculateDragonWeight(eyes: Int, place: Int, firstDamage: Double, yourDamage: Double) =
        getWeightForPlacement(
            if (yourDamage == 0.0) -1 else place,
        ) + 100 * (
            eyes + yourDamage / (firstDamage.takeIf { it != 0.0 } ?: 1.0)
            )

    private fun calculateProtectorWeight(zealots: Int, place: Int, firstDamage: Double, yourDamage: Double) =
        getWeightForPlacement(
            if (yourDamage == 0.0) -1 else place,
        ) + 50 * (
            yourDamage / (firstDamage.takeIf { it != 0.0 } ?: 1.0)
            ) + if (zealots > 100) 100 else zealots

    private fun displayIsEnabled() = config.display && dragonSpawned

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage

        if (handleDragonSpawn(message)) return
        if (handleEyeEvents(message)) return
        if (handleEggSpawn(message)) return
        if (handleEndStart(message)) return
        if (handleEndLeaderboard(message)) return
        if (handleEndPosition(message)) return
        if (handleZealots(message)) return
    }

    private fun handleDragonSpawn(message: String): Boolean {
        dragonSpawnPattern.matchMatcher(message) {
            dragonSpawned = true
            val dragon = group("dragon")
            currentDragonType = DragonType.getByName(dragon.uppercase())
            if (currentDragonType?.equals(DragonType.UNKNOWN) == true) {
                ErrorManager.logErrorStateWithData(
                    userMessage = "Could not read dragon type from spawn message",
                    internalMessage = "DragonType enum is unknown",
                    "dragon" to dragon,
                )
            }
        } ?: return false

        ChatUtils.debug("Dragon Type: $currentDragonType")

        if (config.superiorNotify && currentDragonType == DragonType.SUPERIOR) {
            TitleManager.sendTitle("§6Superior Dragon Spawned!", duration = 1.5.seconds)
        }

        DragonProfitTracker.addEyes(yourEyes)
        return true
    }

    private fun handleEyeEvents(message: String): Boolean {
        eyePlacedPattern.matchMatchers(message) {
            yourEyes++
            return true
        }

        eyeRemovedPattern.matchMatcher(message) {
            yourEyes--
            return true
        }

        return false
    }

    private fun handleEndStart(message: String): Boolean {
        endStartLineDragonPattern.matchMatcher(message) {
            if (!config.chat) {
                reset()
            } else {
                endType = EndType.DRAGON
            }
            return true
        }
        endStartLineProtectorPattern.matchMatcher(message) {
            if (!configProtector) return false
            endType = EndType.GOLEM
            return true
        }
        return false
    }

    private fun handleEndLeaderboard(message: String): Boolean {
        return endLeaderboardPattern.matchMatcher(message) {
            if (endType == null) return false
            if (group("position") != "1") return false

            endTopDamage = group("damage").formatDouble()
            true
        } ?: false
    }

    private fun handleEndPosition(message: String): Boolean {
        val endType = endType ?: return false
        endPositionPattern.matchMatcher(message) {
            endPlace = group("position").formatInt()
            endDamage = group("damage").formatDouble()
        } ?: return false

        when (endType) {
            EndType.DRAGON -> {
                weight = calculateDragonWeight(yourEyes, endPlace, endTopDamage, endDamage)

                if (endDamage > 0) {
                    if (!(yourEyes == 0 && !trackerConfig.countLeechedDragons)) {
                        DragonProfitTracker.addDragonKill(currentDragonType ?: DragonType.UNKNOWN)
                        DragonProfitTracker.addDragonLoot(
                            currentDragonType ?: DragonType.UNKNOWN,
                            "ESSENCE_DRAGON".toInternalName(),
                            if (currentDragonType == DragonType.SUPERIOR) 10 else 5,
                        )
                    }
                }

                DragonProfitTracker.lastDragonPlacement = endPlace
                ChatUtils.debug("Dragon type: $currentDragonType, placement: ${DragonProfitTracker.lastDragonPlacement}")

                printWeight(weight)
                ProfitPerDragon.finishedLoot = false
                reset()
            }

            EndType.GOLEM -> {
                // NO reset because of Zealot Line
            }
        }
        return true
    }

    private fun handleZealots(message: String): Boolean {
        if (endType != EndType.GOLEM) return false

        val zealots = endZealotsPattern.matchMatcher(message) {
            group("amount").toInt()
        } ?: return false

        val weight = calculateProtectorWeight(zealots, endPlace, endTopDamage, endDamage)

        printWeight(weight)
        resetEnd()

        return true
    }

    private fun handleEggSpawn(message: String): Boolean {
        eggSpawnedPattern.matchMatcher(message) {
            eggSpawned = true
            return true
        }
        return false
    }

    private fun printWeight(weight: Double) {
        if (!config.chat) return
        val space = " ".repeat(if (config.skyhanniMessagePrefix) 16 else 30)
        val weightString = weight.roundTo(0).addSeparators()
        ChatUtils.chat(
            "§f$space§r§eYour Weight: §r§a$weightString",
            prefix = config.skyhanniMessagePrefix,
        )
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    fun onScoreBoard(event: ScoreboardUpdateEvent) {
        val index = event.new.indexOfFirstOrNull { scoreDragonPattern.matches(it) } ?: return
        if (eggSpawned) {
            dragonSpawned = true
        }
        scoreDamagePattern.matchMatcher(event.new[index + 1]) {
            currentDamage = group("damage").formatDouble()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    fun onTabList(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.DRAGON)) return
        if (!dragonSpawned) return
        widgetActive = true
        for (i in 1 until event.lines.size) {
            tabDamagePattern.matchMatcher(event.lines[i]) {
                if (i == 1) {
                    currentTopDamage = group("damage").formatDouble()
                }
                if (group("name") == PlayerUtils.getName()) {
                    currentPlace = if (i > 3) null else i
                }
            }
        }
    }

    private val widgetErrorMessage = listOf(Renderable.text("§cDragon Widget is disabled!"))

    private var display = listOf<Renderable>()

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    fun onRender(event: GuiRenderEvent) {
        if (!config.display) return
        if (dirty) {
            display = if (widgetActive) display() else widgetErrorMessage
        }
        config.displayPosition.renderRenderables(display, posLabel = "Dragon Weight")
    }

    private fun display() = listOf(
        Renderable.hoverTips(
            "§6Current Weight: §f${
                calculateDragonWeight(yourEyes, currentPlace ?: 6, currentTopDamage, currentDamage)
                    .roundTo(1).addSeparators()
            }",
            listOf(
                "Eyes: $yourEyes",
                "Place: ${currentPlace ?: if (currentDamage != 0.0) "unknown, assuming 6th" else "not damaged yet"}",
                "Damage Ratio: ${(currentDamage / (currentTopDamage.takeIf { it != 0.0 } ?: 1.0)).formatPercentage()}",
            ),
        ),
    )

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        reset()
        eggSpawned = true
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(78, "combat.dragon", "combat.endIsland.dragon")
        event.move(78, "combat.endstoneProtectorChat", "combat.endIsland.endstoneProtectorChat")
    }
}
