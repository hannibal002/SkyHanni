package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.CTMUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.isStainedGlassPane
import at.hannibal2.skyhanni.utils.compat.DyeCompat.Companion.isDye
import at.hannibal2.skyhanni.utils.compat.container
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonObject
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.item.ItemStack
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.util.Identifier
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.milliseconds

/**
 * Taken with love (and permission), and adapted from, the NEU source code.
 * https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/src/main/java/io/github/moulberry/notenoughupdates/miscfeatures/BetterContainers.java
 */
@SkyHanniModule
object BetterContainers {

    private val patternGroup = RepoPattern.group("inventory.bettercontainers")

    private val config get() = SkyHanniMod.feature.inventory.improvedSBMenus

    private val x: Identifier = Identifier.of("skyhanni", "dynamic_54")

    private val toggleOff = Identifier.of("skyhanni", "dynamic_54/toggle_off.png")
    private val toggleOn = Identifier.of("skyhanni", "dynamic_54/toggle_on.png")
    private val dynamic54Base = Identifier.of("skyhanni", "dynamic_54/style1/dynamic_54.png")
    private val dynamic54Slot = Identifier.of("skyhanni", "dynamic_54/style1/dynamic_54_slot_ctm.png")
    private val dynamic54Button = Identifier.of("skyhanni", "dynamic_54/style1/dynamic_54_button_ctm.png")
    private val customDynamicChest = Identifier.of("skyhanni", "dynamic_chest_inventory.png")

    private val disallowedInventoryPattern by patternGroup.pattern(
        "disallowed",
        "(?i)navigate the maze.*"
    )
    val disallowedInventory = InventoryDetector(disallowedInventoryPattern)

    val isRendering: Boolean get() = loaded && gpuTex != null
    val isOverriding: Boolean get() = chestOpen && isRendering && !disallowedInventory.isInside()

    private var bufferedImageOn: BufferedImage? = null
    private var bufferedImageOff: BufferedImage? = null
    private var bufferedImageBase: BufferedImage? = null
    private var bufferedImageSlot: BufferedImage? = null
    private var bufferedImageButton: BufferedImage? = null
    private var lastSlots: MutableList<Slot?>? = null

    private var loaded = false
    private var gpuNative: NativeImage? = null
    private var gpuTex: NativeImageBackedTexture? = null
    private var lastClickedSlot = 0
    private var clickedSlot = 0
    private var clickedSlotAt: SimpleTimeMark = SimpleTimeMark.farPast()
    private var lastInvHashcode = 0
    private var chestOpen: Boolean = false
    private var hasItem: Boolean = false
    private var hasNullPane: Boolean = false
    private var textColor: Int = 4210752

    @JvmStatic
    fun getTextColor(): Int {
        return if (!isOverriding) 4210752
        else textColor
    }

    @JvmStatic
    fun slotCanBeHighlighted(slot: Slot): Boolean {
        return if (!isOverriding) slot.canBeHighlighted()
        else !isBlankStack(slot.stack)
    }

