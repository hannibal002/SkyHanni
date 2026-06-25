package at.hannibal2.skyhanni.config.features.garden.leaderboards.rankgoals

import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.TypeRankGoalGenericConfig
import at.hannibal2.skyhanni.features.foraging.ForagingLogType
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KProperty0

class ForagingLogTypeRankGoalsConfig : TypeRankGoalGenericConfig<ForagingLogType>() {
    @Expose
    @ConfigOption(name = "Oak Log", desc = "")
    @ConfigEditorText
    val oak: Property<String> = Property.of("10000")
    @Expose
    @ConfigOption(name = "Spruce Log", desc = "")
    @ConfigEditorText
    val spruce: Property<String> = Property.of("10000")
    @Expose
    @ConfigOption(name = "Birch Log", desc = "")
    @ConfigEditorText
    val birch: Property<String> = Property.of("10000")
    @Expose
    @ConfigOption(name = "Jungle Log", desc = "")
    @ConfigEditorText
    val jungle: Property<String> = Property.of("10000")
    @Expose
    @ConfigOption(name = "Acacia Log", desc = "")
    @ConfigEditorText
    val acacia: Property<String> = Property.of("10000")
    @Expose
    @ConfigOption(name = "Dark Oak Log", desc = "")
    @ConfigEditorText
    val darkOak: Property<String> = Property.of("10000")
    @Expose
    @ConfigOption(name = "Mangrove Log", desc = "")
    @ConfigEditorText
    val mangrove: Property<String> = Property.of("10000")
    @Expose
    @ConfigOption(name = "Fig Log", desc = "")
    @ConfigEditorText
    val fig: Property<String> = Property.of("10000")
    override fun getConfig(type: ForagingLogType): KProperty0<Property<String>> = when (type) {
        ForagingLogType.OAK -> this::oak
        ForagingLogType.SPRUCE -> this::spruce
        ForagingLogType.BIRCH -> this::birch
        ForagingLogType.JUNGLE -> this::jungle
        ForagingLogType.ACACIA -> this::acacia
        ForagingLogType.DARK_OAK -> this::darkOak
        ForagingLogType.MANGROVE -> this::mangrove
        ForagingLogType.FIG -> this::fig
    }
}
