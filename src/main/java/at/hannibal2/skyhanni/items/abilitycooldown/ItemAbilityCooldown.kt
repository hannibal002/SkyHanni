package at.hannibal2.skyhanni.items.abilitycooldown

import at.hannibal2.skyhanni.ItemRenderBackground.Companion.background
import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.between
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
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.abilities.itemAbilityCooldown
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
        if (item.stackSize != 1) return

        var stackTip = ""

        val guiOpen = Minecraft.getMinecraft().currentScreen != null
        val itemText = items.filter { it.key == item }
            .firstNotNullOfOrNull { it.value } ?: return
        if (guiOpen && !itemText.onCooldown) return

        val color = itemText.color
        stackTip = color.getChatColor() + itemText.text

        item.background = color.addOpacity(120).rgb

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

        SOULWARD("Soulward", 20, "Soul Esoward"),
        ECHO("Echo", 3, "Ancestral Spade");

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