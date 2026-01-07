package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent.ClickType
import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.isStainedClay
import at.hannibal2.skyhanni.utils.compat.container
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.SimpleContainer
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// Delaying key presses by 300ms comes from NotEnoughUpdates
@SkyHanniModule
object HarpFeatures {

    private val config get() = SkyHanniMod.feature.inventory.helper.harp
    private var lastClick = SimpleTimeMark.farPast()

    private const val CLOSE_BUTTON_SLOT = 40

    private val buttonColors = listOf('d', 'e', 'a', '2', '5', '9', 'b')

    private val patternGroup = RepoPattern.group("harp")

    /**
     * REGEX-TEST: Harp - Amazing Grace
     * REGEX-FAIL: Harpy ➜ Instant Buy
     */
    private val inventoryTitlePattern by patternGroup.pattern(
        "inventory",
        "Harp -.*",
    )
    private val menuTitlePattern by patternGroup.pattern(
        "menu",
        "Melody",
    )
    private val songSelectedPattern by patternGroup.pattern(
        "song.selected",
        "§aSong is selected!",
    )

    private fun isHarpGui(chestName: String) = inventoryTitlePattern.matches(chestName)
    private fun isMenuGui(chestName: String) = menuTitlePattern.matches(chestName)

    @HandleEvent(onlyOnSkyblock = true)
    fun onGui(event: GuiKeyPressEvent) {
        if (!config.keybinds) return
        if (!isHarpGui(InventoryUtils.openInventoryName())) return
        val chest = event.guiContainer as? ContainerScreen ?: return

        for (index in 0..6) {
            val key = getKey(index) ?: error("no key for index $index")
            if (!key.isKeyHeld()) continue
            if (lastClick.passedSince() < 200.milliseconds) break

            event.cancel()

            InventoryUtils.clickSlot(37 + index, chest.container.containerId, mouseButton = 2, mode = ClickType.MIDDLE)
            lastClick = SimpleTimeMark.now()
            break
        }
    }

    private fun getKey(index: Int) = when (index) {
        0 -> config.harpKeybinds.key1
        1 -> config.harpKeybinds.key2
        2 -> config.harpKeybinds.key3
        3 -> config.harpKeybinds.key4
        4 -> config.harpKeybinds.key5
        5 -> config.harpKeybinds.key6
        6 -> config.harpKeybinds.key7

        else -> null
    }

    private var openTime: SimpleTimeMark = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (config.quickRestart && isMenuGui(event.inventoryName)) {
            openTime = SimpleTimeMark.now()
        }
        if (config.guiScale && (isMenuGui(event.inventoryName) || isHarpGui(event.inventoryName))) {
            setGuiScale()
        }
    }

    private fun updateScale() {
        if (Minecraft.getInstance().screen == null) {
            DelayedRun.runNextTick {
                updateScale()
            }
            return
        }
        val minecraft = Minecraft.getInstance()
        RenderSystem.assertOnRenderThread()
        minecraft.window.calculateScale(minecraft.options.guiScale().get(), minecraft.isEnforceUnicode)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryClose() {
        if (!config.guiScale) return
        unSetGuiScale()
    }

    @HandleEvent
    fun onDisconnect(event: ClientDisconnectEvent) {
        if (!config.guiScale) return
        unSetGuiScale()
    }

    @HandleEvent
    fun onIslandChange() {
        if (!config.guiScale) return
        unSetGuiScale()
    }

    private var guiSetting: Int = 0
    private var isGuiScaled = false

    private fun setGuiScale() {
        DelayedRun.runOrNextTick {
            guiSetting = getMinecraftGuiScale()
            setMinecraftGuiScale(0)
            isGuiScaled = true
            updateScale()
        }
    }

    private fun unSetGuiScale() {
        if (!isGuiScaled) return
        DelayedRun.runOrNextTick {
            setMinecraftGuiScale(guiSetting)
            isGuiScaled = false
        }
    }

    private fun getMinecraftGuiScale(): Int {
        val gameSettings = Minecraft.getInstance().options
        RenderSystem.assertOnRenderThread()
        return gameSettings.guiScale().get()
    }

    private fun setMinecraftGuiScale(scale: Int) {
        val gameSettings = Minecraft.getInstance().options
        RenderSystem.assertOnRenderThread()
        gameSettings.guiScale().set(scale)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {

        if (isHarpGui(InventoryUtils.openInventoryName())) {
            if (config.keybinds) {
                // needed to not send duplicate clicks via keybind feature
                if (event.clickType == ClickType.HOTBAR) {
                    event.cancel()
                    return
                }
            }
        }

        if (!config.quickRestart) return
        if (!isMenuGui(InventoryUtils.openInventoryName())) return
        if (event.slot?.index != CLOSE_BUTTON_SLOT) return
        if (openTime.passedSince() > 2.seconds) return
        val indexOfFirst = event.container.slots.filterNotNull().indexOfFirst {
            songSelectedPattern.anyMatches(it.item.getLore())
        }
        indexOfFirst.takeIf { it != -1 }?.let {
            val clickType = event.clickType ?: return
            event.cancel()
            InventoryUtils.clickSlot(it, event.container.containerId, mouseButton = event.clickedButton, mode = clickType)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!config.showNumbers) return
        if (!isHarpGui(InventoryUtils.openInventoryName())) return
        if (!event.stack.isStainedClay()) return

        // Example: §9| §7Click! will select the 9
        val index = buttonColors.indexOfFirst { it == event.stack.hoverName.formattedTextCompatLeadingWhiteLessResets()[1] }
        if (index == -1) return // this should never happen unless there's an update

        val keyCode = getKey(index) ?: return
        event.stackTip = KeyboardManager.getKeyName(keyCode).take(3)
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.harpKeybinds", "inventory.helper.harp.keybinds")
        event.move(2, "misc.harpNumbers", "inventory.helper.harp.showNumbers")
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onToolTip(event: ToolTipEvent) {
        if (!config.hideMelodyTooltip) return
        if (!isHarpGui(InventoryUtils.openInventoryName())) return
        if (event.slot.container !is SimpleContainer) return
        event.cancel()
    }
}
