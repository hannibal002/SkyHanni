package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.events.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import kotlinx.coroutines.launch
import net.minecraft.item.ItemStack

object GardenCropMilestonesFix {

    fun openInventory(inventoryItems: Map<Int, ItemStack>) {
        if (SkyHanniMod.feature.garden.copyMilestoneData) {
            fixForWrongData(inventoryItems)
        }
    }

    private fun fixForWrongData(inventoryItems: Map<Int, ItemStack>) {
        val data = mutableListOf<String>()
        for ((_, stack) in inventoryItems) {
            val crop = GardenCropMilestones.getCropTypeByLore(stack) ?: continue
            checkForWrongData(stack, crop, data)

            CropMilestoneUpdateEvent().postAndCatch()
        }

        if (data.isNotEmpty()) {
            LorenzUtils.chat(
                "Found §c${data.size} §ewrong crop milestone steps in the menu! " +
                    "Correct data got put into clipboard. " +
                    "Please share it on SkyHanni Discord."
            )
            OSUtils.copyToClipboard("```${data.joinToString("\n")}```")
        } else {
            LorenzUtils.chat("No wrong crop milestone steps found!")
        }
    }

    private fun checkForWrongData(
        stack: ItemStack,
        crop: CropType,
        wrongData: MutableList<String>
    ) {
        val pattern = ".*§e(?<having>.*)§6/§e(?<max>.*)".toPattern()
        val name = stack.name ?: return
        val rawNumber = name.removeColor().replace(crop.cropName, "").trim()
        val realTier = if (rawNumber == "") 0 else rawNumber.romanToDecimalIfNeeded()

        val lore = stack.getLore()
        val next = lore.nextAfter({ GardenCropMilestones.totalPattern.matches(it) }, 3) ?: return
        val total = lore.nextAfter({ GardenCropMilestones.totalPattern.matches(it) }, 6) ?: return

        debug(" ")
        debug("crop: $crop")
        debug("realTier: $realTier")

        val guessNextMax = GardenCropMilestones.getCropsForTier(
            realTier + 1,
            crop
        ) - GardenCropMilestones.getCropsForTier(realTier, crop)
        debug("guessNextMax: ${guessNextMax.addSeparators()}")
        val nextMax = pattern.matchMatcher(next) {
            group("max").formatNumber()
        } ?: return
        debug("nextMax real: ${nextMax.addSeparators()}")
        if (nextMax != guessNextMax) {
            debug("wrong, add to list")
            wrongData.add("$crop:$realTier:${nextMax.addSeparators()}")
        }

        val guessTotalMax = GardenCropMilestones.getCropsForTier(46, crop)
        //             println("guessTotalMax: ${guessTotalMax.addSeparators()}")
        val totalMax = pattern.matchMatcher(total) {
            group("max").formatNumber()
        } ?: return
        //             println("totalMax real: ${totalMax.addSeparators()}")
        val totalOffBy = guessTotalMax - totalMax
        debug("totalOffBy: $totalOffBy")
    }

    fun debug(message: String) {
        if (SkyHanniMod.feature.dev.debug.enabled) {
//             println(message)
        }
    }

    /**
     *
     * This helps to fix wrong crop milestone data
     * This command reads the clipboard content,
     * in the format of users sending crop milestone step data.
     *
     * The new data will be compared to the currently saved data,
     * differences are getting replaced, and the result gets put into the clipboard.
     * The clipboard context can be used to update the repo content.
     */
    fun readDataFromClipboard() {
        SkyHanniMod.coroutineScope.launch {
            OSUtils.readFromClipboard()?.let {
                handleInput(it)
            }
        }
    }

    private var totalFixedValues = 0

    private fun handleInput(input: String) {
        println(" ")
        var fixed = 0
        var alreadyCorrect = 0
        for (line in input.lines()) {
            val split = line.replace("```", "").replace(".", ",").split(":")
            if (split.size != 3) continue
            val (rawCrop, tier, amount) = split
            val crop = LorenzUtils.enumValueOf<CropType>(rawCrop)

            if (tryFix(crop, tier.toInt(), amount.formatNumber().toInt())) {
                fixed++
            } else {
                alreadyCorrect++
            }
        }
        totalFixedValues += fixed
        LorenzUtils.chat("Fixed: $fixed/$alreadyCorrect, total fixes: $totalFixedValues")
        val s = ConfigManager.gson.toJsonTree(GardenCropMilestones.cropMilestoneData).toString()
        OSUtils.copyToClipboard("\"crop_milestones\":$s,")
    }

    private fun tryFix(crop: CropType, tier: Int, amount: Int): Boolean {
        val guessNextMax = GardenCropMilestones.getCropsForTier(tier + 1, crop) - GardenCropMilestones.getCropsForTier(
            tier,
            crop
        )
        if (guessNextMax.toInt() == amount) {
            return false
        }
        GardenCropMilestones.cropMilestoneData = GardenCropMilestones.cropMilestoneData.editCopy {
            fix(crop, this, tier, amount)
        }
        return true
    }

    private fun fix(crop: CropType, map: MutableMap<CropType, List<Int>>, tier: Int, amount: Int) {
        map[crop] = map[crop]!!.editCopy {
            this[tier] = amount
        }
    }
}
