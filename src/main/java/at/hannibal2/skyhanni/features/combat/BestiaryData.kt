package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.combat.BestiaryConfig.DisplayTypeEntry
import at.hannibal2.skyhanni.config.features.combat.BestiaryConfig.NumberFormatEntry
import at.hannibal2.skyhanni.data.BestiaryApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.NumberUtil.toRoman
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting

@SkyHanniModule
object BestiaryData {

    private val config get() = SkyHanniMod.feature.combat.bestiary

    private var display = emptyList<Renderable>()

    private val patternGroup = RepoPattern.group("combat.bestiary")

    /**
     * REGEX-TEST: Overall Progress: 100% (MAX!)
     * REGEX-TEST: Families Completed: 100%
     */
    private val completedPattern by patternGroup.list(
        "completed",
        "Overall Progress: 100% \\(MAX!\\)",
        "Families Completed: 100%",
    )

    /**
     * REGEX-TEST: Overall Progress: HIDDEN
     */
    private val hiddenProgressPattern by patternGroup.pattern(
        "hiddenprogress",
        "Overall Progress: HIDDEN",
    )

    @HandleEvent
    private fun onChestGuiRender() {
        if (!isEnabled()) return
        config.position.renderRenderables(
            display, extraSpace = -1, posLabel = "Bestiary Data",
        )
    }

