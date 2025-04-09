package at.hannibal2.skyhanni.features.inventory.chocolatefactory.stray

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.inventory.chocolatefactory.CFStrayWarningConfig
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.hoppity.RabbitFoundEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggType
import at.hannibal2.skyhanni.features.event.hoppity.HoppityTextureHandler
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.data.CFDataLoader
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.ItemUtils.getSingleLineLore
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.KeyboardManager.isInventoryClosure
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColorInt
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CFStrayWarning {

    private val config get() = CFApi.config
    private val warningConfig get() = config.rabbitWarning
    private const val CHROMA_COLOR = "249:255:255:85:85"
    private const val CHROMA_COLOR_ALT = "246:255:255:85:85"
    private const val CHROMA_COLOR_ALT2 = "243:255:255:85:85"

    private var flashScreen = false
    private var activeStraySlots: Set<Int> = setOf()

    private fun reset() {
        flashScreen = false
        activeStraySlots = setOf()
    }

    private fun isRarityOrHigher(stack: ItemStack, rarity: LorenzRarity) =
        stack.getSkullTexture()?.let { skullTexture ->
            HoppityTextureHandler.getRarityBySkullId(skullTexture)?.let { skullRarity ->
                skullRarity.ordinal >= rarity.ordinal
            } ?: false
        } ?: false

    private fun isSpecial(stack: ItemStack) =
        CFDataLoader.clickMeGoldenRabbitPattern.matches(stack.displayName) || stack.getSkullTexture() in CFApi.specialRabbitTextures

    private fun shouldWarnAboutStray(item: ItemStack) = when (config.rabbitWarning.rabbitWarningLevel) {
        CFStrayWarningConfig.StrayTypeEntry.SPECIAL -> isSpecial(item)

        CFStrayWarningConfig.StrayTypeEntry.LEGENDARY_P -> isRarityOrHigher(item, LorenzRarity.LEGENDARY)
        CFStrayWarningConfig.StrayTypeEntry.EPIC_P -> isRarityOrHigher(item, LorenzRarity.EPIC)
        CFStrayWarningConfig.StrayTypeEntry.RARE_P -> isRarityOrHigher(item, LorenzRarity.RARE)
        CFStrayWarningConfig.StrayTypeEntry.UNCOMMON_P -> isRarityOrHigher(item, LorenzRarity.UNCOMMON)

        CFStrayWarningConfig.StrayTypeEntry.ALL -> CFDataLoader.clickMeRabbitPattern.matches(item.displayName) || isSpecial(item)

        CFStrayWarningConfig.StrayTypeEntry.NONE -> false
    }

    private fun handleRabbitWarnings(item: ItemStack) {
        if (CFApi.caughtRabbitPattern.matches(item.getSingleLineLore())) return

        val clickMeMatches = CFDataLoader.clickMeRabbitPattern.matches(item.displayName)
        val goldenClickMeMatches = CFDataLoader.clickMeGoldenRabbitPattern.matches(item.displayName)
        if (!clickMeMatches && !goldenClickMeMatches || !shouldWarnAboutStray(item)) return

        val isSpecial = goldenClickMeMatches || item.getSkullTexture() in CFApi.specialRabbitTextures

        if (isSpecial) SoundUtils.repeatSound(100, warningConfig.repeatSound, CFApi.warningSound)
        else SoundUtils.playBeepSound()
    }

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!CFApi.inCf) return
        if (config.partyMode.get()) event.partyModeHighlight()
        else event.strayHighlight()
    }

    private fun GuiContainerEvent.getEventChest(): ContainerChest? =
        gui.inventorySlots as? ContainerChest

    private fun GuiContainerEvent.BackgroundDrawnEvent.partyModeHighlight() {
        val eventChest = getEventChest() ?: return
        eventChest.getUpperItems().keys.forEach { it.highlight(CHROMA_COLOR_ALT.toSpecialColor()) }
        eventChest.inventorySlots.filter {
            it.slotNumber != it.slotIndex
        }.forEach {
            it.highlight(CHROMA_COLOR_ALT2.toSpecialColor())
        }
    }

    private fun GuiContainerEvent.BackgroundDrawnEvent.strayHighlight() {
        val eventChest = getEventChest() ?: return
        eventChest.getUpperItems().keys.filter {
            it.slotNumber in activeStraySlots
        }.forEach {
            it.highlight(warningConfig.inventoryHighlightColor.toSpecialColor())
        }
    }

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!CFApi.inCf) {
            flashScreen = false
            return
        }
        val strayStacks = HoppityApi.filterMayBeStray(event.inventoryItems)
        strayStacks.forEach { handleRabbitWarnings(it.value) }
        val activeStrays = strayStacks.filterValues { !CFApi.caughtRabbitPattern.matches(it.getSingleLineLore()) }
        activeStraySlots = activeStrays.keys
        flashScreen = activeStrays.any {
            val stack = it.value
            when (config.rabbitWarning.flashScreenLevel) {
                CFStrayWarningConfig.StrayTypeEntry.SPECIAL -> isSpecial(stack)

                CFStrayWarningConfig.StrayTypeEntry.LEGENDARY_P -> isRarityOrHigher(stack, LorenzRarity.LEGENDARY)
                CFStrayWarningConfig.StrayTypeEntry.EPIC_P -> isRarityOrHigher(stack, LorenzRarity.EPIC)
                CFStrayWarningConfig.StrayTypeEntry.RARE_P -> isRarityOrHigher(stack, LorenzRarity.RARE)
                CFStrayWarningConfig.StrayTypeEntry.UNCOMMON_P -> isRarityOrHigher(stack, LorenzRarity.UNCOMMON)

                CFStrayWarningConfig.StrayTypeEntry.ALL -> {
                    CFDataLoader.clickMeRabbitPattern.matches(it.value.displayName) || isSpecial(stack)
                }

                CFStrayWarningConfig.StrayTypeEntry.NONE -> false
            }
        }
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        reset()
    }

    @HandleEvent
    fun onRabbitFound(event: RabbitFoundEvent) {
        if (event.eggType != HoppityEggType.STRAY) return
        flashScreen = false
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!CFApi.inCf) return
        if (!flashScreen && !config.partyMode.get()) return
        val minecraft = Minecraft.getMinecraft()
        val alpha = ((2 + sin(System.currentTimeMillis().toDouble() / 1000)) * 255 / 4).toInt().coerceIn(0..255)
        val toUse = if (config.partyMode.get()) CHROMA_COLOR else warningConfig.flashColor
        val color = (alpha shl 24) or (toUse.toSpecialColorInt() and 0xFFFFFF)
        Gui.drawRect(0, 0, minecraft.displayWidth, minecraft.displayHeight, color)
        GlStateManager.color(1F, 1F, 1F, 1F)
    }

    @JvmStatic
    fun shouldContinueWithKeypress(keycode: Int): Boolean {
        val shouldContinue = !keycode.isInventoryClosure() || !warningConfig.blockClosing || activeStraySlots.isEmpty()
        if (!shouldContinue) {
            TitleManager.sendTitle(
                "§cStray Rabbit Prevented Close",
                subtitleText = "§7Hold §eShift §7to bypass",
                duration = 5.seconds,
                location = TitleManager.TitleLocation.INVENTORY
            )
            SoundUtils.playErrorSound()
        }
        return shouldContinue
    }
}
