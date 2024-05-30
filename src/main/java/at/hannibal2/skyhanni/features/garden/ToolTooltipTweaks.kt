package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.garden.TooltipTweaksConfig.CropTooltipFortuneEntry
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay.getAbilityFortune
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getFarmingForDummiesCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.StringUtils.find
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.text.DecimalFormat
import kotlin.math.roundToInt

class ToolTooltipTweaks {


    private val config get() = GardenAPI.config.tooltipTweak

    private val patternGroup = RepoPattern.group("tooltiptweaks")
    private val tooltipFortunePattern by patternGroup.pattern(
        "tooltipfortune",
        "^§5§o§7Farming Fortune: §a\\+([\\d.]+)(?: §2\\(\\+\\d\\))?(?: §9\\(\\+(\\d+)\\))?\$"
    )
    private val counterStartLinePatterns by patternGroup.list(
        "counterstart",
        "§5§o§6Logarithmic Counter",
        "§5§o§6Collection Analysis",
    )
    private val reforgeEndLinePatterns by patternGroup.list(
        "reforgeend",
        "§5§o",
        "§5§o§7chance for multiple crops.",
    )
    private val abilityDescriptionStartPattern by patternGroup.pattern(
        "abilitydescriptionstart",
        "§5§o§7These boots gain §a+2❈ Defense",
    )
    private val abilityDescriptionEndPattern by patternGroup.pattern(
        "abilitydescriptionend",
        "§5§o§7Skill level.",
    )
    private val bonusPattern by patternGroup.pattern(
        "bonus",
        "^§5§o§7§8Bonus ",
    )
    private val removingDescriptionPattern by patternGroup.pattern(
        "removingdescription",
        "§5§o",
    )
    private val youHavePattern by patternGroup.pattern(
        "youhave",
        "^§5§o§7You have",
    )
    private val bountifulPattern by patternGroup.pattern(
        "bonus",
        "§5§o§9Bountiful Bonus",
    )
    private val blessedPattern by patternGroup.pattern(
        "removingdescription",
        "§5§o§9Blessed Bonus",
    )
    private val miscPattern by patternGroup.pattern(
        "misc",
        "Click to|§7§8This item can be reforged!|Dyed",
    )

    private val statFormatter = DecimalFormat("0.##")

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val itemStack = event.itemStack
        val itemLore = itemStack.getLore()
        val internalName = itemStack.getInternalName()
        val crop = itemStack.getCropType()
        val toolFortune = FarmingFortuneDisplay.getToolFortune(internalName)
        val counterFortune = FarmingFortuneDisplay.getCounterFortune(itemStack)
        val collectionFortune = FarmingFortuneDisplay.getCollectionFortune(itemStack)
        val turboCropFortune = FarmingFortuneDisplay.getTurboCropFortune(itemStack, crop)
        val dedicationFortune = FarmingFortuneDisplay.getDedicationFortune(itemStack, crop)

        val reforgeName = itemStack.getReforgeName()?.firstLetterUppercase()

        val sunderFortune = FarmingFortuneDisplay.getSunderFortune(itemStack)
        val harvestingFortune = FarmingFortuneDisplay.getHarvestingFortune(itemStack)
        val cultivatingFortune = FarmingFortuneDisplay.getCultivatingFortune(itemStack)
        val abilityFortune = getAbilityFortune(internalName, itemLore)

        val ffdFortune = itemStack.getFarmingForDummiesCount() ?: 0
        val hiddenFortune =
            (toolFortune + counterFortune + collectionFortune + turboCropFortune + dedicationFortune + abilityFortune)
        val iterator = event.toolTip.listIterator()

        var removingFarmhandDescription = false
        var removingCounterDescription = false
        var removingReforgeDescription = false
        var removingAbilityDescription = false

