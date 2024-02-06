package at.hannibal2.skyhanni.features.misc.skillprogress

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.SkillAPI
import at.hannibal2.skyhanni.api.SkillAPI.lastUpdate
import at.hannibal2.skyhanni.api.SkillAPI.oldSkillInfoMap
import at.hannibal2.skyhanni.api.SkillAPI.showDisplay
import at.hannibal2.skyhanni.api.SkillAPI.skillMap
import at.hannibal2.skyhanni.api.SkillAPI.skillXPInfoMap
import at.hannibal2.skyhanni.api.SkillAPI.stackMap
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.events.SkillOverflowLevelupEvent
import at.hannibal2.skyhanni.features.misc.skillprogress.SkillUtil.activeSkill
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.interpolate
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SpecialColour
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.init.Items
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

object SkillProgress {

    private val config get() = SkyHanniMod.feature.misc.skillProgressConfig
    private var skillExpPercentage = 0.0
    private var display = emptyList<List<Any>>()
    private var allDisplay = emptyList<List<Any>>()
    private var etaDisplay = emptyList<Renderable>()
    private val defaultStack = Utils.createItemStack(Items.banner, "Default")
    private var lastGainUpdate = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (showDisplay) {
            config.position.renderStringsAndItems(display, posLabel = "Skill Progress")
            if (config.progressBarConfig.enabled.get() && display.isNotEmpty()) {
                val progress = if (config.progressBarConfig.useTexturedBar.get()) {
                    val factor = (skillExpPercentage.toFloat().coerceAtMost(1f)) * 182
                    Renderable.texturedProgressBar(factor,
                        Color(SpecialColour.specialToChromaRGB(config.progressBarConfig.barStartColor)),
                        texture = config.progressBarConfig.texturedBar.usedTexture.get(),
                        useChroma = config.progressBarConfig.useChroma.get())
                } else
                    Renderable.progressBar(skillExpPercentage,
                        Color(SpecialColour.specialToChromaRGB(config.progressBarConfig.barStartColor)),
                        Color(SpecialColour.specialToChromaRGB(config.progressBarConfig.barStartColor)),
                        width = config.progressBarConfig.regularBar.width,
                        height = config.progressBarConfig.regularBar.height,
                        useChroma = config.progressBarConfig.useChroma.get())

                config.barPosition.renderRenderables(listOf(progress), posLabel = "Skill Progress Bar")
            }
        }

        if (config.showAllSkillProgress.get()) {
            config.allSkillPosition.renderStringsAndItems(allDisplay, posLabel = "All Skills Display")
        }

