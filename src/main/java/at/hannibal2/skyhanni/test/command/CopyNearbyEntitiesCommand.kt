package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntityMagmaCube

object CopyNearbyEntitiesCommand {

    fun command(args: Array<String>) {
        var searchRadius = 10
        if (args.size == 1) {
            searchRadius = args[0].toInt()
        }

        val minecraft = Minecraft.getMinecraft()
        val start = LocationUtils.playerLocation()
        val world = minecraft.theWorld

        val resultList = mutableListOf<String>()
        var counter = 0

        for (entity in world.loadedEntityList) {
            val position = entity.position
            val vec = position.toLorenzVec()
            val distance = start.distance(vec)
            if (distance < searchRadius) {
                resultList.add("found entity: '" + entity.name + "'")
                val displayName = entity.displayName
                resultList.add("displayName: '${displayName.formattedText}'")
                val simpleName = entity.javaClass.simpleName
                resultList.add("simpleName: $simpleName")
                resultList.add("vec: $vec")
                resultList.add("distance: $distance")

                val rotationYaw = entity.rotationYaw
                val rotationPitch = entity.rotationPitch
                resultList.add("rotationYaw: $rotationYaw")
                resultList.add("rotationPitch: $rotationPitch")

                val riddenByEntity = entity.riddenByEntity
                resultList.add("riddenByEntity: $riddenByEntity")
                val ridingEntity = entity.ridingEntity
                resultList.add("ridingEntity: $ridingEntity")


                when (entity) {
                    is EntityArmorStand -> {
                        resultList.add("armor stand data:")
                        val headRotation = entity.headRotation.toLorenzVec()
                        val bodyRotation = entity.bodyRotation.toLorenzVec()
                        resultList.add("headRotation: $headRotation")
                        resultList.add("bodyRotation: $bodyRotation")

                        for ((id, stack) in entity.inventory.withIndex()) {
                            resultList.add("id $id = $stack")
                            if (stack != null) {
                                val skullTexture = stack.getSkullTexture()
                                if (skullTexture != null) {
                                    resultList.add("skullTexture: $skullTexture")
                                }
                                val cleanName = stack.cleanName()
                                val type = stack.javaClass.name
                                resultList.add("cleanName: $cleanName")
                                resultList.add("type: $type")

                            }
                        }
                    }

                    is EntityEnderman -> {
                        val heldItem = entity.heldItem
                        resultList.add("enderman heldItem: $heldItem")
                    }

                    is EntityMagmaCube -> {
                        val squishFactor = entity.squishFactor
                        val slimeSize = entity.slimeSize
                        resultList.add("factor: $squishFactor")
                        resultList.add("slimeSize: $slimeSize")
                    }
                }
                if (entity is EntityLivingBase) {
                    val baseMaxHealth = entity.baseMaxHealth.toInt()
                    val health = entity.health.toInt()
                    resultList.add("baseMaxHealth: $baseMaxHealth")
                    resultList.add("health: $health")
                }
                resultList.add("")
                resultList.add("")
                counter++
            }
        }

        if (counter != 0) {
            val string = resultList.joinToString("\n")
            OSUtils.copyToClipboard(string)
            LorenzUtils.chat("§e[SkyHanni] $counter entities copied into the clipboard!")
        } else {
            LorenzUtils.chat("§e[SkyHanni] No entities found in a search radius of $searchRadius!")
        }
    }
}