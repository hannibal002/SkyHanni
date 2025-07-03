package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.CTMUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.ResourceLocation
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

    private val config get() = SkyHanniMod.feature.inventory.improvedSBMenus

    // Todo steal all of NEU's assets
    private val TOGGLE_OFF = ResourceLocation("notenoughupdates:dynamic_54/toggle_off.png")
    private val TOGGLE_ON = ResourceLocation("notenoughupdates:dynamic_54/toggle_on.png")
    private val DYNAMIC_54_BASE = ResourceLocation("notenoughupdates:dynamic_54/style1/dynamic_54.png")
    private val DYNAMIC_54_SLOT = ResourceLocation("notenoughupdates:dynamic_54/style1/dynamic_54_slot_ctm.png")
    private val DYNAMIC_54_BUTTON = ResourceLocation("notenoughupdates:dynamic_54/style1/dynamic_54_button_ctm.png")
    private val rl = ResourceLocation("notenoughupdates:dynamic_chest_inventory.png")

    val disallowedInventory = InventoryDetector { name ->
        name.lowercase().trim().startsWith("navigate the maze")
    }

    val isRendering: Boolean get() = (loaded && texture != null) || lastRenderAt.passedSince() < 200.milliseconds
    val isOverriding: Boolean get() = isChestOpen && isRendering && !disallowedInventory.isInside()

    var bufferedImageOn: BufferedImage? = null
    var bufferedImageOff: BufferedImage? = null
    var bufferedImageBase: BufferedImage? = null
    var bufferedImageSlot: BufferedImage? = null
    var bufferedImageButton: BufferedImage? = null
    var lastSlots: MutableList<Slot?>? = null

    private var loaded = false
    private var texture: DynamicTexture? = null
    var textColor: Int = 4210752
        private set

    private var lastClickedSlot = 0
    private var clickedSlot = 0
    private var clickedSlotAt: SimpleTimeMark = SimpleTimeMark.farPast()
    var lastRenderAt: SimpleTimeMark = SimpleTimeMark.farPast()

    private var lastInvHashcode = 0

    // Todo rename this to something more appropriate?
    fun reset() {
        loaded = false
        clickedSlot = -1
        clickedSlotAt = SimpleTimeMark.farPast()
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isOverriding) return
        val slot = event.slot ?: return
        val isBlankStack = isBlankStack(slot.slotNumber, slot.stack)
        val isButtonStack = isButtonStack(slot.slotNumber, slot.stack)
        if (!(isBlankStack || isButtonStack)) return
        clickSlot(event.slotId)
        if (isBlankStack) event.makePickblock()
    }

    // <editor-fold desc="Resource Reading">
    private fun readImageResources(rl: ResourceLocation, altRl: ResourceLocation): BufferedImage? =
        readImageResource(rl) ?: readImageResource(altRl)

    private fun readImageResource(rl: ResourceLocation): BufferedImage? = runCatching {
        val mcResource = Minecraft.getMinecraft().resourceManager.getResource(rl)
        ImageIO.read(mcResource.inputStream)
    }.onFailure {
        ErrorManager.logErrorWithData(it, "Could not read image resource: ${rl.resourcePath}")
        null
    }.getOrNull()

    private fun readJsonResource(rl: ResourceLocation): BufferedReader? = runCatching {
        val mcResource = Minecraft.getMinecraft().resourceManager.getResource(rl)
        val streamReader = InputStreamReader(mcResource.inputStream, StandardCharsets.UTF_8)
        BufferedReader(streamReader)
    }.onFailure {
        ErrorManager.logErrorWithData(it, "Could not read JSON resource: ${rl.resourcePath}")
        null
    }.getOrNull()
    // </editor-fold>


    enum class BackgroundStyle(private val displayName: String) {
        DARK_1("Dark 1"),
        DARK_2("Dark 2"),
        TRANSPARENT("Transparent"),
        LIGHT_1("Light 1"),
        LIGHT_2("Light 2"),
        LIGHT_3("Light 3"),
        ;

        private val resourceIndex = ordinal + 1
        val configResourceL = ResourceLocation("notenoughupdates:dynamic_54/style$resourceIndex/dynamic_config.json")
        val baseResourceL = ResourceLocation("notenoughupdates:dynamic_54/style$resourceIndex/dynamic_54.png")
        val slotResourceL = ResourceLocation("notenoughupdates:dynamic_54/style$resourceIndex/dynamic_54_slot_ctm.png")
        val buttonResourceL = ResourceLocation("notenoughupdates:dynamic_54/style$resourceIndex/dynamic_54_button_ctm.png")
    }

    private fun generateBufferedImages() {
        val backgroundStyle = config.menuBackgroundStyle
        val buttonStyle = config.buttonBackgroundStyle

        textColor = readJsonResource(backgroundStyle.configResourceL)?.use { reader ->
            val newJson = ConfigManager.gson.fromJson(reader, JsonObject::class.java)
            val textColourS = newJson.get("text-colour").asString
            textColourS.toLong(16).toInt()
        } ?: 4210752

        bufferedImageOn = readImageResource(TOGGLE_ON)
        bufferedImageOff = readImageResource(TOGGLE_OFF)

        bufferedImageBase = readImageResources(backgroundStyle.baseResourceL, DYNAMIC_54_BASE)
        bufferedImageSlot = readImageResources(buttonStyle.slotResourceL, DYNAMIC_54_SLOT)
        bufferedImageButton = readImageResources(buttonStyle.buttonResourceL, DYNAMIC_54_BUTTON)
    }

    // Used by NEU's mixin
    fun shouldRenderStack(index: Int, stack: ItemStack?): Boolean {
        return !isBlankStack(index, stack) && !isToggleOff(stack) && !isToggleOn(stack)
    }






    fun clickSlot(slot: Int) {
        clickedSlotAt = SimpleTimeMark.now()
        clickedSlot = slot
    }

    fun getClickedSlot(): Int = if (clickedSlotAt.passedSince() <= 500.milliseconds) clickedSlot else -1

    fun isBlankStack(
        index: Int,
        stack: ItemStack?,
    ): Boolean {
        val stack = stack?.takeIf { it.hasDisplayName() && it.displayName.trim().isEmpty() } ?: return false
        val isGlassPane = stack.item == Item.getItemFromBlock(Blocks.stained_glass_pane)
        val itemDamageCorrect = stack.getItemDamage() == 15
        return isGlassPane && itemDamageCorrect
    }

    fun isButtonStack(
        index: Int,
        stack: ItemStack?,
    ): Boolean {
        val stack = stack ?: return false
        val isGlassPane = stack.item == Item.getItemFromBlock(Blocks.stained_glass_pane)
        val isUnknownInternalName = stack.getInternalNameOrNull() == null
        val isToggle = isToggleOn(stack) || isToggleOff(stack)
        return !isGlassPane && !isUnknownInternalName && !isToggle
    }

    // Todo try to replace all tag compound uses with just... getLore()
    fun isToggleCommon(stack: ItemStack?, verb: String): Boolean {
        val displayCompound = stack?.tagCompound?.takeIf {
            it.hasKey("display", 10)
        }?.getCompoundTag("display")?.takeIf {
            it.hasKey("Lore", 9)
        } ?: return false

        val lore = displayCompound.getTagList("Lore", 8)?.takeIf { it.tagCount() == 1 } ?: return false
        return lore.getStringTagAt(0).equals(
            EnumChatFormatting.GRAY.toString() + "click to disable!",
            ignoreCase = true
        )
    }

    fun isToggleOn(stack: ItemStack?): Boolean = isToggleCommon(stack, "disable")
    fun isToggleOff(stack: ItemStack?): Boolean = isToggleCommon(stack, "enable")

    // Todo we probably need our own mixin for this
    fun bindHook(textureManager: TextureManager, location: ResourceLocation?) {
        if (isChestOpen) {
            val container = (Minecraft.getMinecraft().currentScreen as GuiChest).inventorySlots
            val invHashcode = container.inventory.hashCode()

            if ((texture != null && lastClickedSlot != getClickedSlot()) || !loaded || lastInvHashcode != invHashcode) {
                lastInvHashcode = invHashcode
                lastClickedSlot = getClickedSlot()
                generateTex()
            }
            if (texture != null && loaded) {
                lastRenderAt = SimpleTimeMark.now()

                GlStateManager.color(1f, 1f, 1f, 1f)
                textureManager.loadTexture(rl, texture)
                textureManager.bindTexture(rl)
                return
            }
        } else if (lastRenderAt.passedSince() < 200.milliseconds && texture != null) {
            GlStateManager.color(1f, 1f, 1f, 1f)
            textureManager.loadTexture(rl, texture)
            textureManager.bindTexture(rl)
            return
        }
        GlStateManager.enableBlend()
        textureManager.bindTexture(location)
    }

    // Todo monster function, split this up and uh, refactor it
    private fun generateTex() {
        if (!hasItem()) return

        // Todo basically anything using (current screen as ... ).inventorySlots
        //  should be replaced with InventoryUtils of some sort..
        loaded = true
        val container = (Minecraft.getMinecraft().currentScreen as GuiChest).inventorySlots
        val inventorySlots = (Minecraft.getMinecraft().currentScreen as GuiChest).inventorySlots.inventorySlots
        if (!hasNullPane() || container !is ContainerChest) {
            texture = null
            return
        }

        if (lastSlots !== inventorySlots) {
            generateBufferedImages()
            lastSlots = inventorySlots
        }

        val bufferedImageBase = bufferedImageBase ?: return
        val horizontalTexMult = bufferedImageBase.width / 256
        val verticalTexMult = bufferedImageBase.height / 256
        val bufferedImageNew = BufferedImage(
            bufferedImageBase.colorModel,
            bufferedImageBase.copyData(null),
            bufferedImageBase.isAlphaPremultiplied,
            null
        )

        val lower = container.lowerChestInventory
        val size = lower.sizeInventory

        val isSlot = Array(9) { BooleanArray(size / 9) }
        val isButton = Array(9) { BooleanArray(size / 9) }

        val unformattedLower = lower.displayName.unformattedText
        val containsStakes = unformattedLower.contains("Stakes")
        val isUltraSequencer = unformattedLower.startsWith("Ultrasequencer") && !containsStakes
        val isSuperpairs = unformattedLower.startsWith("Superpairs") && !containsStakes

        for (index in 0..<size) {
            val stack: ItemStack = getStackFromInventory(lower, index) ?: continue
            // Column and row index
            val cI = index % 9
            val rI = index / 9

            val ultraSequencerOverride = isUltraSequencer && stack.item === Items.dye
            val superpairsOverride = isSuperpairs && index > 9 && index < size - 9
            isButton[cI][rI] = when {
                ultraSequencerOverride || superpairsOverride -> false
                else -> isButtonStack(index, stack)
            }

            // This is weird, but it was a logical flip of a no-op from NEU's code :shrug:
            if (!isButton[cI][rI] || lastClickedSlot != index) {
                isSlot[cI][rI] = !isBlankStack(index, stack) && !isButton[cI][rI]
            }
        }

        try {
            for (index in 0..<size) {
                val stack: ItemStack = getStackFromInventory(lower, index) ?: continue
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

            when (texture) {
                null -> texture = DynamicTexture(bufferedImageNew)
                else -> {
                    bufferedImageNew.getRGB(
                        0, 0, bufferedImageNew.width, bufferedImageNew.height,
                        texture?.textureData, 0, bufferedImageNew.width
                    )
                    texture?.updateDynamicTexture()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val isChestOpen: Boolean
        get() = Minecraft.getMinecraft().currentScreen is GuiChest &&
            SkyBlockUtils.inSkyBlock && config.enabled

    private fun hasItem(): Boolean {
        if (!isChestOpen) return false
        val container = (Minecraft.getMinecraft().currentScreen as GuiChest).inventorySlots
        if (container is ContainerChest) {
            val lower = container.lowerChestInventory
            val size = lower.sizeInventory
            for (index in 0..<size) {
                if (getStackFromInventory(lower, index) != null) return true
            }
        }
        return false
    }

    private fun getStackFromInventory(lower: IInventory, index: Int): ItemStack? {
        return lower.getStackInSlot(index)
    }

    private fun hasNullPane(): Boolean {
        if (!isChestOpen) return false
        val container = (Minecraft.getMinecraft().currentScreen as GuiChest).inventorySlots
        if (container is ContainerChest) {
            val lower = container.lowerChestInventory
            val size = lower.sizeInventory
            for (index in 0..<size) {
                if (isBlankStack(index, getStackFromInventory(lower, index))) return true
            }
        }
        return false
    }
}
