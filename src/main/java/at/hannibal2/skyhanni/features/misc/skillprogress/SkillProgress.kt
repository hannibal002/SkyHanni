package at.hannibal2.skyhanni.features.misc.skillprogress

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.SkillAPI
import at.hannibal2.skyhanni.api.SkillAPI.activeSkill
import at.hannibal2.skyhanni.api.SkillAPI.lastUpdate
import at.hannibal2.skyhanni.api.SkillAPI.showDisplay
import at.hannibal2.skyhanni.api.SkillAPI.skillMap
import at.hannibal2.skyhanni.api.SkillAPI.stackMap
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.onToggle
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
    private val defaultStack = Utils.createItemStack(Items.banner, "Default")

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!showDisplay) return
        config.position.renderStringsAndItems(display, itemScale = 1.5, posLabel = "Skill Progress")
        if (config.showProgressBar.get() && display.isNotEmpty()) {
            val progress = if (config.useTexturedBar.get()) {
                var factor = skillExpPercentage.toFloat()
                if (factor > 1f) factor = 1f
                factor *= 182f
                Renderable.texturedProgressBar(factor, Color(SpecialColour.specialToChromaRGB(config.barStartColor)), width = 182, height = 5, useChroma = config.useChroma.get())
            } else
                Renderable.progressBar(skillExpPercentage, Color(SpecialColour.specialToChromaRGB(config.barStartColor)), Color(SpecialColour.specialToChromaRGB(config.barStartColor)), width = 182)

            config.barPosition.renderRenderables(listOf(progress), posLabel = "Skill Progress Bar")
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

        if (lastUpdate.passedSince() > 3.seconds) showDisplay = config.alwaysShow.get()

        if (event.repeatSeconds(1)) {
            update()
        }
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        val skillMap = skillMap ?: return newDisplay
        val skill = skillMap[activeSkill] ?: return newDisplay

        newDisplay.add(buildList {
            if (config.showLevel.get())
                add("§9[§d${skill.level}§9] ")

            if (config.useIcon.get())
                add(stackMap.getOrDefault(activeSkill.firstLetterUppercase(), defaultStack))

            add(buildString {
                append("§b+${SkillAPI.gained} ")

                if (config.useSkillName.get())
                    append("${activeSkill.firstLetterUppercase()} ")

                val percent = if (skill.currentXpMax == 0L) 100F else 100F * skill.currentXp / skill.currentXpMax
                skillExpPercentage = (percent.toDouble() / 100)

                if (config.usePercentage.get())
                    append("§7(§6${percent.roundToPrecision(2)}%§7)")
                else {
                    if (skill.currentXpMax == 0L)
                        append("§7(§6${skill.currentXp.addSeparators()}§7)")
                    else
                        append("§7(§6${skill.currentXp.addSeparators()}§7/§6${skill.currentXpMax.addSeparators()}§7)")
                }

                if (config.showActionLeft.get() && percent != 100f) {
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
        return newDisplay
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.enabled.onToggle { update() }
        config.alwaysShow.onToggle { update() }
        config.showProgressBar.onToggle { update() }
        config.showOverflow.onToggle { update() }
        config.showLevel.onToggle { update() }
        config.showActionLeft.onToggle { update() }
        config.useIcon.onToggle { update() }
        config.useSkillName.onToggle { update() }
        config.useTexturedBar.onToggle { update() }
        config.useChroma.onToggle { update() }
        config.usePercentage.onToggle { update() }
        update()
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled.get()
}
