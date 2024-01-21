package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SpecialColour
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.moulberry.notenoughupdates.util.Constants
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.ceil

class SkillProgress {

    private val config get() = SkyHanniMod.feature.misc.skillProgressDisplayConfig
    private var skillText = ""
    private var actionBarData = ""
    private var lastSkillXp: String? = ""
    private var skillIsShown = false
    private var skillExpPercentage = 0.0
    private var display = emptyList<List<Any>>()
    private val HpSymbol = '\u2764'
    private val MnSymbol = '\u270e'
    private val reviveSymbol = "Revive"
    private val chickenRaceSymbol = "CHICKEN RACING"
    private val armadilloName = "Armadillo"
    private val multipleNotations = mapOf(
        "k" to 1000,
        "m" to 1000000
    )
    private val stackMap = mapOf(
        "Farming" to Utils.createItemStack(Items.golden_hoe, "Farming"),
        "Combat" to Utils.createItemStack(Items.golden_sword, "Combat"),
        "Foraging" to Utils.createItemStack(Items.golden_axe, "Foraging"),
        "Runecrafting" to Utils.createItemStack(Blocks.piston_head, "Runecrafting"),
        "Alchemy" to Utils.createItemStack(Items.brewing_stand, "Alchemy"),
        "Mining" to Utils.createItemStack(Items.golden_pickaxe, "Mining"),
        "Enchanting" to Utils.createItemStack(Blocks.enchanting_table, "Enchanting"),
        "Fishing" to Utils.createItemStack(Items.fishing_rod, "Fishing")
    )
    private val levelingMap = mutableMapOf<Int, Int>()

    private val patternGroup = RepoPattern.group("skilldisplay")
    private val testPattern by patternGroup.pattern("skillpattern", "[+](?<gained>[0-9,.]+) (?<skillName>\\w+) \\((?<currentXp>[0-9,.km]+)/(?<neededXp>[0-9,.km]+)\\)")

