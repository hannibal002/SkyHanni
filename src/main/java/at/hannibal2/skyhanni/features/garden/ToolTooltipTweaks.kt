package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getFarmingForDummiesCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import java.text.DecimalFormat
import kotlin.math.roundToInt

object ToolTooltipTweaks {
    private var resultList = mutableListOf<String>()
    private val config get() = SkyHanniMod.feature.garden
    private val tooltipFortunePattern = "^§7Farming Fortune: §a\\+([\\d.]+)(?: §2\\(\\+\\d\\))?(?: §9\\(\\+(\\d+)\\))\$".toRegex()
    private val counterStartLine = setOf("§6Logarithmic Counter", "§6Collection Analysis")

    private val reforgeEndLine = setOf("", "§7chance for multiple crops.")

     private fun gardenTooltip(itemStack: ItemStack, tooltip: MutableList<String>, fetching: Boolean = false): MutableList<String> {
        var string = ""
        resultList.clear()
        val crop = itemStack.getCropType()
        val toolFortune = FarmingFortuneDisplay.getToolFortune(itemStack)
        val counterFortune = FarmingFortuneDisplay.getCounterFortune(itemStack)
        val collectionFortune = FarmingFortuneDisplay.getCollectionFortune(itemStack)
        val turboCropFortune = FarmingFortuneDisplay.getTurboCropFortune(itemStack, crop)
        val dedicationFortune = FarmingFortuneDisplay.getDedicationFortune(itemStack, crop)

        val reforgeName = itemStack.getReforgeName()?.firstLetterUppercase()
        val enchantments = itemStack.getEnchantments() ?: emptyMap()
        val sunderFortune = (enchantments["sunder"] ?: 0) * 12.5
        val harvestingFortune = (enchantments["harvesting"] ?: 0) * 12.5
        val cultivatingFortune = (enchantments["cultivating"] ?: 0).toDouble()

        val ffdFortune = itemStack.getFarmingForDummiesCount()?.toDouble() ?: 0.0
        val cropFortune = (toolFortune + counterFortune + collectionFortune + turboCropFortune + dedicationFortune)

        var removingFarmhandDescription = false
        var removingCounterDescription = false
        var removingReforgeDescription = false
        for (line in tooltip) {
            val newLine = line.replace("§5§o", "")
            val match = tooltipFortunePattern.matchEntire(newLine)?.groups
            if (match != null) {
                val displayedFortune = match[1]!!.value.toDouble()
                val reforgeFortune = match[2]!!.value.toDouble()
                val totalFortune = displayedFortune + cropFortune

                val ffdString = if (ffdFortune != 0.0) " §2(+${ffdFortune.formatStat()})" else ""
                val reforgeString = if (reforgeFortune != 0.0) " §9(+${reforgeFortune.formatStat()})" else ""
                val cropString = if (cropFortune != 0.0) " §6[+${cropFortune.roundToInt()}]" else ""

                //TODO for fetch
                val fortuneLine = if (fetching) "§7Farming Fortune: §a+${totalFortune.formatStat()}$ffdString$reforgeString$cropString"
                else when (config.cropTooltipFortune) {
                    0 -> "§7Farming Fortune: §a+${displayedFortune.formatStat()}$ffdString$reforgeString"
                    1 -> "§7Farming Fortune: §a+${displayedFortune.formatStat()}$ffdString$reforgeString$cropString"
                    else -> "§7Farming Fortune: §a+${totalFortune.formatStat()}$ffdString$reforgeString$cropString"
                }
                string += fortuneLine + "\n"

                if (Keyboard.isKeyDown(config.fortuneTooltipKeybind) && !fetching) { // don't want to save this data
                    string += ("  §7Sunder: §a+ $sunderFortune\n")
                    string += ("  §7Harvesting: §a+ $harvestingFortune\n")
                    string += ("  §7Cultivating: §a+ $cultivatingFortune\n")
                    string += ("  §7Farming for Dummies: §2+ $ffdFortune\n")
                    string += ("  §7$reforgeName: §9+ $reforgeFortune\n")
                    string += ("  §7Tool: §6+ $toolFortune\n")
                    string += ("  §7Counter: §6+ $counterFortune\n")
                    string += ("  §7Collection: §6+ $collectionFortune\n")
                    string += ("  §7Dedication: §6+ $dedicationFortune\n")
                    string += ("  §7Turbo-Crop: §6+ $turboCropFortune\n")
                }
                continue
            }
            // Beware, dubious control flow beyond these lines
            if (config.compactToolTooltips || fetching) {
                if (newLine.startsWith("§7§8Bonus ")) removingFarmhandDescription = true
                if (removingFarmhandDescription) {
                    removingFarmhandDescription = newLine != ""
                    continue
                }

                if (removingCounterDescription && !newLine.startsWith("§7You have")) {
                    continue
                } else {
                    removingCounterDescription = false
                }
                if (newLine in counterStartLine) removingCounterDescription = true

                if (newLine == "§9Blessed Bonus") removingReforgeDescription = true
                if (removingReforgeDescription) {
                    removingReforgeDescription = !reforgeEndLine.contains(newLine)
                    continue
                }
                if (newLine == "§9Bountiful Bonus") removingReforgeDescription = true
            }
            string += newLine + "\n"
        }
         resultList = string.split("\n").toMutableList()
         resultList.removeLast()
         return resultList
    }
    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return

        gardenTooltip(event.itemStack, event.toolTip)
        val iterator = event.toolTip.listIterator()
        for (line in iterator) {
            iterator.remove()
        }
        for (line in resultList) {
            iterator.add(line)
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