package at.hannibal2.skyhanni.features.itemabilities.abilitycooldown

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ItemRenderBackground.Companion.background
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class ItemAbilityCooldown {

    var lastAbility = ""
    var tick = 0
    val items = mutableMapOf<ItemStack, ItemText>()

    @SubscribeEvent
    fun onSoundEvent(event: PlaySoundEvent) {
        if (event.soundName == "mob.zombie.remedy") {
            if (event.pitch == 0.6984127f && event.volume == 1f) {
                ItemAbility.HYPERION.sound()
            }
        }
        if (event.soundName == "mob.enderdragon.growl") {
            if (event.pitch == 1f && event.volume == 1f) {
                ItemAbility.ICE_SPRAY_WAND.sound()
            }
        }
        if (event.soundName == "mob.endermen.portal") {
            if (event.pitch == 0.61904764f && event.volume == 1f) {
                ItemAbility.GYROKINETIC_WAND.sound()
            }
        }
        if (event.soundName == "random.anvil_land") {
            if (event.pitch == 0.4920635f && event.volume == 1f) {
                ItemAbility.GIANTS_SWORD.sound()
            }
        }
        if (event.soundName == "mob.ghast.affectionate_scream") {
            if (event.pitch == 0.4920635f && event.volume == 0.15f) {
                ItemAbility.ATOMSPLIT_KATANA.sound()
            }
        }
        if (event.soundName == "random.click") {
            if (event.pitch == 2.0f && event.volume == 0.55f) {
                ItemAbility.RAGNAROCK_AXE.sound()
            }
        }
        if (event.soundName == "liquid.lavapop") {
            if (event.pitch == 0.7619048f && event.volume == 0.15f) {
                ItemAbility.WAND_OF_ATONEMENT.sound()
            }
        }
        if (event.soundName == "mob.bat.hurt") {
            if (event.volume == 0.1f) {
                ItemAbility.STARLIGHT_WAND.sound()
            }
        }
        if (event.soundName == "mob.guardian.curse") {
            if (event.volume == 0.2f) {
                ItemAbility.VOODOO_DOLL.sound()
            }
        }
        if (event.soundName == "random.explode") {
            if (event.pitch == 4.047619f && event.volume == 0.2f) {
                ItemAbility.GOLEM_SWORD.sound()
            }
        }
        if (event.soundName == "mob.wolf.howl") {
            if (event.volume == 0.5f) {
                ItemAbility.WEIRD_TUBA.sound()
            }
        }
        if (event.soundName == "mob.zombie.unfect") {
            if (event.pitch == 2.0f && event.volume == 0.3f) {
                ItemAbility.END_STONE_SWORD.sound()
            }
        }
        if (event.soundName == "mob.wolf.panting") {
            if (event.pitch == 1.3968254f && event.volume == 0.4f) {
                ItemAbility.SOUL_ESOWARD.sound()
            }
        }
        if (event.soundName == "mob.zombiepig.zpigangry") {
            if (event.pitch == 2.0f && event.volume == 0.3f) {
                ItemAbility.PIGMAN_SWORD.sound()
            }
        }
        if (event.soundName == "mob.ghast.fireball") {
            if (event.pitch == 1.0f && event.volume == 0.3f) {
                ItemAbility.EMBER_ROD.sound()
            }
        }
        if (event.soundName == "mob.guardian.elder.idle") {
            if (event.pitch == 2.0f && event.volume == 0.2f) {
                ItemAbility.FIRE_FREEZE_STAFF.sound()
            }
        }
        if (event.soundName == "random.explode") {
            if (event.pitch == 0.4920635f && event.volume == 0.5f) {
                ItemAbility.STAFF_OF_THE_VOLCANO.sound()
            }
        }
        if (event.soundName == "random.eat") {
            if (event.pitch == 1.0f && event.volume == 1.0f) {
                ItemAbility.STAFF_OF_THE_VOLCANO.sound()
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
        if (!LorenzUtils.inSkyBlock) return
        itemInHand?.getInternalName()?.run {
            ItemAbility.getByInternalName(this)?.setItemClick()
        }
    }

    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        if (!isEnabled()) return

        val message: String = event.message
        if (message.contains(" (§6")) {
            if (message.contains("§b) ")) {
                val name: String = message.between(" (§6", "§b) ")
                if (name == lastAbility) return
                lastAbility = name
                for (ability in ItemAbility.values()) {
                    if (ability.abilityName == name) {
                        click(ability)
                        return
                    }
                }
                return
            }
        }
        lastAbility = ""
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.itemAbilities.itemAbilityCooldown
    }

    private fun click(ability: ItemAbility) {
        if (ability.actionBarDetection) {
            ability.activate()
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        tick++
        if (tick % 2 == 0) {
            checkHotbar()
        }
    }

    private fun checkHotbar() {
        items.clear()
        for ((stack, _) in ItemUtils.getItemsInInventoryWithSlots(true)) {
            val ability = hasAbility(stack)
            if (ability != null) {

                if (ability.isOnCooldown()) {
                    val duration: Long = ability.lastActivation + ability.getCooldown() - System.currentTimeMillis()
                    val color = ability.specialColor ?: if (duration < 600) LorenzColor.RED else LorenzColor.YELLOW
                    items[stack] = ItemText(color, ability.getDurationText(), true)
                } else {
                    ability.specialColor = null
                    items[stack] = ItemText(LorenzColor.GREEN, "R", false)
                }
            }
        }

    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!isEnabled()) return

        val stack = event.stack

        val guiOpen = Minecraft.getMinecraft().currentScreen != null
        val itemText = items.filter { it.key == stack }
            .firstNotNullOfOrNull { it.value } ?: return
        if (guiOpen && !itemText.onCooldown) return

        val color = itemText.color
        event.stackTip = color.getChatColor() + itemText.text

        if (SkyHanniMod.feature.itemAbilities.itemAbilityCooldownBackground) {
            var opacity = 130
            if (color == LorenzColor.GREEN) {
                opacity = 80
            }
            stack.background = color.addOpacity(opacity).rgb
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        val message = event.message
        if (message == "§dCreeper Veil §r§aActivated!") {
            ItemAbility.WITHER_CLOAK.activate(LorenzColor.LIGHT_PURPLE)
        }
        if (message == "§dCreeper Veil §r§cDe-activated! §r§8(Expired)" ||
            message == "§cNot enough mana! §r§dCreeper Veil §r§cDe-activated!"
        ) {
            ItemAbility.WITHER_CLOAK.activate()
        }
        if (message == "§dCreeper Veil §r§cDe-activated!") {
            ItemAbility.WITHER_CLOAK.activate(null, -5000L)
        }
    }

    private fun hasAbility(stack: ItemStack): ItemAbility? {
        val itemName: String = stack.cleanName()
        val internalName = stack.getInternalName()

        for (ability in ItemAbility.values()) {
            if (ability.newVariant) {
                if (ability.internalNames.contains(internalName)) {
                    return ability
                }
            } else {
                for (name in ability.itemNames) {
                    if (itemName.contains(name)) {
                        return ability
                    }
                }
            }
        }
        return null
    }

    private fun ItemAbility.sound() {
        val ping = System.currentTimeMillis() - lastItemClick
        if (ping < 400) {
            activate()
        }
    }

    class ItemText(val color: LorenzColor, val text: String, val onCooldown: Boolean)
}
