package at.hannibal2.skyhanni.features.combat.killDetection

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.EntityClickEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.features.combat.killDetection.EntityKill.ENTITY_RENDER_RANGE_IN_BLOCKS
import at.hannibal2.skyhanni.features.combat.killDetection.EntityKill.addToMobHitList
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawCylinderInWorld
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyblockMobUtils.isSkyBlockMob
import at.hannibal2.skyhanni.utils.SkyblockMobUtils.rayTraceForSkyblockMob
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.absoluteValue

private const val CLEAVE_HIT_LIMIT = 12

private const val CLEAVE_MAX_Y_DIFFERENCE = 5.0

private const val CLEAVE_EXTEND_RANGE = 1.5 //Magic Value (determent by Testing)

object MobHitTrigger {
    // Implement TODOs
    // TODO(Dungeon Ability's)
    // TODO(Conjuring/Mage Sheep)
    // TODO(Voodoo Doll) Crazy Pain
    // TODO(Firewand)
    // TODO(Wither Implosion)
    // TODO(Wither Impact)
    // TODO(Frozen Scythe)
    // TODO(Terminater Beam)
    // TODO(Thornes) I don't know if it even counts for anything
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
    // TODO(Horrow Wand) I don't like this one
    // TODO(Ice Spray Wand)
    // TODO(Damaging Pets) Guardian, Bal
    // TODO(Swing Range) Pain
    // TODO(Berserk Swing Cone) Even more Pain
    // TODO(Exlosion Bow)
    // TODO(Multi Arrow)
    // TODO(Fishing Rod)
    // TODO(Special Fishing Rods)
    // TODO(Special Arrows)
    // TODO(Blaze Armor)
    // TODO(Flower of Truth)
    // TODO(Livid Dagger Ability)
    // TODO(Bingo Blaster)
    // TODO(Alchemist Wand)
    // TODO(Adaptive Blade)

    // TODO(Summons) IDK


    //Working on it
    // TODO(Bow) Priority, separate System

    //Needs Testing
    // TODO(Left Click Mage, Aurora Staff)

    //Needs Bugfixing

    private val config get() = SkyHanniMod.feature.dev.mobKillDetection.mobHitDetecion


    @SubscribeEvent
    fun onEntityClicked(event: EntityClickEvent) {
        val entity = event.clickedEntity ?: return
        if (!entity.isSkyBlockMob()) return

        //Base Melee Hit
        if (event.clickType.isLeftClick()) {
            addToMobHitList(entity, hitTrigger.Melee)

            val itemInHand = InventoryUtils.getItemInHand() ?: return
            val enchantmentsOfItemInHand = itemInHand.getEnchantments()

            //Cleave Hit
            if (enchantmentsOfItemInHand != null && enchantmentsOfItemInHand.any { it.key == "cleave" }) {
                val range: Double = when (enchantmentsOfItemInHand.getValue("cleave")) {
                    1 -> 3.3
                    2 -> 3.6
                    3 -> 3.9
                    4 -> 4.2
                    5 -> 4.5
                    6 -> 4.8
                    else -> -CLEAVE_EXTEND_RANGE
                } + CLEAVE_EXTEND_RANGE //TODO fix Range (for all Levels)
                val cleaveHits = EntityUtils.getEntitiesNearbyIgnoreY<EntityLivingBase>(entity.getLorenzVec(), range)
                    .filter { ((entity.posY - it.posY).absoluteValue < CLEAVE_MAX_Y_DIFFERENCE) && (it != entity) && it.isSkyBlockMob() }
                    .sortedBy { it.distanceTo(entity) }.take(CLEAVE_HIT_LIMIT)
                cleaveHits.forEach { addToMobHitList(it, hitTrigger.Cleave) }

                if (config.cleaveDebug) {
                    cleaveEntity = entity.getLorenzVec()
                    cleaveRange = range
                    LorenzDebug.log("Cleave Triggers: ${cleaveHits.count()} for ${cleaveHits.map { it.name }}")
                }
            }
        }
    }

