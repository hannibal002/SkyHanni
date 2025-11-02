package at.hannibal2.skyhanni.test.command import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets import at.hannibal2.skyhanni.utils.compat.formattedTextCompat import at.hannibal2.skyhanni.utils.compat.findHealthReal import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.data.mob.MobFilter.isDisplayNpc
import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.data.mob.MobFilter.isSkyBlockMob
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.getArmorInventory
import at.hannibal2.skyhanni.utils.EntityUtils.getBlockInHand
import at.hannibal2.skyhanni.utils.EntityUtils.getSkinTexture
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.ItemUtils.isEnchanted
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.getFirstPassenger
import at.hannibal2.skyhanni.utils.compat.getInventoryItems
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.boss.WitherEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.mob.CreeperEntity
import net.minecraft.entity.mob.EndermanEntity
import net.minecraft.entity.mob.MagmaCubeEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack

@SkyHanniModule
object CopyNearbyEntitiesCommand {

    private fun command(args: Array<String>) {
        var searchRadius = 10
        if (args.size == 1) {
            searchRadius = args[0].toInt()
        }

        val start = LocationUtils.playerLocation()

        var counter = 0

        val resultList = buildList {
            for (entity in EntityUtils.getAllEntities().sortedBy { it.id }) {
                val position = entity.blockPos
                val vec = position.toLorenzVec()
                val distance = start.distance(vec)
                val mob = MobData.entityToMob[entity]
                if (distance >= searchRadius) continue

                val simpleName = entity.javaClass.simpleName
                add("entity: $simpleName")
                val displayName = entity.displayName
                add("name: '" + entity.name.formattedTextCompatLessResets() + "'")
                if (entity is ArmorStandEntity) add("cleanName: '" + entity.cleanName() + "'")
                add("displayName: '${displayName.formattedTextCompat()}'")
                add("entityId: ${entity.id}")
                add("Type of Mob: ${getType(entity, mob)}")
                add("uuid version: ${entity.uuid.version()} (${entity.uuid})")
                add("location data:")
                add("-  vec: $vec")
                add("-  distance: $distance")

                val rotationYaw = entity.yaw
                val rotationPitch = entity.pitch
                add("-  rotationYaw: $rotationYaw")
                add("-  rotationPitch: $rotationPitch")

                val firstPassenger = entity.getFirstPassenger()
                add("firstPassenger: $firstPassenger")
                val ridingEntity = entity.vehicle
                add("ridingEntity: $ridingEntity")

                if (entity.isInvisible) {
                    add("Invisible: true")
                }
                //#if MC > 1.21
                if (entity.isGlowing) {
                    add("Glowing: true")
                }
                //#endif

                if (entity is LivingEntity) {
                    add("EntityLivingBase:")
                    val baseMaxHealth = entity.baseMaxHealth
                    val health = entity.findHealthReal().toInt()
                    add("-  baseMaxHealth: $baseMaxHealth")
                    add("-  health: $health")
                }

                if (entity is PlayerEntity) {
                    val armor = entity.getArmorInventory()
                    if (armor != null) {
                        add("armor:")
                        for ((i, itemStack) in armor.withIndex()) {
                            val name = itemStack?.name.formattedTextCompatLeadingWhiteLessResets() ?: "null"
                            add("-  at: $i: $name")
                        }
                    }
                }

                when (entity) {
                    is ArmorStandEntity -> addArmorStand(entity)
                    is EndermanEntity -> addEnderman(entity)
                    is MagmaCubeEntity -> addMagmaCube(entity)
                    is ItemEntity -> addItem(entity)
                    is OtherClientPlayerEntity -> addOtherPlayer(entity)
                    is CreeperEntity -> addCreeper(entity)
                    is WitherEntity -> addWither(entity)
                    //#if MC > 1.21
                    is net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity -> addItemDisplayEntity(entity)
                    is net.minecraft.entity.passive.TropicalFishEntity -> addTropicalFish(entity)
                    is net.minecraft.entity.mob.ShulkerEntity -> addShulker(entity)
                    is net.minecraft.entity.passive.PandaEntity -> addPanda(entity)
                    is net.minecraft.entity.decoration.DisplayEntity.BlockDisplayEntity -> addBlockDisplayEntity(entity)
                    is net.minecraft.entity.passive.FrogEntity -> addFrogEntity(entity)
                    //#endif
                }
                if (mob != null && mob.mobType != Mob.Type.PLAYER) {
                    add("MobInfo: ")
                    addAll(getMobInfo(mob).map { "-  $it" })
                }
                add("")
                add("")
                counter++
            }
        }

        if (counter != 0) {
            val string = resultList.joinToString("\n")
            OSUtils.copyToClipboard(string)
            ChatUtils.chat("$counter entities copied into the clipboard!")
        } else {
            ChatUtils.chat("No entities found in a search radius of $searchRadius!")
        }
    }

