package at.hannibal2.skyhanni.config.features.mining.nucleus

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.HasLegacyId
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class PowderTrackerConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the Powder Tracker overlay for mining.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Only when Grinding", desc = "Only show the overlay when powder grinding.")
    @ConfigEditorBoolean
    var onlyWhenPowderGrinding: Boolean = false

    @Expose
    @ConfigOption(name = "Text Format", desc = "Drag text to change the appearance of the overlay.")
    @ConfigEditorDraggableList
    val textFormat: Property<MutableList<PowderDisplayEntry>> = Property.of(
        mutableListOf(
            PowderDisplayEntry.TOTAL_CHESTS,
            PowderDisplayEntry.DOUBLE_POWDER,
            PowderDisplayEntry.GEMSTONE_POWDER,
            PowderDisplayEntry.SPACER_1,
            PowderDisplayEntry.DIAMOND_ESSENCE,
            PowderDisplayEntry.GOLD_ESSENCE,
            PowderDisplayEntry.SPACER_2,
            PowderDisplayEntry.HARD_STONE,
            PowderDisplayEntry.SPACER_3,
            PowderDisplayEntry.RUBY,
            PowderDisplayEntry.SAPPHIRE,
            PowderDisplayEntry.AMBER,
            PowderDisplayEntry.AMETHYST,
            PowderDisplayEntry.JADE,
            PowderDisplayEntry.TOPAZ,
            PowderDisplayEntry.FTX,
            PowderDisplayEntry.ELECTRON,
            PowderDisplayEntry.ROBOTRON
        )
    )

    enum class PowderDisplayEntry(
        private val displayName: String,
        private val legacyId: Int = -1
    ) : HasLegacyId {
        TOTAL_CHESTS("§d852 Total chests Picked §7(950/h)", 2),
        DOUBLE_POWDER("§bx2 Powder: §aActive!", 3),
        GEMSTONE_POWDER("§b250,420 §dGemstone Powder §7(350,000/h)", 5),
        SPACER_1("", 6),
        DIAMOND_ESSENCE("§b129 §bDiamond Essence §7(600/h)", 7),
        GOLD_ESSENCE("§b234 §6Gold Essence §7(700/h)", 8),
        SPACER_2("", 9),
        HARD_STONE("§b1000 §fHard Stone §bCompacted §7(500/h)"),
        SPACER_3(""),
        RUBY("§50§7-§90§7-§a0§f-0 §cRuby Gemstone", 10),
        SAPPHIRE("§50§7-§90§7-§a0§f-0 §bSapphire Gemstone", 11),
        AMBER("§50§7-§90§7-§a0§f-0 §6Amber Gemstone", 12),
        AMETHYST("§50§7-§90§7-§a0§f-0 §5Amethyst Gemstone", 13),
        JADE("§50§7-§90§7-§a0§f-0 §aJade Gemstone", 14),
        TOPAZ("§50§7-§90§7-§a0§f-0 §eTopaz Gemstone", 15),
        FTX("§b14 §9FTX 3070", 16),
        ELECTRON("§b14 §9Electron Transmitter", 17),
        ROBOTRON("§b14 §9Robotron Reflector", 18),
        SUPERLITE("§b14 §9Superlite Motor", 19),
        CONTROL_SWITCH("§b14 §9Control Switch", 20),
        SYNTHETIC_HEART("§b14 §9Synthetic Heart", 21),
        TOTAL_ROBOT_PARTS("§b14 §9Total Robot Parts", 22),
        GOBLIN_EGGS("§30§7-§c0§7-§e0§f-§a0§f-§90 §fGoblin Egg", 23),
        WISHING_COMPASS("§b12 §aWishing Compass", 24),
        SLUDGE_JUICE("§b320 §aSludge Juice", 25),
        ASCENSION_ROPE("§b2 §9Ascension Rope", 26),
        TREASURITE("§b6 §5Treasurite", 27),
        JUNGLE_HEART("§b4 §6Jungle Heart", 28),
        PICKONIMBUS("§b1 §5Pickonimbus 2000", 29),
        YOGGIE("§b14 §aYoggie", 30),
        PREHISTORIC_EGG("§b9 §fPrehistoric Egg", 31),
        OIL_BARREL("§b25 §aOil Barrel", 32),
        ;

        override fun getLegacyId() = legacyId
        override fun toString() = displayName
    }

    @Expose
    @ConfigLink(owner = PowderTrackerConfig::class, field = "enabled")
    val position: Position = Position(-274, 0)
}
