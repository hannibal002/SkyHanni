package at.hannibal2.skyhanni.features.misc.ghostcounter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SkillExperience
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.bazaar.BazaarApi.Companion.getBazaarData
import at.hannibal2.skyhanni.features.misc.ghostcounter.GhostData.Option.*
import at.hannibal2.skyhanni.features.misc.ghostcounter.GhostData.bestiaryData
import at.hannibal2.skyhanni.features.misc.ghostcounter.GhostUtil.formatBestiary
import at.hannibal2.skyhanni.features.misc.ghostcounter.GhostUtil.formatText
import at.hannibal2.skyhanni.features.misc.ghostcounter.GhostUtil.isUsingCTGhostCounter
import at.hannibal2.skyhanni.features.misc.ghostcounter.GhostUtil.preFormat
import at.hannibal2.skyhanni.features.misc.ghostcounter.GhostUtil.prettyTime
import at.hannibal2.skyhanni.utils.CombatUtils._isKilling
import at.hannibal2.skyhanni.utils.CombatUtils.calculateETA
import at.hannibal2.skyhanni.utils.CombatUtils.calculateXP
import at.hannibal2.skyhanni.utils.CombatUtils.interp
import at.hannibal2.skyhanni.utils.CombatUtils.isKilling
import at.hannibal2.skyhanni.utils.CombatUtils.killGainHour
import at.hannibal2.skyhanni.utils.CombatUtils.killGainHourLast
import at.hannibal2.skyhanni.utils.CombatUtils.lastKillUpdate
import at.hannibal2.skyhanni.utils.CombatUtils.lastUpdate
import at.hannibal2.skyhanni.utils.CombatUtils.xpGainHour
import at.hannibal2.skyhanni.utils.CombatUtils.xpGainHourLast
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.chat
import at.hannibal2.skyhanni.utils.LorenzUtils.clickableChat
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.moulberry.notenoughupdates.util.Utils
import io.github.moulberry.notenoughupdates.util.XPInformation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.io.File
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object GhostCounter {

    val config get() = SkyHanniMod.feature.ghostCounter
    val hidden get() = ProfileStorageData.profileSpecific?.ghostCounter
    private var display = emptyList<List<Any>>()
    var ghostCounterV3File =
        File("." + File.separator + "config" + File.separator + "ChatTriggers" + File.separator + "modules" + File.separator + "GhostCounterV3" + File.separator + ".persistantData.json")
    private val skillXPPattern = "[+](?<gained>[0-9,.]+) \\((?<current>[0-9,.]+)(?:/(?<total>[0-9,.]+))?\\)".toPattern()
    private val combatSectionPattern =
        ".*[+](?<gained>[0-9,.]+) (?<skillName>[A-Za-z]+) \\((?<progress>(?<current>[0-9.,]+)/(?<total>[0-9.,]+)|(?<percent>[0-9.]+)%)\\).*".toPattern()
    private val killComboExpiredPattern =
        "§cYour Kill Combo has expired! You reached a (?<combo>.*) Kill Combo!".toPattern()
    private val ghostXPPattern =
        "(?<current>\\d+(?:\\.\\d+)?(?:,\\d+)?[kK]?)/(?<total>\\d+(?:\\.\\d+)?(?:,\\d+)?[kKmM]?)".toPattern()
    private val bestiaryPattern =
        ".*(?:§\\d|§\\w)+BESTIARY (?:§\\d|§\\w)+Ghost (?:§\\d|§\\w)(?<previousLevel>\\d+)➜(?:§\\d|§\\w)(?<nextLevel>\\d+).*".toPattern() //   &3&lBESTIARY &b&lGhost &89➜&b10
    private val skillLevelPattern = ".*§e§lSkills: §r§a(?<skillName>.*) (?<skillLevel>\\d+).*".toPattern()
    private val format = NumberFormat.getInstance()
    private var percent: Float = 0.0f
    private var totalSkillXp = 0
    private var currentSkillXp = 0.0f
    private var skillText = ""
    private var lastParsedSkillSection = ""
    private var lastSkillProgressString: String? = null
    private var lastXp: String = "0"
    private var gain: Int = 0
    private var num: Double = 0.0
    private var inMist = false
    private var notifyCTModule = true
    var bestiaryCurrentKill = 0
    private var killETA = ""
    private var currentSkill = ""
    private var currentSkillLevel = -1
    private const val CONFIG_VALUE_VERSION = 1
    private val SORROW = "SORROW".asInternalName()
    private val PLASMA = "PLASMA".asInternalName()
    private val VOLTA = "VOLTA".asInternalName()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        if (config.onlyOnMist && !inMist) return
        config.position.renderStringsAndItems(
            display,
            extraSpace = config.extraSpace,
            posLabel = "Ghost Counter"
        )
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
        val textFormatting = config.textFormatting
        val ghostKillPerSorrow: Int = when (SORROWCOUNT.get()) {
            0.0 -> 0
            else -> "${((((KILLS.get() / SORROWCOUNT.get()) + Math.ulp(1.0)) * 100) / 100).roundToInt()}".toInt()
        }
        val avgMagicFind = when (TOTALDROPS.get()) {
            0.0 -> "0"
            else -> "${((((hidden?.totalMF!! / TOTALDROPS.get()) + Math.ulp(1.0)) * 100) / 100).roundToPrecision(2)}"
        }

        val xpHourFormatting = textFormatting.xpHourFormatting
        val xpInterp: Float
        val xp = if (xpGainHourLast == xpGainHour && xpGainHour <= 0) {
            xpHourFormatting.noData
        } else {
            xpInterp = interp(xpGainHour, xpGainHourLast, lastUpdate)
            val part = "([0-9]{3,}[^,]+)".toRegex().find(format.format(xpInterp))?.groupValues?.get(1) ?: "N/A"
            "$part ${if (isKilling) "" else xpHourFormatting.paused}"
        }

        val killHourFormatting = textFormatting.killHourFormatting
        val killHour: String
        var killInterp: Long = 0
        if (killGainHourLast == killGainHour && killGainHour <= 0) {
            killHour = killHourFormatting.noData
        } else {
            killInterp = interp(killGainHour.toFloat(), killGainHourLast.toFloat(), lastKillUpdate).toLong()
            killHour = "${format.format(killInterp)} ${if (_isKilling) "" else killHourFormatting.paused}"
        }

        val bestiaryFormatting = textFormatting.bestiaryFormatting
        val currentKill = hidden?.bestiaryCurrentKill?.toInt() ?: 0
        val killNeeded = hidden?.bestiaryKillNeeded?.toInt() ?: 0
        val nextLevel = hidden?.bestiaryNextLevel?.toInt() ?: -1
        val bestiary = if (config.showMax) {
            when (nextLevel) {
                26 -> bestiaryFormatting.maxed.replace("%currentKill%", currentKill.addSeparators())
                in 1..25 -> {
                    val sum = bestiaryData.filterKeys { it <= nextLevel - 1 }.values.sum()

                    val cKill = sum + currentKill
                    bestiaryCurrentKill = cKill
                    bestiaryFormatting.showMax_progress
                }

                else -> bestiaryFormatting.openMenu
            }
        } else {
            when (nextLevel) {
                26 -> bestiaryFormatting.maxed
                in 1..25 -> bestiaryFormatting.progress
                else -> bestiaryFormatting.openMenu
            }
        }

        val etaFormatting = textFormatting.etaFormatting
        val remaining: Int = when (config.showMax) {
            true -> 250_000 - bestiaryCurrentKill
            false -> killNeeded - currentKill
        }

        val eta = if (remaining < 0) {
            etaFormatting.maxed
        } else {
            if (killGainHour < 1) {
                etaFormatting.noData
            } else {
                val timeMap = prettyTime(remaining.toLong() * 1000 * 60 * 60 / killInterp)
                val time = buildString {
                    if (timeMap.isNotEmpty()) {
                        val formatMap = mapOf(
                            "%days%" to "days",
                            "%hours%" to "hours",
                            "%minutes%" to "minutes",
                            "%seconds%" to "seconds"
                        )
                        for ((format, key) in formatMap) {
                            if (etaFormatting.time.contains(format)) {
                                timeMap[key]?.let { value ->
                                    append("$value${format[1]}")
                                }
                            }
                        }
                    } else {
                        append("§cEnded!")
                    }
                }
                killETA = time
                etaFormatting.progress + if (_isKilling) "" else etaFormatting.paused
            }
        }

        addAsSingletonList(Utils.chromaStringByColourCode(textFormatting.titleFormat.replace("&", "§")))
        addAsSingletonList(textFormatting.ghostKilledFormat.formatText(KILLS))
        addAsSingletonList(textFormatting.sorrowsFormat.formatText(SORROWCOUNT))
        addAsSingletonList(textFormatting.ghostSinceSorrowFormat.formatText(GHOSTSINCESORROW.getInt()))
        addAsSingletonList(textFormatting.ghostKillPerSorrowFormat.formatText(ghostKillPerSorrow))
        addAsSingletonList(textFormatting.voltasFormat.formatText(VOLTACOUNT))
        addAsSingletonList(textFormatting.plasmasFormat.formatText(PLASMACOUNT))
        addAsSingletonList(textFormatting.ghostlyBootsFormat.formatText(GHOSTLYBOOTS))
        addAsSingletonList(textFormatting.bagOfCashFormat.formatText(BAGOFCASH))
        addAsSingletonList(textFormatting.avgMagicFindFormat.formatText(avgMagicFind))
        addAsSingletonList(textFormatting.scavengerCoinsFormat.formatText(SCAVENGERCOINS))
        addAsSingletonList(textFormatting.killComboFormat.formatText(MAXKILLCOMBO))
        addAsSingletonList(textFormatting.highestKillComboFormat.formatText(MAXKILLCOMBO))
        addAsSingletonList(textFormatting.skillXPGainFormat.formatText(SKILLXPGAINED))
        addAsSingletonList(
            bestiaryFormatting.base.preFormat(bestiary, nextLevel - 1, nextLevel)
                .formatBestiary(currentKill, killNeeded)
        )

        addAsSingletonList(xpHourFormatting.base.formatText(xp))
        addAsSingletonList(killHourFormatting.base.formatText(killHour))
        addAsSingletonList(etaFormatting.base.formatText(eta).formatText(killETA))

        val rate = 0.12 * (1 + (avgMagicFind.toDouble() / 100))
        val sorrowValue = SORROW.getBazaarData()?.buyPrice?.toLong() ?: 0L
        val final: String = (killInterp * sorrowValue * (rate / 100)).toLong().addSeparators()
        val plasmaValue = PLASMA.getBazaarData()?.buyPrice?.toLong() ?: 0L
        val voltaValue = VOLTA.getBazaarData()?.buyPrice?.toLong() ?: 0L
        var moneyMade: Long = 0
        val priceMap = listOf(
            Triple("Sorrow", SORROWCOUNT.getInt(), sorrowValue),
            Triple("Plasma", PLASMACOUNT.getInt(), plasmaValue),
            Triple("Volta", VOLTACOUNT.getInt(), voltaValue),
            Triple("Bag Of Cash", BAGOFCASH.getInt(), 1_000_000),
            Triple("Scavenger Coins", SCAVENGERCOINS.getInt(), 1),
            Triple("Ghostly Boots", GHOSTLYBOOTS.getInt(), 77_777)
        )
        val moneyMadeTips = buildList {
            for ((name, count, value) in priceMap) {
                moneyMade += (count.toLong() * value.toLong())
                add("$name: §b${value.addSeparators()} §fx §b${count.addSeparators()} §f= §6${(value.toLong() * count.toLong()).addSeparators()}")
            }
            add("§bTotal: §6${moneyMade.addSeparators()}")
            add("§eClick to copy to clipboard!")
        }
        val moneyMadeWithClickableTips = Renderable.clickAndHover(
            textFormatting.moneyMadeFormat.formatText(moneyMade.addSeparators()),
            moneyMadeTips
        ) { OSUtils.copyToClipboard(moneyMadeTips.joinToString("\n").removeColor()) }
        addAsSingletonList(textFormatting.moneyHourFormat.formatText(final))
        addAsSingletonList(moneyMadeWithClickableTips)
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (event.repeatSeconds(1)) {
            skillXPPattern.matchMatcher(skillText) {
                val gained = group("gained").formatNumber().toDouble()
                val current = group("current")
                if (current != lastXp) {
                    val res = current.formatNumber().toString()
                    gain = (res.toLong() - lastXp.toLong()).toDouble().roundToInt()
                    num = (gain.toDouble() / gained)
                    if (gained in 150.0..450.0) {
                        if (lastXp != "0") {
                            if (num >= 0) {
                                KILLS.add(num)
                                KILLS.add(num, true)
                                GHOSTSINCESORROW.add(num)
                                KILLCOMBO.add(num)
                                SKILLXPGAINED.add(gained * num.roundToLong())
                                SKILLXPGAINED.add(gained * num.roundToLong(), true)
                                hidden?.bestiaryCurrentKill = hidden?.bestiaryCurrentKill?.plus(num) ?: num
                            }
                        }
                    }
                    lastXp = res
                }
            }
            if (notifyCTModule && ProfileStorageData.profileSpecific?.ghostCounter?.ctDataImported != true) {
                notifyCTModule = false
                if (isUsingCTGhostCounter()) {
                    clickableChat(
                        "§6[SkyHanni] GhostCounterV3 ChatTriggers module has been detected, do you want to import saved data ? Click here to import data",
                        "shimportghostcounterdata"
                    )
                }
            }
            inMist = LorenzUtils.skyBlockArea == "The Mist"
            update()
        }
        if (event.repeatSeconds(2)) {
            calculateXP()
            calculateETA()
        }
    }

    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        if (!isEnabled()) return
        if (!inMist) return
        combatSectionPattern.matchMatcher(event.message) {
            if (group("skillName").lowercase() != "combat") return
            parseCombatSection(event.message)
        }
    }

    private fun parseCombatSection(section: String) {
        val sb = StringBuilder()
        val nf = NumberFormat.getInstance(Locale.US)
        nf.maximumFractionDigits = 2
        if (lastParsedSkillSection == section) {
            sb.append(lastSkillProgressString)
        } else if (combatSectionPattern.matcher(section).find()) {
            combatSectionPattern.matchMatcher(section) {
                sb.append("+").append(group("gained"))
                val skillName = group("skillName")
                val skillPercent = group("percent") != null
                var parse = true
                if (skillPercent) {
                    percent = nf.parse(group("percent")).toFloat()
                    val level =
                        if (currentSkill == "Combat" && currentSkillLevel != -1) currentSkillLevel else XPInformation.getInstance()
                            .getSkillInfo(skillName)?.level ?: 0
                    if (level > 0) {
                        totalSkillXp = SkillExperience.getExpForNextLevel(level)
                        currentSkillXp = totalSkillXp * percent / 100
                    } else {
                        parse = false
                    }
                } else {
                    currentSkillXp = nf.parse(group("current")).toFloat()
                    totalSkillXp = nf.parse(group("total")).toInt()
                }
                percent = 100f.coerceAtMost(percent)
                if (!parse) {
                    sb.append(" (").append(String.format("%.2f", percent)).append("%)")
                } else {
                    sb.append(" (").append(nf.format(currentSkillXp))
                    if (totalSkillXp != 0) {
                        sb.append("/")
                        sb.append(nf.format(totalSkillXp))
                    }
                    sb.append(")")
                }
                lastParsedSkillSection = section
                lastSkillProgressString = sb.toString()
            }
            if (sb.toString().isNotEmpty()) {
                skillText = sb.toString()
            }
        }
    }

    @SubscribeEvent
    fun onTabUpdate(event: TabListUpdateEvent) {
        if (!isEnabled()) return
        for (line in event.tabList) {
            skillLevelPattern.matchMatcher(line) {
                currentSkill = group("skillName")
                currentSkillLevel = group("skillLevel").toInt()
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (LorenzUtils.skyBlockIsland != IslandType.DWARVEN_MINES) return
        for (opt in entries) {
            val pattern = opt.pattern ?: continue
            pattern.matchMatcher(event.message) {
                when (opt) {
                    SORROWCOUNT, VOLTACOUNT, PLASMACOUNT, GHOSTLYBOOTS -> {
                        opt.add(1.0)
                        opt.add(1.0, true)
                        hidden?.totalMF = hidden?.totalMF?.plus(group("mf").substring(4).toDouble())
                            ?: group("mf").substring(4).toDouble()
                        TOTALDROPS.add(1.0)
                        if (opt == SORROWCOUNT)
                            GHOSTSINCESORROW.set(0.0)
                        update()
                    }

                    BAGOFCASH -> {
                        BAGOFCASH.add(1.0)
                        BAGOFCASH.add(1.0, true)
                        update()
                    }

                    KILLCOMBOCOINS -> {
                        KILLCOMBOCOINS.set(KILLCOMBOCOINS.get() + group("coin").toDouble())
                        update()
                    }

                    else -> {}
                }
            }
        }
        killComboExpiredPattern.matchMatcher(event.message) {
            if (KILLCOMBO.getInt() > MAXKILLCOMBO.getInt()) {
                MAXKILLCOMBO.set(group("combo").formatNumber().toDouble())
            }
            if (KILLCOMBO.getInt() > MAXKILLCOMBO.getInt(true)) {
                MAXKILLCOMBO.set(group("combo").formatNumber().toDouble(), true)
            }
            KILLCOMBOCOINS.set(0.0)
            KILLCOMBO.set(0.0)
            update()
        }
        //replace with BestiaryLevelUpEvent ?
        bestiaryPattern.matchMatcher(event.message) {
            val currentLevel = group("nextLevel").toInt()
            when (val nextLevel = if (currentLevel >= 25) 26 else currentLevel + 1) {
                26 -> {
                    hidden?.bestiaryNextLevel = 26.0
                    hidden?.bestiaryCurrentKill = 250_000.0
                    hidden?.bestiaryKillNeeded = 0.0
                }

                else -> {
                    val killNeeded: Int = bestiaryData[nextLevel] ?: -1
                    hidden?.bestiaryNextLevel = nextLevel.toDouble()
                    hidden?.bestiaryCurrentKill = 0.0
                    hidden?.bestiaryKillNeeded = killNeeded.toDouble()
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
        SCAVENGERCOINS.add(event.coins, true)
        SCAVENGERCOINS.add(event.coins)
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val inventoryName = event.inventoryName
        if (inventoryName != "Bestiary ➜ Dwarven Mines") return
        val stacks = event.inventoryItems
        val ghostStack = stacks[10] ?: return
        val bestiaryNextLevel =
            if ("§\\wGhost".toRegex().matches(ghostStack.displayName)) 1 else ghostStack.displayName.substring(8)
                .romanToDecimal() + 1
        hidden?.bestiaryNextLevel = bestiaryNextLevel.toDouble()
        var kills = 0.0
        for (line in ghostStack.getLore()) {
            val l = line.removeColor().trim()
            if (l.startsWith("Kills: ")) {
                kills = "Kills: (.*)".toRegex().find(l)?.groupValues?.get(1)?.formatNumber()?.toDouble() ?: 0.0
            }
            ghostXPPattern.matchMatcher(line.removeColor().trim()) {
                hidden?.bestiaryCurrentKill = if (kills > 0) kills else group("current").formatNumber().toDouble()
                hidden?.bestiaryKillNeeded = group("total").formatNumber().toDouble()
            }
        }
        update()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        if (hidden?.configUpdateVersion == 0) {
            config.textFormatting.bestiaryFormatting.base = "  &6Bestiary %display%: &b%value%"
            chat("§e[SkyHanni] Your GhostCounter config has been automatically adjusted.")
            hidden?.configUpdateVersion = CONFIG_VALUE_VERSION
        }
    }

    fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && config.enabled && LorenzUtils.skyBlockIsland == IslandType.DWARVEN_MINES
    }
}