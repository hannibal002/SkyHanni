package at.hannibal2.skyhanni.config.core.dependency

import kotlin.reflect.KMutableProperty1

/** Represents a toggleable integration that can back feature requirements. */
interface FeatureDependencyToggle {
    val id: String
    val displayName: String
    val description: String
    val toggleField: KMutableProperty1<out Any, Boolean>?
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
}