    private fun MutableList<String>.addArmorStand(entity: ArmorStandEntity) {
        add("EntityArmorStand:")
        val headRotation = entity.headRotation.toLorenzVec()
        val bodyRotation = entity.bodyRotation.toLorenzVec()
        add("-  headRotation: $headRotation")
        add("-  bodyRotation: $bodyRotation")

        add("-  inventory:")
        for ((id, stack) in entity.getInventoryItems().withIndex()) {
            val adjustedStack = stack.orNull()
            add("-  id $id ($adjustedStack)")
            printItemStackData(adjustedStack)
        }
    }

    private fun MutableList<String>.addEnderman(entity: EndermanEntity) {
        add("EntityEnderman:")
        val heldBlockState = entity.getBlockInHand()
        add("-  heldBlockState: $heldBlockState")
        if (heldBlockState != null) {
            val block = heldBlockState.block
            add("-  block: $block")
        }
    }

    private fun MutableList<String>.addMagmaCube(entity: MagmaCubeEntity) {
        add("EntityMagmaCube:")
        val squishFactor = entity.stretch
        val slimeSize = entity.size
        add("-  factor: $squishFactor")
        add("-  slimeSize: $slimeSize")
    }

    private fun MutableList<String>.addItem(entity: ItemEntity) {
        add("EntityItem:")
        val stack = entity.stack
        val stackName = stack.name.formattedTextCompatLeadingWhiteLessResets()
        val stackDisplayName = stack.name.formattedTextCompatLeadingWhiteLessResets()
        val cleanName = stack.cleanName()
        val itemEnchanted = stack.hasEnchantments()
        //#if MC < 1.16
        //$$ val itemDamage = stack.itemDamage
        //#endif
        val stackSize = stack.count
        val maxStackSize = stack.maxCount
        val skullTexture = stack.getSkullTexture()
        add("-  name: '$stackName'")
        add("-  stackDisplayName: '$stackDisplayName'")
        add("-  cleanName: '$cleanName'")
        add("-  itemEnchanted: '$itemEnchanted'")
        //#if MC < 1.16
        //$$ add("-  itemDamage: '$itemDamage'")
        //#endif
        add("-  stackSize: '$stackSize'")
        add("-  maxStackSize: '$maxStackSize'")
        skullTexture?.let { add("-  skullTexture: '$it'") }
    }

    private fun MutableList<String>.addOtherPlayer(entity: OtherClientPlayerEntity) {
        add("EntityOtherPlayerMP:")

        val skinTexture = entity.getSkinTexture()
        add("-  skin texture: $skinTexture")
    }

    private fun MutableList<String>.addCreeper(entity: CreeperEntity) {
        add("EntityCreeper:")
        //#if MC < 1.16
        //$$ val creeperState = entity.creeperState
        //#endif
        val ignite = entity.isIgnited
        val powered = entity.isCharged
        //#if MC < 1.16
        //$$ add("-  creeperState: '$creeperState'")
        //#endif
        add("-  ignite: '$ignite'")
        add("-  powered: '$powered'")
    }

    private fun MutableList<String>.addWither(entity: WitherEntity) {
        add("EntityWither:")
        val invulTime = entity.invulnerableTimer
        val isArmored = entity.shouldRenderOverlay()
        add("-  invulTime: '$invulTime'")
        add("-  armored: '$isArmored'")
    }

    //#if MC > 1.21
    private fun MutableList<String>.addItemDisplayEntity(entity: net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity) {
        add("EntityItemDisplay:")
        val stack = entity.itemStack
        val rotation = entity.rotationVector

        add("-  itemStack:")
        printItemStackData(stack)
        add("-  rotation: $rotation")
    }

    private fun MutableList<String>.addTropicalFish(entity: net.minecraft.entity.passive.TropicalFishEntity) {
        add("EntityTropicalFish:")
        val variety = entity.variety
        val patternColor = entity.patternColor
        val baseColor = entity.baseColor
        add("-  variety: $variety")
        add("-  patternColor: $patternColor")
        add("-  baseColor: $baseColor")
    }

