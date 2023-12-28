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

    val dragonNames = listOf("Protector", "Old", "Wise", "Unstable", "Young", "Strong", "Superior")

    val dragonNamesAsRegex = dragonNames.joinToString("|")
    val dragonNamesUpperCaseAsRegex = dragonNames.joinToString("|") { it.uppercase() }

    val repoGroup = RepoPattern.group("combat.dragon")
    val chatGroup = repoGroup.group("chat")
    val scoreBoardGroup = repoGroup.group("scoreboard")
    val tabListGroup = repoGroup.group("tablist")

    val eyePlaced by chatGroup.pattern("eye.placed.you", "§5☬ §r§dYou placed a Summoning Eye! §r§7\\(§r§e\\d§r§7\\/§r§a8§r§7\\)|§5☬ §r§dYou placed a Summoning Eye! Brace yourselves! §r§7\\(§r§a8§r§7\\/§r§a8§r§7\\)")
    val eyeRemoved by chatGroup.pattern("eye.removed.you", "§5You recovered a Summoning Eye!")

    val eggSpawned by chatGroup.pattern("egg.spawn", "§5☬ §r§dThe Dragon Egg has spawned!")
    val endStartLine by chatGroup.pattern("end.boss", "§f +§r§6§l(?<Dragon>${dragonNamesUpperCaseAsRegex}) DRAGON DOWN!")
    val endPosition by chatGroup.pattern("end.position", "§f +§r§eYour Damage: §r§a(?<Damage>[\\d.,]+) (?:§r§d§l\\(NEW RECORD!\\) )?§r§7\\(Position #(?<Position>\\d)\\)")

    // val endFinalHit by chatGroup.pattern("end.final", "§f                 §r§b[^ ]+ (?<Name>.*)§r§f §r§7dealt the final blow.")
    val endPlace by chatGroup.pattern("end.place", "§f +§r§.§l(?<Position>\\d+).. Damager §r§7- §r§.(?:\\[[^ ]+\\] )?(?<Name>.*)§r§. §r§7- §r§e(?<Damage>[\\d.,]+)")
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

    var endText = false
    var endTopDamage = 0.0

    var currentDamage = 0.0
    var currentTopDamage = 0.0
    var currentPlace: Int? = null
    var egg = true


    fun reset() {
        endText = false
        endTopDamage = 0.0
        dragonSpawned = false
        currentTopDamage = 0.0
        currentDamage = 0.0
        currentPlace = null
        yourEyes = 0
    }

    fun enable() = LorenzUtils.inSkyBlock && IslandType.THE_END.isInIsland()

    fun enableDisplay() = enable() && config.display

    fun weightMap(place: Int) = when (place) {
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

    fun calculateWeight(eyes: Int, place: Int, firstDamage: Double, yourDamage: Double) =
        weightMap(if (yourDamage == 0.0) -1 else place) + 100 * (eyes + yourDamage / (firstDamage.takeIf { it != 0.0 }
            ?: 1.0))


    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!enable()) return
        val message = event.message
        if (!config.chat && !config.display && !config.superiorNotify) return
        dragonSpawn.matchMatcher(message) {
            dragonSpawned = true
            if (config.superiorNotify && this.group("Dragon") == "Superior") {
                TitleManager.sendTitle("§6Superior Dragon Spawned!", 1.5.seconds)
            }
            return
        }
        if (!config.chat && !config.display) return
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

            endStartLine.matches(message) -> {
                endText = true
            }

            else -> {
                endPlace.matchMatcher(message) {
                    if (!endText) return@matchMatcher
                    if (this.group("Position") != "1") return@matchMatcher
                    endTopDamage = this.group("Damage").replace(",", "").toDouble()
                    return
                }
                endPosition.matchMatcher(message) {
                    if (!endText) return@matchMatcher
                    val weight = calculateWeight(
                        yourEyes, this.group("Position")?.toInt()
                        ?: -1, endTopDamage, this.group("Damage").replace(",", "").toDouble()
                    )
                    if (config.chat) {
                        LorenzUtils.chat("§f                §r§eYour Weight: §r§a${formatDouble(weight)}")
                    }
                    reset()
                    return
                }
            }
        }

    }

    @SubscribeEvent
    fun onScoreBoard(event: ScoreboardChangeEvent) {
        if (!(enableDisplay())) return
        val index = event.newList.indexOfFirst {
            scoreDamage.matchMatcher(it) {
                currentDamage = this.group("Damage").replace(",", "").toDouble()
                true
            } ?: false
        }
        if (dragonSpawned || index == -1) return
        if (egg && scoreDragon.matches(event.newList[index - 1])) {
            dragonSpawned = true
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
        config.displayPosition.renderRenderables(listOf(Renderable.hoverTips("§6Current Weight: §f${formatDouble(calculateWeight(yourEyes, currentPlace ?: 6, currentTopDamage, currentDamage))}", listOf("Eyes: $yourEyes", "Place: ${currentPlace ?: if (currentDamage != 0.0) "unknown, assuming 6th" else "not damaged yet"}", "Damage Ratio: ${formatPercentage(currentDamage / (currentTopDamage.takeIf { it != 0.0 } ?: 1.0))}%"))), posLabel = "Dragon Weight")
    }

    @SubscribeEvent
    fun on(event: IslandChangeEvent) {
        reset()
        egg = true
    }
}
