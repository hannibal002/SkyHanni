package at.hannibal2.skyhanni.config.features.chat

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

data class SlayerFilterDropsConfig(
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable filtering of Slayer drop messages.")
    @ConfigEditorBoolean
    val enabled: Boolean = true,

    @Expose
    @ConfigOption(name = "Zombie Slayer Common Items", desc = "Hide reward messages for listed Zombie Slayer items.")
    @ConfigEditorDraggableList
    val simpleZombieSlayerTypes: MutableList<SimpleZombieSlayerRewardTypes> = mutableListOf(),

    @Expose
    @ConfigOption(name = "Tarantula Slayer Common Items", desc = "Hide reward messages for listed Tarantula Slayer items.")
    @ConfigEditorDraggableList
    val simpleTarantulaSlayerTypes: MutableList<SimpleTarantulaSlayerRewardTypes> = mutableListOf(),

    @Expose
    @ConfigOption(name = "Enderman Slayer Common Items", desc = "Hide reward messages for listed Enderman Slayer items.")
    @ConfigEditorDraggableList
    val simpleEndermanSlayerTypes: MutableList<SimpleEndermanSlayerRewardTypes> = mutableListOf(),

    @Expose
    @ConfigOption(name = "Blaze Slayer Common Items", desc = "Hide reward messages for listed Blaze Slayer items.")
    @ConfigEditorDraggableList
    val simpleBlazeSlayerTypes: MutableList<SimpleBlazeSlayerRewardTypes> = mutableListOf(),
) {
    enum class SimpleZombieSlayerRewardTypes(val displayName: String) {
        REVENANT_VISCERA("§9Revenant Viscera"),
        FOUL_FLESH("§9Foul Flesh"),
        GOLDEN_POWDER("§5Golden Powder"),
        PESTILENCE_RUNE("§2Pestilence Rune I"),
        REVENANT_CATALYST("§5Revenant Catalyst"),
        UNDEAD_CATALYST("§9Undead Catalyst");

        override fun toString() = displayName
    }

    enum class SimpleTarantulaSlayerRewardTypes(val displayName: String) {
        ARACHNES_KEEPER_FRAGMENT("§9Arachne's Keeper Fragment"),
        TRAVEL_SCROLL("§5Travel Scroll to Spider's Den Top of Nest"),
        BITE_RUNE("§aBite Rune I"),
        TOXIC_ARROW_POISON("§aToxic Arrow Poison"),
        BANE_OF_ARTHROPODS("§9Bane of Arthropods VI");

        override fun toString() = displayName
    }

    enum class SimpleEndermanSlayerRewardTypes(val displayName: String) {
        TWILIGHT_ARROW_POISON("§aTwilight Arrow Poison"),
        MANA_STEAL("§fMana Steal I"),
        SINFUL_DICE("§5Sinful Dice"),
        NULL_ATOM("§9Null Atom"),
        TRANSMISSION_TUNER("§5Transmission Tuner"),
        ENDERSNAKE_RUNE("§5Endersnake Rune I"),
        POCKET_ESPRESSO_MACHINE("§fPocket Espresso Machine"),
        END_RUNE("§5End Rune I"),
        HAZMAT_ENDERMAN("§6Hazmat Enderman");

        override fun toString() = displayName
    }

    enum class SimpleBlazeSlayerRewardTypes(val displayName: String) {
        WISPS_ICE_FLAVORED_WATER("§fWisp's Ice-Flavored Water I"),
        MAGMA_ARROWS("§5Bundle of Magma Arrows"),
        DISTILLATE("§9Distillate");

        override fun toString() = displayName
    }
}
