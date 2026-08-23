package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ModGuiSwitcherJson(
    @Expose val mods: Map<String, OtherModInfo>,
)

data class OtherModInfo(
    @Expose val description: List<String>,
    @Expose val command: String,
    @Expose @SerializedName(value = "gui_path", alternate = ["guiPath"]) val guiPath: List<String>,
)
