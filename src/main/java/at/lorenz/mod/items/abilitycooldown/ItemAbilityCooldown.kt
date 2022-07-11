package at.lorenz.mod.items.abilitycooldown

import at.lorenz.mod.LorenzMod
import at.lorenz.mod.events.GuiRenderItemEvent
import at.lorenz.mod.events.LorenzActionBarEvent
import at.lorenz.mod.utils.ItemUtils
import at.lorenz.mod.utils.ItemUtils.cleanName
import at.lorenz.mod.utils.LorenzColor
import at.lorenz.mod.utils.LorenzUtils
import at.lorenz.mod.utils.LorenzUtils.between
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class ItemAbilityCooldown {

    var lastAbility = ""
    var tick = 0
    val items = mutableMapOf<ItemStack, ItemText>()
    val witherImpactDetection = WitherImpactDetection(this)

    init {
        MinecraftForge.EVENT_BUS.register(witherImpactDetection)
    }

    fun clickWitherImpact() {
        Ability.WITHER_IMPACT.click()
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
                for (ability in Ability.values()) {
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
        return LorenzUtils.inSkyblock && LorenzMod.feature.items.itemAbilityCooldown
    }

    private fun click(ability: Ability) {
//        if (ability.isActive()) return
        if (!ability.actionBarDetection) return
        ability.click()
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
        for ((stack, slot) in ItemUtils.getItemsInInventoryWithSlots(true)) {
//            val inHotbar = slot in 36..43

            val itemName: String = stack.cleanName()
            val ability = hasAbility(itemName)
            if (ability != null) {

                if (ability.isOnCooldown()) {
                    val duration: Long = ability.lastClick + ability.getCooldown() - System.currentTimeMillis()
                    val color = if (duration < 600) LorenzColor.RED else LorenzColor.YELLOW
                    items[stack] = ItemText(color, ability.getDurationText(), true)
                } else {
                    items[stack] = ItemText(LorenzColor.GREEN, "R", false)
                }
            }
        }

    }

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.Post) {
        if (!isEnabled()) return

        val item = event.stack ?: return
//        if (item.stackSize != 1 || item.tagCompound?.hasKey("SkytilsNoItemOverlay") == true) return
        if (item.stackSize != 1) return

        var stackTip = ""

        val guiOpen = Minecraft.getMinecraft().currentScreen != null
        val itemText = items.filter { it.key == item }
            .firstNotNullOfOrNull { it.value } ?: return
        if (guiOpen && !itemText.onCooldown) return

        stackTip = itemText.color.getChatColor() + itemText.text

        if (stackTip.isNotEmpty()) {
            GlStateManager.disableLighting()
            GlStateManager.disableDepth()
            GlStateManager.disableBlend()
            event.fr.drawStringWithShadow(
                stackTip,
                (event.x + 17 - event.fr.getStringWidth(stackTip)).toFloat(),
                (event.y + 9).toFloat(),
                16777215
            )
            GlStateManager.enableLighting()
            GlStateManager.enableDepth()
        }
    }

//    @SubscribeEvent
//    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.Pre) {
//        if (!isEnabled()) return
//
//        val item = event.stack ?: return
////        if (item.stackSize != 1 || item.tagCompound?.hasKey("SkytilsNoItemOverlay") == true) return
//        if (item.stackSize != 1) return
//
//        var stackTip = ""
//
//        val isActive = Minecraft.getMinecraft().currentScreen == null
//        val itemText = items.filter { it.key == item && it.value.active }
//            .firstNotNullOfOrNull { it.value }
//
//        if (itemText != null) return
////        if (!isActive && !itemText.active) return
//
//        stackTip = "T"
//
////        items.filter { it.key == item }
////            .forEach { stackTip = it.value.color.getChatColor() + it.value.text }
//
//        if (stackTip.isNotEmpty()) {
//            GlStateManager.disableLighting()
//            GlStateManager.disableDepth()
//            GlStateManager.disableBlend()
//            event.fr.drawStringWithShadow(
//                stackTip,
//                (event.x + 17 - event.fr.getStringWidth(stackTip)).toFloat(),
//                (event.y + 9).toFloat(),
//                16777215
//            )
//            GlStateManager.enableLighting()
//            GlStateManager.enableDepth()
//        }
//    }

//    @SubscribeEvent
//    fun onGuiDrawEvent(event: GuiContainerEvent.BackgroundDrawnEvent) {
//        if (!isEnabled())
//
//        val guiContainer: GuiContainer? = event.gui
//        if (guiContainer != null && guiContainer !is GuiInventory) return
//        val chest = guiContainer.inventorySlots
//
////        val chestName = chest.lowerChestInventory.displayName.unformattedText.trim()
////        if (chestName != "Spirit Leap") return
//
//        for (slot in chest.inventorySlots) {
//            if (slot == null) continue
////            if (slot.slotNumber == slot.slotIndex) continue
//            if (slot.stack == null) continue
//
//            val stack = slot.stack
//
////            for ((item, text) in map) {
////                if (item == stack) {
////                    slot highlight text.color
////                }
////            }
//            map.filter { it.key == stack }.forEach { slot highlight it.value.color }
//
////            slot highlight LorenzColor.WHITE
//
////            val displayName = stack.displayName
////            if (displayName == " ") continue
//
////            val itemLore = stack.getLore()
////            if (itemLore.size == 1 && itemLore[0] == "§eClick to teleport!") {
////
////                if (displayName.contains(witherDoorName)) {
////                    if (lastWitherDoorOpened + 10_000 > System.currentTimeMillis()) {
////                        slot highlight LorenzColor.YELLOW
////                        return
////                    }
////                }
////                if (displayName.contains(teleportName)) {
////                    if (lastTeleport + 10_000 > System.currentTimeMillis()) {
////                        slot highlight LorenzColor.AQUA
////                        return
////                    }
////                }
////            } else {
////                //TODO hide the item totally?
////                slot highlight LorenzColor.RED
////            }
//
//        }
//    }

    private fun hasAbility(itemName: String): Ability? {
        for (ability in Ability.values()) {
            for (name in ability.itemNames) {
                if (itemName.contains(name)) {
                    return ability
                }
            }
        }
        return null
    }

    enum class Ability(
        val abilityName: String,
        val cooldownInSeconds: Long,
        vararg val itemNames: String,
        var lastClick: Long = 0L,
        val actionBarDetection: Boolean = true
    ) {
        ATOMSPLIT("Soulcry", 4, "Atomsplit Katana", "Vorpal Katana", "Voidedge Katana"),
        WITHER_IMPACT("Wither Impact", 5, "Hyperion", "Scylla", "Valkyrie", "Astrea", actionBarDetection = false),

        HEAL_1("Small Heal", 7, "Wand of Healing"),
        HEAL_2("Medium Heal", 7, "Wand of Mending"),
        HEAL_3("Big Heal", 7, "Wand of Restoration"),
        HEAL_4("Huge Heal", 7, "Wand of Atonement"),

        ICE_SPRAY("Ice Spray", 5, "Ice Spray Wand"),
        GYRO("Gravity Storm", 30, "Gyrokinetic Wand"),
        GIANTS_SWORD("Giant's Slam", 30, "Giant's Sword"),

        STAR_FALL("Starfall", 2, "Starlight Wand"),
        VODOO_DOLL("Acupuncture", 5, "Voodoo Doll"),
        INK_WAND("Ink Bomb", 30, "Ink Wand"),
        GOLEM_SWORD("Iron Punch", 3, "Golem Sword"),
        EMBER_ROD("Fire Blast", 30, "Ember Rod"),
        ENDER_BOW("Ender Warp", 30, "Ender Bow"),

        LIVID_DAGGER("Throw", 5, "Livid Dagger"),
        WEIRD_TUBA("Howl", 20, "Weird Tuba"),

        ENDSTONE_SWORD("Extreme Focus", 5, "End Stone Sword"),
        PIGMAN_SWORD("Burning Souls", 5, "Pigman Sword"),

        SOULWARD("Soulward", 20, "Soul Esoward");

        fun click() {
            lastClick = System.currentTimeMillis()
        }

        fun isOnCooldown(): Boolean = lastClick + getCooldown() > System.currentTimeMillis()

        fun getCooldown(): Long = cooldownInSeconds * 1000

        fun getDurationText(): String {
            var duration: Long = lastClick + getCooldown() - System.currentTimeMillis()
            return if (duration < 1600) {
                duration /= 100
                var d = duration.toDouble()
                d /= 10.0
                LorenzUtils.formatDouble(d)
            } else {
                duration /= 1000
                duration++
                LorenzUtils.formatInteger(duration.toInt())
            }
        }

    }

    class ItemText(val color: LorenzColor, val text: String, val onCooldown: Boolean)
}