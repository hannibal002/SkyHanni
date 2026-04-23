package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.utils.system.ModVersion
import com.google.gson.JsonPrimitive
import moe.nea.libautoupdate.UpdateData

class ModrinthUpdateData(
    versionNumber: ModVersion,
    val htmlUrl: String,
) : UpdateData(versionNumber.asString, JsonPrimitive(versionNumber.asString), null, null)
