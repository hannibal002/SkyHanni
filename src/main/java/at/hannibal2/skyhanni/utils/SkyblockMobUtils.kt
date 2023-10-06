package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.Entity

object SkyblockMobUtils {
    val mobNameFilter = "\\[.*\\] (.*) \\d+".toRegex()
    class SkyblockMob(val baseEntity: Entity) {
        //Fun Fact the corresponding ArmorStand for a mob has always the mobId + 1
        val armorStand = EntityUtils.getEntityById(baseEntity.entityId+1)

        val name: String = armorStand?.name?.let { mobNameFilter.find(it.removeColor())?.groupValues?.get(1) }
            ?: "Skyblock Name of Mob ${baseEntity.name} found"

        override fun toString() : String = name
    }

}