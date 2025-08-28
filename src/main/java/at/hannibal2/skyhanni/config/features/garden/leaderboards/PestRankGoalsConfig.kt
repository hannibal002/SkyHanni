package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.features.garden.pests.PestType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KProperty0

class PestRankGoalsConfig(
    private val mode: EliteLeaderboardMode
) {
    @Expose
    @ConfigOption(name = "All pests", desc = "")
    @ConfigEditorText
    val allPests: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Field Mouse", desc = "")
    @ConfigEditorText
    val fieldMouse: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Fly", desc = "")
    @ConfigEditorText
    val fly: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Cricket", desc = "")
    @ConfigEditorText
    val cricket: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Locust", desc = "")
    @ConfigEditorText
    val locust: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Beetle", desc = "")
    @ConfigEditorText
    val beetle: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Rat", desc = "")
    @ConfigEditorText
    val rat: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Earthworm", desc = "")
    @ConfigEditorText
    val earthworm: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Moth", desc = "")
    @ConfigEditorText
    val moth: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Mosquito", desc = "")
    @ConfigEditorText
    val mosquito: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Mite", desc = "")
    @ConfigEditorText
    val mite: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Slug", desc = "")
    @ConfigEditorText
    val slug: Property<String> = Property.of("10000")

    val goalMap: Map<PestType?, KProperty0<Property<String>>> = mapOf(
        PestType.FLY to this::fly,
        PestType.CRICKET to this::cricket,
        PestType.LOCUST to this::locust,
        PestType.BEETLE to this::beetle,
        PestType.RAT to this::rat,
        PestType.EARTHWORM to this::earthworm,
        PestType.MOTH to this::moth,
        PestType.MOSQUITO to this::mosquito,
        PestType.MITE to this::mite,
        PestType.SLUG to this::slug,
        PestType.FIELD_MOUSE to this::fieldMouse,
        null to this::allPests
    )
}
