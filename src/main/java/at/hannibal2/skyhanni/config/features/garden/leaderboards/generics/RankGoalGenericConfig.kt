package at.hannibal2.skyhanni.config.features.garden.leaderboards.generics

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

open class RankGoalGenericConfig {

    @Expose
    @ConfigOption(
        name = "Use Rank Goal",
        desc = "Use the Rank Goal number instead of the next upcoming rank. Useful when your rank is in the " +
            "ten thousands and you don't want to see small ETAs.",
    )
    @ConfigEditorBoolean
    val useRankGoal: Property<Boolean> = Property.of(true)
}
