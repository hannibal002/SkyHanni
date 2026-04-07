package at.hannibal2.skyhanni.utils

import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.PatchedDataComponentMap

internal class DeferredPatchedDataComponentMap : PatchedDataComponentMap(EMPTY) {

    var realDelegate: PatchedDataComponentMap? = null

    override fun asPatch(): DataComponentPatch = realDelegate?.asPatch() ?: super.asPatch()
    override fun applyPatch(patch: DataComponentPatch) = realDelegate?.applyPatch(patch) ?: super.applyPatch(patch)
    override fun restorePatch(patch: DataComponentPatch) = realDelegate?.restorePatch(patch) ?: super.restorePatch(patch)
    override fun copy(): PatchedDataComponentMap = realDelegate?.copy() ?: super.copy()
    override fun setAll(components: DataComponentMap) = realDelegate?.setAll(components) ?: super.setAll(components)
    override fun toImmutableMap(): DataComponentMap = realDelegate?.toImmutableMap() ?: super.toImmutableMap()
    override fun hasNonDefault(type: DataComponentType<*>) = realDelegate?.hasNonDefault(type) ?: super.hasNonDefault(type)
    @Suppress("OVERRIDE_BY_INLINE")
    override fun <T : Any> get(type: DataComponentType<out T>): T? = realDelegate?.get(type) ?: super.get(type)
    override fun has(type: DataComponentType<*>) = realDelegate?.has(type) ?: super.has(type)
    override fun size() = realDelegate?.size() ?: super.size()
    override fun keySet(): Set<DataComponentType<*>> = realDelegate?.keySet() ?: super.keySet()
    override fun equals(other: Any?) = realDelegate?.equals(other) ?: super.equals(other)
    override fun hashCode() = realDelegate?.hashCode() ?: super.hashCode()
}