    private var cleaveEntity: LorenzVec? = null
    private var cleaveRange = 0.0

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (!config.cleaveDebug) return
        cleaveEntity?.let {
            drawCylinderInWorld(
                LorenzColor.DARK_AQUA.addOpacity(50),
                it.x,
                it.y,
                it.z,
                cleaveRange.toFloat(),
                2.0f,
                event.partialTicks
            )
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

    private val abilityRegex = Regex("Ability:(.*?)\\s(RIGHT|LEFT)\\sCLICK")

    private data class Ability(val name: String, val clickType: ClickType)

    private fun handleItemClick(itemInHand: ItemStack?, clickType: ClickType) {
        if (itemInHand == null) return

        val lastLore = itemInHand.getLore().last().removeColor()
        val itemName = itemInHand.displayName ?: "How"
        val armor = InventoryUtils.getArmor()
        val player = Minecraft.getMinecraft().thePlayer
        val classInDungeon = DungeonAPI.playerClass
        val partialTick = 0.5f //IDK how to make it correctly but ignoring partialTicks(=0.5) works fine
        //LorenzDebug.log("Item Press: ${itemInHand.displayName.removeColor()} ItemTag: $lastLore")

        //Ability
        val abilityLores = itemInHand.getLore().filter { it.removeColor().contains("Ability:") }
        val abilityList = mutableListOf<Ability>()

        abilityLores.map { it.removeColor() }.forEach {
            val match = abilityRegex.find(it) ?: return@forEach
            abilityList.add(
                Ability(
                    match.groupValues[1].trim(), if (match.groupValues[2] == "RIGHT") ClickType.RIGHT_CLICK else
                        ClickType.LEFT_CLICK
                )
            )
        }

        //TODO(Cooldowns)
        abilityList.forEach { ability ->
            if (ability.clickType != clickType) return@forEach
            when (ability.name) {
                //Aurora Staff
                "Arcane Zap" -> rayTraceForSkyblockMob( //TODO fix inaccuracy when moving + correct range
                    player, ENTITY_RENDER_RANGE_IN_BLOCKS, partialTick, offset = LorenzVec(0.0, -0.6, 0.0)
                )?.let { addToMobHitList(it, hitTrigger.AuroraStaff) }

                else -> return@forEach
            }
        }

        //Special Cases
        when {
            //Bow TODO(Cooldown)
            lastLore.endsWith("BOW") && (clickType.isRightClick() || (clickType.isLeftClick() && itemName.contains(
                "Shortbow"
            ))) -> {
                val piercingDepth = (itemInHand.getEnchantments()?.getValue("piercing")
                    ?: 0) + if (itemName.contains("Juju")) 3 else 0
                val bowStrength = 3  //TODO (Correct BowStrength) ~60 Blocks/s at Full Draw
                val direction = ArrowDetection.getMotionVector(player)
                val origin = player.getPositionEyes(partialTick).toLorenzVec().subtract(LorenzVec(0.0, 0.1, 0.0))
                    .add(direction.multiply(0.15))
                val velocity = direction.multiply(bowStrength)
                //TODO(Terror Armor)
                when {
                    itemName.contains("Runaan") -> ArrowDetection.newArrows(
                        origin,
                        velocity,
                        3,
                        12.5,
                        piercingDepth,
                        false
                    ) //{val arrowCount = 3; val spread = 12.5}
                    itemName.contains("Terminator") -> ArrowDetection.newArrows(
                        origin,
                        velocity,
                        3,
                        5.0,
                        piercingDepth,
                        false
                    )//{val arrowCount = 3; val spread = 5.0}
                    else -> ArrowDetection.newArrow(origin, velocity, piercingDepth, itemName.contains("Juju"))
                }

            }
            //Mage Left Click TODO(Cooldown)
            classInDungeon == DungeonAPI.DungeonClass.MAGE && clickType.isLeftClick() -> rayTraceForSkyblockMob(
                player,
                ENTITY_RENDER_RANGE_IN_BLOCKS,
                partialTick
            )?.let {
                addToMobHitList(
                    it,
                    hitTrigger.LMage
                )
            }
        }
    }
}
