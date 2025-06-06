package at.hannibal2.skyhanni.data.jsonobjects.local

import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.Gson
import com.google.gson.annotations.Expose


class HotxTree {

    @Expose
    val perks = mutableMapOf<String, HotxPerk>()

    fun deepCopy(): HotxTree {
        val gson = Gson()
        val json = gson.toJson(this)
        return gson.fromJson<HotxTree>(json)
    }

    class HotxPerk {

        @Expose
        var level: Int = 0

        @Expose
        var enabled: Boolean = false

        @Expose
        var isUnlocked: Boolean = false
    }
}
