package at.hannibal2.skyhanni.features.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyblockMobUtils.isSkillBlockMob
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EntityKill {
    // TODO(Left Click Mage, Aurora Staff)
    // TODO(Dungeon Ability's)
    // TODO(Voodoo Doll) Crazy Pain
    // TODO(Firewand)
    // TODO(Wither Implosion)
    // TODO(Wither Impact)
    // TODO(Frozen Scythe)
    // TODO(Bow) Priority, separate System
    // TODO(Terminater Beam)
    // TODO(Thornes)
    // TODO(Celeste Wand)
    // TODO(Vampire Mask) No clue how to implement
    // TODO(Starlight Wand)
    // TODO(Fire Veil Wand)
    // TODO(Staff of the Volcano)
    // TODO(Hellstorm Wand)
    // TODO(Jerrychine Gun)
    // TODO(Midas Staff)
    // TODO(Bonzo Staff)
    // TODO(Spirit Scepter)
    // TODO(Implosion Belt)
    // TODO(Horrow Wand)
    // TODO(Ice Spray Wand)
    // TODO(Damaging Pets) Guardian, Bal
    // TODO(Swing Range) Pain
    // TODO(Berserk Swing Range) Even more Pain
    // TODO(Exlosion Bow)
    // TODO(Multi Arrow)
    // TODO(Fishing Rod)
    // TODO(Special Fishing Rods)
    // TODO(Special Arrows)
    // TODO(Blaze Armor)

    var playerName = " " //Just Debug

    private var mobHitList = mutableSetOf<SkyblockMobUtils.SkyblockMob>()

    val currentEntityLiving = mutableSetOf<EntityLivingBase>()
    private val previousEntityLiving = mutableSetOf<EntityLivingBase>()


    val config get() = SkyHanniMod.feature.dev

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        previousEntityLiving.clear()
        previousEntityLiving.addAll(currentEntityLiving)
        currentEntityLiving.clear()
        currentEntityLiving.addAll(EntityUtils.getEntities<EntityLivingBase>().filter { isSkillBlockMob(it) })

        //Spawned EntityLiving
        (currentEntityLiving - previousEntityLiving).forEach { EntityLivingSpawnEvent(it).postAndCatch() }
        //Despawned EntityLiving
        (previousEntityLiving - currentEntityLiving).forEach { EntityLivingDeathEvent(it).postAndCatch() }

        if(config.mobKilldetetctionLogMobHitList) {
            if(config.mobKilldetetctionLogMobHitListId) {
                val id = mobHitList.map{it.baseEntity.entityId}
                LorenzDebug.log("Hit List: $id")
            }else{
                LorenzDebug.log("Hit List: $mobHitList")
            }
        }
    }

    @SubscribeEvent
    fun onEntityLivingSpawn(event: EntityLivingSpawnEvent) {
        val entity = event.entity
        if (entity.name == playerName) {
            val properties = entity.javaClass.declaredFields

            for (property in properties) {
                property.isAccessible = true
                val propertyName = property.name
                val propertyValue = property.get(entity)

                // Log the property name and value
                LorenzDebug.log("$propertyName: $propertyValue")
            }
        }
    }

    @SubscribeEvent
    fun onEntityLivingDeath(event: EntityLivingDeathEvent) {
        LorenzDebug.log("Entity Death Id=${event.entity.entityId}")
        mobHitList.firstOrNull { it.baseEntity == event.entity }
            ?.let {
                LorenzDebug.log("Hi i'm not living anymore")
                SkyblockMobKillEvent(it, false).postAndCatch() }
    }

    @SubscribeEvent
    fun onSkyblockMobKill(event: SkyblockMobKillEvent) {
        LorenzDebug.chatAndLog("Mob Name: ${event.mob.name}")
        mobHitList.remove(event.mob)
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        //Backup to avoid Memory Leak (if any exists)
        mobHitList.clear()
    }

    private fun addToMobHitList(mobId: Int) {
        EntityUtils.getEntityById(mobId)?.let {
            addToMobHitList(it)
        }
    }

    private fun addToMobHitList(entity: Entity) {
        if (!isSkillBlockMob(entity)) return
        mobHitList.add(SkyblockMobUtils.SkyblockMob(entity))
    }

    fun checkAndAddToMobHitList(mobId: Int) {
        EntityUtils.getEntityById(mobId)?.let {
            checkAndAddToMobHitList(it)
        }
    }

    fun checkAndAddToMobHitList(entity: Entity) {
        if (!isInMobHitList(entity)) {
            addToMobHitList(entity)
        }
    }

    private fun isInMobHitList(entity: Entity): Boolean {
        return mobHitList.any { it.baseEntity == entity }
    }



    @SubscribeEvent
    fun onEntityHit(event: EntityClickEvent) {
        val entity = event.clickedEntity ?: return
        if (!isSkillBlockMob(entity)) return

        //Base Melee Hit
        if (event.clickType == ClickType.LEFT_CLICK) {
            checkAndAddToMobHitList(entity)

            val itemInHand = InventoryUtils.getItemInHand() ?: return
            val enchantmentsOfItemInHand = itemInHand.getEnchantments()

            //Cleave Hit (range isn't 100% correct) //TODO fix Range and add Blacklist for certain mobs (Star Sentry etc.)
            if (enchantmentsOfItemInHand != null && enchantmentsOfItemInHand.any { it.key == "cleave" }) {
                val range: Double = when (enchantmentsOfItemInHand.getValue("cleave")) {
                    1 -> 3.3
                    2 -> 3.6
                    3 -> 3.9
                    4 -> 4.2
                    5 -> 4.5
                    6 -> 4.8
                    else -> 0.0
                }
                var i = 0
                EntityUtils.getEntitiesNearbyIgnoreY<EntityLivingBase>(entity.getLorenzVec(), range)
                    .filterNot { isSkillBlockMob(it) }.forEach {
                        checkAndAddToMobHitList(it)
                        i++
                        LorenzDebug.log("Name: ${it.name}")
                    }
                LorenzDebug.log("Cleave Triggers: $i")
            }
        }
    }

    @SubscribeEvent
    fun onBlockClickSend(event: BlockClickEvent) {
        handleItemClick(event.itemInHand, event.clickType)
    }

    @SubscribeEvent
    fun onItemClick(event: ItemClickEvent) {
        //LorenzDebug.log("Mouse Button" + Mouse.getEventButton().toString())
        handleItemClick(event.itemInHand, ClickType.LEFT_CLICK)
    }

    private fun handleItemClick(itemInHand: ItemStack?, clickType: ClickType) {
        if (itemInHand == null) return

        val lastLore = itemInHand.getLore().last().removeColor()
        val itemName = itemInHand.displayName ?: "How"
        val armor = InventoryUtils.getArmor()
        val player = Minecraft.getMinecraft().thePlayer
        LorenzDebug.log("Item Press: ${itemInHand.displayName.removeColor()} ItemTag: $lastLore")

        when {
            //Bow TODO(Cooldown)
            lastLore.endsWith("BOW") && (clickType == ClickType.RIGHT_CLICK || (clickType == ClickType.LEFT_CLICK && itemName.contains(
                "Shortbow"
            ))) -> {
                val piercingDepth = (itemInHand.getEnchantments()?.getValue("piercing")
                    ?: 0) + if (itemName.contains("Juju")) 3 else 0
                val bowStrength = 4.5  //TODO (Correct BowStrength) ~60 Blocks/s at Full Draw
                val origin = player.getPositionEyes(0.0f).toLorenzVec().subtract(LorenzVec(0.0, 0.1, 0.0))
                val direction = player.getLook(0.0f).toLorenzVec().normalize().multiply(bowStrength)
                //TODO(Terror Armor)
                when {
                    itemName.contains("Runaan") -> ArrowUtils.newArrows(
                        origin,
                        direction,
                        3,
                        12.5,
                        piercingDepth,
                        false
                    )  //{val arrowCount = 3; val spread = 12.5}
                    itemName.contains("Terminator") -> ArrowUtils.newArrows(
                        origin,
                        direction,
                        3,
                        5.0,
                        piercingDepth,
                        false
                    )//{val arrowCount = 3; val spread = 5.0}
                    else -> ArrowUtils.newArrows(origin, direction, piercingDepth, itemName.contains("Juju"))
                }

            }
            //Terminator Ability
            itemName.contains("Terminator") -> {

            }
        }
    }
}

/* Old Cold Snippeds
//Minecraft.getMinecraft().thePlayer.lookVec.normalize().toLorenzVec()
            val player = Minecraft.getMinecraft().thePlayer
            val raycastResult = EntityUtils.getEntities<EntityLiving>().filter {
                it.position.toLorenzVec().subtract(player.getLorenzVec())
                    .dotPorduct(player.lookVec.normalize().toLorenzVec()).absoluteValue < 1.5
            }
            val nearArrowHit =
                raycastResult //.filter { it !is EntityPlayerSP && it !is EntityArmorStand && it !is EntityXPOrb && it !is EntityOtherPlayerMP }
            nearArrowHit.forEach {
                LorenzDebug.log(it.toString())
                checkAndAddToMobHitList(it)
            }
            shouldTrackArrow = true

 */