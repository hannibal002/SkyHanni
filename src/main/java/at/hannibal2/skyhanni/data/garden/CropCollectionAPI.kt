package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

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

}
