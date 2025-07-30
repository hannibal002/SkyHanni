package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.entity.Entity

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
