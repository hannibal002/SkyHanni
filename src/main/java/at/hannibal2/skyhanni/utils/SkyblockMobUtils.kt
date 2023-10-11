package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.features.mobs.EntityKill
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand

object SkyblockMobUtils {
    val mobNameFilter = "\\[.*\\] (.*) \\d+".toRegex()
    class SkyblockMob(val baseEntity: Entity) {
        //Fun Fact the corresponding ArmorStand for a mob has always the mobId + 1
        val armorStand = EntityUtils.getEntityById(baseEntity.entityId+1)

        val name: String = armorStand?.name?.let { mobNameFilter.find(it.removeColor())?.groupValues?.get(1) }
            ?: "Skyblock Name of Mob ${baseEntity.name} found"

        override fun toString() : String = name
    }

    fun isSkillBlockMob(entity: Entity): Boolean {
        if(entity !is EntityLivingBase) return false
        if (entity is EntityArmorStand) return false
        //Protection that no real Player gets added. Only difference to a custom mob is that every Skyblockitem has a nbtTag
        if (entity.inventory != null) {
            if (entity.inventory.isNotEmpty() && entity.inventory.any { it != null && it.tagCompound == null })
            {
                if(EntityKill.config.mobKilldetetctionLogPlayerCantBeAdded) {
                    LorenzDebug.log("Entity ${entity.name} is not allowed in HitList")
                }
                return false
            }
        }
        return true
    }

}