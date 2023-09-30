package at.hannibal2.skyhanni.features.mobs

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.EntityClickEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLiving
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EntityKill {
    private var mobHitMap = HashMap<Int, Entity>(200)
    private var mobHitList = mutableListOf<Int>()


    private val mobNameFilter = "\\[.*\\] (.*) \\d+/\\d+❤".toRegex()

    var tickDelayer = 0

    //Made with help from ChatGPT Link: https://chat.openai.com/share/1e5b11ed-b72e-4a69-bfe9-71a8d5fd2fa6
    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        tickDelayer++
        if (tickDelayer < 21) return
        tickDelayer = 0
        val listA = EntityUtils.getAllEntities()
        val listB = mobHitList
        val listC = ArrayList<Int>(20);

        val setA = listA.map { it.entityId }.toSet()

        //LorenzDebug.log("$setA")
        LorenzDebug.log("$listB")

        for (i: Int in listB.indices) {
            val elementB = listB[i];
            if (elementB !in setA) {
                // Element in List B not found in List A
                killEvent(elementB)
                listC.add(i);
            }
        }
        listC.reversed().forEach { listB.removeAt(it) };
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
                EntityUtils.getEntitiesNearby<EntityLiving>(event.clickedEntity.getLorenzVec(), range).forEach {
                    addToMobHitList(it.entityId)
                    i++
                    LorenzDebug.log(it.name)
                }
                LorenzDebug.log("Cleave Triggers: $i")
            }
        }
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        mobHitMap.clear()
        mobHitList.clear()
    }

    private fun addToMobHitList(mobId: Int) {
        val index = mobHitList.binarySearch(mobId);
        if (index >= 0) return
        mobHitList.add(-(index + 1), mobId);
        mobHitMap[mobId] = EntityUtils.getAllEntities().firstOrNull { it.entityId == mobId + 1 } ?: return
    }

}