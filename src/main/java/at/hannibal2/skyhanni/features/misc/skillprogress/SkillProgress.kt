package at.hannibal2.skyhanni.features.misc.skillprogress

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.SkillAPI
import at.hannibal2.skyhanni.api.SkillAPI.lastUpdate
import at.hannibal2.skyhanni.api.SkillAPI.oldSkillInfoMap
import at.hannibal2.skyhanni.api.SkillAPI.showDisplay
import at.hannibal2.skyhanni.api.SkillAPI.skillMap
import at.hannibal2.skyhanni.api.SkillAPI.skillXPInfoMap
import at.hannibal2.skyhanni.config.features.misc.skillprogress.SkillProgressConfig
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.events.SkillOverflowLevelupEvent
import at.hannibal2.skyhanni.features.misc.skillprogress.SkillUtil.activeSkill
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.LorenzUtils.chat
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.interpolate
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.SpecialColour
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.horizontalContainer
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.util.ChatComponentText
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

object SkillProgress {

    private val config get() = SkyHanniMod.feature.misc.skillProgressConfig
    private var skillExpPercentage = 0.0
    private var display = emptyList<Renderable>()
    private var allDisplay = emptyList<List<Any>>()
    private var etaDisplay = emptyList<Renderable>()
    private var lastGainUpdate = SimpleTimeMark.farPast()
    private var maxWidth = 0
    var hideInActionBar = mutableListOf<String>()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (display.isEmpty()) return