        if (config.showEtaSkillProgress.get()) {
            config.etaPosition.renderRenderables(etaDisplay, posLabel = "Skill ETA")
        }

    }

    @SubscribeEvent
    fun onProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
        allDisplay = emptyList()
        etaDisplay = emptyList()
        skillExpPercentage = 0.0
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (lastUpdate.passedSince() > 3.seconds) showDisplay = config.alwaysShow.get()

        if (event.repeatSeconds(1)){
            allDisplay = formatAllDisplay(drawAllDisplay())
            etaDisplay = drawETADisplay()
        }

        if (event.repeatSeconds(2)) {
            update()
            updateSkillInfo(activeSkill)
        }
    }

    @SubscribeEvent
    fun onLevelUp(event: SkillOverflowLevelupEvent) {
        if (!isEnabled()) return
        if (!config.overflowConfig.enableInChat) return
        val skillName = event.skillName
        val oldLevel = event.oldLevel
        val newLevel = event.newLevel

        val rewards = buildList {
            add("  §r§7§8+§b1 Flexing Point")
            if (newLevel % 5 == 0)
                add("  §r§7§8+§d50 SkyHanni User Luck")
        }

        LorenzUtils.chat("§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬", false)
        LorenzUtils.chat("  §r§b§lSKILL LEVEL UP §3${skillName.firstLetterUppercase()} §8$oldLevel➜§3$newLevel", false)
        LorenzUtils.chat("", false)
        LorenzUtils.chat("  §r§a§lREWARDS", false)
        for (reward in rewards)
            LorenzUtils.chat(reward, false)
        LorenzUtils.chat("§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬", false)
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        LorenzUtils.onToggle(
            config.enabled,
            config.progressBarConfig.enabled,
            config.progressBarConfig.useChroma,
            config.progressBarConfig.useTexturedBar,
            config.alwaysShow,
            config.showActionLeft,
            config.useIcon,
            config.usePercentage,
            config.useSkillName,
            config.overflowConfig.enableInDisplay,
            config.overflowConfig.enableInProgressBar,
            config.overflowConfig.enableInEtaDisplay,
            config.showAllSkillProgress,
            config.showEtaSkillProgress
        ) {
            updateDisplay()
            update()
        }
    }

    fun updateDisplay() {
        display = drawDisplay()
    }

    private fun update() {
        lastGainUpdate = SimpleTimeMark.now()
        skillXPInfoMap.forEach{
            it.value.xpGainLast = it.value.xpGainHour
        }
    }

    private fun formatAllDisplay(map: List<List<Any>>): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.allskillEntryList) {
            newList.add(map[index.ordinal])
        }
        return newList
    }


    private fun drawAllDisplay() = buildList {
        val skillMap = skillMap ?: return@buildList
        val sortedMap = skillMap.toList().sortedBy { (name,_) ->
            if (name.length >= 2) name.substring(0, 2) else name
        }.toMap()
        for ((skillName, skillInfo) in sortedMap) {
            val (level, currentXp, currentXpMax, totalXp) =
                if (config.overflowConfig.enableInAllDisplay.get())
                    LorenzUtils.Quad(skillInfo.overflowLevel, skillInfo.overflowCurrentXp, skillInfo.overflowCurrentXpMax, skillInfo.overflowTotalXp)
                else
                    LorenzUtils.Quad(skillInfo.level, skillInfo.currentXp, skillInfo.currentXpMax, skillInfo.totalXp)

            val tips = buildList {
                add("§6Level: §b${level}")
                add("§6Current XP: §b${currentXp.addSeparators()}")
                add("§6Needed XP: §b${currentXpMax.addSeparators()}")
                add("§6Total XP: §b${totalXp.addSeparators()}")
            }
            val nameColor = if (skillName == activeSkill) "§e" else "§6"
            addAsSingletonList(Renderable.hoverTips(buildString {
                append("$nameColor${skillName.firstLetterUppercase()} $level ")
                append("§7(")
                append("§b${currentXp.addSeparators()}")
                if (currentXpMax != 0L) {
                    append("§6/")
                    append("§b${currentXpMax.addSeparators()}")
                }
                append("§7)")
            }, tips))
        }
    }

    private fun drawETADisplay() = buildList {
        val skillInfo = skillMap?.get(activeSkill) ?: return@buildList
        val xpInfo = skillXPInfoMap[activeSkill] ?: return@buildList
        val skillInfoLast = oldSkillInfoMap[activeSkill] ?: return@buildList
        oldSkillInfoMap[activeSkill] = skillInfo
        val level = if (config.overflowConfig.enableInEtaDisplay.get()) skillInfo.overflowLevel else skillInfo.level

        add(Renderable.string("§6Skill: §b${activeSkill.firstLetterUppercase()}"))
        add(Renderable.string("§6Level: §b$level"))

        var xpInterp = xpInfo.xpGainHour
        if (xpInfo.xpGainLast == xpInfo.xpGainHour && xpInfo.xpGainHour <= 0){
            add(Renderable.string("§6XP/h: §cN/A"))
        }else{
            xpInterp = interpolate(xpInfo.xpGainHour, xpInfo.xpGainLast, lastGainUpdate.toMillis())
            add(Renderable.string("§6XP/h: §b${xpInterp.addSeparators()}"))
        }

        var remaining = skillInfo.overflowCurrentXpMax - skillInfo.overflowCurrentXp
        if (skillInfo.overflowCurrentXpMax == skillInfoLast.overflowCurrentXpMax) {
            remaining = interpolate(remaining.toFloat(), (skillInfoLast.overflowCurrentXpMax - skillInfoLast.overflowCurrentXp).toFloat(), lastGainUpdate.toMillis()).toLong()
        }

        if (xpInfo.xpGainHour < 1000) {
            add(Renderable.string("§6ETA: §cN/A"))
        }else{
            add(Renderable.string("§6ETA: §b${Utils.prettyTime((remaining) * 1000 * 60 * 60 / xpInterp.toLong())}"))
        }
    }

    private fun drawDisplay() = buildList {
        val skillMap = skillMap ?: return@buildList
        val skill = skillMap[activeSkill] ?: return@buildList

        val (level, currentXp, currentXpMax, total) = if (config.overflowConfig.enableInDisplay.get())
            LorenzUtils.Quad(skill.overflowLevel, skill.overflowCurrentXp, skill.overflowCurrentXpMax, skill.overflowTotalXp)
        else
            LorenzUtils.Quad(skill.level, skill.currentXp, skill.currentXpMax, skill.totalXp)

        add(buildList {
            if (config.showLevel.get())
                add("§9[§d$level§9] ")

            if (config.useIcon.get())
                add(Renderable.itemStack(stackMap.getOrDefault(activeSkill.firstLetterUppercase(), defaultStack), 1.5))

            add(buildString {
                append("§b+${skill.lastGain} ")

                if (config.useSkillName.get())
                    append("${activeSkill.firstLetterUppercase()} ")

                val (barCurrent, barMax) = if (config.overflowConfig.enableInProgressBar.get())
                    Pair(skill.overflowCurrentXp, skill.overflowCurrentXpMax)
                else
                    Pair(skill.currentXp, skill.currentXpMax)

                val barPercent = if (barMax == 0L) 100F else 100F * barCurrent / barMax
                skillExpPercentage = (barPercent.toDouble() / 100)

                val percent = if (currentXpMax == 0L) 100F else 100F * currentXp / currentXpMax

                if (config.usePercentage.get())
                    append("§7(§6${percent.roundToPrecision(2)}%§7)")
                else {
                    if (currentXpMax == 0L)
                        append("§7(§6${currentXp.addSeparators()}§7)")
                    else
                        append("§7(§6${currentXp.addSeparators()}§7/§6${currentXpMax.addSeparators()}§7)")
                }

                if (config.showActionLeft.get() && percent != 100f) {
                    append(" - ")
                    if (skill.lastGain != "") {
                        val actionLeft = (ceil(currentXpMax.toDouble() - currentXp) / skill.lastGain.formatNumber()).toLong().addSeparators()
                        append("§6$actionLeft Left")
                    } else {
                        append("∞ Left")
                    }
                }
            })
        })
    }

    private fun updateSkillInfo(skill: String) {
        val xpInfo = skillXPInfoMap.getOrPut(skill) { SkillAPI.SkillXPInfo() }
        val skillInfo = skillMap?.get(skill) ?: return
        oldSkillInfoMap[skill] = skillInfo

        val totalXp = skillInfo.currentXp

        if (xpInfo.lastTotalXp > 0) {
            val delta = totalXp - xpInfo.lastTotalXp
            if (delta > 0 && delta < 1000) {
                xpInfo.xpGainQueue.add(0, delta)

                calculateXPHour(xpInfo)
            } else if (xpInfo.timer > 0) {
                xpInfo.timer--
                xpInfo.xpGainQueue.add(0, 0f)

                calculateXPHour(xpInfo)
            } else if (delta <= 0) {
                xpInfo.isActive = false
            }
        }
        xpInfo.lastTotalXp = totalXp.toFloat()
    }

    private fun calculateXPHour(xpInfo: SkillAPI.SkillXPInfo) {
        while (xpInfo.xpGainQueue.size > 30) {
            xpInfo.xpGainQueue.removeLast()
        }

        var totalGain = 0f
        for (f in xpInfo.xpGainQueue) totalGain += f

        xpInfo.xpGainHour = totalGain * (60 * 60) / xpInfo.xpGainQueue.size

        xpInfo.isActive = true
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled.get()
}
