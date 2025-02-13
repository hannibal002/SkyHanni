package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class DisabledApiJson(
    @Expose val disabledBazaar: Boolean,
    @Expose val disabledMoulberryLowestBin: Boolean,
    @Expose val disabledNpcPrices: Boolean,
)
