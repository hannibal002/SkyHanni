package at.hannibal2.skyhanni.features.misc.skillprogress

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.SkillAPI
import at.hannibal2.skyhanni.api.SkillAPI.activeSkill
import at.hannibal2.skyhanni.api.SkillAPI.lastUpdate
import at.hannibal2.skyhanni.api.SkillAPI.oldSkillInfoMap
import at.hannibal2.skyhanni.api.SkillAPI.showDisplay
import at.hannibal2.skyhanni.api.SkillAPI.skillMap
import at.hannibal2.skyhanni.api.SkillAPI.skillXPInfoMap
import at.hannibal2.skyhanni.api.SkillAPI.stackMap
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.events.SkillOverflowLevelupEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
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

    private val config get() = SkyHanniMod.feature.misc.skillProgressDisplayConfig
    private var skillExpPercentage = 0.0
    private var display = emptyList<List<Any>>()
    private var allDisplay = emptyList<Renderable>()
    private var etaDisplay = emptyList<Renderable>()
    private val defaultStack = Utils.createItemStack(Items.banner, "Default")

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (showDisplay) {
            config.position.renderStringsAndItems(display, posLabel = "Skill Progress")
            if (config.progressBarConfig.enabled && display.isNotEmpty()) {
                val progress = if (config.progressBarConfig.useTexturedBar) {
                    val factor = (skillExpPercentage.toFloat().coerceAtMost(1f)) * 182
                    Renderable.texturedProgressBar(factor,
                        Color(SpecialColour.specialToChromaRGB(config.progressBarConfig.barStartColor)),
                        texture = config.progressBarConfig.texturedBar.usedTexture.get(),
                        useChroma = config.progressBarConfig.useChroma)
                } else
                    Renderable.progressBar(skillExpPercentage,
                        Color(SpecialColour.specialToChromaRGB(config.progressBarConfig.barStartColor)),
                        Color(SpecialColour.specialToChromaRGB(config.progressBarConfig.barStartColor)),
                        width = config.progressBarConfig.regularBar.width,
                        height = config.progressBarConfig.regularBar.height,
                        useChroma = config.progressBarConfig.useChroma)

                config.barPosition.renderRenderables(listOf(progress), posLabel = "Skill Progress Bar")
            }
        }

        if (config.showAllSkillProgress) {
            config.allSkillPosition.renderRenderables(allDisplay, posLabel = "All Skills Display")
        }

        if (config.showEtaSkillProgress) {
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
        if (lastUpdate.passedSince() > 3.seconds) showDisplay = config.alwaysShow

        if (event.repeatSeconds(1)) {
            update()
        }

        if (event.repeatSeconds(2)) {
            updateSkillInfo(activeSkill)

            val skill = skillXPInfoMap[activeSkill] ?: SkillAPI.SkillXPInfo()
            skillXPInfoMap[activeSkill]?.xpGainLast = skill.xpGainHour
        }
    }

    @SubscribeEvent
    fun onLevelUp(event: SkillOverflowLevelupEvent) {
        val skillName = event.skillName
        val oldLevel = event.oldLevel
        val newLevel = event.newLevel

        val rewards = buildList {
            add("  §r§7§8+§b1 Flexing Point\n")
            if (newLevel % 5 == 0)
                add("  §r§7§8+§d50 SkyHanni User Luck\n")
        }

        LorenzUtils.chat(
            "§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n" +
                "  §r§b§lSKILL LEVEL UP §3${skillName.firstLetterUppercase()} §8$oldLevel➜§3$newLevel\n" +
                "\n" +
                "  §r§a§lREWARDS\n" +
                rewards.joinToString("") +
                "  §3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
    }


    fun updateDisplay() {
        display = drawDisplay()
    }

    private fun update() {
        allDisplay = drawAllDisplay()
        etaDisplay = drawETADisplay()
    }


    private fun drawAllDisplay() = buildList {
        val skillMap = skillMap ?: return@buildList
        for ((skillName, skillInfo) in skillMap) {
            val tips = buildList {
                add("§6Level: §b${skillInfo.level}")
                add("§6Current XP: §b${skillInfo.currentXp.addSeparators()}")
                add("§6Needed XP: §b${skillInfo.currentXpMax.addSeparators()}")
                add("§6Total XP: §b${skillInfo.totalXp.addSeparators()}")
            }
            val nameColor = if (skillName == activeSkill) "§e" else "§6"
            add(Renderable.hoverTips(buildString {
                append("$nameColor${skillName.firstLetterUppercase()} ${skillInfo.level} ")
                append("§7(")
                append("§b${skillInfo.currentXp.addSeparators()}")
                if (skillInfo.currentXpMax != 0L) {
                    append("§6/")
                    append("§b${skillInfo.currentXpMax.addSeparators()}")
                }
                append("§7)")
            }, tips))
        }
    }

    private fun drawETADisplay() = buildList {
        val skillInfo = skillMap?.get(activeSkill) ?: return@buildList
        val xpInfo = skillXPInfoMap[activeSkill] ?: return@buildList
        val xpInfoLast = oldSkillInfoMap[activeSkill] ?: return@buildList
        var remaining = skillInfo.currentXpMax - skillInfo.currentXp
        oldSkillInfoMap[activeSkill] = skillInfo
        if (skillInfo.currentXpMax == xpInfoLast.currentXpMax) {
            remaining = interpolate(remaining.toFloat(), (xpInfoLast.currentXpMax - xpInfoLast.currentXp).toFloat(), xpInfo.lastUpdate.toMillis()).toLong()
        }

        add(Renderable.string("§6Skill: §b${activeSkill.firstLetterUppercase()}"))
        add(Renderable.string("§6Level: §b${skillInfo.level}"))
        add(Renderable.string("§6XP/h: §b${xpInfo.xpGainHour.addSeparators()}"))

        if (xpInfo.xpGainLast != 0f) {
            val xpInterp = interpolate(xpInfo.xpGainHour, xpInfo.xpGainLast, xpInfo.lastUpdate.toMillis())
            add(Renderable.string("§6ETA: §b${Utils.prettyTime((remaining) * 1000 * 60 * 60 / xpInterp.toLong())}"))
        }
    }

    private fun drawDisplay() = buildList {
        val skillMap = skillMap ?: return@buildList
        val skill = skillMap[activeSkill] ?: return@buildList
        add(buildList {
            if (config.showLevel)
                add("§9[§d${skill.level}§9] ")

            if (config.useIcon)
                add(Renderable.itemStack(stackMap.getOrDefault(activeSkill.firstLetterUppercase(), defaultStack), 1.5))

            add(buildString {
                append("§b+${skill.lastGain} ")

                if (config.useSkillName)
                    append("${activeSkill.firstLetterUppercase()} ")

                val percent = if (skill.currentXpMax == 0L) 100F else 100F * skill.currentXp / skill.currentXpMax
                skillExpPercentage = (percent.toDouble() / 100)

                if (config.usePercentage)
                    append("§7(§6${percent.roundToPrecision(2)}%§7)")
                else {
                    if (skill.currentXpMax == 0L)
                        append("§7(§6${skill.currentXp.addSeparators()}§7)")
                    else
                        append("§7(§6${skill.currentXp.addSeparators()}§7/§6${skill.currentXpMax.addSeparators()}§7)")
                }

                if (config.showActionLeft && percent != 100f) {
                    append(" - ")
                    if (skill.lastGain != "") {
                        val actionLeft = (ceil(skill.currentXpMax.toDouble() - skill.currentXp) / skill.lastGain.formatNumber()).toLong().addSeparators()
                        append("§6$actionLeft Left")
                    } else {
                        append("∞ Left")
                    }
                }
            })
        })
    }

    private fun updateSkillInfo(skill: String) {
        if (skill.isEmpty()) return
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
        xpInfo.lastUpdate = SimpleTimeMark.now()
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

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
