package at.hannibal2.skyhanni.config.features.garden.leaderboards

import at.hannibal2.skyhanni.features.garden.pests.PestType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KProperty0

class PestRankGoalsConfig {
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

    fun getGoal(type: PestType?): KProperty0<Property<String>> = when (type) {
        PestType.FLY -> this::fly
        PestType.CRICKET -> this::cricket
        PestType.LOCUST -> this::locust
        PestType.BEETLE -> this::beetle
        PestType.RAT -> this::rat
        PestType.EARTHWORM -> this::earthworm
        PestType.MOTH -> this::moth
        PestType.MOSQUITO -> this::mosquito
        PestType.MITE -> this::mite
        PestType.SLUG -> this::slug
        PestType.FIELD_MOUSE -> this::fieldMouse
        else -> this::allPests
    }
}