    fun reset() {
        loaded = false
        clickedSlot = -1
        clickedSlotAt = SimpleTimeMark.farPast()
        chestOpen = false
        gpuTex = null
        gpuNative = null
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isOverriding) return
        val slot = event.slot ?: return
        val isBlankStack = isBlankStack(slot.stack)
        val isButtonStack = isButtonStack(slot.stack)
        if (!(isBlankStack || isButtonStack)) return
        clickSlot(event.slotId)
        if (isBlankStack) event.makePickblock()
    }

    @HandleEvent
    fun onSlotPre(event: GuiContainerEvent.DrawSlotEvent.GuiContainerDrawSlotPre) {
        if (!isOverriding) return
        val slot = event.slot
        val shouldRender = shouldRenderStack(slot.stack)
        if (!shouldRender) event.cancel()
    }

    @HandleEvent
    fun onGuiContainerPreDraw(event: GuiContainerEvent.PreDraw) {
        if (event.gui !is GenericContainerScreen) return reset()
        chestOpen = SkyBlockUtils.inSkyBlock && config.enabled
        if (!chestOpen) return

        val inventory = (event.container as GenericContainerScreenHandler).inventory
        lastInvHashcode = inventory.hashCode()
        hasItem = (0 until inventory.size()).any { slotIndex ->
            val stack = inventory.getStack(slotIndex)
            stack != null
        }
        hasNullPane = (0 until inventory.size()).any { slotIndex ->
            val stack = inventory.getStack(slotIndex)
            isBlankStack(stack)
        }
    }

    // <editor-fold desc="Resource Reading">
    private fun readImageResources(id: Identifier, altId: Identifier): BufferedImage? =
        readImageResource(id) ?: readImageResource(altId)

    private fun readImageResource(id: Identifier): BufferedImage? = runCatching {
        val mcResource = MinecraftClient.getInstance().resourceManager.getResource(id).get()
        ImageIO.read(mcResource.inputStream)
    }.onFailure {
        ErrorManager.logErrorWithData(it, "Could not read image resource: ${id.path}")
        null
    }.getOrNull()

    private fun readJsonResource(id: Identifier): BufferedReader? = runCatching {
        val mcResource =  MinecraftClient.getInstance().resourceManager.getResource(id).get()
        val streamReader = InputStreamReader(mcResource.inputStream, StandardCharsets.UTF_8)
        BufferedReader(streamReader)
    }.getOrNull()
    // </editor-fold>

    private fun tintMask(mask: BufferedImage, colour: Int): BufferedImage {
        val w = mask.width
        val h = mask.height
        val out = BufferedImage(mask.colorModel, mask.copyData(null), mask.isAlphaPremultiplied, null)
        for (y in 0 until h) for (x in 0 until w) {
            val p = mask.getRGB(x, y)
            val a = p ushr 24 and 0xFF
            if (a < 10) continue
            out.setRGB(x, y, (a shl 24) or (colour and 0xFFFFFF))
        }
        return out
    }

    private fun generateBufferedImages() {
        val backgroundStyle = config.menuBackgroundStyle
        val buttonStyle = config.buttonBackgroundStyle

        textColor = readJsonResource(backgroundStyle.configId)?.use { reader ->
            val newJson = ConfigManager.gson.fromJson(reader, JsonObject::class.java)
            val textColourS = newJson.get("text-colour").asString
            textColourS.toLong(16).toInt()
        } ?: 4210752

        bufferedImageOn = readImageResource(toggleOn)
        bufferedImageOff = readImageResource(toggleOff)

        bufferedImageBase = readImageResources(backgroundStyle.baseId, dynamic54Base)
        bufferedImageSlot = readImageResources(buttonStyle.slotId, dynamic54Slot)
        bufferedImageButton = readImageResources(buttonStyle.buttonId, dynamic54Button)
    }

    private fun shouldRenderStack(stack: ItemStack): Boolean {
        return !isBlankStack(stack) && !isToggleOff(stack) && !isToggleOn(stack)
    }

    fun clickSlot(slot: Int) {
        clickedSlotAt = SimpleTimeMark.now()
        clickedSlot = slot
    }

    fun getClickedSlot(): Int = if (clickedSlotAt.passedSince() <= 500.milliseconds) clickedSlot else -1

    fun isBlankStack(
        stack: ItemStack,
    ): Boolean = stack.isStainedGlassPane(ColoredBlockCompat.BLACK)

    fun isButtonStack(
        stack: ItemStack?,
    ): Boolean {
        val stack = stack ?: return false
        val isGlassPane = stack.isStainedGlassPane()
        val isUnknownInternalName = stack.getInternalNameOrNull() == null
        val isToggle = isToggleOn(stack) || isToggleOff(stack)
        return !isGlassPane && !isUnknownInternalName && !isToggle
    }

    fun isToggleOn(stack: ItemStack): Boolean = isToggleCommon(stack, "disable")
    fun isToggleOff(stack: ItemStack): Boolean = isToggleCommon(stack, "enable")
    fun isToggleCommon(stack: ItemStack, verb: String): Boolean =
        stack.getLore().takeIfNotEmpty()?.last()?.endsWith("Click to $verb!") ?: false

    fun getTextureIdentifier(original: Identifier): Identifier {
        if (!chestOpen) return original
        val inv = (MinecraftClient.getInstance().currentScreen as? GenericContainerScreen)?.container
            ?: return original
        if (inv !is GenericContainerScreenHandler) return original
        val invHash = inv.hashCode()
        if (gpuTex == null || lastClickedSlot != getClickedSlot() || lastInvHashcode != invHash) {
            lastInvHashcode = invHash
            lastClickedSlot = getClickedSlot()
            generateModernTex(inv)
        }

        return if (gpuTex != null && loaded) customDynamicChest
        else original
    }

    private fun uploadDynamicTexture(bImg: BufferedImage) {
        val native = NativeImage(NativeImage.Format.RGBA, bImg.width, bImg.height, false)
        for (y in 0 until bImg.height) {
            for (x in 0 until bImg.width) {
                native.setColorArgb(x, y, bImg.getRGB(x, y))
            }
        }
        val backed = NativeImageBackedTexture(::toString, native)
        MinecraftClient.getInstance().textureManager.registerTexture(customDynamicChest, backed)
        gpuTex = backed
        gpuNative = native
    }

    private fun updateDynamicTexture(bImg: BufferedImage) {
        val native = gpuNative ?: return uploadDynamicTexture(bImg)
        val backed = gpuTex ?: return uploadDynamicTexture(bImg)
        for (y in 0 until bImg.height) {
            for (x in 0 until bImg.width) {
                native.setColorArgb(x, y, bImg.getRGB(x, y))
            }
        }
        backed.upload()
    }

    private fun generateModernTex(handler: GenericContainerScreenHandler) {
        if (!hasItem || !hasNullPane) {
            gpuTex = null
            return
        }

        loaded = true
        val inventorySlots = handler.slots
        if (lastSlots !== inventorySlots) {
            generateBufferedImages()
            lastSlots = inventorySlots
        }

        val handlerInventory = handler.inventory
        val bufferedImageBase = bufferedImageBase ?: return
        val horizontalTexMult = bufferedImageBase.width / 256
        val verticalTexMult = bufferedImageBase.height / 256

        val bufferedImageNew = BufferedImage(
            bufferedImageBase.width,
            bufferedImageBase.height,
            BufferedImage.TYPE_INT_ARGB
        )
        val g = bufferedImageNew.createGraphics()
        g.drawImage(bufferedImageBase, 0, 0, null)
        g.dispose()

        val size = handlerInventory.size()
        val isSlot = Array(9) { BooleanArray(size / 9) }
        val isButton = Array(9) { BooleanArray(size / 9) }

        val unformattedLower = InventoryUtils.openInventoryName()
        val containsStakes = unformattedLower.contains("Stakes")
        val isUltraSequencer = unformattedLower.startsWith("Ultrasequencer") && !containsStakes
        val isSuperpairs = unformattedLower.startsWith("Superpairs") && !containsStakes

        for (index in 0..<size) {
            val stack: ItemStack = handlerInventory.getStack(index) ?: continue
            // Column and row index
            val cI = index % 9
            val rI = index / 9

            val ultraSequencerOverride = isUltraSequencer && stack.isDye()
            val superpairsOverride = isSuperpairs && index > 9 && index < size - 9
            isButton[cI][rI] = when {
                ultraSequencerOverride || superpairsOverride -> false
                else -> isButtonStack(stack)
            }

            // This is weird, but it was a logical flip of a no-op from NEU's code :shrug:
            if (!isButton[cI][rI] || lastClickedSlot != index) {
                isSlot[cI][rI] = !isBlankStack(stack) && !isButton[cI][rI]
            }
        }

        try {
            for (index in 0..<size) {
                val stack: ItemStack = handlerInventory.getStack(index) ?: continue
                val xi = index % 9
                val yi = index / 9

                val isThisButton = isButton[xi][yi]
                val isThisSlot = isSlot[xi][yi]
                if (!isThisButton && !isThisSlot) continue

                val x = 7 * horizontalTexMult + xi * 18 * horizontalTexMult
                val y = 17 * verticalTexMult + yi * 18 * verticalTexMult

                val on: Boolean = isToggleOn(stack)
                val off: Boolean = isToggleOff(stack)

                if (on || off) {
                    for (x2 in 0..17) {
                        for (y2 in 0..17) {
                            val toggle: BufferedImage = (if (on) bufferedImageOn else bufferedImageOff) ?: continue
                            val c = Color(toggle.getRGB(x2, y2), true)
                            if (c.alpha < 10) continue
                            bufferedImageNew.setRGB(x + x2, y + y2, c.rgb)
                        }
                    }
                    continue
                }

                val targetArr = if (isThisButton) isButton else isSlot
                val targetBuffer = (if (isThisButton) bufferedImageButton else bufferedImageSlot) ?: continue

                val up = yi > 0 && targetArr[xi][yi - 1]
                val right = xi < targetArr.size - 1 && targetArr[xi + 1][yi]
                val down = yi < targetArr[xi].size - 1 && targetArr[xi][yi + 1]
                val left = xi > 0 && targetArr[xi - 1][yi]

                val upLeft = yi > 0 && xi > 0 && targetArr[xi - 1][yi - 1]
                val upRight = yi > 0 && xi < targetArr.size - 1 && targetArr[xi + 1][yi - 1]
                val downRight = xi < targetArr.size - 1 && yi < targetArr[xi + 1].size - 1 && targetArr[xi + 1][yi + 1]
                val downLeft = xi > 0 && yi < targetArr[xi - 1].size - 1 && targetArr[xi - 1][yi + 1]

                val ctmData = CTMUtils.CTMData(
                    up, right, down, left,
                    upLeft, upRight, downRight, downLeft
                )
                val ctmIndex: Int = CTMUtils.getCTMIndex(ctmData)

                val rgbArray = targetBuffer.getRGB(
                    (ctmIndex % 12) * 19 * horizontalTexMult,
                    (ctmIndex / 12) * 19 * verticalTexMult,
                    18 * horizontalTexMult,
                    18 * verticalTexMult,
                    null,
                    0,
                    18 * verticalTexMult
                )
                bufferedImageNew.setRGB(
                    x,
                    y,
                    18 * horizontalTexMult,
                    18 * verticalTexMult,
                    rgbArray,
                    0,
                    18 * verticalTexMult
                )
            }

            when (gpuTex) {
                null -> uploadDynamicTexture(bufferedImageNew)
                else -> updateDynamicTexture(bufferedImageNew)
            }
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e,
                "Failed to generate dynamic texture for inventory",
                "lastClickedSlot" to lastClickedSlot,
                "clickedSlot" to clickedSlot,
                "lastInvHashcode" to lastInvHashcode,
                "chestOpen" to chestOpen,
                "hasItem" to hasItem,
                "hasNullPane" to hasNullPane,
                "openInventoryName" to InventoryUtils.openInventoryName()
            )
        }
    }
}