    @SubscribeEvent
    fun onActionBar(event: ClientChatReceivedEvent) {
        if (!isEnabled()) return
        if (
            (event.message.unformattedText.contains(HpSymbol.toString())
                || event.message.unformattedText.contains(MnSymbol.toString())
                || event.message.unformattedText.contains(reviveSymbol)
                || event.message.unformattedText.contains(chickenRaceSymbol))
            || event.message.unformattedText.contains(armadilloName)
        ) {
            actionBarData = event.message.unformattedText
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (event.repeatSeconds(1)) {
            val segmentString = segmentString(actionBarData, ")", '+', ' ', 1, 1, SegmentationOptions.ALL_INSTANCES_LEFT)
            if (segmentString != lastSkillXp) {
              //  println("segmentString: $segmentString")
                var inBetweenBrackets: String? = null
                var percentage = -1f

                if (segmentString != null) {
                    inBetweenBrackets = segmentString(segmentString, "(", '(', ')', 1, 1, SegmentationOptions.TOTALLY_EXCLUSIVE)
                   //println("inBetweenBrackets: $inBetweenBrackets")
                }

                if (inBetweenBrackets != null) {
                    percentage = praseSkillPercentage(inBetweenBrackets)
                   // println("percentage: $percentage")
                }

                if (percentage != -1f) {
                    lastSkillXp = segmentString
                    val wholeString = segmentString?.removeColor()
                  //  println("wholeString: $wholeString")

                    skillIsShown = true
                    skillText = wholeString!!
                    //skillExpPercentage = percentage
                } else {
                    skillIsShown = false
                }
                update()
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!skillIsShown) return
        config.position.renderStringsAndItems(display, itemScale = 1.5, posLabel = "Skill Progress")
        if (config.showProgressBar) {
            val progress = Renderable.progressBar(skillExpPercentage, Color(SpecialColour.specialToChromaRGB(config.barColor)), Color.PINK, width = 125)
            config.barPosition.renderRenderables(listOf(progress), posLabel = "Skill Progress Bar")
        }
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        testPattern.matchMatcher(skillText) {
            newDisplay.add(buildList {
                val gained = group("gained")
                val skillName = group("skillName")
                val currentXp = group("currentXp").formatNumber()
                val neededXp = group("neededXp").formatNumber()
                val level = getLevel(neededXp)

                val (skillLevel, skillCurrentXp, skillNeededXp) = getSkillInfo(level, currentXp, neededXp)

               // println(getSkillInfo(level, currentXp.formatNumber(), neededXp.formatNumber()))
                if (config.showLevel) {
                    add("§9[§d$skillLevel§9] ")
                }

                if (config.useIcon)
                    add(stackMap.getOrDefault(skillName, Utils.createItemStack(Items.banner, "Default")))
                var percent = if (skillNeededXp == 0L) 100F else 100F * skillCurrentXp.toFloat() / skillNeededXp
                skillExpPercentage = (percent.toDouble() / 100)
                println("p: $skillExpPercentage")

                if (config.usePercentage) {
                    if (config.useSkillName)
                        add("§b+$gained $skillName §7(§6${percent.roundToPrecision(2)}%§7)")
                    else
                        add("§b+$gained §7(§6${percent.roundToPrecision(2)}%§7)")
                } else {
                    if (config.useSkillName)
                        add("§b+$gained $skillName §7(§6${skillCurrentXp.addSeparators()}§7/§6${skillNeededXp.addSeparators()}§7)")
                    else
                        add("§b+$gained §7(§6${skillCurrentXp.addSeparators()}§7/§6${skillNeededXp.addSeparators()}§7)")
                }

                percent = 100f.coerceAtMost(percent)
              //  println("percent: $percent")
                if (config.showActionLeft && percent != 100f) {
                    add(" - ")
                    if (gained != "0") {
                        val actionLeft = (ceil(skillNeededXp - skillCurrentXp.toFloat()) / gained.formatNumber().toFloat()).toLong().addSeparators()
                        add("§6$actionLeft Left")
                    } else {
                        add("∞ action left")
                    }
                }
            })
        }
        return newDisplay
    }

    private fun getSkillInfo(currentLevel: Int, currentXp: Long, neededXp: Long): Triple<Int, Long, Long> {
        var overflowExp = currentXp
        var level = 60
        var slope = 600000L
        var xpForCurrentLevel = 7000000L
        while (overflowExp > xpForCurrentLevel) {
            level += 1
            overflowExp -= xpForCurrentLevel
            xpForCurrentLevel += slope
            if (level % 10 == 0)
                slope *= 2

        }
        return if (currentLevel >= 60 && config.showOverflow) Triple(level, overflowExp, xpForCurrentLevel) else Triple(currentLevel, currentXp, neededXp)
    }

    private fun getLevel(neededXp: Long): Int {
        val map: List<Int> = Gson().fromJson(Utils.getElement(Constants.LEVELING, "leveling_xp").asJsonArray.toString(), object : TypeToken<List<Int>>() {}.type)
        for ((i, e) in map.withIndex()) {
            levelingMap[e] = i
        }
        return levelingMap.getOrDefault(neededXp.toInt(), 60)
    }

    enum class SegmentationOptions {
        TOTALLY_EXCLUSIVE,
        TOTALLY_INCLUSIVE,
        ALL_INSTANCES_RIGHT,
        ALL_INSTANCES_LEFT
    }


    private fun segmentString(string: String, symbol: String, leftChar: Char, rightChar: Char, allowedInstancesL: Int, allowedInstancesR: Int, vararg options: SegmentationOptions): String? {
        var totallyExclusive = false
        var totallyInclusive = false
        var allInstancesR = false
        var allInstancesL = false
        for (option in options) {
            if (option == SegmentationOptions.TOTALLY_EXCLUSIVE) totallyExclusive = true
            if (option == SegmentationOptions.TOTALLY_INCLUSIVE) totallyInclusive = true
            if (option == SegmentationOptions.ALL_INSTANCES_RIGHT) allInstancesR = true
            if (option == SegmentationOptions.ALL_INSTANCES_LEFT) allInstancesL = true
        }
        return segmentString(string, symbol, leftChar, rightChar, allowedInstancesL, allowedInstancesR, totallyExclusive, totallyInclusive, allInstancesR, allInstancesL)
    }

    private fun segmentString(string: String, symbol: String, leftChar: Char, rightChar: Char, allowedInstancesL: Int, allowedInstancesR: Int, totallyExclusive: Boolean, totallyInclusive: Boolean, allInstancesR: Boolean, allInstancesL: Boolean): String? {
        var allowedInstancesL = allowedInstancesL
        var allowedInstancesR = allowedInstancesR
        var leftIdx = 0
        var rightIdx = 0
        return if (string.contains(symbol)) {
            var symbolIdx = string.indexOf(symbol)
            run {
                var i = 0
                while (symbolIdx - i > -1) {
                    leftIdx = symbolIdx - i
                    if (string[symbolIdx - i] == leftChar) allowedInstancesL--
                    if (allowedInstancesL == 0) {
                        break
                    }
                    i++
                }
            }
            symbolIdx += symbol.length - 1
            var i = 0
            while (symbolIdx + i < string.length) {
                rightIdx = symbolIdx + i
                if (string[symbolIdx + i] == rightChar) allowedInstancesR--
                if (allowedInstancesR == 0) {
                    break
                }
                i++
            }
            if (allowedInstancesL != 0 && allInstancesL) return null
            if (allowedInstancesR != 0 && allInstancesR) null else string.substring(leftIdx + if (totallyExclusive) 1 else 0, rightIdx + if (totallyInclusive) 1 else 0)
        } else {
            null
        }
    }

    private fun praseSkillPercentage(skillInfo: String?): Float {
        skillInfo?.let {
            if (skillInfo.contains("%")) {
                val skillInfo2 = skillInfo.replace("%", "")
                return skillInfo2.toFloat() / 100f
            } else if (skillInfo.contains("/")) {
                val twoValues = skillInfo.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val first: Float = hypixelShortValueFormattingToFloat(twoValues[0])
                val second: Float = hypixelShortValueFormattingToFloat(twoValues[1])
                return first / second
            }
            return -1f
        } ?: return -1f
    }

    private fun hypixelShortValueFormattingToFloat(s: String): Float {
        var o = s.replace(",", "")
        for (notation in multipleNotations.keys) {
            if (o.contains(notation)) {
                o = o.replace(notation, "")
                return o.toFloat() * multipleNotations[notation]!!
            }
        }
        return o.toFloat()
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
