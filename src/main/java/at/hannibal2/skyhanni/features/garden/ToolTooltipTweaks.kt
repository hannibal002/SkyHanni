package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay.Companion.getAbilityFortune
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getFarmingForDummiesCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import java.text.DecimalFormat
import kotlin.math.roundToInt

class ToolTooltipTweaks {
    private val config get() = SkyHanniMod.feature.garden
    private val tooltipFortunePattern = "^§5§o§7Farming Fortune: §a\\+([\\d.]+)(?: §2\\(\\+\\d\\))?(?: §9\\(\\+(\\d+)\\))\$".toRegex()
    private val counterStartLine = setOf("§5§o§6Logarithmic Counter", "§5§o§6Collection Analysis")
    private val reforgeEndLine = setOf("§5§o", "§5§o§7chance for multiple crops.")

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val crop = event.itemStack.getCropType()
        val toolFortune = FarmingFortuneDisplay.getToolFortune(event.itemStack)
        val counterFortune = FarmingFortuneDisplay.getCounterFortune(event.itemStack)
        val collectionFortune = FarmingFortuneDisplay.getCollectionFortune(event.itemStack)
        val turboCropFortune = FarmingFortuneDisplay.getTurboCropFortune(event.itemStack, crop)
        val dedicationFortune = FarmingFortuneDisplay.getDedicationFortune(event.itemStack, crop)

        val reforgeName = event.itemStack.getReforgeName()?.firstLetterUppercase()

        val sunderFortune = FarmingFortuneDisplay.getSunderFortune(event.itemStack)
        val harvestingFortune = FarmingFortuneDisplay.getHarvestingFortune(event.itemStack)
        val cultivatingFortune = FarmingFortuneDisplay.getCultivatingFortune(event.itemStack)
        val abilityFortune = getAbilityFortune(event.itemStack)

        val ffdFortune = event.itemStack.getFarmingForDummiesCount()?.toDouble() ?: 0.0
        val hiddenFortune = (toolFortune + counterFortune + collectionFortune + turboCropFortune + dedicationFortune + abilityFortune)
        val iterator = event.toolTip.listIterator()

        var removingFarmhandDescription = false
        var removingCounterDescription = false
        var removingReforgeDescription = false

        for (line in iterator) {
            if (line.contains("Kills:") && event.itemStack.getInternalName().contains("LOTUS")) {
                iterator.set(line.replace("Kills:", "Visitors:"))
            } // cannot test that this works

            val match = tooltipFortunePattern.matchEntire(line)?.groups
            if (match != null) {
                val enchantmentFortune = sunderFortune + harvestingFortune + cultivatingFortune

                FarmingFortuneDisplay.loadFortuneLineData(event.itemStack, enchantmentFortune, match)

                val displayedFortune = FarmingFortuneDisplay.displayedFortune
                val reforgeFortune = FarmingFortuneDisplay.reforgeFortune
                val baseFortune = FarmingFortuneDisplay.itemBaseFortune
                val greenThumbFortune = FarmingFortuneDisplay.greenThumbFortune

                val totalFortune = displayedFortune + hiddenFortune


                val ffdString = if (ffdFortune != 0.0) " §2(+${ffdFortune.formatStat()})" else ""
                val reforgeString = if (reforgeFortune != 0.0) " §9(+${reforgeFortune.formatStat()})" else ""
                val cropString = if (hiddenFortune != 0.0) " §6[+${hiddenFortune.roundToInt()}]" else ""

                val fortuneLine = when (config.cropTooltipFortune) {
                    0 -> "§7Farming Fortune: §a+${displayedFortune.formatStat()}$ffdString$reforgeString"
                    1 -> "§7Farming Fortune: §a+${displayedFortune.formatStat()}$ffdString$reforgeString$cropString"
                    else -> "§7Farming Fortune: §a+${totalFortune.formatStat()}$ffdString$reforgeString$cropString"
                }
                iterator.set(fortuneLine)

                if (Keyboard.isKeyDown(config.fortuneTooltipKeybind)) {
                    iterator.addStat("  §7Base: §a+", baseFortune)
                    iterator.addStat("  §7Tool: §6+", toolFortune)
                    iterator.addStat("  $reforgeName: §9+", reforgeFortune)
                    iterator.addStat("  §7Ability: §a+", abilityFortune)
                    iterator.addStat("  §7Green Thumb: §a+", greenThumbFortune)
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
            if (config.compactToolTooltips) {
                if (line.startsWith("§5§o§7§8Bonus ")) removingFarmhandDescription = true
                if (removingFarmhandDescription) {
                    iterator.remove()
                    removingFarmhandDescription = line != "§5§o"
                }

                if (removingCounterDescription && !line.startsWith("§5§o§7You have")) {
                    iterator.remove()
                } else {
                    removingCounterDescription = false
                }
                if (counterStartLine.contains(line)) removingCounterDescription = true

                if (line == "§5§o§9Blessed Bonus") removingReforgeDescription = true
                if (removingReforgeDescription) {
                    iterator.remove()
                    removingReforgeDescription = !reforgeEndLine.contains(line)
                }
                if (line == "§5§o§9Bountiful Bonus") removingReforgeDescription = true
            }

        }
    }

    private fun Double.formatStat(): String {
        val formatter = DecimalFormat("0.##")
        return formatter.format(this)
    }

    private fun MutableListIterator<String>.addStat(description: String, value: Double) {
        if (value != 0.0) {
            this.add("$description${value.formatStat()}")
        }
    }
}