package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase

object TrevorSolver {
    private var foundID = -1
    var mobLocation = CurrentMobArea.NONE
    var mobCoordinates = LorenzVec(0.0, 0.0, 0.0)

    fun resetLocation() {
        foundID = -1
    }

    fun findMob() {
        val world = Minecraft.getMinecraft().theWorld ?: return
        for (entity in world.getLoadedEntityList()) {
            val name = entity.name
            val entityHealth = if (entity is EntityLivingBase) entity.baseMaxHealth else 0
            if (intArrayOf(100, 500, 1000, 5000, 10000).any { it == entityHealth } ) {
                if (arrayOf("Cow", "Horse", "Sheep", "Pig", "Rabbit", "Chicken").any { it == name }) {
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

    enum class CurrentMobArea(val location: String, val coordinates: LorenzVec) {
        OASIS("Oasis", LorenzVec(171.0, 85.0, -470.0)),
        GORGE("Mushroom Gorge", LorenzVec(300.0, 80.0, -509.0)),
        OVERGROWN("Overgrown Mushroom Cave", LorenzVec(242.0, 60.0, -389.0)),
        SETTLEMENT("Desert Settlement", LorenzVec(184.0, 86.0, -384.0)),
        GLOWING("Glowing Mushroom Cave", LorenzVec(199.0, 50.0, -512.0)),
        MOUNTAIN("Desert Mountain", LorenzVec(255.0, 148.0, -518.0)),
        FOUND("Mob Guess", LorenzVec(0.0, 0.0, 0.0)),
        NONE("   ", LorenzVec(0.0, 0.0, 0.0)),
    }
}
