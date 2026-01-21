package at.hannibal2.skyhanni.mixins.hooks
//? < 1.21.10 {
import net.minecraft.world.entity.Entity

private var savedEntity: Entity? = null

fun setEntity(entity: Entity?) {
    savedEntity = entity
}

fun getEntity(): Entity? {
    return savedEntity
}

fun clearEntity() {
    savedEntity = null
}
//?} else {
/*import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.world.entity.Entity

private var savedEntity: Entity? = null
private var savedEntityRenderState: EntityRenderStateStore? = null

fun setEntity(state: EntityRenderState?) {
    if (state !is EntityRenderStateStore) return
    savedEntity = state.`skyhanni$getEntity`()
    savedEntityRenderState = state
}

fun getEntity(): Entity? {
    return savedEntity
}

fun getEntityRenderState(): EntityRenderStateStore? {
    return savedEntityRenderState
}

fun clearEntity() {
    savedEntity = null
    savedEntityRenderState = null
}
*///?}