        if (showDisplay) {
            when (val textAlignment = config.textAlignmentProperty.get()) {
                SkillProgressConfig.TextAlignment.NONE -> {
                    config.position.renderStringsAndItems(listOf(display), posLabel = "Skill Progress")
                }

                SkillProgressConfig.TextAlignment.CENTERED,
                SkillProgressConfig.TextAlignment.LEFT,
                SkillProgressConfig.TextAlignment.RIGHT -> {
                    config.position.renderRenderables(
                        listOf(Renderable.fixedSizeLine(horizontalContainer(display, textAlignment.alignment), maxWidth)),
                        posLabel = "Skill Progress")
                }

                else -> {}
            }

            if (config.progressBarConfig.enabled.get()) {
                val progress = if (config.progressBarConfig.useTexturedBar.get()) {
                    val factor = (skillExpPercentage.toFloat().coerceAtMost(1f)) * 182
                    maxWidth = 182
                    Renderable.texturedProgressBar(factor,
                        Color(SpecialColour.specialToChromaRGB(config.progressBarConfig.barStartColor)),
                        texture = config.progressBarConfig.texturedBar.usedTexture.get(),
                        useChroma = config.progressBarConfig.useChroma.get())

                } else {
                    maxWidth = config.progressBarConfig.regularBar.width
                    Renderable.progressBar(skillExpPercentage,
                        Color(SpecialColour.specialToChromaRGB(config.progressBarConfig.barStartColor)),
                        Color(SpecialColour.specialToChromaRGB(config.progressBarConfig.barStartColor)),
                        width = maxWidth,
                        height = config.progressBarConfig.regularBar.height,
                        useChroma = config.progressBarConfig.useChroma.get())
                }

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

        if (event.repeatSeconds(1)) {
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
        val skillName = event.skill.displayName
        val oldLevel = event.oldLevel
        val newLevel = event.newLevel

        val rewards = buildList {
            add("  §r§7§8+§b1 Flexing Point")
            if (newLevel % 5 == 0)
                add("  §r§7§8+§d50 SkyHanni User Luck")
        }

        chat("§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬", false)
        chat("  §r§b§lSKILL LEVEL UP §3$skillName §8$oldLevel➜§3$newLevel", false)
        chat("", false)
        chat("  §r§a§lREWARDS", false)
        for (reward in rewards)
            chat(reward, false)
        chat("§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬", false)
        SoundUtils.createSound("random.levelup", 1f, 1f).playSound()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        onToggle(
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


    @SubscribeEvent(priority = EventPriority.LOW)
    fun onActionBar(event: ClientChatReceivedEvent) {
        if (enabled) return //TODO:  remove before release
        if (!config.hideInActionBar) return
        if (event.type.toInt() != 2) return
        if (event.isCanceled) return
        val it = hideInActionBar.iterator()
        var msg = event.message.unformattedText
        while (it.hasNext()) {
            msg = msg.replace(Regex("\\s*" + Regex.escape(it.next())), "")
        }
        msg = msg.trim()
        event.message = ChatComponentText(msg)
    }

    /**
     * TODO: Remove before release
     */
    var start = 0L
    var enabled = false
    var add = 0L

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onActionBar2(event: ClientChatReceivedEvent) {
        if (!enabled) return
        if (event.type.toInt() != 2) return
        if (event.isCanceled) return
        event.message = ChatComponentText("+$add Farming ($start/0)")
        start += add
    }

    /**
     * TODO: Remove before release
     */
    fun setAction(args: Array<String>) {
        if (args.isEmpty()) return
        if (args.size == 1) {
            when (args[0]) {
                "toggle" -> {
                    enabled = !enabled
                    chat(if (enabled) "§aEnabled" else "§cDisabled")
                }
            }
        }
        if (args.size == 2) {
            when (args[0]) {
                "add" -> {
                    val toAdd = args[1].toLongOrNull() ?: error("Cannot parse ${args[1]} as Long")
                    add += toAdd
                    chat("To add: ${toAdd.addSeparators()}")
                }
            }
        }
    }

    fun updateDisplay() {
        display = drawDisplay()
    }

    private fun update() {
        lastGainUpdate = SimpleTimeMark.now()
        skillXPInfoMap.forEach {
            it.value.xpGainLast = it.value.xpGainHour
        }
    }

    private fun formatAllDisplay(map: List<List<Any>>): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        if (map.isEmpty()) return newList
        for (index in config.allskillEntryList) {
            newList.add(map[index.ordinal])
        }
        return newList
    }

    private fun drawAllDisplay() = buildList {
        val skillMap = skillMap ?: return@buildList
        val sortedMap = SkillType.entries.filter { it.displayName.isNotEmpty() }.sortedBy { it.displayName.take(2) }

        for (skill in sortedMap) {
            val skillInfo = skillMap[skill] ?: SkillAPI.SkillInfo(level = -1, overflowLevel = -1)
            val (level, currentXp, currentXpMax, totalXp) =
                if (config.overflowConfig.enableInAllDisplay.get())
                    LorenzUtils.Quad(skillInfo.overflowLevel, skillInfo.overflowCurrentXp, skillInfo.overflowCurrentXpMax, skillInfo.overflowTotalXp)
                else
                    LorenzUtils.Quad(skillInfo.level, skillInfo.currentXp, skillInfo.currentXpMax, skillInfo.totalXp)

            if (level == -1) {
                addAsSingletonList(Renderable.clickAndHover(
                    "§cOpen your skills menu !",
                    listOf("§eClick here to execute §6/skills"),
                    onClick = { LorenzUtils.sendCommandToServer("skills") }
                ))
            } else {
                val tips = buildList {
                    add("§6Level: §b${level}")
                    add("§6Current XP: §b${currentXp.addSeparators()}")
                    add("§6Needed XP: §b${currentXpMax.addSeparators()}")
                    add("§6Total XP: §b${totalXp.addSeparators()}")
                }
                val nameColor = if (skill == activeSkill) "§e" else "§6"
                addAsSingletonList(Renderable.hoverTips(buildString {
                    append("$nameColor${skill.displayName} $level ")
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
    }

    private fun drawETADisplay() = buildList {
        val skillInfo = skillMap?.get(activeSkill) ?: return@buildList
        val xpInfo = skillXPInfoMap[activeSkill] ?: return@buildList
        val skillInfoLast = oldSkillInfoMap[activeSkill] ?: return@buildList
        oldSkillInfoMap[activeSkill] = skillInfo
        val level = if (config.overflowConfig.enableInEtaDisplay.get()) skillInfo.overflowLevel else skillInfo.level

        add(Renderable.string("§6Skill: §b${activeSkill.displayName}"))
        add(Renderable.string("§6Level: §b$level"))

        var xpInterp = xpInfo.xpGainHour
        if (xpInfo.xpGainLast == xpInfo.xpGainHour && xpInfo.xpGainHour <= 0) {
            add(Renderable.string("§6XP/h: §cN/A"))
        } else {
            xpInterp = interpolate(xpInfo.xpGainHour, xpInfo.xpGainLast, lastGainUpdate.toMillis())
            add(Renderable.string("§6XP/h: §b${xpInterp.addSeparators()}"))
        }

        var remaining = skillInfo.overflowCurrentXpMax - skillInfo.overflowCurrentXp
        if (skillInfo.overflowCurrentXpMax == skillInfoLast.overflowCurrentXpMax) {
            remaining = interpolate(remaining.toFloat(), (skillInfoLast.overflowCurrentXpMax - skillInfoLast.overflowCurrentXp).toFloat(), lastGainUpdate.toMillis()).toLong()
        }

        if (xpInfo.xpGainHour < 1000) {
            add(Renderable.string("§6ETA: §cN/A"))
        } else {
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


        if (config.showLevel.get())
            add(Renderable.string("§9[§d$level§9] "))

        if (config.useIcon.get()) {
            add(Renderable.string(" "))
            add(Renderable.itemStack(activeSkill.item, 1.5))
        }

        add(Renderable.string(buildString {
            append("§b+${skill.lastGain} ")

            if (config.useSkillName.get())
                append("${activeSkill.displayName} ")

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
        }))
    }

    private fun updateSkillInfo(skill: SkillType) {
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
