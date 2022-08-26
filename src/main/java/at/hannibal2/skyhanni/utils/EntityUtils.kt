package at.hannibal2.skyhanni.utils

import net.minecraft.entity.EntityLiving
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.AxisAlignedBB

object EntityUtils {

    fun EntityLiving.hasNameTagWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): Boolean {
        return getNameTagWith(y, contains, debugRightEntity, inaccuracy, debugWrongEntity) != null
    }

    fun EntityLiving.getNameTagWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): EntityArmorStand? {
        val center = getLorenzVec().add(0, y, 0)
        val a = center.add(-inaccuracy, -inaccuracy - 3, -inaccuracy).toBlocPos()
        val b = center.add(inaccuracy, inaccuracy + 3, inaccuracy).toBlocPos()
        val alignedBB = AxisAlignedBB(a, b)
        val clazz = EntityArmorStand::class.java
        val found = worldObj.getEntitiesWithinAABB(clazz, alignedBB)
        return found.find {
            val result = it.name.contains(contains)
            if (debugWrongEntity && !result) {
                println("wrong entity in aabb: '" + it.name + "'")
            }
            if (debugRightEntity && result) {
                println("mob: " + center.printWithAccuracy(2))
                println("nametag: " + it.getLorenzVec().printWithAccuracy(2))
                println("accuracy: " + it.getLorenzVec().subtract(center).printWithAccuracy(3))
            }
            result
        }
    }
}