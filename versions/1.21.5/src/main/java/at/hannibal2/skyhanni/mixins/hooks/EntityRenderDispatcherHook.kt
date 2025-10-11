package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.entity.Entity
//#if MC > 1.21.7
//$$ import net.minecraft.client.render.entity.state.EntityRenderState
//#endif

private var savedEntity: Entity? = null

//#if MC < 1.21.9
fun setEntity(entity: Entity?) {
    savedEntity = entity
}
//#else
//$$ fun setEntity(state: EntityRenderState?) {
//$$     savedEntity = (state as EntityRenderStateStore).`skyhanni$getEntity`()
//$$ }
//#endif

fun getEntity(): Entity? {
    return savedEntity
}

fun clearEntity() {
    savedEntity = null
}
