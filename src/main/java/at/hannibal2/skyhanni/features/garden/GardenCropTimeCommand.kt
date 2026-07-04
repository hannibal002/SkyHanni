package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.features.garden.farming.CropMoneyDisplay
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sorted
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenCropTimeCommand {

    private val config get() = GardenApi.config.moneyPerHours

    private fun processCropEntry(internalName: NeuInternalName, amount: Long, searchName: String): Pair<String, Long>? {
        val itemName = internalName.repoItemName
        if (!itemName.removeColor().startsWith(searchName, ignoreCase = true)) return null

        val (baseId, baseAmount) = NeuItems.getPrimitiveMultiplier(internalName)
        val baseName = baseId.repoItemName
        val crop = CropType.getByName(baseName.removeColor())

        val fullAmount = baseAmount.toLong() * amount
        val text = if (baseAmount == 1) {
            "§e${amount.addSeparators()}x $itemName"
        } else {
            "§e${amount.addSeparators()}x $itemName §7(§e${fullAmount.addSeparators()}x $baseName§7)"
        }

        val speed = crop.getSpeed()
        return if (speed == null) {
            "$text §cNo speed data!" to -1L
        } else {
            val missingTime = (fullAmount / speed).seconds
            "$text §b${missingTime.format()}" to missingTime.inWholeSeconds
        }
    }

    private fun onCommand(amount: Long, item: String) {
        if (!config.display) {
            ChatUtils.userError("Command /shcroptime requires 'Show money per Hour' feature to be enabled to work!")
            return
        }

        val multipliers = CropMoneyDisplay.multipliers
        if (multipliers.isEmpty()) {
            ChatUtils.userError("Data not loaded yet. Join the garden and display the money per hour display.")
            return
        }

        val map = mutableMapOf<String, Long>()
        for (entry in multipliers) {
            val (text, time) = processCropEntry(entry.key, amount, item) ?: continue
            map[text] = time
        }

        if (map.isEmpty()) {
            ChatUtils.userError("No crop item found for '$item'.")
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

            simpleCallback {
                ChatUtils.userError("Usage: /shcroptime <amount> <item>")
            }

            arg("amount", BrigadierArguments.long(1)) { amountArg ->
                simpleCallback {
                    ChatUtils.userError("Usage: /shcroptime <amount> <item>")
                }

                argCallback(
                    "item",
                    BrigadierArguments.greedyString(),
                    suggestions = BrigadierUtils.dynamicSuggestionProvider {
                        CropMoneyDisplay.multipliers.keys.map { it.repoItemName.removeColor() }
                    },
                ) { item ->
                    onCommand(getArg(amountArg), item)
                }
            }
        }
    }
}
