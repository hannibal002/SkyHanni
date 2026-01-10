package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators

@SkyHanniModule
object CropCollectionApi {
    var lastGainedCrop: CropType?
        get() = GardenApi.storage?.lastGainedCrop
        set(value) {
            value?.let {
                GardenApi.storage?.lastGainedCrop = it
            }
        }

    fun CropType.addCollectionCounter(type: CropCollectionType, amount: Long) {
        if (amount == 0L) return
        if (type !in listOf(CropCollectionType.UNKNOWN, CropCollectionType.MOOSHROOM_COW) && amount > 1) lastGainedCrop = this

        CropCollectionAddEvent(this, type, amount).post()
    }

    fun CropCollectionType.addsToMilestone(): Boolean =
        this in setOf(
            CropCollectionType.BREAKING_CROPS,
            CropCollectionType.MOOSHROOM_COW,
            CropCollectionType.PEST_BASE,
            CropCollectionType.CROP_FEVER,
            CropCollectionType.GREENHOUSE,
            CropCollectionType.PEST_RNG,
        )

    private fun addCollectionCommand(crop: CropType, amount: Long, type: CropCollectionType) {
        crop.addCollectionCounter(type, amount)
        ChatUtils.chat("Added ${amount.addSeparators()} of type $type to $crop")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shaddcropcollection") {
            description = "Add an amount to a certain crop collection."
            category = CommandCategory.DEVELOPER_TEST
            arg("crop", EnumArgumentType.custom<CropType>({ it.simpleName })) { crop ->
                arg("amount", BrigadierArguments.long()) { amount ->
                    arg("type", EnumArgumentType.custom<CropCollectionType>({ it.toString() }, isGreedy = true)) { type ->
                        callback { addCollectionCommand(getArg(crop), getArg(amount), getArg(type)) }
                    }
                }
            }
        }
    }
}