    @HandleEvent
    private fun onBackgroundDrawn() {
        if (!isEnabled()) return
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            val lore = slot.item.getCleanLore()
            if (completedPattern.anyMatches(lore)) {
                slot.highlight(LorenzColor.GREEN)
            }
            if (!BestiaryApi.overallProgressEnabled && hiddenProgressPattern.anyMatches(lore)) {
                slot.highlight(LorenzColor.RED)
            }
        }
    }

    @HandleEvent
    private fun onInventoryFullyOpened() {
        if (!isEnabled()) return
        update()
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.bestiaryData", "combat.bestiary")
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        if (!BestiaryApi.overallProgressEnabled) {
            addString("§7Bestiary Data")
            addString(" §cPlease enable Overall Progress")
            addString(" §cUsing the Eye of Ender highlighted in red.")
            return@buildList
        }

        addCategories()

        if (BestiaryApi.mobList.isEmpty()) return@buildList

        addList()
        addButtons()
    }

    private fun sortMobList(): MutableList<BestiaryApi.BestiaryMob> {
        val sortedMobList = when (config.displayType) {
            DisplayTypeEntry.GLOBAL_MAX -> BestiaryApi.mobList.sortedBy { it.percentToMax() }
            DisplayTypeEntry.GLOBAL_NEXT -> BestiaryApi.mobList.sortedBy { it.percentToTier() }
            DisplayTypeEntry.LOWEST_TOTAL -> BestiaryApi.mobList.sortedBy { it.actualRealTotalKill }
            DisplayTypeEntry.HIGHEST_TOTAL -> BestiaryApi.mobList.sortedByDescending { it.actualRealTotalKill }
            DisplayTypeEntry.LOWEST_MAX -> BestiaryApi.mobList.sortedBy { it.killNeededToMax() }
            DisplayTypeEntry.HIGHEST_MAX -> BestiaryApi.mobList.sortedByDescending { it.killNeededToMax() }
            DisplayTypeEntry.LOWEST_NEXT -> BestiaryApi.mobList.sortedBy { it.killNeededToNextLevel() }
            DisplayTypeEntry.HIGHEST_NEXT -> BestiaryApi.mobList.sortedByDescending { it.killNeededToNextLevel() }
        }.toMutableList()
        return sortedMobList
    }

    private fun MutableList<Renderable>.addList() {
        val sortedMobList = sortMobList()

        addString("§7Bestiary Data")
        for (mob in sortedMobList) {
            val isUnlocked = mob.actualRealTotalKill != 0.toLong()
            val isMaxed = mob.percentToMax() >= 1
            if (!isUnlocked) {
                val component = componentBuilder {
                    appendWithColor(" - ", ChatFormatting.GRAY)
                    append(mob.name)
                    appendWithColor(": Not unlocked!", ChatFormatting.RED)
                }
                add(Renderable.text(component))
                continue
            }
            if (isMaxed && config.hideMaxed) continue
            val text = getMobLine(mob, isMaxed)
            val tips = getMobHover(mob)
            add(Renderable.hoverTips(text, tips))
        }
    }

    private fun getMobHover(mob: BestiaryApi.BestiaryMob) = listOf(
        "§6Name: §b${mob.name.formattedTextCompatLeadingWhiteLessResets()}",
        "§6Level: §b${mob.romanLevel} ${if (!config.replaceRoman) "§7(${mob.level})" else ""}",
        "§6Total Kills: §b${mob.actualRealTotalKill.formatNumber()}",
        "§6Kills needed to max: §b${mob.killNeededToMax().formatNumber()}",
        "§6Kills needed to next lvl: §b${mob.killNeededToNextLevel().formatNumber()}",
        "§6Current kill to next level: §b${mob.currentKillToNextLevel.formatNumber()}",
        "§6Kill needed for next level: §b${mob.killNeededForNextLevel.formatNumber()}",
        "§6Current kill to max: §b${mob.killToMax.formatNumber()}",
        "§6Percent to max: §b${mob.percentToMaxFormatted()}",
        "§6Percent to tier: §b${mob.percentToTierFormatted()}",
        "",
        "§7More info thing",
    )

    private fun getMobLine(mob: BestiaryApi.BestiaryMob, isMaxed: Boolean): String {
        val type = config.displayType
        var text = ""
        text += " §7- "
        text += "${mob.name.formattedTextCompatLeadingWhiteLessResets()} ${mob.level.romanOrInt()}: "
        text += if (isMaxed) {
            "§c§lMAXED! §7(§b${mob.actualRealTotalKill.formatNumber()}§7 kills)"
        } else {
            when (type) {
                DisplayTypeEntry.GLOBAL_MAX, DisplayTypeEntry.GLOBAL_NEXT -> {
                    val currentKill = when (type) {
                        DisplayTypeEntry.GLOBAL_MAX -> mob.totalKills
                        DisplayTypeEntry.GLOBAL_NEXT -> mob.currentKillToNextLevel
                    }
                    val killNeeded = when (type) {
                        DisplayTypeEntry.GLOBAL_MAX -> mob.killToMax
                        DisplayTypeEntry.GLOBAL_NEXT -> mob.killNeededForNextLevel
                    }
                    val percentage = ((currentKill.toDouble() / killNeeded) * 100).roundTo(2)
                    val suffix = if (type == DisplayTypeEntry.GLOBAL_NEXT) "§ato level ${mob.getNextLevel()}" else ""
                    "§7(§b${currentKill.formatNumber()}§7/§b${killNeeded.formatNumber()}§7) §a$percentage§6% $suffix"
                }

                DisplayTypeEntry.LOWEST_TOTAL, DisplayTypeEntry.HIGHEST_TOTAL -> {
                    "§6${mob.actualRealTotalKill.formatNumber()} §7total kills"
                }

                DisplayTypeEntry.LOWEST_MAX, DisplayTypeEntry.HIGHEST_MAX -> {
                    "§6${mob.killNeededToMax().formatNumber()} §7kills needed"
                }

                DisplayTypeEntry.LOWEST_NEXT, DisplayTypeEntry.HIGHEST_NEXT -> {
                    "§6${mob.killNeededToNextLevel().formatNumber()} §7kills needed"
                }
            }
        }
        return text
    }

    private fun MutableList<Renderable>.addButtons() {
        addRenderableButton<NumberFormatEntry>(
            label = "Number Format",
            current = config.numberFormat,
            onChange = {
                config.numberFormat = it
                update()
            },
        )

        addRenderableButton<DisplayTypeEntry>(
            label = "Display Type",
            current = config.displayType,
            onChange = {
                config.displayType = it
                update()
            },
        )

        addRenderableButton(
            label = "Number Type",
            config = config::replaceRoman,
            enabled = "Normal (1, 2, 3)",
            disabled = "Roman (I, II, III)",
            onChange = {
                update()
            },
        )

        addRenderableButton(
            label = "Hide Maxed",
            config = config::hideMaxed,
            enabled = "Hide",
            disabled = "Show",
            onChange = {
                update()
            },
        )
    }

    private fun MutableList<Renderable>.addCategories() {
        if (BestiaryApi.catList.isEmpty()) return
        addString("§7Category")
        for ((name, _, familiesFound, totalFamilies, familiesCompleted) in BestiaryApi.catList) {
            val info = when {
                familiesCompleted == totalFamilies -> "§c§lCompleted!"
                familiesFound == totalFamilies -> "§b$familiesCompleted§7/§b$totalFamilies §7completed"
                familiesFound < totalFamilies ->
                    "§b$familiesFound§7/§b$totalFamilies §7found, " +
                        "§b$familiesCompleted§7/§b$totalFamilies §7completed"

                else -> continue
            }
            val component = componentBuilder {
                appendWithColor(" - ", ChatFormatting.GRAY)
                append(name)
                appendWithColor(": $info", ChatFormatting.GRAY)
            }
            add(Renderable.text(component))
        }
    }

    private fun Long.formatNumber(): String = when (config.numberFormat) {
        NumberFormatEntry.SHORT -> this.shortFormat()
        NumberFormatEntry.LONG -> this.addSeparators()
    }

    private fun Int.romanOrInt() =
        if (config.replaceRoman || this == 0) this.toString() else this.toRoman()

    private fun BestiaryApi.BestiaryMob.getNextLevel() = (this.level + 1).romanOrInt()

    private fun isEnabled() = config.enabled && BestiaryApi.inInventory
}
