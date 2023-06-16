package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.misc.GhostCounter.Option.*
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.chat
import at.hannibal2.skyhanni.utils.LorenzUtils.clickableChat
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import io.github.moulberry.notenoughupdates.core.util.lerp.LerpUtils
import io.github.moulberry.notenoughupdates.util.Utils
import io.github.moulberry.notenoughupdates.util.XPInformation
import io.github.moulberry.notenoughupdates.util.XPInformation.SkillInfo
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object GhostCounter {

    private val config get() = SkyHanniMod.feature.misc.ghostCounter
    private val counter get() = ProfileStorageData.profileSpecific?.ghostCounter?.data ?: mutableMapOf()
    private var display = listOf<List<Any>>()
    private var ghostCounterV3File = File("." + File.separator + "config" + File.separator + "ChatTriggers" + File.separator + "modules" + File.separator + "GhostCounterV3" + File.separator + ".persistantData.json")
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
    private val format = NumberFormat.getIntegerInstance()
    private const val exportPrefix = "gc/"
    private var tick = 0
    private var lastXp: String = "0"
    private var gain: Int = 0
    private var num: Double = 0.0
    private var inMist = false
    private var lastUpdate: Long = -1
    private var lastTotalXp = -1f
    private var isKilling = false
    private val xpGainQueue = mutableListOf<Float>()
    private var xpGainHourLast = -1f
    private var xpGainHour = -1f
    private var xpGainTimer = 0
    private var skillInfo: SkillInfo? = null
    private var skillInfoLast: SkillInfo? = null
    private var notifyCTModule = true
    private var bestiaryCurrentKill = 0
    private const val skillType = "Combat"
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
        val value: Int = when (SORROWCOUNT.get()) {
            0.0 -> 0
            else -> "${((((KILLS.get() / SORROWCOUNT.get()) + Math.ulp(1.0)) * 100) / 100).roundToInt()}".toInt()
        }
        val mgc = when (TOTALDROPS.get()) {
            0.0 -> "0"
            else -> "${((((TOTALMF.get() / TOTALDROPS.get()) + Math.ulp(1.0)) * 100) / 100).roundToPrecision(2)}"
        }

        val xp: String
        val xpInterp: Float
        if (xpGainHourLast == xpGainHour && xpGainHour <= 0) {
            xp = "N/A"
        } else {
            xpInterp = interp(xpGainHour, xpGainHourLast)
            xp = "${format.format(xpInterp)} ${if (isKilling) "" else "§c(PAUSED)§r"}"
        }

        val bestiaryFormatting = config.textFormatting.bestiaryFormatting
        val currentKill = BESTIARY_CURRENTKILL.getInt()
        val killNeeded = BESTIARY_KILLNEEDED.getInt()
        val nextLevel = BESTIARY_NEXTLEVEL.getInt()
        val bestiary = if (config.showMax) {
            when (nextLevel) {
                -1 -> bestiaryFormatting.maxed
                in 1..46 -> {
                    val sum = bestiaryData.filterKeys { it <= nextLevel - 1 }.values.sum()
                    val cKill = sum + currentKill
                    bestiaryCurrentKill = cKill
                    bestiaryFormatting.showMax_progress
                }

                else -> bestiaryFormatting.openMenu
            }
        } else {
            when (nextLevel) {
                -1 -> bestiaryFormatting.maxed
                in 1..46 -> bestiaryFormatting.progress
                else -> bestiaryFormatting.openMenu
            }
        }

        addAsSingletonList(Utils.chromaStringByColourCode(config.textFormatting.titleFormat.replace("&", "§")))
        addAsSingletonList(config.textFormatting.ghostKiledFormat.formatText(KILLS.getInt(), SESSION_KILLS.getInt(true)))
        addAsSingletonList(config.textFormatting.sorrowsFormat.formatText(SORROWCOUNT.getInt(), SESSION_SORROWCOUNT.getInt(true)))
        addAsSingletonList(config.textFormatting.ghostSinceSorrowFormat.formatText(GHOSTSINCESORROW.getInt()))
        addAsSingletonList(config.textFormatting.ghostKillPerSorrowFormat.formatText(value))
        addAsSingletonList(config.textFormatting.voltasFormat.formatText(VOLTACOUNT.getInt(), SESSION_VOLTACOUNT.getInt(true)))
        addAsSingletonList(config.textFormatting.plasmasFormat.formatText(PLASMACOUNT.getInt(), SESSION_PLASMACOUNT.getInt(true)))
        addAsSingletonList(config.textFormatting.ghostlyBootsFormat.formatText(GHOSTLYBOOTS.getInt(), SESSION_GHOSTLYBOOTS.getInt(true)))
        addAsSingletonList(config.textFormatting.bagOfCashFormat.formatText(BAGOFCASH.getInt(), SESSION_BAGOFCASH.getInt(true)))
        addAsSingletonList(config.textFormatting.avgMagicFindFormat.formatText(mgc))
        addAsSingletonList(config.textFormatting.scavengerCoinsFormat.formatText(SCAVENGERCOINS.getInt(), SESSION_SCAVENGERCOINS.getInt(true)))
        addAsSingletonList(config.textFormatting.killComboFormat.formatText(KILLCOMBO.getInt(), SESSION_MAXKILLCOMBO.getInt(true)))
        addAsSingletonList(config.textFormatting.highestKillComboFormat.formatText(MAXKILLCOMBO.getInt(), SESSION_MAXKILLCOMBO.getInt(true)))
        addAsSingletonList(config.textFormatting.skillXPGainFormat.formatText(SKILLXPGAINED.get(), SESSION_SKILLXPGAINED.get(true)))
        addAsSingletonList(bestiaryFormatting.base.formatText(bestiary).formatBestiary(currentKill, killNeeded))
        addAsSingletonList(config.textFormatting.xpHourFormat.formatText(xp))
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

            when (val nextLevel = if (currentLevel >= 46) 47 else currentLevel + 1) {
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

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return
        tick++
        if (tick % 20 == 0) {
            inMist = LorenzUtils.skyBlockArea == "The Mist"
            update()
        }

        if (tick % 40 == 20) {
            calculateXP()
        }

        if (notifyCTModule && ProfileStorageData.profileSpecific?.ghostCounter?.ctDataImported == false) {
            notifyCTModule = false
            if (isUsingCTGhostCounter()) {
                clickableChat("§6[SkyHanni] GhostCounterV3 ChatTriggers module has been detected, do you want to import saved data ? Click here to import data", "shimportghostcounterdata")
            }
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


    private fun percent(number: Double, total: Double): String {
        return "${((number / total) * 100).roundToPrecision(4)}"
    }

    /**
     * Taken from NotEnoughUpdates
     */
    private fun interp(now: Float, last: Float): Float {
        var interp = now
        if (last >= 0 && last != now) {
            var factor = (System.currentTimeMillis() - lastUpdate) / 1000f
            factor = LerpUtils.clampZeroOne(factor)
            interp = last + (now - last) * factor
        }
        return interp
    }

    /**
     * Taken from NotEnoughUpdates
     */
    private fun calculateXP() {
        lastUpdate = System.currentTimeMillis()
        xpGainHourLast = xpGainHour
        skillInfoLast = skillInfo
        skillInfo = XPInformation.getInstance().getSkillInfo(skillType) ?: return
        val totalXp: Float = skillInfo!!.totalXp
        if (lastTotalXp > 0) {
            val delta: Float = totalXp - lastTotalXp
            if (delta > 0 && delta < 1000) {
                xpGainTimer = 3
                xpGainQueue.add(0, delta)
                while (xpGainQueue.size > 30) {
                    xpGainQueue.removeLast()
                }
                var totalGain = 0f
                for (f in xpGainQueue) totalGain += f
                xpGainHour = totalGain * (60 * 60) / xpGainQueue.size
                isKilling = true
            } else if (xpGainTimer > 0) {
                xpGainTimer--
                xpGainQueue.add(0, 0f)
                while (xpGainQueue.size > 30) {
                    xpGainQueue.removeLast()
                }
                var totalGain = 0f
                for (f in xpGainQueue) totalGain += f
                xpGainHour = totalGain * (60 * 60) / xpGainQueue.size
                isKilling = true
            } else if (delta <= 0) {
                isKilling = false
            }
        }
        lastTotalXp = totalXp
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

    private fun isUsingCTGhostCounter(): Boolean {
        return ghostCounterV3File.exists() && ghostCounterV3File.isFile
    }

    fun importCTGhostCounterData() {
        val c = ProfileStorageData.profileSpecific?.ghostCounter ?: return
        if (isUsingCTGhostCounter()) {
            if (c.ctDataImported) {
                chat("§e[SkyHanni] §cYou already imported GhostCounterV3 data!")
                return
            }
            val json = ConfigManager.gson.fromJson(FileReader(ghostCounterV3File), com.google.gson.JsonObject::class.java)
            GHOSTSINCESORROW.add(json["ghostsSinceSorrow"].asDouble)
            SORROWCOUNT.add(json["sorrowCount"].asDouble)
            BAGOFCASH.add(json["BagOfCashCount"].asDouble)
            PLASMACOUNT.add(json["PlasmaCount"].asDouble)
            VOLTACOUNT.add(json["VoltaCount"].asDouble)
            GHOSTLYBOOTS.add(json["GhostlyBootsCount"].asDouble)
            KILLS.add(json["ghostsKilled"].asDouble)
            TOTALMF.add(json["TotalMF"].asDouble)
            TOTALDROPS.add(json["TotalDrops"].asDouble)
            c.ctDataImported = true
            chat("§e[SkyHanni] §aImported data successfully!")
        } else
            chat("§e[SkyHanni] §cGhostCounterV3 ChatTriggers module not found!")
    }

    private fun String.formatText(value: Int, session: Int = -1): String {
        return Utils.chromaStringByColourCode(this.replace("%value%", value.addSeparators())
                .replace("%session%", session.addSeparators())
                .replace("&", "§"))
    }

    private fun String.formatText(t: String): String {
        return Utils.chromaStringByColourCode(this.replace("%value%", t)
                .replace("&", "§"))
    }

    private fun String.formatText(value: Double, session: Double): String {
        return Utils.chromaStringByColourCode(this.replace("%value%", value.roundToPrecision(2).addSeparators())
                .replace("%session%", session.roundToPrecision(2).addSeparators())
                .replace("&", "§"))
    }

    private fun String.formatBestiary(currentKill: Int, killNeeded: Int): String {
        return Utils.chromaStringByColourCode(
                this.replace("%currentKill%", if (config.showMax) bestiaryCurrentKill.addSeparators() else currentKill.addSeparators())
                        .replace("%percentNumber%", percent(bestiaryCurrentKill.toDouble(), 3_000_000.0))
                        .replace("%killNeeded%", NumberUtil.format(killNeeded))
                        .replace("%currentLevel%", if (BESTIARY_NEXTLEVEL.getInt() < 0) "46" else "${BESTIARY_NEXTLEVEL.getInt() - 1}")
                        .replace("%nextLevel%", if (config.showMax) "46" else "${BESTIARY_NEXTLEVEL.getInt()}")
                        .replace("&", "§"))
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

        fun get(s: Boolean = false): Double {
            return if (s)
                session[this] ?: 0.0
            else
                counter[this] ?: 0.0
        }
    }

    fun importFormatting() {
        val base64: String = try {
            Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
        } catch (e: Exception) {
            return
        }

        if(base64.length <= exportPrefix.length) return

        val jsonString = try {
            val t = String(Base64.getDecoder().decode(base64.trim()))
            if (!t.startsWith(exportPrefix)) return
            t.substring(exportPrefix.length)
        } catch (e: IllegalArgumentException) {
            return
        }

        val list = try {
            JsonParser().parse(jsonString).asJsonArray
                    .filter { it.isJsonPrimitive }
                    .map { it.asString }
        } catch (e: Exception) {
            return
        }

        if (list.size == 19) {
            with(config.textFormatting) {
                val b = bestiaryFormatting
                titleFormat = list[0]
                ghostKiledFormat = list[1]
                sorrowsFormat = list[2]
                ghostSinceSorrowFormat = list[3]
                ghostKillPerSorrowFormat = list[4]
                voltasFormat = list[5]
                plasmasFormat = list[6]
                ghostlyBootsFormat = list[7]
                bagOfCashFormat = list[8]
                avgMagicFindFormat = list[9]
                scavengerCoinsFormat = list[10]
                killComboFormat = list[11]
                highestKillComboFormat = list[12]
                skillXPGainFormat = list[13]
                b.base = list[14]
                b.openMenu = list[15]
                b.maxed = list[16]
                b.showMax_progress = list[17]
                b.progress = list[18]
            }
        }
    }

    fun exportFormatting() {
        val list = mutableListOf<String>()
        with(config.textFormatting){
            list.add(titleFormat)
            list.add(ghostKiledFormat)
            list.add(sorrowsFormat)
            list.add(ghostSinceSorrowFormat)
            list.add(ghostKillPerSorrowFormat)
            list.add(voltasFormat)
            list.add(plasmasFormat)
            list.add(ghostlyBootsFormat)
            list.add(bagOfCashFormat)
            list.add(avgMagicFindFormat)
            list.add(scavengerCoinsFormat)
            list.add(killComboFormat)
            list.add(highestKillComboFormat)
            list.add(skillXPGainFormat)
            with(bestiaryFormatting){
                list.add(base)
                list.add(openMenu)
                list.add(maxed)
                list.add(showMax_progress)
                list.add(progress)
            }
        }
        val jsonArray = JsonArray()
        for (l in list) {
            jsonArray.add(JsonPrimitive(l))
        }
        val base64 = Base64.getEncoder().encodeToString((exportPrefix + jsonArray).toByteArray(StandardCharsets.UTF_8))
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(base64), null)
    }

    fun resetFormatting() {
        with(config.textFormatting) {
            titleFormat = "&6Ghost Counter"
            ghostKiledFormat = "  &6Ghost Killed: &b%value% &7(%session%)"
            sorrowsFormat = "  &6Sorrow: &b%value% &7(%session%)"
            ghostSinceSorrowFormat = "  &6Ghost since Sorrow: &b%value%"
            ghostKillPerSorrowFormat = "  &6Ghosts/Sorrow: &b%value%"
            voltasFormat = "  &6Volta: &b%value% &7(%session%)"
            plasmasFormat = "  &6Plasmas: &b%value% &7(%session%)"
            ghostlyBootsFormat = "  &6Ghostly Boots: &b%value% &7(%session%)"
            bagOfCashFormat = "  &6Bag Of Cash: &b%value% &7(%session%)"
            avgMagicFindFormat = "  &6Avg Magic Find: &b%value%"
            scavengerCoinsFormat = "  &6Scavenger Coins: &b%value% &7(%session%)"
            killComboFormat = "  &6Kill Combo: &b%value%"
            highestKillComboFormat = "  &6Highest Kill Combo: &b%value% &7(%session%)"
            skillXPGainFormat = "  &6Skill XP Gained: &b%value% &7(%session%)"
            xpHourFormat = "  &6XP/h: &b%value%"
            with(bestiaryFormatting) {
                base = "  &6Bestiary %currentLevel%->%nextLevel%: &b%value%"
                openMenu = "§cOpen Bestiary Menu !"
                maxed = "%currentKill% (&c&lMaxed!)"
                showMax_progress = "%currentKill%/3M (%percentNumber%%)"
                progress = "%currentKill%/%killNeeded%"
            }
        }
    }
}
