package at.hannibal2.skyhanni.config.features.garden.leaderboards.rankgoals

import at.hannibal2.skyhanni.config.features.garden.leaderboards.PestTypeWithAll
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.TypeRankGoalGenericConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KProperty0

class PestTypeRankGoalsConfig : TypeRankGoalGenericConfig<PestTypeWithAll>() {

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

    @Expose
    @ConfigOption(name = "Dragonfly", desc = "")
    @ConfigEditorText
    val dragonfly: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Firefly", desc = "")
    @ConfigEditorText
    val firefly: Property<String> = Property.of("10000")

    @Expose
    @ConfigOption(name = "Praying Mantis", desc = "")
    @ConfigEditorText
    val mantis: Property<String> = Property.of("10000")

    override fun getConfig(type: PestTypeWithAll): KProperty0<Property<String>> = when (type) {
        PestTypeWithAll.FLY -> this::fly
        PestTypeWithAll.CRICKET -> this::cricket
        PestTypeWithAll.LOCUST -> this::locust
        PestTypeWithAll.BEETLE -> this::beetle
        PestTypeWithAll.RAT -> this::rat
        PestTypeWithAll.EARTHWORM -> this::earthworm
        PestTypeWithAll.MOTH -> this::moth
        PestTypeWithAll.MOSQUITO -> this::mosquito
        PestTypeWithAll.MITE -> this::mite
        PestTypeWithAll.SLUG -> this::slug
        PestTypeWithAll.FIELD_MOUSE -> this::fieldMouse
        PestTypeWithAll.DRAGONFLY -> this::dragonfly
        PestTypeWithAll.FIREFLY -> this::firefly
        PestTypeWithAll.PRAYING_MANTIS -> this::mantis
        else -> this::allPests
    }
}
