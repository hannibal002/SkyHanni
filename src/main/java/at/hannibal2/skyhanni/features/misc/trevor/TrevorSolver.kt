package at.hannibal2.skyhanni.features.misc.trevor

import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.derpy
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import kotlin.time.Duration.Companion.seconds

object TrevorSolver {
    private val animalHealths = intArrayOf(100, 200, 500, 1000, 2000, 5000, 10000, 30000)

    var currentMob: TrevorMobs? = null
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
        var canSee = false
        Minecraft.getMinecraft().theWorld ?: return
        for (entity in EntityUtils.getAllEntities()) {
            if (entity is EntityOtherPlayerMP) continue
            val name = entity.name
            val entityHealth = if (entity is EntityLivingBase) entity.baseMaxHealth.derpy() else 0
            currentMob = TrevorMobs.entries.firstOrNull { it.mobName.contains(name) }
            if (animalHealths.any { it == entityHealth } && currentMob != null) {
                if (foundID == entity.entityId) {
                    val dist = entity.position.toLorenzVec().distanceToPlayer()
                    if ((currentMob == TrevorMobs.RABBIT || currentMob == TrevorMobs.SHEEP) && mobLocation == CurrentMobArea.OASIS) {
                        println("This is unfortunate")
                    } else canSee = LocationUtils.canSee(
                        LocationUtils.playerEyeLocation(),
                        entity.position.toLorenzVec().add(0.0, 0.5, 0.0)
                    ) && dist < currentMob!!.renderDistance

                    if (!canSee) {
                        val nameTagEntity = Minecraft.getMinecraft().theWorld.getEntityByID(foundID + 1)
                        if (nameTagEntity is EntityArmorStand) canSee = true
                    }
                    if (canSee) {
                        if (mobLocation != CurrentMobArea.FOUND) {
                            LorenzUtils.sendTitle("§2Saw ${currentMob!!.mobName}!", 3.seconds)
                        }
                        mobLocation = CurrentMobArea.FOUND
                        mobCoordinates = entity.position.toLorenzVec()
                    }
                } else {
                    foundID = entity.entityId
                }
                return
            }
        }
        if (foundID != -1) {
            println("Cannot find mob anymore")
            mobCoordinates = LorenzVec(0.0, 0.0, 0.0)
            foundID = -1
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
