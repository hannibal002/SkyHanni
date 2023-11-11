package at.hannibal2.skyhanni.data.skyblockentities

import at.hannibal2.skyhanni.utils.SkyblockMobUtils.getArmorStandByRangeAll
import net.minecraft.entity.EntityLivingBase

class DisplayNPC(baseEntity: EntityLivingBase) : SkyblockEntity(baseEntity, null) {
    override val armorStand
        get() = getArmorStandByRangeAll(baseEntity, 1.0).firstOrNull { !it.name.startsWith("§e§lCLICK") }
    override val name get() = armorStand?.name ?: ""
}
