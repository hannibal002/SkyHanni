package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
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
        get() = GardenApi.storage?.lastGainedCrop
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

        cropCollectionCounter?.getOrPut(this) { CropCollection() }?.addCollection(type, amount)

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

    fun CropType.setCollectionCounter(counter: Long) {
        cropCollectionCounter?.getOrPut(this) { CropCollection() }?.setTotal(counter)
        // Some displays update off add events
        CropCollectionAddEvent(this, CropCollectionType.UNKNOWN, 0).post()
        ChatUtils.debug("Set $this collection to $counter")
    }

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
            val total = cropCollectionType.filter { it.key != CropCollectionType.UNKNOWN }.sumAllValues().toLong()
            val diff = amount - total
            setCollection(CropCollectionType.UNKNOWN, diff)
        }

        fun getCollection(collectionType: CropCollectionType): Long {
            return cropCollectionType.getOrPut(collectionType) { 0 }
        }

        fun addCollection(collectionType: CropCollectionType, amount: Long) {
            val collection = getCollection(collectionType)
            setCollection(collectionType, collection + amount)
        }

        private fun setCollection(collectionType: CropCollectionType, amount: Long) {
            cropCollectionType[collectionType] = amount
        }

        @Expose
        var cropCollectionType: MutableMap<CropCollectionType, Long> = EnumMap(CropCollectionType::class.java)
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("crop collection")
        event.addIrrelevant {
            cropCollectionCounter?.forEach {
                add("Crop: ${it.key}")
                add("Total: ${it.value.getTotal()}")
                it.value.cropCollectionType.forEach { collectionType ->
                    add("$collectionType: ${collectionType.value}")
                }
            }
        }
    }
}
