package at.hannibal2.skyhanni.data.skyblockentities

import net.minecraft.entity.Entity

class DisplayNPC(baseEntity: Entity) : SkyblockEntity(baseEntity) {
    override val name = armorStand?.name ?: ""
}
