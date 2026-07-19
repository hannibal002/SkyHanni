package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class DisabledEventsJson(
    @Expose @SerializedName(value = "disabled_handlers", alternate = ["disabledHandlers"]) val disabledHandlers: Set<String> = emptySet(),
    @Expose @SerializedName(value = "disabled_invokers", alternate = ["disabledInvokers"]) val disabledInvokers: Set<String> = emptySet(),
)
