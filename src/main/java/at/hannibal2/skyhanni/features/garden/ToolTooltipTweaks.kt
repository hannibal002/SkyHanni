package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
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
        val crop = GardenAPI.getCropTypeFromItem(event.itemStack) ?: return
        val toolFortune = FarmingFortuneDisplay.getToolFortune(event.itemStack)
        val counterFortune = FarmingFortuneDisplay.getCounterFortune(event.itemStack)
        val collectionFortune = FarmingFortuneDisplay.getCollectionFortune(event.itemStack)
        val turboCropFortune = FarmingFortuneDisplay.getTurboCropFortune(event.itemStack, crop)
        val dedicationFortune = FarmingFortuneDisplay.getDedicationFortune(event.itemStack, crop)

        val reforgeName = event.itemStack.getReforgeName()?.firstLetterUppercase()
        val enchantments = event.itemStack.getEnchantments() ?: emptyMap()
        val sunderFortune = (enchantments["sunder"] ?: 0) * 12.5
        val harvestingFortune = (enchantments["harvesting"] ?: 0) * 12.5
        val cultivatingFortune = (enchantments["cultivating"] ?: 0).toDouble()

        val ffdFortune = event.itemStack.getFarmingForDummiesCount()?.toDouble() ?: 0.0
        val cropFortune = (toolFortune + counterFortune + collectionFortune + turboCropFortune + dedicationFortune)
        val iterator = event.toolTip.listIterator()

        var removingFarmhandDescription = false
        var removingCounterDescription = false
        var removingReforgeDescription = false

        for (line in iterator) {
            val match = tooltipFortunePattern.matchEntire(line)?.groups
            if (match != null) {
                val displayedFortune = match[1]!!.value.toDouble()
                val reforgeFortune = match[2]!!.value.toDouble()
                val totalFortune = displayedFortune + cropFortune

                val ffdString = if (ffdFortune != 0.0) " §2(+${ffdFortune.formatStat()})" else ""
                val reforgeString = if (reforgeFortune != 0.0) " §9(+${reforgeFortune.formatStat()})" else ""
                val cropString = if (cropFortune != 0.0) " §6[+${cropFortune.roundToInt()}]" else ""

                val fortuneLine = when (config.cropTooltipFortune) {
                    0 -> "§7Farming Fortune: §a+${displayedFortune.formatStat()}$ffdString$reforgeString"
                    1 -> "§7Farming Fortune: §a+${displayedFortune.formatStat()}$ffdString$reforgeString$cropString"
                    else -> "§7Farming Fortune: §a+${totalFortune.formatStat()}$ffdString$reforgeString$cropString"
                }
                iterator.set(fortuneLine)

                if (Keyboard.isKeyDown(config.fortuneTooltipKeybind)) {
                    iterator.addStat("  §7Sunder: §a+", sunderFortune)
                    iterator.addStat("  §7Harvesting: §a+", harvestingFortune)
                    iterator.addStat("  §7Cultivating: §a+", cultivatingFortune)
                    iterator.addStat("  §7Farming for Dummies: §2+", ffdFortune)
                    iterator.addStat("  §7$reforgeName: §9+", reforgeFortune)
                    iterator.addStat("  §7Tool: §6+", toolFortune)
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
                if (removingReforgeDescription ) {
                    iterator.remove()
                    removingReforgeDescription = !reforgeEndLine.contains(line)
                }
                if (line == "§5§o§9Bountiful Bonus") removingReforgeDescription = true
            }

        }
    }

    companion object {
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
}