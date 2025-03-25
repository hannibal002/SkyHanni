package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object DragonFeatures {

    private val config get() = SkyHanniMod.feature.combat.endIsland.dragon
    private val leechBool get() = SkyHanniMod.feature.combat.endIsland.dragonProfitTracker
    private val configProtector get() = SkyHanniMod.feature.combat.endIsland.endstoneProtectorChat

    private val dragonNames: List<String> = DragonType.entries
        .filter { it != DragonType.UNKNOWN }
        .map { it.name.firstLetterUppercase() }

    private val dragonNamesAsRegex = dragonNames.joinToString("|")
    private val dragonNamesAsRegexUppercase = dragonNames.joinToString("|") { it.uppercase() }

    private val protectorRepoGroup = RepoPattern.group("combat.boss.protector")
    private val repoGroup = RepoPattern.group("combat.boss.dragon")
    private val chatGroup = repoGroup.group("chat")
    private val scoreBoardGroup = repoGroup.group("scoreboard")
    private val tabListGroup = repoGroup.group("tablist")

    /**
     * REGEX-TEST: §5☬ §r§dYou placed a Summoning Eye! §r§7(§r§e2§r§7/§r§a8§r§7)
     * REGEX-TEST: §5☬ §r§dYou placed a Summoning Eye! Brace yourselves! §r§7(§r§a8§r§7/§r§a8§r§7)
     */
    @Suppress("MaxLineLength")
    private val eyePlaced by chatGroup.pattern(
        "eye.placed.you",
        "§5☬ §r§dYou placed a Summoning Eye! §r§7\\(§r§e\\d§r§7\\/§r§a8§r§7\\)|§5☬ §r§dYou placed a Summoning Eye! Brace yourselves! §r§7\\(§r§a8§r§7\\/§r§a8§r§7\\)",
    )

    /**
     * REGEX-TEST: §5You recovered a Summoning Eye!
     */
    private val eyeRemoved by chatGroup.pattern("eye.removed.you", "§5You recovered a Summoning Eye!")

    /**
     * REGEX-TEST: §5☬ §r§dThe Dragon Egg has spawned!
     */
    private val eggSpawned by chatGroup.pattern("egg.spawn", "§5☬ §r§dThe Dragon Egg has spawned!")

    /**
     * REGEX-TEST: §f                      §r§6§lPROTECTOR DRAGON DOWN!
     */
    private val endStartLineDragon by chatGroup.pattern(
        "end.boss",
        "§f +§r§6§l(?<Dragon>$dragonNamesAsRegexUppercase) DRAGON DOWN!",
    )

    /**
     * REGEX-TEST: §f                    §r§6§lENDSTONE PROTECTOR DOWN!
     */
    private val endStartLineProtector by protectorRepoGroup.pattern(
        "chat.end.boss",
        "§f +§r§6§lENDSTONE PROTECTOR DOWN!",
    )

    /**
     * REGEX-TEST: §f                   §r§eYour Damage: §r§a88,966 §r§7(Position #5)
     * REGEX-TEST: §f                 §r§eYour Damage: §r§a3,198,068 §r§7(Position #1)
     */
    @Suppress("MaxLineLength")
    private val endPosition by chatGroup.pattern(
        "end.position",
        "§f +§r§eYour Damage: §r§a(?<Damage>[\\d.,]+) (?:§r§d§l\\(NEW RECORD!\\) )?§r§7\\(Position #(?<Position>\\d+)\\)",
    )

    // val endFinalHit by chatGroup.pattern("end.final", "§f                 §r§b[^ ]+ (?<Name>.*)§r§f §r§7dealt the final blow.")
    /**
     * REGEX-TEST: §f             §r§e§l1st Damager §r§7- §r§a[VIP] Jarre07§r§f §r§7- §r§e9,659,033
     * REGEX-TEST: §f          §r§6§l2nd Damager §r§7- §r§b[MVP§r§9+§r§b] FlamingZoom§r§f §r§7- §r§e1,459,691
     * REGEX-TEST: §f          §r§c§l3rd Damager §r§7- §r§b[MVP§r§f+§r§b] Dustbringer§r§f §r§7- §r§e1,091,163
     * REGEX-TEST: §f              §r§e§l1st Damager §r§7- §r§a[VIP] filip_zd§r§f §r§7- §r§e3,965,533
     */
    @Suppress("MaxLineLength")
    private val endLeaderboard by chatGroup.pattern(
        "end.place",
        "§f +§r§.§l(?<Position>\\d+).. Damager §r§7- §r§.(?:\\[[^ ]+\\] )?(?<Name>.*)§r§. §r§7- §r§e(?<Damage>[\\d.,]+)",
    )

    /**
     * REGEX-TEST: §f                       §r§eZealots Contributed: §r§a27§r§e/100
     */
    private val endZealots by protectorRepoGroup.pattern(
        "chat.end.zealot",
        "§f +§r§eZealots Contributed: §r§a(?<Amount>\\d+)§r§e/100",
    )

    /**
     * REGEX-TEST: §5☬ §r§d§lThe §r§5§c§lProtector Dragon§r§d§l has spawned!
     * REGEX-TEST: §5☬ §r§d§lThe §r§5§c§lYoung Dragon§r§d§l has spawned!
     */
    private val dragonSpawn by chatGroup.pattern(
        "spawn",
        "§5☬ §r§d§lThe §r§5§c§l(?<Dragon>$dragonNamesAsRegex) Dragon§r§d§l has spawned!",
    )

    /**
     * REGEX-TEST: Your Damage: §c2,003.2
     */
    private val scoreDamage by scoreBoardGroup.pattern("damage", "Your Damage: §c(?<Damage>[\\w,.]+)")

    /**
     * REGEX-TEST: Dragon HP: §a14,659,354 §c❤
     */
    private val scoreDragon by scoreBoardGroup.pattern("dragon", "Dragon HP: .*")

    // private val scoreProtector by protectorRepoGroup.pattern("scoreboard.protector", "Protector HP: .*")

    /**
     * REGEX-TEST:  §r§bJamBeastie: §r§c7.4M❤
     * REGEX-TEST:  §r§a42069HzMonitor: §r§c3M❤
     * REGEX-TEST:  §r§bItsJxxxxx2001: §r§c457k❤
     * REGEX-TEST:  §r§bThunderblade73: §r§c12.3k❤
     */
    private val tabDamagePattern by tabListGroup.pattern(
        "fight.player",
        ".*§r§.(?<name>.+): §r§c(?<damage>[\\d.]+[kM]?)❤",
    )

    private var yourEyes = 0

    private var dragonSpawned = false
        set(value) {
            field = value
            if (dragonSpawned) {
                egg = false
            }
        }

    private enum class EndType {
        GOLEM,
        DRAGON
    }

    private var endType: EndType? = null
    private var endTopDamage = 0.0
    private var endDamage = 0.0
    private var endPlace = 0

    private var currentDamage = 0.0
        set(value) {
            if (field != value) display = null
            field = value
        }
    private var currentTopDamage = 0.0
        set(value) {
            if (field != value) display = null
            field = value
        }
    private var currentPlace: Int? = null
        set(value) {
            if (field != value) display = null
            field = value
        }
    private var widgetActive = false
    var egg = true
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
        display = null
    }

    private fun weightMap(place: Int) = when (place) {
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
        weightMap(
            if (yourDamage == 0.0) -1 else place,
        ) + 100 * (
            eyes + yourDamage / (firstDamage.takeIf { it != 0.0 } ?: 1.0)
            )

    private fun calculateProtectorWeight(zealots: Int, place: Int, firstDamage: Double, yourDamage: Double) =
        weightMap(
            if (yourDamage == 0.0) -1 else place,
        ) + 50 * (
            yourDamage / (firstDamage.takeIf { it != 0.0 } ?: 1.0)
            ) + if (zealots > 100) 100 else zealots

    private fun displayIsEnabled() = config.display && dragonSpawned

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    fun onChat(event: SkyHanniChatEvent) {
        val message = event.message

        if (!config.chat && !config.display && !config.superiorNotify && !configProtector) return

        if (handleDragonSpawn(message)) return

        if (!config.chat && !config.display && !configProtector) return

        if (handleEyeEvents(message)) return

        if (handleEggSpawn(message)) return

        if (handleEndStart(message)) return

        if (handleEndLeaderboard(message)) return

        if (handleEndPosition(message)) return

        if (handleZealots(message)) return
    }

    private fun handleDragonSpawn(message: String): Boolean {
        dragonSpawn.matchMatcher(message) {
            dragonSpawned = true
            currentDragonType = DragonType.valueOf(group("Dragon").uppercase())
        } ?: return false

        ChatUtils.debug("Dragon Type: $currentDragonType")

        if (config.superiorNotify && currentDragonType == DragonType.SUPERIOR) {
            TitleManager.sendTitle("§6Superior Dragon Spawned!", 1.5.seconds)
        }

        DragonProfitTracker.addEyes(yourEyes)
        return true
    }

    private fun handleEyeEvents(message: String): Boolean = when {
        eyePlaced.matches(message) -> {
            yourEyes++
            true
        }

        eyeRemoved.matches(message) -> {
            yourEyes--
            true
        }

        else -> false
    }

    private fun handleEndStart(message: String): Boolean {
        when {
            endStartLineDragon.matches(message) -> {
                if (!config.chat) {
                    reset()
                } else {
                    endType = EndType.DRAGON
                }
                return true
            }

            endStartLineProtector.matches(message) -> {
                if (!configProtector) return false
                endType = EndType.GOLEM
                return true
            }
        }
        return false
    }

    private fun handleEndLeaderboard(message: String): Boolean {
        return endLeaderboard.matchMatcher(message) {
            if (endType == null) return false
            if (group("Position") != "1") return false

            endTopDamage = group("Damage").replace(",", "").toDouble()
            true
        } ?: false
    }

    private fun handleEndPosition(message: String): Boolean {
        val endType = endType ?: return false
        endPosition.matchMatcher(message) {
            endPlace = group("Position")?.toInt() ?: -1
            endDamage = group("Damage").replace(",", "").toDouble()
        } ?: return false

        when (endType) {
            EndType.DRAGON -> {
                weight = calculateDragonWeight(yourEyes, endPlace, endTopDamage, endDamage)

                if (endDamage > 0) {
                    if (!(yourEyes == 0 && !leechBool.countLeechedDragons)) {
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

        val zealots = endZealots.matchMatcher(message) {
            group("Amount").toInt()
        } ?: return false

        val weight = calculateProtectorWeight(zealots, endPlace, endTopDamage, endDamage)

        printWeight(weight)
        resetEnd()

        return true
    }

    private fun handleEggSpawn(message: String): Boolean {
        if (eggSpawned.matches(message)) {
            egg = true
            return true
        }
        return false
    }

    private fun printWeight(weight: Double) {
        ChatUtils.chat(
            "§f${
                " ".repeat(if (config.skyhanniMessageTag) 16 else 30)
            }§r§eYour Weight: §r§a${
                weight.roundTo(0).addSeparators()
            }",
            config.skyhanniMessageTag,
        )
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    fun onScoreBoard(event: ScoreboardUpdateEvent) {
        val index = event.new.indexOfFirst { scoreDragon.matches(it) }
        if (index == -1) return
        if (egg) {
            dragonSpawned = true
        }
        scoreDamage.matchMatcher(event.new[index + 1]) {
            currentDamage = this.group("Damage").replace(",", "").toDouble()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    fun onTabList(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.DRAGON)) return
        if (!displayIsEnabled()) return
        widgetActive = true
        for (i in 1 until event.lines.size) {
            tabDamagePattern.matchMatcher(event.lines[i]) {
                if (i == 1) {
                    currentTopDamage = group("damage").formatDouble()
                }
                if (group("name") == LorenzUtils.getPlayerName()) {
                    currentPlace = if (i > 3) null else i
                }
            }
        }
    }

    private val widgetErrorGUI = listOf(Renderable.string("§cDragon Widget is disabled!"))

    private var display: List<Renderable>? = null

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    fun onRender(event: GuiRenderEvent) {
        if (!displayIsEnabled()) return
        val display = display ?: (if (!widgetActive) widgetErrorGUI else display()).also { display = it }
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
        egg = true
    }
}
