package at.hannibal2.skyhanni.features.misc.skillprogress

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.SkillAPI
import at.hannibal2.skyhanni.api.SkillAPI.activeSkill
import at.hannibal2.skyhanni.api.SkillAPI.lastUpdate
import at.hannibal2.skyhanni.api.SkillAPI.showDisplay
import at.hannibal2.skyhanni.api.SkillAPI.skillMap
import at.hannibal2.skyhanni.api.SkillAPI.stackMap
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.events.SkillOverflowLevelupEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SpecialColour
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.init.Items
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

class SkillProgress {

    private val config get() = SkyHanniMod.feature.misc.skillProgressDisplayConfig
    private var skillExpPercentage = 0.0
    private var display = emptyList<List<Any>>()
    private var allDisplay = emptyList<Renderable>()
    private val defaultStack = Utils.createItemStack(Items.banner, "Default")

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!showDisplay) return
        config.position.renderStringsAndItems(display, posLabel = "Skill Progress")
        if (config.showProgressBar && display.isNotEmpty()) {
            val progress = if (config.useTexturedBar) {
                var factor = skillExpPercentage.toFloat()
                if (factor > 1f) factor = 1f
                factor *= 182f
                Renderable.texturedProgressBar(factor, Color(SpecialColour.specialToChromaRGB(config.barStartColor)), width = 182, height = 5, useChroma = config.useChroma)
            } else
                Renderable.progressBar(skillExpPercentage, Color(SpecialColour.specialToChromaRGB(config.barStartColor)), Color(SpecialColour.specialToChromaRGB(config.barStartColor)), width = 182)

            config.barPosition.renderRenderables(listOf(progress), posLabel = "Skill Progress Bar")
        }
        if (config.allSkillProgress) {
            config.allSkillPosition.renderRenderables(allDisplay, posLabel = "All Skills Display")
        }
    }

    @SubscribeEvent
    fun onProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
        skillExpPercentage = 0.0
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        if (lastUpdate.passedSince() > 3.seconds) showDisplay = config.alwaysShow

        if (event.repeatSeconds(1)) {
            update()
            updateAllDisplay()
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


    private fun update() {
        display = drawDisplay()
    }

    private fun updateAllDisplay() {
        allDisplay = drawAllDisplay()
    }

    private fun drawAllDisplay() = buildList {
        val skillMap = skillMap ?: return@buildList
        for ((skillName, skillInfo) in skillMap) {
            add(Renderable.string(buildString {
                append("§6${skillName.firstLetterUppercase()} ${skillInfo.level} ")
                append("§7(")
                append("§b${skillInfo.currentXp.addSeparators()}")
                if (skillInfo.currentXpMax != 0L) {
                    append("§6/")
                    append("§b${skillInfo.currentXpMax.addSeparators()}")
                    append("§7)")
                }
            }))
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
                append("§b+${SkillAPI.gained} ")

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
                    if (SkillAPI.gained != "") {
                        val actionLeft = (ceil(skill.currentXpMax.toDouble() - skill.currentXp) / SkillAPI.gained.formatNumber()).toLong().addSeparators()
                        append("§6$actionLeft Left")
                    } else {
                        append("∞ Left")
                    }
                }
            })
        })
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
