package at.hannibal2.skyhanni.features.skillprogress

import at.hannibal2.skyhanni.api.SkillAPI
import at.hannibal2.skyhanni.api.SkillAPI.activeSkill
import at.hannibal2.skyhanni.api.SkillAPI.allSkillConfig
import at.hannibal2.skyhanni.api.SkillAPI.barConfig
import at.hannibal2.skyhanni.api.SkillAPI.config
import at.hannibal2.skyhanni.api.SkillAPI.customGoalConfig
import at.hannibal2.skyhanni.api.SkillAPI.etaConfig
import at.hannibal2.skyhanni.api.SkillAPI.lastUpdate
import at.hannibal2.skyhanni.api.SkillAPI.oldSkillInfoMap
import at.hannibal2.skyhanni.api.SkillAPI.overflowConfig
import at.hannibal2.skyhanni.api.SkillAPI.showDisplay
import at.hannibal2.skyhanni.api.SkillAPI.skillColorConfig
import at.hannibal2.skyhanni.api.SkillAPI.skillXPInfoMap
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.skillprogress.SkillETADisplayConfig.TextEntry
import at.hannibal2.skyhanni.config.features.skillprogress.SkillProgressConfig
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.SkillOverflowLevelupEvent
import at.hannibal2.skyhanni.features.chroma.ChromaShaderManager
import at.hannibal2.skyhanni.features.chroma.ChromaType
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.XP_NEEDED_FOR_60
import at.hannibal2.skyhanni.features.skillprogress.SkillUtil.getColorForLevel
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils.chat
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.interpolate
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.Quad
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.SpecialColour
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.horizontalContainer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SkillProgress {

    private var skillExpPercentage = 0.0
    private var display = emptyList<Renderable>()
    private var allDisplay = emptyList<Renderable>()
    private var etaDisplay = emptyList<Renderable>()
    private var lastGainUpdate = SimpleTimeMark.farPast()
    private var maxWidth = 182
    private var inventoryOpen = false
    var hideInActionBar = listOf<String>()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (display.isEmpty()) return

        if (showDisplay) {
            val useChroma = barConfig.useChroma.get() && config.skillColorConfig.matchBarColor.get()
            if (useChroma)
                ChromaShaderManager.begin(ChromaType.TEXTURED)
            renderDisplay()
            if (useChroma)
                ChromaShaderManager.end()

            if (barConfig.enabled.get()) {
                renderBar()
            }
        }

        if (etaConfig.enabled.get()) {
            config.etaPosition.renderRenderables(etaDisplay, posLabel = "Skill ETA")
        }
    }

    @SubscribeEvent
    fun onGuiRender(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (display.isEmpty()) return

        if (allSkillConfig.enabled.get()) {
            config.allSkillPosition.renderRenderables(allDisplay, posLabel = "All Skills Display")
        }
    }

    private fun renderDisplay() {
        when (val textAlignment = config.textAlignmentProperty.get()) {
            SkillProgressConfig.TextAlignment.NONE -> {
                config.displayPosition.renderStringsAndItems(listOf(display), posLabel = "Skill Progress")
            }

            SkillProgressConfig.TextAlignment.CENTERED,
            SkillProgressConfig.TextAlignment.LEFT,
            SkillProgressConfig.TextAlignment.RIGHT,
            -> {
                val content = horizontalContainer(display, horizontalAlign = textAlignment.alignment)
                val renderables = listOf(Renderable.fixedSizeLine(content, maxWidth))
                config.displayPosition.renderRenderables(renderables, posLabel = "Skill Progress")
            }

            else -> {}
        }
    }

    private fun renderBar() {
        val skill = activeSkill ?: return
        val color = if (barConfig.colorPerSkill) SkillType.getBarColor(skill) else barConfig.barStartColor

        val progress = if (barConfig.useTexturedBar.get()) {
            val factor = (skillExpPercentage.toFloat().coerceAtMost(1f)) * 182
            maxWidth = 182
            Renderable.progressBar(
                percent = factor.toDouble(),
                startColor = Color(SpecialColour.specialToChromaRGB(color)),
                endColor = Color(SpecialColour.specialToChromaRGB(color)),
                texture = barConfig.texturedBar.usedTexture.get(),
                useChroma = barConfig.useChroma.get(),
            )

        } else {
            maxWidth = barConfig.regularBar.width
            val factor = skillExpPercentage.coerceAtMost(1.0)
            Renderable.progressBar(
                percent = factor,
                startColor = Color(SpecialColour.specialToChromaRGB(color)),
                endColor = Color(SpecialColour.specialToChromaRGB(color)),
                width = maxWidth,
                height = barConfig.regularBar.height,
                useChroma = barConfig.useChroma.get(),
            )
        }

        config.barPosition.renderRenderables(listOf(progress), posLabel = "Skill Progress Bar")
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        display = emptyList()
        allDisplay = emptyList()
        etaDisplay = emptyList()
        skillExpPercentage = 0.0
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        if (lastUpdate.passedSince() > 3.seconds) showDisplay = config.alwaysShow.get()
        inventoryOpen = Minecraft.getMinecraft().currentScreen is GuiInventory
        allDisplay = formatAllDisplay(drawAllDisplay())
        etaDisplay = buildFinalDisplay(formatETADisplay(drawETADisplay()))

        update()
        updateSkillInfo()
    }

    private fun buildFinalDisplay(rawList: List<Renderable>): List<Renderable> = rawList.toMutableList().also {
        if (it.isEmpty()) return@also
        if (inventoryOpen) {
            it.add(buildSessionResetButton())
        }
    }

    private fun buildSessionResetButton() = Renderable.clickAndHover(
        "§cReset session!",
        listOf(
            "§cThis will reset your",
            "§ccurrent session time.",
        ),
        onClick = {
            val xpInfo = skillXPInfoMap[activeSkill] ?: return@clickAndHover
            xpInfo.sessionTimerActive = false
            xpInfo.timeActive = 0L
            chat("Timer for §b${activeSkill?.displayName} §ehas been reset!")
        },
    )

    @HandleEvent
    fun onLevelUp(event: SkillOverflowLevelupEvent) {
        if (!isEnabled()) return
        if (!config.overflowConfig.enableInChat) return
        val skillName = event.skill.displayName
        val oldLevel = event.oldLevel
        val newLevel = event.newLevel
        val skill = SkillAPI.storage?.get(event.skill) ?: return
        val goalReached = newLevel == skill.customGoalLevel && customGoalConfig.enableInChat

        val rewards = buildList {
            add("  §r§7§8+§b1 Flexing Point")
            if (newLevel % 5 == 0)
                add("  §r§7§8+§d50 SkyHanni User Luck")
        }
        val messages = listOf(
            "§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
            "  §r§b§lSKILL LEVEL UP §3$skillName §8$oldLevel➜§3$newLevel",
            if (goalReached)
                listOf(
                    "",
                    "  §r§d§lGOAL REACHED!",
                    "",
                ).joinToString("\n") else
                "",
            "  §r§a§lREWARDS",
            rewards.joinToString("\n"),
            "§3§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
        )

        chat(messages.joinToString("\n"), false)

        if (goalReached)
            chat("§lYou have reached your goal level of §b§l${skill.customGoalLevel} §e§lin the §b§l$skillName §e§lskill!")

        SoundUtils.createSound("random.levelup", 1f, 1f).playSound()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        onToggle(
            config.enabled,
            config.alwaysShow,
            config.showActionLeft,
            config.useIcon,
            config.usePercentage,
            config.useSkillName,
            overflowConfig.enableInDisplay,
            overflowConfig.enableInProgressBar,
            overflowConfig.enableInEtaDisplay,
            skillColorConfig.matchBarColor,
            skillColorConfig.scalingColorLevel,
            barConfig.enabled,
            barConfig.useChroma,
            barConfig.useTexturedBar,
            barConfig.combatBarColor,
            barConfig.miningBarColor,
            barConfig.alchemyBarColor,
            barConfig.carpentryBarColor,
            barConfig.tamingBarColor,
            barConfig.foragingBarColor,
            barConfig.farmingBarColor,
            barConfig.enchantingBarColor,
            barConfig.fishingBarColor,
            allSkillConfig.enabled,
            etaConfig.enabled,
        ) {
            updateDisplay()
            update()
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onActionBar(event: ActionBarUpdateEvent) {
        if (!config.hideInActionBar || !isEnabled()) return
        if (event.isCanceled) return
        var msg = event.actionBar
        for (line in hideInActionBar) {
            msg = msg.replace(Regex("\\s*" + Regex.escape(line)), "")
        }
        msg = msg.trim()

        event.changeActionBar(msg)
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

    private fun formatAllDisplay(map: Map<SkillType, Renderable>): List<Renderable> {
        val newList = mutableListOf<Renderable>()
        if (map.isEmpty()) return newList
        for (skillType in allSkillConfig.skillEntryList) {
            map[skillType]?.let {
                newList.add(it)
            }
        }
        return newList
    }

    private fun formatETADisplay(map: Map<TextEntry, Renderable>): List<Renderable> {
        val newList = mutableListOf<Renderable>()
        if (map.isEmpty()) return newList
        for (text in etaConfig.textEntry) {
            map[text]?.let {
                newList.add(it)
            }
        }
        return newList
    }

    private fun drawAllDisplay() = buildMap {
        val skillMap = SkillAPI.storage ?: return@buildMap
        val sortedMap = SkillType.entries.filter { it.displayName.isNotEmpty() }.sortedBy { it.displayName.take(2) }

        for (skill in sortedMap) {
            val skillInfo = skillMap[skill] ?: SkillAPI.SkillInfo(level = -1, overflowLevel = -1)
            val lockedLevels = skillInfo.overflowCurrentXp > skillInfo.overflowCurrentXpMax
            val useCustomGoalLevel =
                skillInfo.customGoalLevel != 0 && skillInfo.customGoalLevel > skillInfo.overflowLevel && customGoalConfig.enableInAllDisplay
            val targetLevel = skillInfo.customGoalLevel
            var xp = skillInfo.overflowTotalXp
            if (targetLevel in 50..60 && skillInfo.overflowLevel >= 50) xp += SkillUtil.xpRequiredForLevel(50.0)
            else if (targetLevel > 60 && skillInfo.overflowLevel >= 60) xp += SkillUtil.xpRequiredForLevel(60.0)

            var have = skillInfo.overflowTotalXp
            val need = SkillUtil.xpRequiredForLevel(targetLevel.toDouble())
            if (targetLevel in 51..59) have += SkillUtil.xpRequiredForLevel(50.0)
            else if (targetLevel > 60) have += SkillUtil.xpRequiredForLevel(60.0)

            val (level, currentXp, currentXpMax, totalXp) =
                if (useCustomGoalLevel)
                    Quad(skillInfo.overflowLevel, have, need, xp)
                else if (config.overflowConfig.enableInAllDisplay.get() && !lockedLevels)
                    Quad(
                        skillInfo.overflowLevel,
                        skillInfo.overflowCurrentXp,
                        skillInfo.overflowCurrentXpMax,
                        skillInfo.overflowTotalXp,
                    )
                else
                    Quad(skillInfo.level, skillInfo.currentXp, skillInfo.currentXpMax, skillInfo.totalXp)

            this[skill] = if (level == -1) {
                Renderable.clickAndHover(
                    "§cOpen your skills menu!",
                    listOf("§eClick here to execute §6/skills"),
                    onClick = { HypixelCommands.skills() },
                )
            } else {
                val tips = buildList {
                    add("§6Level: §b${level}")
                    add("§6Current XP: §b${currentXp.addSeparators()}")
                    add("§6Needed XP: §b${currentXpMax.addSeparators()}")
                    add("§6Total XP: §b${totalXp.addSeparators()}")
                }
                val nameColor = if (skill == activeSkill) "§2" else "§a"
                Renderable.hoverTips(
                    buildString {
                        append("$nameColor${skill.displayName} $level ")
                        append("§7(")
                        append("§b${currentXp.addSeparators()}")
                        if (currentXpMax != 0L) {
                            append("§6/")
                            append("§b${currentXpMax.addSeparators()}")
                        }
                        append("§7)")
                    },
                    tips,
                )
            }
        }
    }

    private fun drawETADisplay() = buildMap {
        val activeSkill = activeSkill ?: return@buildMap
        val skillInfo = SkillAPI.storage?.get(activeSkill) ?: return@buildMap
        val xpInfo = skillXPInfoMap[activeSkill] ?: return@buildMap
        val skillInfoLast = oldSkillInfoMap[activeSkill] ?: return@buildMap
        oldSkillInfoMap[activeSkill] = skillInfo
        val level =
            if (config.overflowConfig.enableInEtaDisplay.get() ||
                config.customGoalConfig.enableInETADisplay
            ) skillInfo.overflowLevel else skillInfo.level

        val useCustomGoalLevel =
            skillInfo.customGoalLevel != 0 && skillInfo.customGoalLevel > skillInfo.overflowLevel && customGoalConfig.enableInETADisplay
        var targetLevel = if (useCustomGoalLevel) skillInfo.customGoalLevel else level + 1
        if (targetLevel <= level || targetLevel > 400) targetLevel = (level + 1)

        val need = skillInfo.overflowCurrentXpMax
        val have = skillInfo.overflowCurrentXp

        val currentLevelNeededXp = SkillUtil.xpRequiredForLevel(level.toDouble()) + have
        val targetNeededXp = SkillUtil.xpRequiredForLevel(targetLevel.toDouble())

        var remaining = if (useCustomGoalLevel) targetNeededXp - currentLevelNeededXp else need - have

        if (!useCustomGoalLevel && have < need) {
            if (skillInfo.overflowCurrentXpMax == skillInfoLast.overflowCurrentXpMax) {
                remaining =
                    interpolate(remaining.toFloat(), (need - have).toFloat(), lastGainUpdate.toMillis()).toLong()
            }
        }

        this[TextEntry.SKILL] = Renderable.string("§6Skill: §a${activeSkill.displayName} §8$level➜§3$targetLevel")
        this[TextEntry.XP_NEEDED] = Renderable.string("§7Needed XP: §e${remaining.addSeparators()}")

        var xpInterp = xpInfo.xpGainHour

        if (have > need) {
            this[TextEntry.TIME] = Renderable.string("§7In §cIncrease level cap!")
        } else if (xpInfo.xpGainHour < 1000) {
            this[TextEntry.TIME] = Renderable.string("§7In §cN/A")
        } else {
            val duration = ((remaining) * 1000 * 60 * 60 / xpInterp.toLong()).milliseconds
            val format = duration.format(TimeUnit.DAY)
            this[TextEntry.TIME] = Renderable.string(
                "§7In §b$format " +
                    if (xpInfo.isActive) "" else "§c(PAUSED)",
            )
        }

        if (xpInfo.xpGainLast == xpInfo.xpGainHour && xpInfo.xpGainHour <= 0) {
            this[TextEntry.XP_HOUR] = Renderable.string("§7XP/h: §cN/A")
        } else {
            xpInterp = interpolate(xpInfo.xpGainHour, xpInfo.xpGainLast, lastGainUpdate.toMillis())
            this[TextEntry.XP_HOUR] = Renderable.string(
                "§7XP/h: §e${xpInterp.toLong().addSeparators()} " +
                    if (xpInfo.isActive) "" else "§c(PAUSED)",
            )
        }

        val session = xpInfo.timeActive.seconds.format(TimeUnit.HOUR)
        this[TextEntry.SESSION] = Renderable.string("§7Session: §e$session ${if (xpInfo.sessionTimerActive) "" else "§c(PAUSED)"}")
    }

    private fun drawDisplay() = buildList {
        val activeSkill = activeSkill ?: return@buildList
        val skillMap = SkillAPI.storage ?: return@buildList
        val skill = skillMap[activeSkill] ?: return@buildList
        val useCustomGoalLevel = skill.customGoalLevel != 0 && skill.customGoalLevel > skill.overflowLevel
        val targetLevel = skill.customGoalLevel
        val xp = skill.totalXp
        val currentLevel = if (xp <= XP_NEEDED_FOR_60) {
            SkillUtil.getLevel(xp)
        } else {
            SkillUtil.calculateOverFlow(xp).first
        }
        var have = skill.overflowTotalXp
        val need = SkillUtil.xpRequiredForLevel(targetLevel.toDouble())
        if (targetLevel in 51..59) have += SkillUtil.xpRequiredForLevel(50.0)
        else if (targetLevel > 60) have += SkillUtil.xpRequiredForLevel(60.0)

        val (level, currentXp, currentXpMax, _) =
            if (useCustomGoalLevel && customGoalConfig.enableInDisplay)
                Quad(currentLevel, have, need, xp)
            else if (config.overflowConfig.enableInDisplay.get())
                Quad(skill.overflowLevel, skill.overflowCurrentXp, skill.overflowCurrentXpMax, skill.overflowTotalXp)
            else
                Quad(skill.level, skill.currentXp, skill.currentXpMax, skill.totalXp)

        val matchColor = config.skillColorConfig.matchBarColor.get()
        val color = Color(SpecialColour.specialToChromaRGB(SkillType.getBarColor(activeSkill)))

        if (config.showLevel.get()) {
            val colorLevel = if (config.skillColorConfig.scalingColorLevel.get()) getColorForLevel(level) else "§d"
            val levelString = if (matchColor) "[$level] " else "§9[$colorLevel$level§9] "
            add(Renderable.string(levelString, color = color))
        }

        if (config.useIcon.get()) {
            val item = skill.item ?: activeSkill.item
            add(Renderable.itemStack(item, 1.2))
        }

        add(
            Renderable.string(
                buildString {
                    val gainString = if (matchColor) "+${skill.lastGain} " else "§b+${skill.lastGain} "
                    append(gainString)

                    if (config.useSkillName.get())
                        append("${activeSkill.displayName} ")

                    val (barCurrent, barMax) =
                        if (useCustomGoalLevel && customGoalConfig.enableInProgressBar)
                            Pair(have, need)
                        else if (config.overflowConfig.enableInProgressBar.get())
                            Pair(skill.overflowCurrentXp, skill.overflowCurrentXpMax)
                        else
                            Pair(skill.currentXp, skill.currentXpMax)

                    val barPercent = if (barMax == 0L) 100F else 100F * barCurrent / barMax
                    skillExpPercentage = (barPercent.toDouble() / 100)

                    val percent = if (currentXpMax == 0L) 100F else 100F * currentXp / currentXpMax

                    val percentColor = if (config.skillColorConfig.enabledDisplayColor) {
                        "§${SkillUtil.getColorForPercentage(barPercent.toInt())}"
                    } else "§6"

                    if (config.usePercentage.get()) {
                        val percentString = if (matchColor) "(${percent.roundToPrecision(2)}%)" else {
                            "§7($percentColor${percent.roundToPrecision(2)}%§7)"
                        }
                        append(percentString)
                    } else {
                        if (currentXpMax == 0L) {
                            val progressString = if (matchColor) "(${currentXp.addSeparators()})" else {
                                "§7($percentColor${currentXp.addSeparators()}§7)"
                            }
                            append(progressString)
                        } else {
                            val progressString = if (matchColor) "(${currentXp.addSeparators()}/${currentXpMax.addSeparators()})" else {
                                "§7($percentColor${currentXp.addSeparators()}§7/$percentColor${currentXpMax.addSeparators()}§7)"
                            }
                            append(progressString)
                        }
                    }

                    if (config.showActionLeft.get() && percent != 100f) {
                        append(" - ")
                        val gain = skill.lastGain.formatDouble()
                        val actionLeft = (ceil(currentXpMax.toDouble() - currentXp) / gain).toLong().addSeparators()
                        if (skill.lastGain != "" && !actionLeft.contains("-")) {
                            val actionLeftString = if (matchColor) "$actionLeft Left" else "$percentColor$actionLeft Left"
                            append(actionLeftString)
                        } else {
                            append("$percentColor∞ Left")
                        }
                    }
                },
                color = color,
            ),
        )
    }

    private fun updateSkillInfo() {
        val activeSkill = activeSkill ?: return
        val xpInfo = skillXPInfoMap.getOrPut(activeSkill) { SkillAPI.SkillXPInfo() }
        val skillInfo = SkillAPI.storage?.get(activeSkill) ?: return
        oldSkillInfoMap[activeSkill] = skillInfo

        val totalXp = skillInfo.currentXp

        if (xpInfo.lastTotalXp > 0) {
            val delta = totalXp - xpInfo.lastTotalXp
            if (delta > 0 && delta < 1000) {

                xpInfo.timer = when (SkillAPI.activeSkill) {
                    SkillType.FARMING -> etaConfig.farmingPauseTime
                    SkillType.MINING -> etaConfig.miningPauseTime
                    SkillType.COMBAT -> etaConfig.combatPauseTime
                    SkillType.FORAGING -> etaConfig.foragingPauseTime
                    SkillType.FISHING -> etaConfig.fishingPauseTime
                    else -> 3
                }

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
