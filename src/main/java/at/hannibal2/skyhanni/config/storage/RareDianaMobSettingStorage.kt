package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.features.event.diana.dianararemobstorage.RareDianaMobSettings
import com.google.gson.annotations.Expose

class RareDianaMobSettingStorage {
    @Expose
    var RareDianaMobSettingStorage: MutableMap<String, RareDianaMobSettings> = mutableMapOf()
}
