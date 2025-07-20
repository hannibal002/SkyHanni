package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DungeonFilterConfig {

    @Expose
    @ConfigOption(name = "Dungeon Filters", desc = "Hide specific message types in Dungeons.")
    @ConfigEditorDraggableList
    val dungeonFilteredMessageTypes: MutableList<DungeonMessageTypes> = mutableListOf()

    @Expose
    @ConfigOption(name = "Fairy Dialogue", desc = "Hide the dialogue when a fairy is killed.")
    @ConfigEditorBoolean
    @FeatureToggle
    var fairy: Boolean = false

    @Expose
    @ConfigOption(
        name = "Keys and Doors",
        desc = "Hide the chat message when picking up keys or opening doors in Dungeons."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var keysAndDoors: Boolean = false

    @Expose
    @ConfigOption(name = "Rare Drops", desc = "Hide the chat message when other players get rare drops from chests.")
    @ConfigEditorBoolean
    @FeatureToggle
    var rareDrops: Boolean = false

    @Expose
    @ConfigOption(name = "Solo Class", desc = "Hide the message that sends when you play a class alone.")
    @ConfigEditorBoolean
    @FeatureToggle
    var soloClass: Boolean = false

    @Expose
    @ConfigOption(name = "Solo Class Stats", desc = "Hide the boosted class stats when starting a dungeon.")
    @ConfigEditorBoolean
    @FeatureToggle
    var soloStats: Boolean = false

    @Expose
    @ConfigOption(name = "Dungeon Boss Messages", desc = "Hide messages from the Watcher and bosses in Dungeons.")
    @ConfigEditorBoolean
    @FeatureToggle
    var dungeonBossMessages: Boolean = false

    enum class DungeonMessageTypes(private val displayName: String) {
        PREPARE("§bPreparation"),
        START("§aClass Buffs §r/ §cMort Dialogue"),
        AMBIENCE("§bAmbience"),
        PICKUP("§ePickup"),
        REMINDER("§cReminder"),
        BUFF("§dBlessings"),
        NOT_POSSIBLE("§cNot possible"),
        DAMAGE("§cDamage"),
        ABILITY("§dAbilities"),
        PUZZLE("§dPuzzle §r/ §cQuiz"),
        END("§cEnd §a(End of run spam)"),
        ;

        override fun toString() = displayName
    }
}