    private fun MutableList<String>.addShulker(entity: net.minecraft.entity.mob.ShulkerEntity) {
        add("EntityShulker:")
        val color = entity.color
        val attachedFace = entity.attachedFace
        add("-  color: $color")
        add("-  attachedFace: $attachedFace")
    }

    private fun MutableList<String>.addPanda(entity: net.minecraft.entity.passive.PandaEntity) {
        add("EntityPanda:")
        val mainGene = entity.mainGene
        val hiddenGene = entity.hiddenGene
        add("-  mainGene: $mainGene")
        add("-  hiddenGene: $hiddenGene")
    }

    private fun MutableList<String>.addBlockDisplayEntity(entity: net.minecraft.entity.decoration.DisplayEntity.BlockDisplayEntity) {
        add("EntityBlockDisplay:")
        val block = entity.blockState.block
        val rotation = entity.rotationVector

        add("-  block: ${block.name.formattedTextCompat()}")
        add("-  rotation: $rotation")
    }

    private fun MutableList<String>.addFrogEntity(entity: net.minecraft.entity.passive.FrogEntity) {
        add("EntityFrog:")
        val variant = entity.variant

        add("-  Variant: $variant")
    }
    //#endif

    private fun MutableList<String>.printItemStackData(stack: ItemStack?) {
        if (stack != null) {
            val skullTexture = stack.getSkullTexture()
            if (skullTexture != null) {
                add("-     skullTexture:")
                add("-     $skullTexture")
            }
            val cleanName = stack.cleanName()
            val stackName = stack.name.formattedTextCompatLeadingWhiteLessResets()
            val type = stack.javaClass.name
            add("-     name: '$stackName'")
            add("-     cleanName: '$cleanName'")
            add("-     type: $type")
        }
    }

    private fun getType(entity: Entity, mob: Mob?) = buildString {
        if (entity is LivingEntity && entity.isDisplayNpc()) append("DisplayNPC, ")
        if (entity is PlayerEntity && entity.isNpc()) append("NPC, ")
        if (entity is PlayerEntity && entity.isRealPlayer()) append("RealPlayer, ")
        if (mob?.mobType == Mob.Type.SUMMON) append("Summon, ")
        if (entity.isSkyBlockMob()) {
            append("SkyblockMob(")

            if (mob == null) {
                append(if (entity.distanceToPlayer() > MobData.DETECTION_RANGE) "Not in Range" else "None")
                append(")")
            } else {
                append(mob.mobType.name)
                if (mob.baseEntity == entity) append("/Base")
                append(")\"")
                append(mob.name)
                append("\"")
            }
            append(", ")
        }

        if (isNotEmpty()) {
            delete(length - 2, length) // Remove the last ", "
        } else {
            append("NONE")
        }
    }

    fun getMobInfo(mob: Mob) = buildList<String> {
        add("Name: ${mob.name}")
        add("Type: ${mob.mobType}")
        add("Base Entity: ${mob.baseEntity.asString()}")
        add("ArmorStand: ${mob.armorStand?.asString()}")
        if (mob.extraEntities.isNotEmpty()) {
            add("Extra Entities")
            addAll(mob.extraEntities.map { "  " + it.asString() })
        }
        if (mob.hologram1Delegate.isInitialized()) {
            add("Hologram1: ${mob.hologram1?.asString()}")
        }
        if (mob.hologram2Delegate.isInitialized()) {
            add("Hologram2: ${mob.hologram2?.asString()}")
        }
        if (mob.owner != null) {
            add("Owner: ${mob.owner.ownerName}")
        }
        add("Level or Tier: ${mob.levelOrTier.takeIf { it != -1 }}")
        if (mob.mobType == Mob.Type.DUNGEON) {
            add("Is Starred: ${mob.hasStar}")
            add("Attribute: ${mob.attribute ?: "NONE"}")
        }
        if (mob.boundingBox != mob.baseEntity.boundingBox) {
            add("Bounding Box: ${mob.boundingBox}")
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcopyentities") {
            description = "Copies the entities in the specified radius around the player into the clipboard"
            category = CommandCategory.DEVELOPER_DEBUG
            legacyCallbackArgs { command(it) }
        }
    }

    private fun LivingEntity.asString() =
        this.id.toString() + " - " + this.javaClass.simpleName + " \"" + this.name.formattedTextCompatLessResets() + "\""
}
