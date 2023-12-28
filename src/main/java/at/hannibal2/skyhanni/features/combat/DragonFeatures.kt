package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ScoreboardChangeEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.formatDouble
import at.hannibal2.skyhanni.utils.LorenzUtils.formatPercentage
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class DragonFeatures {

    val config get() = SkyHanniMod.configManager.features.combat.dragon
    val configProtector get() = SkyHanniMod.configManager.features.combat.endstoneProtectorChat

    val dragonNames = listOf("Protector", "Old", "Wise", "Unstable", "Young", "Strong", "Superior")

    val dragonNamesAsRegex = dragonNames.joinToString("|")
    val dragonNamesUpperCaseAsRegex = dragonNames.joinToString("|") { it.uppercase() }

    val repoGroup = RepoPattern.group("combat.boss.dragon")
    val chatGroup = repoGroup.group("chat")
    val scoreBoardGroup = repoGroup.group("scoreboard")
    val tabListGroup = repoGroup.group("tablist")

    val eyePlaced by chatGroup.pattern("eye.placed.you", "§5☬ §r§dYou placed a Summoning Eye! §r§7\\(§r§e\\d§r§7\\/§r§a8§r§7\\)|§5☬ §r§dYou placed a Summoning Eye! Brace yourselves! §r§7\\(§r§a8§r§7\\/§r§a8§r§7\\)")
    val eyeRemoved by chatGroup.pattern("eye.removed.you", "§5You recovered a Summoning Eye!")

    val eggSpawned by chatGroup.pattern("egg.spawn", "§5☬ §r§dThe Dragon Egg has spawned!")
    val endStartLineDragon by chatGroup.pattern("end.boss", "§f +§r§6§l(?<Dragon>${dragonNamesUpperCaseAsRegex}) DRAGON DOWN!")
    val endStartLineProtector by RepoPattern.pattern("combat.boss.protector.chat.end.boss", "§f +§r§6§l ENDSTONE PROTECTOR DOWN!")
    val endPosition by chatGroup.pattern("end.position", "§f +§r§eYour Damage: §r§a(?<Damage>[\\d.,]+) (?:§r§d§l\\(NEW RECORD!\\) )?§r§7\\(Position #(?<Position>\\d+)\\)")

    // val endFinalHit by chatGroup.pattern("end.final", "§f                 §r§b[^ ]+ (?<Name>.*)§r§f §r§7dealt the final blow.")
    val endLeaderboard by chatGroup.pattern("end.place", "§f +§r§.§l(?<Position>\\d+).. Damager §r§7- §r§.(?:\\[[^ ]+\\] )?(?<Name>.*)§r§. §r§7- §r§e(?<Damage>[\\d.,]+)")
    val endZealots by RepoPattern.pattern("combat.boss.protector.chat.end.zealot", "§f +§r§eZealots Contributed: §r§a(?<Amount>\\d+)§r§e/100")
    val dragonSpawn by chatGroup.pattern("spawn", "§5☬ §r§d§lThe §r§5§c§l(?<Dragon>${dragonNamesAsRegex}) Dragon§r§d§l has spawned!")
    val scoreDamage by scoreBoardGroup.pattern("damage", "Your Damage: §c(?<Damage>[\\w,.]+)")
    val scoreDragon by scoreBoardGroup.pattern("dragon", "Dragon HP: .*")
    val fightInfo by tabListGroup.pattern("fight.info", "§b§lDragon Fight: §r§f\\(\\w+\\)")
    val tabDamage by tabListGroup.pattern("fight.player", ".*§r§f(?<Name>.+): §r§c(?<Damage>[\\d.]+)(?<Unit>[kM])?❤")


    var yourEyes = 0

    var dragonSpawned = false
        set(value) {
            field = value
            if (dragonSpawned) {
                egg = false
            }
        }

    private enum class Type {
        golem, dragon
    }

    private var endType: Type? = null
    var endTopDamage = 0.0
    var endDamage = 0.0
    var endPlace = 0

    var currentDamage = 0.0
    var currentTopDamage = 0.0
    var currentPlace: Int? = null
    var egg = true


    fun resetEnd() {
        endType = null
        endTopDamage = 0.0
        endDamage = 0.0
        endPlace = 0
    }

    fun reset() {
        resetEnd()
        dragonSpawned = false
        currentTopDamage = 0.0
        currentDamage = 0.0
        currentPlace = null
        yourEyes = 0
    }

    fun enable() = LorenzUtils.inSkyBlock && IslandType.THE_END.isInIsland()

    fun enableDisplay() = enable() && config.display

    fun dragonWeightMap(place: Int) = when (place) {
        -1 -> 10
        1 -> 300
        2 -> 250
        3 -> 200
        4 -> 125
        5 -> 110
        6, 7, 8 -> 100
        9, 10 -> 90
        11, 12 -> 80
        else -> 70
    }

    fun protectorWeightMap(place: Int) = when (place) {
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

    fun calculateDragonWeight(eyes: Int, place: Int, firstDamage: Double, yourDamage: Double) =
        dragonWeightMap(if (yourDamage == 0.0) -1 else place) + 100 * (eyes + yourDamage / (firstDamage.takeIf { it != 0.0 }
            ?: 1.0))

    fun calculateProtectorWeight(zealots: Int, place: Int, firstDamage: Double, yourDamage: Double) =
        protectorWeightMap(if (yourDamage == 0.0) -1 else place) + 50 * (yourDamage / (firstDamage.takeIf { it != 0.0 }
            ?: 1.0)) + if (zealots > 100) 100 else zealots


    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!enable()) return
        val message = event.message
        if (!config.chat && !config.display && !config.superiorNotify && !configProtector) return
        dragonSpawn.matchMatcher(message) {
            dragonSpawned = true
            if (config.superiorNotify && this.group("Dragon") == "Superior") {
                TitleManager.sendTitle("§6Superior Dragon Spawned!", 1.5.seconds)
            }
            return
        }
        if (!config.chat && !config.display && !configProtector) return
        when {
            eyePlaced.matches(message) -> {
                yourEyes++
            }

            eyeRemoved.matches(message) -> {
                yourEyes--
            }

            eggSpawned.matches(message) -> {
                egg = true
            }

            endStartLineDragon.matches(message) -> {
                if (!config.chat) {
                    reset()
                    return
                }
                endType = Type.dragon
            }

            endStartLineProtector.matches(message) -> {
                if (!configProtector) return
                endType = Type.golem
            }

            else -> {
                endLeaderboard.matchMatcher(message) {
                    if (endType == null) return@matchMatcher
                    if (this.group("Position") != "1") return@matchMatcher
                    endTopDamage = this.group("Damage").replace(",", "").toDouble()
                    return
                }
                endPosition.matchMatcher(message) {
                    if (endType == null) return@matchMatcher
                    endPlace = this.group("Position")?.toInt() ?: -1
                    endDamage = this.group("Damage").replace(",", "").toDouble()
                    when (endType) {
                        Type.dragon -> {
                            val weight = calculateDragonWeight(
                                yourEyes, endPlace, endTopDamage, endDamage
                            )

                            printWeight(weight)
                            reset()
                        }

                        Type.golem -> {

                            // NO reset because of Zealot Line
                        }

                        null -> return@matchMatcher
                    }
                    return
                }
                endZealots.matchMatcher(message) {
                    if (endType != Type.golem) return@matchMatcher
                    val zealots = this.group("Amount").toInt()
                    val weight = calculateProtectorWeight(zealots, endPlace, endTopDamage, endDamage)
                    printWeight(weight)
                    resetEnd()
                }
            }
        }

    }

    private fun printWeight(weight: Double) {
        LorenzUtils.chat("§f                §r§eYour Weight: §r§a${formatDouble(weight)}")
    }

    @SubscribeEvent
    fun onScoreBoard(event: ScoreboardChangeEvent) {
        if (!(enableDisplay())) return
        val index = event.newList.indexOfFirst { scoreDragon.matches(it) }
        if (index == -1) return
        if (egg) {
            dragonSpawned = true
        }
        scoreDamage.matchMatcher(event.newList[index + 1]) {
            currentDamage = this.group("Damage").replace(",", "").toDouble()
        }

    }

    @SubscribeEvent
    fun onTabList(event: TabListUpdateEvent) {
        if (!(enableDisplay() && dragonSpawned)) return
        val infoIndex = event.tabList.indexOfFirst { fightInfo.matches(it) }
        if (infoIndex == -1) return
        for (i in 1..3) {
            tabDamage.matchMatcher(event.tabList[infoIndex + i]) {
                if (i == 1) {
                    currentTopDamage = this.group("Damage").toDouble() * this.group("Unit").let {
                        when (it) {
                            "k" -> 1_000
                            "M" -> 1_000_000
                            else -> 1
                        }
                    }
                }
                if (this.group("Name") == LorenzUtils.getPlayerName()) {
                    currentPlace = i
                }
            }
        }
    }


    @SubscribeEvent
    fun onRender(event: GuiRenderEvent) {
        if (!(enableDisplay() && dragonSpawned)) return
        config.displayPosition.renderRenderables(listOf(Renderable.hoverTips("§6Current Weight: §f${formatDouble(calculateDragonWeight(yourEyes, currentPlace ?: 6, currentTopDamage, currentDamage))}", listOf("Eyes: $yourEyes", "Place: ${currentPlace ?: if (currentDamage != 0.0) "unknown, assuming 6th" else "not damaged yet"}", "Damage Ratio: ${formatPercentage(currentDamage / (currentTopDamage.takeIf { it != 0.0 } ?: 1.0))}%"))), posLabel = "Dragon Weight")
    }

    @SubscribeEvent
    fun on(event: IslandChangeEvent) {
        reset()
        egg = true
    }
}
