package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.misc.GhostCounter.Option.*
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object GhostCounter {

    private val config get() = SkyHanniMod.feature.misc.ghostCounter
    private val counter get() = ProfileStorageData.profileSpecific?.ghostCounter?.data ?: mutableMapOf()
    private var display = listOf<List<Any>>()
    private val sorrowPattern = "§6§lRARE DROP! §r§9Sorrow §r§b\\([+](?<mf>.*)% §r§b✯ Magic Find§r§b\\)".toPattern()
    private val plasmaPattern = "§6§lRARE DROP! §r§9Plasma §r§b\\([+](?<mf>.*)% §r§b✯ Magic Find§r§b\\)".toPattern()
    private val voltaPattern = "§6§lRARE DROP! §r§9Volta §r§b\\([+](?<mf>.*)% §r§b✯ Magic Find§r§b\\)".toPattern()
    private val ghostlybootPattern = "§6§lRARE DROP! §r§9Ghostly Boots §r§b\\([+](?<mf>.*)% §r§b✯ Magic Find§r§b\\)".toPattern()
    private val coinsPattern = "§eThe ghost's death materialized §r§61,000,000 coins §r§efrom the mists!".toPattern()
    private val skillXPPattern = ".*§3\\+(?<gained>.*) .* \\((?<total>.*)\\/(?<current>.*)\\).*".toPattern()
    private val killComboPattern = "[+]\\d+ Kill Combo [+](?<coin>.*) coins per kill".toPattern()
    private val killComboExpiredPattern = "§cYour Kill Combo has expired! You reached a (?<combo>.*) Kill Combo!".toPattern()
    private val ghostXPPattern = "(?<current>\\d+(?:\\.\\d+)?(?:,\\d+)?[kK]?)\\/(?<total>\\d+(?:\\.\\d+)?(?:,\\d+)?[kKmM]?)".toPattern()
    private val bestiaryPattern = "BESTIARY Ghost .*➜(?<newLevel>.*)".toPattern()
    private var tick = 0
    private var lastXp: String = "0"
    private var gain: Int = 0
    private var num: Double = 0.0
    private var inMist = false
    private var session = mutableMapOf(
            SESSION_KILLS to 0.0,
            SESSION_SORROWCOUNT to 0.0,
            SESSION_VOLTACOUNT to 0.0,
            SESSION_PLASMACOUNT to 0.0,
            SESSION_GHOSTLYBOOTS to 0.0,
            SESSION_BAGOFCASH to 0.0,
            SESSION_TOTALDROPS to 0.0,
            SESSION_SCAVENGERCOINS to 0.0,
            SESSION_MAXKILLCOMBO to 0.0,
            SESSION_SKILLXPGAINED to 0.0
    )
    private val bestiaryData = mutableMapOf<Int, Int>().apply {
        val commonValue = 100_000

        for (i in 1..46) {
            this[i] = when (i) {
                1 -> 10
                2 -> 15
                3 -> 75
                4 -> 150
                5 -> 250
                6 -> 500
                7 -> 1_500
                8 -> 2_500
                9 -> 5_000
                10 -> 15_000
                11 -> 25_000
                12 -> 50_000
                else -> commonValue
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        if (config.onlyOnMist && !inMist) return
        config.position.renderStringsAndItems(display,
                extraSpace = config.extraSpace,
                posLabel = "Ghost Counter")
    }

    private fun formatDisplay(map: List<List<Any>>): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.ghostDisplayText) {
            newList.add(map[index])
        }
        return newList
    }

    fun update() {
        display = formatDisplay(drawDisplay())
    }

    private fun drawDisplay() = buildList<List<Any>> {
        val value = when (SORROWCOUNT.get()) {
            0.0 -> "0"
            else -> "${((((KILLS.get() / SORROWCOUNT.get()) + Math.ulp(1.0)) * 100) / 100).roundToInt()}"
        }
        val mgc = when (TOTALDROPS.get()) {
            0.0 -> "0"
            else -> "${((((TOTALMF.get() / TOTALDROPS.get()) + Math.ulp(1.0)) * 100) / 100).roundToPrecision(2)}"
        }
        val currentKill = BESTIARY_CURRENTKILL.getInt()
        val killNeeded = BESTIARY_KILLNEEDED.getInt()
        val nextLevel = BESTIARY_NEXTLEVEL.getInt()
        val bestiary = if (config.showMax){
            when (nextLevel){
                -1 -> "${currentKill.addSeparators()} (§c§lMaxed!)"
                in 1 .. 46 -> {
                    val sum = bestiaryData.filterKeys { it <= nextLevel-1 }.values.sum()
                    val cKill = sum + currentKill
                    "${cKill.addSeparators()}/3M (${percent(cKill.toDouble(), 3_000_000.0)})"
                }
                else -> "§cOpen Bestiary Menu !"
            }
        }else{
            when (nextLevel) {
                -1 -> "${currentKill.addSeparators()} (§c§lMaxed!)"
                in 1..46 -> "${currentKill.addSeparators()}/${NumberUtil.format(killNeeded)}"
                else -> "§cOpen Bestiary Menu !"
            }
        }

        addAsSingletonList("§6Ghosts counter")
        val list = mapOf(
                "Ghosts Killed" to Pair(KILLS.getInt(), SESSION_KILLS.getInt(true)),
                "Sorrows" to Pair(SORROWCOUNT.getInt(), SESSION_SORROWCOUNT.getInt(true)),
                "Ghost Since Sorrow" to Pair(GHOSTSINCESORROW.getInt(), ""),
                "Ghosts/Sorrow" to Pair(value, ""),
                "Volta" to Pair(VOLTACOUNT.getInt(), SESSION_VOLTACOUNT.getInt(true)),
                "Plasma" to Pair(PLASMACOUNT.getInt(), SESSION_PLASMACOUNT.getInt(true)),
                "Ghostly Boots" to Pair(GHOSTLYBOOTS.getInt(), SESSION_GHOSTLYBOOTS.getInt(true)),
                "Bag Of Cash" to Pair(BAGOFCASH.getInt(), SESSION_BAGOFCASH.getInt(true)),
                "Avg Magic Find" to Pair(mgc, ""),
                "Scavenger Coins" to Pair(SCAVENGERCOINS.getInt(), SESSION_SCAVENGERCOINS.getInt(true)),
                "Kill Combo" to Pair(KILLCOMBO.getInt(), ""),
                "Highest Kill Combo" to Pair(MAXKILLCOMBO.getInt(), SESSION_MAXKILLCOMBO.getInt(true)),
                "Skill XP Gained" to Pair(SKILLXPGAINED.get().roundToPrecision(2), SESSION_SKILLXPGAINED.get(true).roundToPrecision(2)),
                "Bestiary %bestiaryLevel%->%bestiaryNextLevel%" to Pair(bestiary, "")
        )
        for ((text, v) in list) {
            val (total, session) = v
            val se = if (session == "") "" else "($session)"
            addAsSingletonList(config.formatText.replace("&", "§")
                    .replace("%text%", text)
                    .replace("%value%", "$total")
                    .replace("%session%", se)
                    .replace("%bestiaryLevel%", if (BESTIARY_NEXTLEVEL.getInt() < 0) "46" else "${BESTIARY_NEXTLEVEL.getInt()-1}")
                    .replace("%bestiaryNextLevel%", if (config.showMax) "46" else "${BESTIARY_NEXTLEVEL.getInt()}"))
        }
    }

    // Part of this was taken from GhostCounterV3 CT module
    // maybe replace this with a SkillXpGainEvent ?
    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        if (!isEnabled()) return
        skillXPPattern.matchMatcher(event.message) {
            val gained = group("gained").toDouble()
            val total = group("total")
            if (total != lastXp) {
                val res = total.replace("\\D".toRegex(), "")
                gain = (res.toLong() - lastXp.toLong()).toDouble().roundToInt()
                num = (gain.toDouble() / gained)
                if (gained in 150.0..450.0) {
                    if (lastXp != "0") {
                        if (num >= 0) {
                            KILLS.add(num)
                            SESSION_KILLS.add(num, true)
                            GHOSTSINCESORROW.add(num)
                            KILLCOMBO.add(num)
                            SKILLXPGAINED.add(gained * num.roundToLong())
                            SESSION_SKILLXPGAINED.add(gained * num.roundToLong(), true)

                            BESTIARY_CURRENTKILL.add(num)
                        }
                    }
                }
                lastXp = res
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (LorenzUtils.skyBlockIsland != IslandType.DWARVEN_MINES) return
        sorrowPattern.matchMatcher(event.message) {
            SORROWCOUNT.add(1.0)
            TOTALMF.add(group("mf").substring(4).toDouble())
            GHOSTSINCESORROW.set(0.0)
            TOTALDROPS.add(1.0)
            SESSION_SORROWCOUNT.add(1.0, true)
            update()
        }
        voltaPattern.matchMatcher(event.message) {
            VOLTACOUNT.add(1.0)
            TOTALMF.add(group("mf").substring(4).toDouble())
            TOTALDROPS.add(1.0)
            SESSION_VOLTACOUNT.add(1.0, true)
            update()
        }
        plasmaPattern.matchMatcher(event.message) {
            PLASMACOUNT.add(1.0)
            TOTALMF.add(group("mf").substring(4).toDouble())
            TOTALDROPS.add(1.0)
            SESSION_PLASMACOUNT.add(1.0, true)
            update()
        }
        ghostlybootPattern.matchMatcher(event.message) {
            GHOSTLYBOOTS.add(1.0)
            TOTALMF.add(group("mf").substring(4).toDouble())
            TOTALDROPS.add(1.0)
            SESSION_GHOSTLYBOOTS.add(1.0, true)
            update()
        }
        coinsPattern.matchMatcher(event.message) {
            BAGOFCASH.add(1.0)
            SESSION_BAGOFCASH.add(1.0, true)
            update()
        }
        killComboExpiredPattern.matchMatcher(event.message) {
            if (KILLCOMBO.getInt() > MAXKILLCOMBO.getInt()) {
                MAXKILLCOMBO.set(group("combo").toDouble())
            }
            if (KILLCOMBO.getInt() > SESSION_MAXKILLCOMBO.getInt(true)) {
                SESSION_MAXKILLCOMBO.set(group("combo").toDouble(), true)
            }
            KILLCOMBOCOINS.set(0.0)
            KILLCOMBO.set(0.0)
            update()
        }
        killComboPattern.matchMatcher(event.message.removeColor()) {
            KILLCOMBOCOINS.set(KILLCOMBOCOINS.get() + group("coin").toDouble())
            update()
        }

        //replace with BestiaryLevelUpEvent ?
        bestiaryPattern.matchMatcher(event.message.removeColor()) {
            val currentLevel = group("newLevel").toInt()
            val nextLevel = if (currentLevel >= 46) 47 else currentLevel + 1

            when (nextLevel) {
                47 -> {
                    BESTIARY_NEXTLEVEL.set(-1.0)
                    BESTIARY_CURRENTKILL.set(3_000_000.0)
                    BESTIARY_KILLNEEDED.set(0.0)
                }

                else -> {
                    BESTIARY_NEXTLEVEL.set(nextLevel.toDouble())
                    BESTIARY_CURRENTKILL.set(0.0)
                    val killNeeded: Int = bestiaryData[nextLevel] ?: 0
                    BESTIARY_KILLNEEDED.set(killNeeded.toDouble())
                }
            }
            update()
        }
    }

    @SubscribeEvent
    fun onPurseChange(event: PurseChangeEvent) {
        if (!isEnabled()) return
        if (LorenzUtils.skyBlockArea != "The Mist") return
        if (event.reason != PurseChangeCause.GAIN_MOB_KILL) return
        SESSION_SCAVENGERCOINS.add(event.coins, true)
        SCAVENGERCOINS.add(event.coins)
    }

    fun reset() {
        for (opt in Option.values()) {
            if (opt.reset) {
                if (opt.save)
                    opt.set(0.0)
                else
                    opt.set(0.0, true)
            }
        }
        update()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return
        if (tick++ % 20 == 0) {
            inMist = LorenzUtils.skyBlockArea == "The Mist"
            update()
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val inventoryName = event.inventoryName
        if (inventoryName != "Bestiary ➜ Deep Caverns") return
        val stacks = event.inventoryItems
        val ghostStack = stacks[13] ?: return
        val bestiaryNextLevel = Utils.parseIntOrRomanNumeral(ghostStack.displayName.substring(8)) + 1
        BESTIARY_NEXTLEVEL.set(bestiaryNextLevel.toDouble())

        for (line in ghostStack.getLore()) {
            ghostXPPattern.matchMatcher(line.removeColor().trim()) {
                BESTIARY_CURRENTKILL.set(group("current").formatNumber().toDouble())
                BESTIARY_KILLNEEDED.set(group("total").formatNumber().toDouble())
            }
        }
        update()
    }

    fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && config.enabled && LorenzUtils.skyBlockIsland == IslandType.DWARVEN_MINES
    }

    enum class Option(val save: Boolean = true, val reset: Boolean = true) {
        KILLS,
        SORROWCOUNT,
        VOLTACOUNT,
        PLASMACOUNT,
        GHOSTLYBOOTS,
        BAGOFCASH,
        KILLCOMBOCOINS,
        TOTALDROPS,
        GHOSTSINCESORROW,
        TOTALMF,
        SCAVENGERCOINS,
        MAXKILLCOMBO,
        KILLCOMBO,
        SKILLXPGAINED,
        SESSION_KILLS(false),
        SESSION_SORROWCOUNT(false),
        SESSION_VOLTACOUNT(false),
        SESSION_PLASMACOUNT(false),
        SESSION_GHOSTLYBOOTS(false),
        SESSION_BAGOFCASH(false),
        SESSION_TOTALDROPS(false),
        SESSION_SCAVENGERCOINS(false),
        SESSION_MAXKILLCOMBO(false),
        SESSION_SKILLXPGAINED(false),

        BESTIARY_NEXTLEVEL(true, false),
        BESTIARY_CURRENTKILL(true, false),
        BESTIARY_KILLNEEDED(true, false);

        fun add(i: Double, s: Boolean = false) {
            if (s)
                session[this] = session[this]?.plus(i) ?: i
            else
                counter[this] = counter[this]?.plus(i) ?: i
        }

        fun set(i: Double, s: Boolean = false) {
            if (s)
                session[this] = i
            else
                counter[this] = i
        }

        fun getInt(s: Boolean = false): Int {
            return if (s)
                session[this]?.roundToInt() ?: 0
            else
                counter[this]?.roundToInt() ?: 0
        }

        fun getLong(s: Boolean = false): Long {
            return if (s)
                session[this]?.toLong() ?: 0
            else
                counter[this]?.toLong() ?: 0
        }

        fun get(s: Boolean = false): Double {
            return if (s)
                session[this] ?: 0.0
            else
                counter[this] ?: 0.0
        }

    }

    private fun percent(number: Double, total: Double): String {
        return "${((number / total) * 100).roundToPrecision(4)}%"
    }
}
