package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.dynamicSuggestionProvider
import at.hannibal2.skyhanni.features.garden.farming.CropMoneyDisplay
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatLongOrUserError
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sorted
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenCropTimeCommand {

    private val config get() = GardenApi.config.moneyPerHours

    private fun onCommand(amount: Int, item: String) {
        if (!config.display) {
            ChatUtils.userError("Command /shcroptime requires 'Show money per Hour' feature to be enabled to work!")
            return
        }

        val multipliers = CropMoneyDisplay.multipliers
        if (multipliers.isEmpty()) {
            ChatUtils.userError("Data not loaded yet. Join the garden and display the money per hour display.")
            return
        }

        val searchName = item.lowercase()
        val amountLong = amount.toLong()

        val map = mutableMapOf<String, Long>()
        for (entry in multipliers) {
            val internalName = entry.key
            val itemName = internalName.repoItemName
            if (itemName.lowercase().contains(searchName)) {
                val (baseId, baseAmount) = NeuItems.getPrimitiveMultiplier(internalName)
                val baseName = baseId.repoItemName
                val crop = CropType.getByName(baseName)

                val fullAmount = baseAmount.toLong() * amountLong
                val text = if (baseAmount == 1) {
                    "§e${amountLong.addSeparators()}x $itemName"
                } else {
                    "§e${amountLong.addSeparators()}x $itemName §7(§e${fullAmount.addSeparators()}x $baseName§7)"
                }

                val speed = crop.getSpeed()
                if (speed == null) {
                    map["$text §cNo speed data!"] = -1
                } else {
                    val missingTime = (fullAmount / speed).seconds
                    val duration = missingTime.format()
                    map["$text §b$duration"] = missingTime.inWholeSeconds
                }
            }
        }

        if (map.isEmpty()) {
            ChatUtils.chat("Crop Speed for ${map.size} items:\n" + map.sorted().keys.joinToString("\n"))
            return
        }

        ChatUtils.chat("Crop Speed for ${map.size} items:\n" + map.sorted().keys.joinToString("\n"))
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcroptime") {
            description =
                "Calculates with your current crop per second speed how long you need to farm a crop to collect this amount of items"
            category = CommandCategory.USERS_ACTIVE

            arg("amount", BrigadierArguments.integer(1)) { amountArg ->
                argCallback(
                    "item",
                    BrigadierArguments.greedyString(),
                    suggestions = dynamicSuggestionProvider {
                        CropMoneyDisplay.multipliers.keys.map { it.repoItemName.removeColor().lowercase() }
                    }
                ) { item ->
                    onCommand(getArg(amountArg), item)
                }
            }
        }
    }
}
