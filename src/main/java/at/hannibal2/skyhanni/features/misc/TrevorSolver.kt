package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import net.minecraft.client.Minecraft
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
        val playerPosition = Minecraft.getMinecraft().thePlayer.getLorenzVec().toAccurateVec(2)
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
            // looking at 2 diff entities rn
            val entityHealth = if (entity is EntityLivingBase) entity.baseMaxHealth else 0
            if (intArrayOf(100, 500, 1000, 5000, 10000).any { it == entityHealth } ) {
                if (animalNames.any { it == name }) {
                    if (LocationUtils.canSee(LocationUtils.playerLocation(), entity.position.toLorenzVec())  ) {
                        if (entity.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer)) return

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

            if (entity is EntityArmorStand) {
                for (animal in animalNames){
                    if (name.contains(animal)) {
                        if (foundID == entity.entityId) {
                            mobLocation = CurrentMobArea.FOUND
                            mobCoordinates = entity.position.toLorenzVec()
                        } else {
                            foundID = entity.entityId
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

    enum class CurrentMobArea(val location: String, val coordinates: LorenzVec) {
        OASIS("Oasis", LorenzVec(126.0, 77.0, -456.0)),
        GORGE("Mushroom Gorge", LorenzVec(300.0, 80.0, -509.0)),
        OVERGROWN("Overgrown Mushroom Cave", LorenzVec(242.0, 60.0, -389.0)),
        SETTLEMENT("Desert Settlement", LorenzVec(184.0, 86.0, -384.0)),
        GLOWING("Glowing Mushroom Cave", LorenzVec(199.0, 50.0, -512.0)),
        MOUNTAIN("Desert Mountain", LorenzVec(255.0, 148.0, -518.0)),
        FOUND("Mob Location", LorenzVec(0.0, 0.0, 0.0)),
        NONE("   ", LorenzVec(0.0, 0.0, 0.0)),
    }
}
