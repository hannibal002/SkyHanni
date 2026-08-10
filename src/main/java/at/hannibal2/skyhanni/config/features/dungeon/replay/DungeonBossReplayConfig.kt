package at.hannibal2.skyhanni.config.features.dungeon.replay

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.dungeon.DungeonFloor
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream

class DungeonBossReplayConfig {
    @Expose
    @ConfigOption(name = "Enable", desc = "Play and Save replays of your best time for a dungeon boss.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Enabled Floors", desc = "Floors that should save replays")
    @ConfigEditorDraggableList
    var enabledFloors: List<DungeonFloorWithBoss> = listOf(F7, M7)

    // todo: idk why this shows in a random(?) order in the config, please help
    enum class DungeonFloorWithBoss(val isMaster: Boolean = false) {
        F1,
        F2,
        F3,
        F4,
        F5,
        F6,
        F7,
        M1(true),
        M2(true),
        M3(true),
        M4(true),
        M5(true),
        M6(true),
        M7(true),
        ;

        fun isEnabled() = SkyHanniMod.feature.dungeon.bossReplay.enabledFloors.contains(this)

        override fun toString(): String {
            return buildString {
                if (isMaster) {
                    append("§cMaster Mode ")
                }

                append("§rFloor " + name.substring(1))
            }
        }

        companion object {
            fun findByStringOrNull(floorName: String): DungeonFloorWithBoss? {
                return entries.find { it.name == floorName }
            }
        }
    }
}
