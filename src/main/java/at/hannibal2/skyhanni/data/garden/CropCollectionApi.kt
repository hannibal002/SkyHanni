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
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import com.google.gson.annotations.Expose
import java.util.EnumMap

@SkyHanniModule
object CropCollectionApi {
    private val storage get() = GardenApi.storage

    private val cropCollectionCounter:
        MutableMap<CropType, CropCollection>? get() = storage?.cropCollectionCounter

    var lastGainedCrop: CropType?
        get() = storage?.lastGainedCrop
        set(value) {
            value?.let {
                GardenApi.storage?.lastGainedCrop = it
            }
        }

    var lastGainedCollectionTime = SimpleTimeMark.farPast()

    var needCollectionUpdate = true

    fun CropType.getCollection() =
        cropCollectionCounter?.get(this)?.getTotal() ?: 0L

    fun CropType.getCollection(type: CropCollectionType) =
        cropCollectionCounter?.get(this)?.getCollection(type)

    fun CropType.addCollectionCounter(type: CropCollectionType, amount: Long) {
        if (amount == 0L) return
        if (type !in listOf(CropCollectionType.UNKNOWN, CropCollectionType.MOOSHROOM_COW) && amount > 1) lastGainedCrop = this
        if (type != CropCollectionType.UNKNOWN) {
            lastGainedCollectionTime = SimpleTimeMark.now()
        }

        cropCollectionCounter?.get(this)?.addCollection(type, amount)

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

    fun CropType.setCollectionCounter(counter: Long) {
        cropCollectionCounter?.get(this)?.setTotal(counter)
        // Some displays update off add events
        CropCollectionAddEvent(this, CropCollectionType.UNKNOWN, 0).post()
    }

    private fun addCollectionCommand(cropText: String, amount: Long, typeText: String) {
        val crop = CropType.getByNameOrNull(cropText.replace("_", " ")) ?: run {
            ChatUtils.userError("Invalid crop! Format is /shaddcropcollection <crop> <amount> <type>")
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
                        callback { addCollectionCommand(getArg(crop), getArg(amount), getArg(type)) }
                    }
                }
            }
        }
        event.registerBrigadier("shshowcropcollection") {
            description = "Show current crop collection amounts"
            category = CommandCategory.DEVELOPER_DEBUG
            callback {
                for (entry in CropType.entries) {
                    ChatUtils.chat("$entry collection: ${entry.getCollection()}")
                }
                ChatUtils.debug("$cropCollectionCounter")
            }
        }
    }

    class CropCollection {
        fun getTotal(): Long {
            return cropCollectionType.sumAllValues().toLong()
        }

        fun setTotal(amount: Long) {
            val diff = amount - getTotal()
            setCollection(CropCollectionType.UNKNOWN, diff)
        }

        fun getCollection(collectionType: CropCollectionType): Long {
            return cropCollectionType.getOrPut(collectionType) { 0 }
        }

        fun addCollection(collectionType: CropCollectionType, amount: Long) {
            val collection = getCollection(collectionType)
            setCollection(collectionType, collection + amount)
        }

        fun setCollection(collectionType: CropCollectionType, amount: Long) {
            cropCollectionType[collectionType] = amount
        }

        @Expose
        var cropCollectionType: MutableMap<CropCollectionType, Long> = EnumMap(CropCollectionType::class.java)
    }
}
