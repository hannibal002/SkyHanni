package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.data.MobData
import at.hannibal2.skyhanni.data.MobFilter.isDisplayNPC
import at.hannibal2.skyhanni.data.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.data.MobFilter.isSkyBlockMob
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.getBlockInHand
import at.hannibal2.skyhanni.utils.EntityUtils.getSkinTexture
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.ItemUtils.isEnchanted
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntityMagmaCube
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

object CopyNearbyEntitiesCommand {

    fun command(args: Array<String>) {
        var searchRadius = 10
        if (args.size == 1) {
            searchRadius = args[0].toInt()
        }

        val start = LocationUtils.playerLocation()

        val resultList = mutableListOf<String>()
        var counter = 0

        for (entity in EntityUtils.getAllEntities().sortedBy { it.entityId }) {
            val position = entity.position
            val vec = position.toLorenzVec()
            val distance = start.distance(vec)
            if (distance < searchRadius) {
                val simpleName = entity.javaClass.simpleName
                resultList.add("entity: $simpleName")
                val displayName = entity.displayName
                resultList.add("name: '" + entity.name + "'")
                if (entity is EntityArmorStand) resultList.add("cleanName: '" + entity.cleanName() + "'")
                resultList.add("displayName: '${displayName.formattedText}'")
                resultList.add("entityId: ${entity.entityId}")
                resultList.add("Type of Mob: ${getType(entity)}")
                resultList.add("uuid version: ${entity.uniqueID.version()} (${entity.uniqueID})")
                resultList.add("location data:")
                resultList.add("-  vec: $vec")
                resultList.add("-  distance: $distance")

                val rotationYaw = entity.rotationYaw
                val rotationPitch = entity.rotationPitch
                resultList.add("-  rotationYaw: $rotationYaw")
                resultList.add("-  rotationPitch: $rotationPitch")

                val riddenByEntity = entity.riddenByEntity
                resultList.add("riddenByEntity: $riddenByEntity")
                val ridingEntity = entity.ridingEntity
                resultList.add("ridingEntity: $ridingEntity")

                if (entity is EntityLivingBase) {
                    resultList.add("EntityLivingBase:")
                    val baseMaxHealth = entity.baseMaxHealth
                    val health = entity.health.toInt()
                    resultList.add("-  baseMaxHealth: $baseMaxHealth")
                    resultList.add("-  health: $health")
                }

                if (entity is EntityPlayer) {
                    val inventory = entity.inventory
                    if (inventory != null) {
                        resultList.add("armor:")
                        for ((i, itemStack) in inventory.armorInventory.withIndex()) {
                            val name = itemStack?.name ?: "null"
                            resultList.add("-  at: $i: $name")
                        }
                    }
                }

                when (entity) {
                    is EntityArmorStand -> {
                        resultList.add("EntityArmorStand:")
                        val headRotation = entity.headRotation.toLorenzVec()
                        val bodyRotation = entity.bodyRotation.toLorenzVec()
                        resultList.add("-  headRotation: $headRotation")
                        resultList.add("-  bodyRotation: $bodyRotation")

                        resultList.add("-  inventory:")
                        for ((id, stack) in entity.inventory.withIndex()) {
                            resultList.add("-  id $id ($stack)")
                            printItemStackData(stack, resultList)
                        }
                    }

                    is EntityEnderman -> {
                        resultList.add("EntityEnderman:")
                        val heldBlockState = entity.getBlockInHand()
                        resultList.add("-  heldBlockState: $heldBlockState")
                        if (heldBlockState != null) {
                            val block = heldBlockState.block
                            resultList.add("-  block: $block")
                        }
                    }

                    is EntityMagmaCube -> {
                        resultList.add("EntityMagmaCube:")
                        val squishFactor = entity.squishFactor
                        val slimeSize = entity.slimeSize
                        resultList.add("-  factor: $squishFactor")
                        resultList.add("-  slimeSize: $slimeSize")
                    }

                    is EntityItem -> {
                        resultList.add("EntityItem:")
                        val stack = entity.entityItem
                        val stackName = stack.name
                        val stackDisplayName = stack.displayName
                        val cleanName = stack.cleanName()
                        val itemEnchanted = stack.isEnchanted()
                        val itemDamage = stack.itemDamage
                        val stackSize = stack.stackSize
                        val maxStackSize = stack.maxStackSize
                        resultList.add("-  name: '$stackName'")
                        resultList.add("-  stackDisplayName: '$stackDisplayName'")
                        resultList.add("-  cleanName: '$cleanName'")
                        resultList.add("-  itemEnchanted: '$itemEnchanted'")
                        resultList.add("-  itemDamage: '$itemDamage'")
                        resultList.add("-  stackSize: '$stackSize'")
                        resultList.add("-  maxStackSize: '$maxStackSize'")
                    }

                    is EntityOtherPlayerMP -> {
                        resultList.add("EntityOtherPlayerMP:")

                        val skinTexture = entity.getSkinTexture()
                        resultList.add("-  skin texture: $skinTexture")
                    }
                }
                resultList.add("")
                resultList.add("")
                counter++
            }
        }

        if (counter != 0) {
            val string = resultList.joinToString("\n")
            OSUtils.copyToClipboard(string)
            LorenzUtils.chat("$counter entities copied into the clipboard!")
        } else {
            LorenzUtils.chat("No entities found in a search radius of $searchRadius!")
        }
    }

    private fun printItemStackData(stack: ItemStack?, resultList: MutableList<String>) {
        if (stack != null) {
            val skullTexture = stack.getSkullTexture()
            if (skullTexture != null) {
                resultList.add("-     skullTexture:")
                resultList.add("-     $skullTexture")
            }
            val cleanName = stack.cleanName()
            val stackName = stack.name
            val type = stack.javaClass.name
            resultList.add("-     name: '$stackName'")
            resultList.add("-     cleanName: '$cleanName'")
            resultList.add("-     type: $type")
        }
    }

    private fun getType(entity: Entity) = buildString {
        if (entity is EntityLivingBase && entity.isDisplayNPC()) append("DisplayNPC, ")
        if (entity is EntityPlayer && entity.isNPC()) append("NPC, ")
        if (entity is EntityPlayer && entity.isRealPlayer()) append("RealPlayer, ")
        if (MobData.summoningMobs.any { it.baseEntity == entity }) append("Summon, ")
        if (entity.isSkyBlockMob()) {
            append("SkyblockMob(")
            val mob = MobData.entityToMob[entity]
            append(mob?.mobType?.name ?: "None")
            if (mob?.baseEntity == entity) append("/Base")
            append(")\"")
            append(mob?.name ?: "")
            append("\", ")
        }

        if (isNotEmpty()) {
            delete(length - 2, length) // Remove the last ", "
        } else {
            append("NONE")
        }
    }
}
