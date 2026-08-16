package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.entity.EntityTransparencyManager
import at.hannibal2.skyhanni.utils.EntityUtils
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

object SkyHanniRenderStateData {
    private const val NO_ENTITY_ID = -1
    private const val NO_ENTITY_TRANSPARENCY = -1

    private val ENTITY_ID: RenderStateDataKey<Int> =
        RenderStateDataKey.create { "skyhanni:entity_id" }

    private val ENTITY_TRANSPARENCY: RenderStateDataKey<Int> =
        RenderStateDataKey.create { "skyhanni:entity_transparency" }

    private val USING_CUSTOM_OUTLINE: RenderStateDataKey<Boolean> =
        RenderStateDataKey.create { "skyhanni:using_custom_outline" }

    @JvmStatic
    fun setFromEntity(state: EntityRenderState, entity: Entity) {
        state.setData(ENTITY_ID, entity.id)
        setUsingCustomOutline(state, RenderLivingEntityHelper.getEntityGlowColor(entity) != null)
        val transparency = (entity as? LivingEntity)?.let { EntityTransparencyManager.getEntityTransparency(it) }
        state.setData(ENTITY_TRANSPARENCY, transparency ?: NO_ENTITY_TRANSPARENCY)
    }

    @JvmStatic
    fun clearEntityId(state: EntityRenderState) {
        state.setData(ENTITY_ID, NO_ENTITY_ID)
    }

    @JvmStatic
    fun getEntity(state: EntityRenderState): Entity? {
        val entityId = state.getDataOrDefault(ENTITY_ID, NO_ENTITY_ID)
        if (entityId == NO_ENTITY_ID) return null
        return EntityUtils.getEntityByID(entityId)
    }

    @JvmStatic
    fun getEntityTransparency(state: EntityRenderState): Int? {
        val alpha = state.getDataOrDefault(ENTITY_TRANSPARENCY, NO_ENTITY_TRANSPARENCY)
        if (alpha == NO_ENTITY_TRANSPARENCY) return null
        return alpha
    }

    @JvmStatic
    fun setEntityTransparency(state: EntityRenderState, alpha: Int) {
        state.setData(ENTITY_TRANSPARENCY, alpha.coerceIn(0, 255))
    }

    @JvmStatic
    fun isUsingCustomOutline(state: EntityRenderState): Boolean = state.getDataOrDefault(USING_CUSTOM_OUTLINE, false)

    @JvmStatic
    fun setUsingCustomOutline(state: EntityRenderState, usingCustomOutline: Boolean) {
        state.setData(USING_CUSTOM_OUTLINE, usingCustomOutline)
    }
}
