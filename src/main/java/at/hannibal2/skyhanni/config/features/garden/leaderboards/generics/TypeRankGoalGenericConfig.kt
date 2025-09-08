package at.hannibal2.skyhanni.config.features.garden.leaderboards.generics

import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.reflect.KProperty0

abstract class TypeRankGoalGenericConfig<E : Enum<E>> {
    fun getGoal(type: E): Property<String> = getConfig(type).get()
    abstract fun getConfig(type: E): KProperty0<Property<String>>
}
