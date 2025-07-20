package at.hannibal2.skyhanni.config.features.chat

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class UselessDropsFilterConfig {

    @Expose
    @ConfigOption(name = "Combat Drops", desc = "Hides useless combat drops")
    @ConfigEditorBoolean
    var combatDrops: Boolean = false

    @Expose
    @ConfigOption(name = "Dungeon Drops", desc = "Hides useless dungeon drops")
    @ConfigEditorBoolean
    var dungeonDrops: Boolean = false
}
