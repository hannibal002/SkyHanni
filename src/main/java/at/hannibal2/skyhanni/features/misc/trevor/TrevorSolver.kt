package at.hannibal2.skyhanni.features.misc.trevor

import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand

object TrevorSolver {
    private val animalNames = arrayOf("Cow", "Horse", "Sheep", "Pig", "Rabbit", "Chicken")

    private var maxHeight: Double = 0.0
    private var minHeight: Double = 0.0
    private var foundID = -1
    var mobCoordinates = LorenzVec(0.0, 0.0, 0.0)
    var mobLocation = CurrentMobArea.NONE
    var averageHeight = (minHeight + maxHeight) / 2

    fun findMobHeight(height: Int, above: Boolean) {
        val playerPosition = LocationUtils.playerLocation().round(2)
        val mobHeight = if (above) playerPosition.y + height else playerPosition.y - height
        if (maxHeight == 0.0) {

            maxHeight = mobHeight + 2.5
            minHeight = mobHeight - 2.5
        } else {
            if (mobHeight + 2.5 in minHeight..maxHeight) {
                maxHeight = mobHeight + 2.5
            } else if (mobHeight - 2.5 in minHeight..maxHeight) {
                minHeight = mobHeight - 2.5
            } else {
                maxHeight = mobHeight + 2.5
                minHeight = mobHeight - 2.5
            }
        }
        averageHeight = (minHeight + maxHeight) / 2
    }

    fun findMob() {
        val world = Minecraft.getMinecraft().theWorld ?: return
        for (entity in world.getLoadedEntityList()) {
            val name = entity.name
            if (entity !is EntityOtherPlayerMP) {
                // looking at 2 diff entities rn - Mostly fixed I think as it returns
                val entityHealth = if (entity is EntityLivingBase) entity.baseMaxHealth else 0
                if (intArrayOf(100, 500, 1000, 5000, 10000).any { it == entityHealth }) {
                    if (animalNames.any { it == name }) {
                        if (LocationUtils.canSee(LocationUtils.playerLocation(), entity.position.toLorenzVec())) {
                            if (!entity.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer)) {
                                if (foundID == entity.entityId) {
                                    mobLocation = CurrentMobArea.FOUND
                                    mobCoordinates = entity.position.toLorenzVec()
                                } else {
                                    foundID = entity.entityId
                                }
                                return
                            }
                        }
                    }
                }

                if (entity is EntityArmorStand) {
                    for (animal in animalNames) {
                        if (name.contains(animal)) {
                            if (foundID == entity.entityId) {
                                mobLocation = CurrentMobArea.FOUND
                                mobCoordinates = entity.position.toLorenzVec()
                            } else {
                                foundID = entity.entityId
                            }
                            return
                        }
                    }
                }
            }
        }
    }

    fun resetLocation() {
        maxHeight = 0.0
        minHeight = 0.0
        averageHeight = (minHeight + maxHeight) / 2
        foundID = -1
        mobCoordinates = LorenzVec(0.0, 0.0, 0.0)
    }

}
