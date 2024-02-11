package at.hannibal2.skyhanni.features.itemabilities.abilitycooldown

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ItemRenderBackground.Companion.background
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.events.RenderObject
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.cachedData
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemId
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ItemAbilityCooldown {

    private val config get() = SkyHanniMod.feature.itemAbilities

    private val activeAbilities = mutableMapOf<ItemAbilityType, ActiveAbility>()
    private val youAlignedOthersPattern = "§eYou aligned §r§a.* §r§eother player(s)?!".toPattern()
    private val youBuffedYourselfPattern = "§aYou buffed yourself for §r§c\\+\\d+❁ Strength".toPattern()
//     private val WEIRD_TUBA = "WEIRD_TUBA".asInternalName()
//     private val WEIRDER_TUBA = "WEIRDER_TUBA".asInternalName()
//     private val VOODOO_DOLL_WILTED = "VOODOO_DOLL_WILTED".asInternalName()

//     @SubscribeEvent
//     fun onSoundEvent(event: PlaySoundEvent) {
//         when {
//             // Hyperion
//             event.soundName == "mob.zombie.remedy" && event.pitch == 0.6984127f && event.volume == 1f -> {
//                 val abilityScrolls = InventoryUtils.getItemInHand()?.getAbilityScrolls() ?: return
//                 if (abilityScrolls.size != 3) return
//
//                 ItemAbility.HYPERION.sound()
//             }
//             // Fire Fury Staff
//             event.soundName == "liquid.lavapop" && event.pitch == 1.0f && event.volume == 1f -> {
//                 ItemAbility.FIRE_FURY_STAFF.sound()
//             }
//             // Ice Spray Wand
//             event.soundName == "mob.enderdragon.growl" && event.pitch == 1f && event.volume == 1f -> {
//                 ItemAbility.ICE_SPRAY_WAND.sound()
//             }
//             // Gyrokinetic Wand & Shadow Fury
//             event.soundName == "mob.endermen.portal" -> {
//                 // Gryokinetic Wand
//                 if (event.pitch == 0.61904764f && event.volume == 1f) {
//                     ItemAbility.GYROKINETIC_WAND_LEFT.sound()
//                 }
//                 // Shadow Fury
//                 if (event.pitch == 1f && event.volume == 1f) {
//                     val internalName = InventoryUtils.getItemInHand()?.getInternalName() ?: return
//                     if (!internalName.equalsOneOf(
//                             "SHADOW_FURY".asInternalName(),
//                             "STARRED_SHADOW_FURY".asInternalName()
//                         )
//                     ) return
//
//                     ItemAbility.SHADOW_FURY.sound()
//                 }
//             }
//             // Giant's Sword
//             event.soundName == "random.anvil_land" && event.pitch == 0.4920635f && event.volume == 1f -> {
//                 ItemAbility.GIANTS_SWORD.sound()
//             }
//             // Atomsplit Katana
//             event.soundName == "mob.ghast.affectionate_scream" && event.pitch == 0.4920635f && event.volume == 0.15f -> {
//                 ItemAbility.ATOMSPLIT_KATANA.sound()
//             }
//             // Wand of Atonement
//             event.soundName == "liquid.lavapop" && event.pitch == 0.7619048f && event.volume == 0.15f -> {
//                 ItemAbility.WAND_OF_ATONEMENT.sound()
//             }
//             // Starlight Wand
//             event.soundName == "mob.bat.hurt" && event.volume == 0.1f -> {
//                 ItemAbility.STARLIGHT_WAND.sound()
//             }
//             // Voodoo Doll
//             event.soundName == "mob.guardian.curse" && event.volume == 0.2f -> {
//                 ItemAbility.VOODOO_DOLL.sound()
//             }
//             // Jinxed Voodoo Doll Hit
//             event.soundName == "random.successful_hit" && event.volume == 1.0f && event.pitch == 0.7936508f -> {
//                 ItemAbility.VOODOO_DOLL_WILTED.sound()
//             }
//             // Jinxed Voodoo Doll Miss
//             event.soundName == "mob.ghast.scream" && event.volume == 1.0f && event.pitch >= 1.6 && event.pitch <= 1.7 -> {
//                 val recentItems = InventoryUtils.recentItemsInHand.values
//                 if (VOODOO_DOLL_WILTED in recentItems) {
//                     ItemAbility.VOODOO_DOLL_WILTED.sound()
//                 }
//             }
//             // Golem Sword
//             event.soundName == "random.explode" && event.pitch == 4.047619f && event.volume == 0.2f -> {
//                 ItemAbility.GOLEM_SWORD.sound()
//             }
//             // Weird Tuba & Weirder Tuba
//             event.soundName == "mob.wolf.howl" && event.volume == 0.5f -> {
//                 val recentItems = InventoryUtils.recentItemsInHand.values
//                 if (WEIRD_TUBA in recentItems) {
//                     ItemAbility.WEIRD_TUBA.sound()
//                 }
//                 if (WEIRDER_TUBA in recentItems) {
//                     ItemAbility.WEIRDER_TUBA.sound()
//                 }
//             }
//             // End Stone Sword
//             event.soundName == "mob.zombie.unfect" && event.pitch == 2.0f && event.volume == 0.3f -> {
//                 ItemAbility.END_STONE_SWORD.sound()
//             }
//             // Soul Esoward
//             event.soundName == "mob.wolf.panting" && event.pitch == 1.3968254f && event.volume == 0.4f -> {
//                 ItemAbility.SOUL_ESOWARD.sound()
//             }
//             // Pigman Sword
//             event.soundName == "mob.zombiepig.zpigangry" && event.pitch == 2.0f && event.volume == 0.3f -> {
//                 ItemAbility.PIGMAN_SWORD.sound()
//             }
//             // Ember Rod
//             event.soundName == "mob.ghast.fireball" && event.pitch == 1.0f && event.volume == 0.3f -> {
//                 ItemAbility.EMBER_ROD.sound()
//             }
//             // Fire Freeze Staff
//             event.soundName == "mob.guardian.elder.idle" && event.pitch == 2.0f && event.volume == 0.2f -> {
//                 ItemAbility.FIRE_FREEZE_STAFF.sound()
//             }
//             // Staff of the Volcano
//             event.soundName == "random.explode" && event.pitch == 0.4920635f && event.volume == 0.5f -> {
//                 ItemAbility.STAFF_OF_THE_VOLCANO.sound()
//             }
//             // Staff of the Volcano
//             event.soundName == "random.eat" && event.pitch == 1.0f && event.volume == 1.0f -> {
//                 ItemAbility.STAFF_OF_THE_VOLCANO.sound()
//             }
//             // Holy Ice
//             event.soundName == "random.drink" && event.pitch.round(1) == 1.8f && event.volume == 1.0f -> {
//                 ItemAbility.HOLY_ICE.sound()
//             }
//         }
//     }

    @SubscribeEvent
    fun onItemClick(event: ItemClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val itemInHand = event.itemInHand ?: return

        for (ability in itemInHand.getActiveAbilities()) {
            ability.onClick(event.clickType)
        }
    }

    private fun ItemStack.getActiveAbilities(): List<ActiveAbility> {
        val internalName = getInternalName()

        val list = mutableListOf<ActiveAbility>()
        for (ability in ItemAbilityType.entries) {
            if (internalName in ability.internalNames) {
                list.add(activeAbilities.getOrPut(ability) { ActiveAbility(ability) })
            }
        }

        return list
    }

    @SubscribeEvent
    fun onIslandChange(event: LorenzWorldChangeEvent) {
        activeAbilities.clear()
    }

    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        if (!isEnabled()) return

        val message = event.message
//         handleOldAbilities(message)

        when {
//             message.contains("§lCASTING IN ") -> {
//                 if (!ItemAbilityType.RAGNAROCK_AXE.isOnCooldown()) {
//                     ItemAbilityType.RAGNAROCK_AXE.activate(LorenzColor.WHITE, 3.seconds)
//                 }
//             }
//
//             message.contains("§lCASTING") -> {
//                 if (ItemAbilityType.RAGNAROCK_AXE.specialColor != LorenzColor.DARK_PURPLE) {
//                     ItemAbilityType.RAGNAROCK_AXE.activate(LorenzColor.DARK_PURPLE, 10.seconds)
//                 }
//             }
//
//             message.contains("§c§lCANCELLED") -> {
//                 ItemAbilityType.RAGNAROCK_AXE.activate(null, 17.seconds)
//             }
        }
    }

