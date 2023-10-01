package at.hannibal2.skyhanni.features.mobs

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.item.EntityXPOrb
import net.minecraft.entity.projectile.EntityArrow
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
    // TODO(Bow) Priority
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


    private var mobHitMap = HashMap<Int, Entity>(200)
    private var mobHitList = mutableListOf<Int>()

    private val mobNameFilter = "\\[.*\\] (.*) \\d+".toRegex()

    private var tickDelayer = 0

    private var shouldTrackArrow = false


    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        tickDelayer++

        if (shouldTrackArrow) {
            val arrowCatch = EntityUtils.getEntitiesNearby<EntityArrow>(Minecraft.getMinecraft().thePlayer.getLorenzVec(), 100.0)
            if (arrowCatch.count() > 0) {
                val arrow = arrowCatch.first()
                LorenzDebug.log(arrow.toString())
            }
            shouldTrackArrow = false
        }

        if (tickDelayer < 21) return
        tickDelayer = 0

        //This function was made with help from ChatGPT Link: https://chat.openai.com/share/1e5b11ed-b72e-4a69-bfe9-71a8d5fd2fa6
        val listA = EntityUtils.getEntities<EntityLiving>()
        val listB = mobHitList
        val listC = ArrayList<Int>(20)

        val setA = listA.map { it.entityId }.toSet()

        //LorenzDebug.log("$setA")
        LorenzDebug.log("$listB")

        for (i: Int in listB.indices) {
            val elementB = listB[i]
            if (elementB !in setA) {
                // Element in List B not found in List A
                killEvent(elementB)
                listC.add(i)
            }
        }
        listC.reversed().forEach { listB.removeAt(it) }
    }

    fun killEvent(id: Int) {
        val itemStand = mobHitMap[id] ?: return
        val mobName = mobNameFilter.find(itemStand.name.removeColor())?.groupValues?.get(1)
        LorenzDebug.chatAndLog("Mob Name: $mobName")

    }

    @SubscribeEvent
    fun onEntityHit(event: EntityClickEvent) {
        val entity = event.clickedEntity ?: return
        if (entity.entityId in mobHitMap) return

        //Base Melee Hit
        if (event.clickType == ClickType.LEFT_CLICK) {
            addToMobHitList(entity.entityId)

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
                    addToMobHitList(it.entityId)
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
                it.position.toLorenzVec().subtract(player.getLorenzVec()).dotPorduct(player.lookVec.normalize().toLorenzVec()).absoluteValue < 1.5
            }
            val nearArrowHit = raycastResult //.filter { it !is EntityPlayerSP && it !is EntityArmorStand && it !is EntityXPOrb && it !is EntityOtherPlayerMP }
            nearArrowHit.forEach {
                LorenzDebug.log(it.toString())
                addToMobHitList(it.entityId)
            }
            shouldTrackArrow = true
        }
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        mobHitMap.clear()
        mobHitList.clear()
    }

    private fun addToMobHitList(mobId: Int) {
        val index = mobHitList.binarySearch(mobId)
        if (index >= 0) return
        mobHitList.add(-(index + 1), mobId)

        val theWorld = Minecraft.getMinecraft().theWorld ?: return
        mobHitMap[mobId] = theWorld.getEntityByID(mobId + 1 )
                ?: return //Fun Fact the corresponding ArmorStand for a mob has always the mobId + 1
    }

}