        for (line in iterator) {
            if (tooltipFortunePattern.matches(line)) {
                val enchantmentFortune = sunderFortune + harvestingFortune + cultivatingFortune

                FarmingFortuneDisplay.loadFortuneLineData(itemStack, enchantmentFortune)

                val displayedFortune = FarmingFortuneDisplay.displayedFortune
                val reforgeFortune = FarmingFortuneDisplay.reforgeFortune
                val gemstoneFortune = FarmingFortuneDisplay.gemstoneFortune
                val baseFortune = FarmingFortuneDisplay.itemBaseFortune
                val greenThumbFortune = FarmingFortuneDisplay.greenThumbFortune
                val pesterminatorFortune = FarmingFortuneDisplay.pesterminatorFortune

                val totalFortune = displayedFortune + hiddenFortune

                val ffdString = if (ffdFortune != 0) " §2(+${ffdFortune.formatStat()})" else ""
                val reforgeString = if (reforgeFortune != 0.0) " §9(+${reforgeFortune.formatStat()})" else ""
                val cropString = if (hiddenFortune != 0.0) " §6[+${hiddenFortune.roundToInt()}]" else ""

                val fortuneLine = when (config.cropTooltipFortune) {
                    CropTooltipFortuneEntry.DEFAULT -> "§7Farming Fortune: §a+${displayedFortune.formatStat()}$ffdString$reforgeString"
                    CropTooltipFortuneEntry.SHOW -> "§7Farming Fortune: §a+${displayedFortune.formatStat()}$ffdString$reforgeString$cropString"
                    else -> "§7Farming Fortune: §a+${totalFortune.formatStat()}$ffdString$reforgeString$cropString"
                }
                iterator.set(fortuneLine)

                if (config.fortuneTooltipKeybind.isKeyHeld()) {
                    iterator.addStat("  §7Base: §6+", baseFortune)
                    iterator.addStat("  §7Tool: §6+", toolFortune)
                    iterator.addStat("  §7${reforgeName?.removeColor()}: §9+", reforgeFortune)
                    iterator.addStat("  §7Gemstone: §d+", gemstoneFortune)
                    iterator.addStat("  §7Ability: §2+", abilityFortune)
                    iterator.addStat("  §7Green Thumb: §a+", greenThumbFortune)
                    iterator.addStat("  §7Pesterminator: §a+", pesterminatorFortune)
                    iterator.addStat("  §7Sunder: §a+", sunderFortune)
                    iterator.addStat("  §7Harvesting: §a+", harvestingFortune)
                    iterator.addStat("  §7Cultivating: §a+", cultivatingFortune)
                    iterator.addStat("  §7Farming for Dummies: §2+", ffdFortune)
                    iterator.addStat("  §7Counter: §6+", counterFortune)
                    iterator.addStat("  §7Collection: §6+", collectionFortune)
                    iterator.addStat("  §7Dedication: §6+", dedicationFortune)
                    iterator.addStat("  §7Turbo-Crop: §6+", turboCropFortune)
                }
            }
            // Beware, dubious control flow beyond these lines
            if (config.compactToolTooltips || FFGuideGUI.isInGui()) {
                if (bonusPattern.find(line)) removingFarmhandDescription = true
                if (removingFarmhandDescription) {
                    iterator.remove()
                    removingFarmhandDescription = !removingDescriptionPattern.matches(line)
                } else if (removingCounterDescription && !youHavePattern.find(line)) {
                    iterator.remove()
                } else {
                    removingCounterDescription = false
                }
                if (counterStartLinePatterns.any { it.find(line) }) removingCounterDescription = true

                if (blessedPattern.matches(line)) removingReforgeDescription = true
                if (removingReforgeDescription) {
                    iterator.remove()
                    removingReforgeDescription = reforgeEndLinePatterns.any { it.find(line) }
                }
                if (bountifulPattern.matches(line)) removingReforgeDescription = true

                if (FFGuideGUI.isInGui()) {
                    if (miscPattern.find(line)) {
                        iterator.remove()
                    }

                    if (abilityDescriptionStartPattern.matches(line)) {
                        removingAbilityDescription = true
                    }
                    if (removingAbilityDescription) {
                        iterator.remove()
                        if (abilityDescriptionEndPattern.matches(line)) {
                            removingAbilityDescription = false
                        }
                    }
                }
            }
        }
    }

    private fun Number.formatStat() = statFormatter.format(this)

    private fun MutableListIterator<String>.addStat(description: String, value: Number) {
        if (value.toDouble() != 0.0) {
            add("$description${value.formatStat()}")
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.compactToolTooltips", "garden.tooltipTweak.compactToolTooltips")
        event.move(3, "garden.fortuneTooltipKeybind", "garden.tooltipTweak.fortuneTooltipKeybind")
        event.move(3, "garden.cropTooltipFortune", "garden.tooltipTweak.cropTooltipFortune")

        event.transform(15, "garden.tooltipTweak.cropTooltipFortune") { element ->
            ConfigUtils.migrateIntToEnum(element, CropTooltipFortuneEntry::class.java)
        }
    }
}
