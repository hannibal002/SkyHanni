package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators

@SkyHanniModule
object CropCollectionAPI {
    var lastGainedCrop: CropType?
        get() = GardenApi.storage?.lastGainedCrop
        set(value) {
            value?.let {
                GardenApi.storage?.lastGainedCrop = it
            }
        }

    fun CropType.addCollectionCounter(type: CropCollectionType, amount: Long) {
        if (amount == 0L) return
        if (type != CropCollectionType.UNKNOWN && amount > 1) lastGainedCrop = this

        CropCollectionAddEvent(this, type, amount).post()
    }

    fun CropCollectionType.addsToMilestone(): Boolean =
        this in setOf(
            CropCollectionType.BREAKING_CROPS,
            CropCollectionType.MOOSHROOM_COW,
            CropCollectionType.PEST_BASE,
            CropCollectionType.DICER,
            CropCollectionType.PEST_RNG,
        )

    private fun addMilestoneCommand(cropText: String, amount: Long, typeText: String) {
        val crop = CropType.getByNameOrNull(cropText.replace("_", " ")) ?: run {
            ChatUtils.userError("Invalid type! Format is /shaddcropcollection <crop> <amount> <type>")
            return
        }
        val type = if (typeText == "") CropCollectionType.UNKNOWN else CropCollectionType.getByName(typeText.replace("_", " ")) ?: run {
            ChatUtils.userError("Invalid type! Format is /shaddcropcollection <crop> <amount> <type>")
            return
        }

        crop.addCollectionCounter(type, amount)
        ChatUtils.chat("Added ${amount.addSeparators()} of type $type to $cropText")

    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shaddcropcollection") {
            description = "Add an amount to a certain crop collection."
            category = CommandCategory.DEVELOPER_DEBUG
            arg("crop", BrigadierArguments.string()) { crop ->
                arg("amount", BrigadierArguments.long()) { amount ->
                    arg("type", BrigadierArguments.string()) { type ->
                        callback { addMilestoneCommand(getArg(crop), getArg(amount), getArg(type)) }
                    }
                }
            }
        }
    }
}