//     private fun handleOldAbilities(message: String) {
//         // TODO use regex
//         if (message.contains(" (§6") && message.contains("§b) ")) {
//             val name: String = message.between(" (§6", "§b) ")
//             if (name == lastAbility) return
//             lastAbility = name
//             for (ability in ItemAbility.entries) {
//                 if (ability.abilityName == name) {
//                     click(ability)
//                     return
//                 }
//             }
//             return
//         }
//         lastAbility = ""
//     }

    private fun isEnabled(): Boolean = LorenzUtils.inSkyBlock && config.itemAbilityCooldown

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        for (activeAbility in activeAbilities.values) {
            activeAbility.text = createItemText(activeAbility)
        }
    }

    private fun createItemText(ability: ActiveAbility): ItemText {
        val specialColor = ability.specialColor
        val readyText = if (config.itemAbilityShowWhenReady) "R" else ""
        return if (ability.isOnCooldown()) {
            val color =
                specialColor ?: if (ability.getDuration() < 600.milliseconds) LorenzColor.RED else LorenzColor.YELLOW
            ItemText(color, ability.getDurationText(), true, ability.type.alternativePosition)
        } else {
            if (specialColor != null) {
                ability.specialColor = null
                tryHandleNextPhase(ability, specialColor)
                return createItemText(ability)
            }
            ItemText(LorenzColor.GREEN, readyText, false, ability.type.alternativePosition)
        }
    }

    private fun tryHandleNextPhase(ability: ActiveAbility, specialColor: LorenzColor) {
        if (ability.type == ItemAbilityType.GYROKINETIC_WAND_RIGHT && specialColor == LorenzColor.BLUE) {
            ability.activate(null, 4.seconds)
        }
        if (ability.type == ItemAbilityType.RAGNAROCK_AXE && specialColor == LorenzColor.DARK_PURPLE) {
            val duration = (20.seconds * ability.getMultiplier()) - 13.seconds
            ability.activate(
                null, if (duration < 0.seconds) {
                    0.seconds
                } else duration
            )
        }
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!isEnabled()) return

        val stack = event.stack
        val cachedData = stack.cachedData

        val itemAbilities = cachedData.itemAbilities ?: run {
            val itemAbilities = stack.getActiveAbilities()
            cachedData.itemAbilities = itemAbilities
            itemAbilities
        }

        val guiOpen = Minecraft.getMinecraft().currentScreen != null
        for (ability in itemAbilities) {
            if (!ability.isAllowed()) continue
            val itemText = ability.text ?: continue
            if (guiOpen && !itemText.onCooldown) continue
            val color = itemText.color
            val renderObject = RenderObject(color.getChatColor() + itemText.text)
            if (itemText.alternativePosition) {
                renderObject.offsetX = -8
                renderObject.offsetY = -10
            }
            event.renderObjects.add(renderObject)

            // fix multiple problems when having multiple abilities
            if (config.itemAbilityCooldownBackground) {
                var opacity = 130
                if (color == LorenzColor.GREEN) {
                    opacity = 80
                    if (!config.itemAbilityShowWhenReady) return
                }
                stack.background = color.addOpacity(opacity).rgb
            }
        }
    }

    private fun ItemStack.getIdentifier() = getItemUuid() ?: getItemId()

