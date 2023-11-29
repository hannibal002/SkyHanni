package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils.openInventoryName
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.item.Item
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

// Delaying key presses by 300ms comes from NotEnoughUpdates
object HarpFeatures {
    private val config get() = SkyHanniMod.feature.inventory.helper.harp
    private var lastClick = SimpleTimeMark.farPast()

    private object KeyIterable : Iterable<Int> {
        override fun iterator() = object : Iterator<Int> {
            private var currentIndex = 0

            override fun hasNext() = currentIndex < 7

            override fun next() = getKey(currentIndex++) ?: throw NoSuchElementException("currentIndex: $currentIndex")
        }
    }

    private val buttonColors = listOf('d', 'e', 'a', '2', '5', '9', 'b')
    private val inventoryTitleRegex by RepoPattern.pattern("harp.inventory", "^Harp.*")

    private fun isHarpGui() = inventoryTitleRegex.matches(openInventoryName())

    fun getKey(index: Int) = when (index) {
        0 -> config.harpKeybinds.key1
        1 -> config.harpKeybinds.key2
        2 -> config.harpKeybinds.key3
        3 -> config.harpKeybinds.key4
        4 -> config.harpKeybinds.key5
        5 -> config.harpKeybinds.key6
        6 -> config.harpKeybinds.key7

        else -> null
    }


    @SubscribeEvent
    fun onGui(event: GuiScreenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.keybinds) return
        if (!isHarpGui()) return
        val chest = event.gui as? GuiChest ?: return

        for ((index, key) in KeyIterable.withIndex()) {
            if (!key.isKeyHeld()) continue
            if (lastClick.passedSince() < 200.milliseconds) break

            Minecraft.getMinecraft().playerController.windowClick(
                chest.inventorySlots.windowId,
                37 + index,
                2,
                3,
                Minecraft.getMinecraft().thePlayer
            ) // middle clicks > left clicks
            lastClick = SimpleTimeMark.now()
            break
        }
    }

    private var openTime: SimpleTimeMark = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onInventoryFullyOpenedEvent(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.guiScale) return
        when {
            event.inventoryName.startsWith("Melody") -> {
                setGUI()
                openTime = SimpleTimeMark.now()
            }

            event.inventoryName.startsWith("Harp") -> {
                setGUI()
            }
        }
    }

    fun updateScale() {
        if (Minecraft.getMinecraft().currentScreen == null) {
            DelayedRun.runDelayed(100.milliseconds) {
                updateScale()
            }
            return
        }
        // Copied from Minecraft Code to update the scale
        val minecraft = Minecraft.getMinecraft()
        val scaledresolution = ScaledResolution(minecraft)
        val i = scaledresolution.scaledWidth
        val j = scaledresolution.scaledHeight
        minecraft.currentScreen.setWorldAndResolution(minecraft, i, j)
    }

    @SubscribeEvent
    fun onInventoryCloseEvent(event: InventoryCloseEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.guiScale) return
        unSetGUI()
    }

    @SubscribeEvent
    fun onLeave(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        if (!config.guiScale) return
        unSetGUI()
    }

    private var guiSetting: Int = 0
    private var isGUIScaled = false

    private fun setGUI() {
        val gameSettings = Minecraft.getMinecraft().gameSettings ?: return
        guiSetting = gameSettings.guiScale
        gameSettings.guiScale = 0
        isGUIScaled = true
        updateScale()
    }

    private fun unSetGUI() {
        if (!isGUIScaled) return
        Minecraft.getMinecraft().gameSettings.guiScale = guiSetting
        isGUIScaled = false
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.quickRestart) return
        if (!InventoryUtils.openInventoryName().startsWith("Melody")) return
        if (event.slot?.slotNumber != 40) return
        if (openTime.passedSince() > 2.seconds) return
        event.container.inventory.indexOfFirst { it.getLore().contains("§aSong is selected!") }.takeIf { it != -1 }?.let {
            event.isCanceled = true
            Minecraft.getMinecraft().playerController.windowClick(
                event.container.windowId,
                it,
                event.clickedButton,
                event.clickType,
                Minecraft.getMinecraft().thePlayer
            )
        }
    }


    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.showNumbers) return
        if (!isHarpGui()) return
        if (Item.getIdFromItem(event.stack.item) != 159) return // Stained hardened clay item id = 159

        // Example: §9| §7Click! will select the 9
        val index = buttonColors.indexOfFirst { it == event.stack.displayName[1] }
        if (index == -1) return // this should never happen unless there's an update

        val keyCode = getKey(index) ?: return
        event.stackTip = KeyboardManager.getKeyName(keyCode).take(3)
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.harpKeybinds", "inventory.helper.harp.keybinds")
        event.move(2, "misc.harpNumbers", "inventory.helper.harp.showNumbers")
    }
}
