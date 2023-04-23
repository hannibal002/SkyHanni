package at.hannibal2.skyhanni.config.migration

import java.lang.reflect.Field

sealed class ResolutionPath {
    abstract val parent: ResolutionPath?
    abstract val label: String
    fun path(): String = (parent?.path()?.let { "$it." } ?: "") + label

    object Root : ResolutionPath() {
        override val parent: ResolutionPath? get() = null
        override val label: String get() = "Root"
    }

    class FieldChild(val field: Field, override val parent: ResolutionPath) : ResolutionPath() {
        override val label: String = field.name
    }

    class IndirectChild(override val label: String, override val parent: ResolutionPath) : ResolutionPath()

    override fun toString(): String {
        return path()
    }
}