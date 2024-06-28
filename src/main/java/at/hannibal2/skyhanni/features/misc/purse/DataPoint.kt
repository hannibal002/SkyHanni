package at.hannibal2.skyhanni.features.misc.purse

import com.google.gson.annotations.Expose

data class DataPoint(
    @Expose val time: Long,
    @Expose val value: Double,
)
