package at.hannibal2.skyhanni.features.mobs

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLiving
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.absoluteValue

class EntityKill {
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

    private var mobHitList = mutableSetOf<SkyblockMobUtils.SkyblockMob>()

    private var shouldTrackArrow = false


    private val currentEntityLiving = mutableSetOf<EntityLiving>()
    private val previousEntityLiving = mutableSetOf<EntityLiving>()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        previousEntityLiving.clear()
        previousEntityLiving.addAll(currentEntityLiving)
        currentEntityLiving.clear()
        currentEntityLiving.addAll(EntityUtils.getEntities())

        //Spawned EntityLiving
        (currentEntityLiving - previousEntityLiving).forEach { EntityLivingSpawnEvent(it).postAndCatch() }
        //Despawned EntityLiving
        (previousEntityLiving - currentEntityLiving).forEach { EntityLivingDeathEvent(it).postAndCatch() }
    }

    @SubscribeEvent
    fun onEntityLivingDeath(event: EntityLivingDeathEvent) {
        //LorenzDebug.log("Hit List: $mobHitList")
        mobHitList.firstOrNull { it.baseEntity == event.entity }
            ?.let { SkyblockMobKillEvent(it, false).postAndCatch() }
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
            mobHitList.add(SkyblockMobUtils.SkyblockMob(entity))
    }

    private fun checkAndAddToMobHitList(mobId: Int) {
        EntityUtils.getEntityById(mobId)?.let {
            checkAndAddToMobHitList(it)
        }
    }

    private fun checkAndAddToMobHitList(entity: Entity) {
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

        //Base Melee Hit
        if (event.clickType == ClickType.LEFT_CLICK) {
            checkAndAddToMobHitList(entity)

            val itemInHand = InventoryUtils.getItemInHand() ?: return
            val enchantmentsOfItemInHand = itemInHand.getEnchantments()

            //Cleave Hit (range isn't 100% correct)
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
                EntityUtils.getEntitiesNearbyIgnoreY<EntityLiving>(event.clickedEntity.getLorenzVec(), range).forEach {
                    checkAndAddToMobHitList(it)
                    i++
                    LorenzDebug.log(it.name)
                }
                LorenzDebug.log("Cleave Triggers: $i")
            }
        }
    }

    @SubscribeEvent
    fun onBlockClickSend(event: BlockClickEvent) {
        handleItemClick(event.itemInHand)
    }

    @SubscribeEvent
    fun onItemClick(event: ItemClickEvent) {
        handleItemClick(event.itemInHand)
    }

    private fun handleItemClick(itemInHand: ItemStack?) {
        if (itemInHand == null) return

        val lastLore = itemInHand.getLore().last().removeColor()
        LorenzDebug.log("Item Press: ${itemInHand.displayName.removeColor()} ItemTag: $lastLore")
        //Bow
        if (lastLore.endsWith("BOW")) {
            val piercingDepth = (itemInHand.getEnchantments()?.getValue("piercing")
                ?: 0) + if (itemInHand.displayName.contains("Juju")) 3 else 0

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
        }
    }
}