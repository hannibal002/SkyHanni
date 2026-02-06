package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.data.mob.MobFilter.isDisplayNpc
import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.data.mob.MobFilter.isSkyBlockMob
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
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
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.findHealthReal
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.compat.getInventoryItems
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.fish.TropicalFish
import net.minecraft.world.entity.animal.frog.Frog
import net.minecraft.world.entity.animal.panda.Panda
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.MagmaCube
import net.minecraft.world.entity.monster.Shulker
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object CopyNearbyEntitiesCommand {

    private var entityCounter = 0

    // Only runs on the command, so performance impact is minimal
    @OptIn(AllEntitiesGetter::class)
    private fun buildCommandResult(searchRadius: Int): List<String> = buildList {
        val start = LocationUtils.playerLocation()
        for (entity in EntityUtils.getAllEntities().sortedBy { it.id }) {
            val position = entity.blockPosition()
            val vec = position.toLorenzVec()
            val distance = start.distance(vec)
            val mob = MobData.entityToMob[entity]
            if (distance >= searchRadius) continue

            val simpleName = entity.javaClass.simpleName
            add("entity: $simpleName")
            val displayName = entity.displayName
            add("name: '" + entity.name.formattedTextCompatLessResets() + "'")
            if (entity is ArmorStand) add("cleanName: '" + entity.cleanName() + "'")
            add("displayName: '${displayName.formattedTextCompat()}'")
            add("entityId: ${entity.id}")
            add("Type of Mob: ${getType(entity, mob)}")
            add("uuid version: ${entity.uuid.version()} (${entity.uuid})")
            add("location data:")
            add("-  vec: $vec")
            add("-  distance: $distance")

            val rotationYaw = entity.yRot
            val rotationPitch = entity.xRot
            add("-  rotationYaw: $rotationYaw")
            add("-  rotationPitch: $rotationPitch")

            val firstPassenger = entity.firstPassenger
            add("firstPassenger: $firstPassenger")
            val ridingEntity = entity.vehicle
            add("ridingEntity: $ridingEntity")

            if (entity.isInvisible) {
                add("Invisible: true")
            }
            if (entity.isCurrentlyGlowing) {
                add("Glowing: true")
            }

            if (entity is LivingEntity) {
                add("EntityLivingBase:")
                val baseMaxHealth = entity.baseMaxHealth
                val health = entity.findHealthReal().toInt()
                add("-  baseMaxHealth: $baseMaxHealth")
                add("-  health: $health")
            }

            if (entity is Player) {
                val armor = entity.getArmorInventory()
                if (armor != null) {
                    add("armor:")
                    for ((i, itemStack) in armor.withIndex()) {
                        val name = itemStack?.hoverName.formattedTextCompatLeadingWhiteLessResets()
                        add("-  at: $i: $name")
                    }
                }
            }

            when (entity) {
                is ArmorStand -> addArmorStand(entity)
                is EnderMan -> addEnderman(entity)
                is MagmaCube -> addMagmaCube(entity)
                is ItemEntity -> addItem(entity)
                is RemotePlayer -> addOtherPlayer(entity)
                is Creeper -> addCreeper(entity)
                is WitherBoss -> addWither(entity)
                is Display.ItemDisplay -> addItemDisplayEntity(entity)
                is TropicalFish -> addTropicalFish(entity)
                is Shulker -> addShulker(entity)
                is Panda -> addPanda(entity)
                is Display.BlockDisplay -> addBlockDisplayEntity(entity)
                is Frog -> addFrogEntity(entity)
            }
            if (mob != null && mob.mobType != Mob.Type.PLAYER) {
                add("MobInfo: ")
                addAll(getMobInfo(mob).map { "-  $it" })
            }
            add("")
            add("")
            entityCounter++
        }
    }

    private fun MutableList<String>.addArmorStand(entity: ArmorStand) {
        add("EntityArmorStand:")
        val headRotation = entity.headPose.toLorenzVec()
        val bodyRotation = entity.bodyPose.toLorenzVec()
        add("-  headRotation: $headRotation")
        add("-  bodyRotation: $bodyRotation")

        add("-  inventory:")
        for ((id, stack) in entity.getInventoryItems().withIndex()) {
            val adjustedStack = stack.orNull()
            add("-  id $id ($adjustedStack)")
            printItemStackData(adjustedStack)
        }
    }

    private fun MutableList<String>.addEnderman(entity: EnderMan) {
        add("EntityEnderman:")
        val heldBlockState = entity.getBlockInHand()
        add("-  heldBlockState: $heldBlockState")
        if (heldBlockState != null) {
            val block = heldBlockState.block
            add("-  block: $block")
        }
    }

    private fun MutableList<String>.addMagmaCube(entity: MagmaCube) {
        add("EntityMagmaCube:")
        val squishFactor = entity.squish
        val slimeSize = entity.size
        add("-  factor: $squishFactor")
        add("-  slimeSize: $slimeSize")
    }

    private fun MutableList<String>.addItem(entity: ItemEntity) {
        add("EntityItem:")
        val stack = entity.item
        val stackName = stack.hoverName.formattedTextCompatLeadingWhiteLessResets()
        val stackDisplayName = stack.hoverName.formattedTextCompatLeadingWhiteLessResets()
        val cleanName = stack.cleanName()
        val itemEnchanted = stack.isEnchanted
        val stackSize = stack.count
        val maxStackSize = stack.maxStackSize
        val skullTexture = stack.getSkullTexture()
        add("-  name: '$stackName'")
        add("-  stackDisplayName: '$stackDisplayName'")
        add("-  cleanName: '$cleanName'")
        add("-  itemEnchanted: '$itemEnchanted'")
        add("-  stackSize: '$stackSize'")
        add("-  maxStackSize: '$maxStackSize'")
        skullTexture?.let { add("-  skullTexture: '$it'") }
    }

    private fun MutableList<String>.addOtherPlayer(entity: RemotePlayer) {
        add("EntityOtherPlayerMP:")

        val skinTexture = entity.getSkinTexture()
        add("-  skin texture: $skinTexture")
    }

    private fun MutableList<String>.addCreeper(entity: Creeper) {
        add("EntityCreeper:")
        val ignite = entity.isIgnited
        val powered = entity.isPowered
        add("-  ignite: '$ignite'")
        add("-  powered: '$powered'")
    }

    private fun MutableList<String>.addWither(entity: WitherBoss) {
        add("EntityWither:")
        val invulTime = entity.invulnerableTicks
        val isArmored = entity.isPowered
        add("-  invulTime: '$invulTime'")
        add("-  armored: '$isArmored'")
    }

    private fun MutableList<String>.addItemDisplayEntity(entity: Display.ItemDisplay) {
        add("EntityItemDisplay:")
        val stack = entity.itemStack
        val rotation = entity.lookAngle

        add("-  itemStack:")
        printItemStackData(stack)
        add("-  rotation: $rotation")
    }

    private fun MutableList<String>.addTropicalFish(entity: TropicalFish) {
        add("EntityTropicalFish:")
        val variety = entity.pattern
        val patternColor = entity.patternColor
        val baseColor = entity.baseColor
        add("-  variety: $variety")
        add("-  patternColor: $patternColor")
        add("-  baseColor: $baseColor")
    }

    private fun MutableList<String>.addShulker(entity: Shulker) {
        add("EntityShulker:")
        val color = entity.color
        val attachedFace = entity.attachFace
        add("-  color: $color")
        add("-  attachedFace: $attachedFace")
    }

    private fun MutableList<String>.addPanda(entity: Panda) {
        add("EntityPanda:")
        val mainGene = entity.mainGene
        val hiddenGene = entity.hiddenGene
        add("-  mainGene: $mainGene")
        add("-  hiddenGene: $hiddenGene")
    }

    private fun MutableList<String>.addBlockDisplayEntity(entity: Display.BlockDisplay) {
        add("EntityBlockDisplay:")
        val block = entity.blockState.block
        val rotation = entity.lookAngle

        add("-  block: ${block.name.formattedTextCompat()}")
        add("-  rotation: $rotation")
    }

    private fun MutableList<String>.addFrogEntity(entity: Frog) {
        add("EntityFrog:")
        val variant = entity.variant

        add("-  Variant: $variant")
    }

    private fun MutableList<String>.printItemStackData(stack: ItemStack?) {
        if (stack != null) {
            val skullTexture = stack.getSkullTexture()
            if (skullTexture != null) {
                add("-     skullTexture:")
                add("-     $skullTexture")
            }
            val cleanName = stack.cleanName()
            val stackName = stack.hoverName.formattedTextCompatLeadingWhiteLessResets()
            val type = stack.javaClass.name
            add("-     name: '$stackName'")
            add("-     cleanName: '$cleanName'")
            add("-     type: $type")
        }
    }

    private fun getType(entity: Entity, mob: Mob?) = buildString {
        if (entity is LivingEntity && entity.isDisplayNpc()) append("DisplayNPC, ")
        if (entity is Player && entity.isNpc()) append("NPC, ")
        if (entity is Player && entity.isRealPlayer()) append("RealPlayer, ")
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
            argCallback("radius", BrigadierArguments.integer()) { radius ->
                command(radius)
            }
            simpleCallback {
                command()
            }
        }
    }

    private fun command(searchRadius: Int = 10) {
        val resultList = buildCommandResult(searchRadius)

        if (entityCounter != 0) {
            val string = resultList.joinToString("\n")
            OSUtils.copyToClipboard(string)
            ChatUtils.chat("$entityCounter entities copied into the clipboard!")
        } else {
            ChatUtils.chat("No entities found in a search radius of $searchRadius!")
        }
    }

    private fun LivingEntity.asString() =
        this.id.toString() + " - " + this.javaClass.simpleName + " \"" + this.name.formattedTextCompatLessResets() + "\""
}