//     @SubscribeEvent
//     fun onChat(event: LorenzChatEvent) {
//         if (!isEnabled()) return
//
//         val message = event.message
//         if (message == "§dCreeper Veil §r§aActivated!") {
//             ItemAbility.WITHER_CLOAK.activate(LorenzColor.LIGHT_PURPLE)
//         }
//         if (message == "§dCreeper Veil §r§cDe-activated! §r§8(Expired)"
//             || message == "§cNot enough mana! §r§dCreeper Veil §r§cDe-activated!"
//         ) {
//             ItemAbility.WITHER_CLOAK.activate()
//         }
//         if (message == "§dCreeper Veil §r§cDe-activated!") {
//             ItemAbility.WITHER_CLOAK.activate(null, 5.seconds)
//         }
//
//         youAlignedOthersPattern.matchMatcher(message) {
//             ItemAbility.GYROKINETIC_WAND_RIGHT.activate(LorenzColor.BLUE, 6.seconds)
//         }
//         if (message == "§eYou §r§aaligned §r§eyourself!") {
//             ItemAbility.GYROKINETIC_WAND_RIGHT.activate(LorenzColor.BLUE, 6.seconds)
//         }
//         if (message == "§cRagnarock was cancelled due to being hit!") {
//             ItemAbility.RAGNAROCK_AXE.activate(null, 17.seconds)
//         }
//         youBuffedYourselfPattern.matchMatcher(message) {
//             ItemAbility.SWORD_OF_BAD_HEALTH.activate()
//         }
//     }

//     private fun ItemAbility.sound() {
//         val ping = System.currentTimeMillis() - lastItemClick
//         if (ping < 400) {
//             activate()
//         }
//     }
}
