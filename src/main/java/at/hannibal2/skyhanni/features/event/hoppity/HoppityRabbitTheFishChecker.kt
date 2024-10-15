package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

@SkyHanniModule
object HoppityRabbitTheFishChecker {

    //<editor-fold desc="Patterns">
    /**
     * REGEX-TEST: Chocolate Breakfast Egg
     * REGEX-TEST: Chocolate Lunch Egg
     * REGEX-TEST: Chocolate Dinner Egg
     */
    private val mealEggInventoryPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "inventory.mealegg.name",
        "(?:§.)*Chocolate (?:Breakfast|Lunch|Dinner) Egg.*",
    )

    /**
     * REGEX-TEST: §cRabbit the Fish
     */
    private val rabbitTheFishItemPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "item.rabbitthefish",
        "(?:§.)*Rabbit the Fish",
    )

    /**
     * REGEX-TEST: Click to open Chocolate Factory!
     */
    private val openCfSlotLorePattern by ChocolateFactoryAPI.patternGroup.pattern(
        "inventory.mealegg.continue",
        "(?:§.)*Click to open Chocolate Factory!"
    )
    //</editor-fold>

    private val config get() = SkyHanniMod.feature.event.hoppityEggs
    private var rabbitTheFishIndex: Int? = null

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return

        rabbitTheFishIndex?.let {
            InventoryUtils.getItemsInOpenChest()[it] highlight LorenzColor.RED
        }
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!isEnabled() || !mealEggInventoryPattern.matches(event.inventoryName)) return

        rabbitTheFishIndex = event.inventoryItems.filter { it.value.hasDisplayName() }.entries.firstOrNull {
            rabbitTheFishItemPattern.matches(it.value.displayName)
        }?.key
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() || rabbitTheFishIndex == null) return

        // Prevent opening chocolate factory when Rabbit the Fish is present
        event.slot?.stack?.let {
            if (openCfSlotLorePattern.anyMatches(it.getLore())) {
                event.cancel()
                warn()
            } else if (rabbitTheFishIndex == event.slot.slotNumber) {
                rabbitTheFishIndex = null
            }
        }
    }

    @JvmStatic
    fun shouldContinueWithKeypress(keycode: Int) = !keycode.isInventoryClosure() || !isEnabled() || rabbitTheFishIndex == null
    private fun Int.isInventoryClosure(): Boolean {
        return this == Minecraft.getMinecraft().gameSettings.keyBindInventory.keyCode ||
            this == Keyboard.KEY_ESCAPE
    }

    @JvmStatic
    fun warn() {
        if (!isEnabled() || rabbitTheFishIndex == null) return
        SoundUtils.playErrorSound()
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && HoppityAPI.isHoppityEvent() &&
        config.preventMissingFish
}
