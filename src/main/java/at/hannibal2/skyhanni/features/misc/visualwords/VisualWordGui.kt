package at.hannibal2.skyhanni.features.misc.visualwords

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.LorenzUtils.chat
import at.hannibal2.skyhanni.utils.StringUtils.convertToFormatted
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.MathHelper
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.io.File
import java.io.FileReader
import java.io.IOException

open class VisualWordGui : GuiScreen() {
    private var guiLeft = 0
    private var guiTop = 0
    private var screenHeight = 0
    private val sizeX = 360
    private val sizeY = 180
    private val maxTextLength = 75

    private var mouseX = 0
    private var mouseY = 0
    private var lastMouseScroll = 0
    private var noMouseScrollFrames = 0

    private var pageScroll = 0
    private var scrollVelocity = 0.0
    private val maxNoInputFrames = 100

    private var lastClickedHeight = 0
    private var lastClickedWidth = 0
    private var changedIndex = -1
    private var changedAction = ""

    private var currentlyEditing = false
    private var currentIndex = -1

    private var currentTextBox = ""
    private var currentText = ""

    private var modifiedWords = mutableListOf<VisualWord>()

    private val shouldDrawImport get() = sbeConfigPath.exists() && !SkyHanniMod.feature.storage.visualWordsImported

    companion object {
        fun isInGui() = Minecraft.getMinecraft().currentScreen is VisualWordGui
        var sbeConfigPath = File("." + File.separator + "config" + File.separator + "SkyblockExtras.cfg")
    }

    override fun drawScreen(unusedX: Int, unusedY: Int, partialTicks: Float) {
        super.drawScreen(unusedX, unusedY, partialTicks)
        drawDefaultBackground()
        screenHeight = height
        guiLeft = (width - sizeX) / 2
        guiTop = (height - sizeY) / 2

        mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth
        mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1

        GlStateManager.pushMatrix()
        drawRect(guiLeft, guiTop, guiLeft + sizeX, guiTop + sizeY, 0x50000000)
        val scale = 0.75f
        val inverseScale = 1 / scale

        if (!currentlyEditing) {
            val adjustedY = guiTop + 30 + pageScroll
            var toRemove = -1

            val x = guiLeft + 180
            val y = guiTop + 170

            GuiRenderUtils.drawStringCentered("§aAdd New", x, y)
            val colour =
                if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x - 30, y - 10, 60, 20)) 0x50828282 else 0x50303030
            drawRect(x - 30, y - 10, x + 30, y + 10, colour)

            if (shouldDrawImport){
                val importX = guiLeft + sizeX - 45
                val importY = guiTop + sizeY - 10
                GuiRenderUtils.drawStringCentered("§aImport from SBE", importX, importY)
                val importColor =
                    if (GuiRenderUtils.isPointInRect(mouseX, mouseY, importX - 45, importY - 10, 90, 20)) 0x50828282 else 0x50303030
                drawRect(importX - 45, importY - 10, importX + 45, importY + 10, importColor)
            }

            GlStateManager.scale(scale, scale, 1f)

            GuiRenderUtils.drawStringCentered(
                "§7Modify Words. Replaces the top with the bottom",
                (guiLeft + 180) * inverseScale,
                (guiTop + 9) * inverseScale
            )
            GuiRenderUtils.drawString("§bPhrase", (guiLeft + 30) * inverseScale, (guiTop + 5) * inverseScale)
            GuiRenderUtils.drawString("§1Status", (guiLeft + 310) * inverseScale, (guiTop + 5) * inverseScale)

            for ((index, phrase) in modifiedWords.withIndex()) {
                if (adjustedY + 30 * index < guiTop + 20) continue
                if (adjustedY + 30 * index > guiTop + 125) continue

                if (phrase.phrase == "" && phrase.replacement == "") {
                    toRemove = index
                }

                var inBox = false
                if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft, adjustedY + 30 * index, sizeX, 30)) inBox = true

                GuiRenderUtils.drawString("${index + 1}.", (guiLeft + 5) * inverseScale, (adjustedY + 10 + 30 * index) * inverseScale)

                if (GuiRenderUtils.isPointInRect(lastClickedWidth, lastClickedHeight, guiLeft + 335, adjustedY + 30 * index + 7, 16, 16)) {
                    lastClickedWidth = 0
                    lastClickedHeight = 0
                    phrase.enabled = !phrase.enabled
                    saveChanges()
                    SoundUtils.playClickSound()
                } else if (GuiRenderUtils.isPointInRect(lastClickedWidth, lastClickedHeight, guiLeft + 295,
                        adjustedY + 30 * index + 7, 16, 16) && index != 0) {
                    lastClickedWidth = 0
                    lastClickedHeight = 0
                    SoundUtils.playClickSound()
                    changedIndex = index
                    changedAction = "up"
                } else if (GuiRenderUtils.isPointInRect(lastClickedWidth, lastClickedHeight, guiLeft + 315,
                        adjustedY + 30 * index + 7, 16, 16) && index != modifiedWords.size - 1) {
                    lastClickedWidth = 0
                    lastClickedHeight = 0
                    SoundUtils.playClickSound()
                    changedIndex = index
                    changedAction = "down"
                } else if (GuiRenderUtils.isPointInRect(lastClickedWidth, lastClickedHeight, guiLeft, adjustedY + 30 * index, sizeX, 30)) {
                    lastClickedWidth = 0
                    lastClickedHeight = 0
                    SoundUtils.playClickSound()
                    currentlyEditing = true
                    currentIndex = index
                }

                if (inBox) {
                    GuiRenderUtils.drawScaledRec(guiLeft, adjustedY + 30 * index, guiLeft + sizeX, adjustedY + 30 * index + 30, 0x50303030, inverseScale)
                }

                val statusBlock = if (phrase.enabled) {
                    ItemStack(Blocks.stained_hardened_clay, 1, 13)
                } else {
                    ItemStack(Blocks.stained_hardened_clay, 1, 14)
                }

                GlStateManager.scale(inverseScale, inverseScale, 1f)

                if (index != 0) {
                    val skullItem = ItemUtils.createSkull("Up", "7f68dd73-1ff6-4193-b246-820975d6fab1", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzczMzRjZGRmYWI0NWQ3NWFkMjhlMWE0N2JmOGNmNTAxN2QyZjA5ODJmNjczN2RhMjJkNDk3Mjk1MjUxMDY2MSJ9fX0=")
                    GuiRenderUtils.renderItemAndBackground(skullItem, guiLeft + 295, adjustedY + 30 * index + 7, 0x50828282)
                }
                if (index != modifiedWords.size - 1) {
                    val skullItem = ItemUtils.createSkull("Down", "e4ace6de-0629-4719-aea3-3e113314dd3f", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTc3NDIwMzRmNTlkYjg5MGM4MDA0MTU2YjcyN2M3N2NhNjk1YzQzOTlkOGUwZGE1Y2U5MjI3Y2Y4MzZiYjhlMiJ9fX0=")
                    GuiRenderUtils.renderItemAndBackground(skullItem, guiLeft + 315, adjustedY + 30 * index + 7, 0x50828282)
                }

                GuiRenderUtils.renderItemAndBackground(statusBlock, guiLeft + 335, adjustedY + 30 * index + 7, 0x50828282)

                GlStateManager.scale(scale, scale, 1f)

                if (inBox) {
                    GuiRenderUtils.drawString(phrase.phrase, (guiLeft + 15) * inverseScale, (adjustedY + 5 + 30 * index) * inverseScale)
                    GuiRenderUtils.drawString(phrase.replacement, (guiLeft + 15) * inverseScale, (adjustedY + 15 + 30 * index) * inverseScale)
                } else {
                    GuiRenderUtils.drawString(phrase.phrase.convertToFormatted(), (guiLeft + 15) * inverseScale, (adjustedY + 5 + 30 * index) * inverseScale)
                    GuiRenderUtils.drawString(phrase.replacement.convertToFormatted(), (guiLeft + 15) * inverseScale, (adjustedY + 15 + 30 * index) * inverseScale)
                }
            }

            if (modifiedWords.size < 1) {
                modifiedWords = SkyHanniMod.feature.storage.modifiedWords
            }

            if (toRemove != -1) {
                modifiedWords.removeAt(toRemove)
                saveChanges()
            }

            GlStateManager.scale(inverseScale, inverseScale, 1f)

            scrollScreen()
        }
        else {
            val x = guiLeft + 180
            var y = guiTop + 140
            GuiRenderUtils.drawStringCentered("§cDelete", x, y)
            var colour = if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x - 30, y - 10, 60, 20)) 0x50828282 else 0x50303030
            drawRect(x - 30, y - 10, x + 30, y + 10, colour)
            y += 30
            GuiRenderUtils.drawStringCentered("§eBack", x, y)
            colour = if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x - 30, y - 10, 60, 20)) 0x50828282 else 0x50303030
            drawRect(x - 30, y - 10, x + 30, y + 10, colour)

            if (currentIndex < modifiedWords.size && currentIndex != -1) {
                y -= 150
                val currentPhrase = modifiedWords[currentIndex]
                val status = if (currentPhrase.enabled) "§2Enabled" else "§4Disabled"
                GuiRenderUtils.drawStringCentered(status, x, y)
                colour = if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x - 30, y - 10, 60, 20)) 0x50828282 else 0x50303030
                drawRect(x - 30, y - 10, x + 30, y + 10, colour)

                GuiRenderUtils.drawString("§bIs replaced by:", guiLeft + 30, guiTop + 75)

                if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft, guiTop + 35, sizeX, 30)) {
                    drawRect(guiLeft, guiTop + 35, guiLeft + sizeX, guiTop + 35 + 30, 0x50303030)
                }
                if (currentTextBox == "phrase") {
                    drawRect(guiLeft, guiTop + 35, guiLeft + sizeX, guiTop + 35 + 30, 0x50828282)
                }

                if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft, guiTop + 90, sizeX, 30)) {
                    drawRect(guiLeft, guiTop + 90, guiLeft + sizeX, guiTop + 90 + 30, 0x50303030)
                }
                if (currentTextBox == "replacement") {
                    drawRect(guiLeft, guiTop + 90, guiLeft + sizeX, guiTop + 90 + 30, 0x50828282)
                }

                GlStateManager.scale(0.75f, 0.75f, 1f)

                GuiRenderUtils.drawTwoLineString("§bThe top line of each section is the preview of the bottom text",
                    (guiLeft + 10) * inverseScale, (guiTop + 17) * inverseScale)
                GuiRenderUtils.drawTwoLineString("§bTo get the minecraft formatting sign use \"&&\"",
                    (guiLeft + 220) * inverseScale, (guiTop + 17) * inverseScale)

                GuiRenderUtils.drawString(currentPhrase.phrase.convertToFormatted(), (guiLeft + 30) * inverseScale, (guiTop + 40) * inverseScale)
                GuiRenderUtils.drawString(currentPhrase.phrase, (guiLeft + 30) * inverseScale, (guiTop + 55) * inverseScale)

                GuiRenderUtils.drawString(currentPhrase.replacement.convertToFormatted(), (guiLeft + 30) * inverseScale, (guiTop + 95) * inverseScale)
                GuiRenderUtils.drawString(currentPhrase.replacement, (guiLeft + 30) * inverseScale, (guiTop + 110) * inverseScale)

                GlStateManager.scale(inverseScale, inverseScale, 1f)
            }
        }

        if (changedIndex != -1) {
            if (changedAction == "up") {
                if (changedIndex > 0) {
                    val temp = modifiedWords[changedIndex]
                    modifiedWords[changedIndex] = modifiedWords[changedIndex - 1]
                    modifiedWords[changedIndex - 1] = temp
                }
            }
            else if (changedAction == "down") {
                if (changedIndex < modifiedWords.size - 1) {
                    val temp = modifiedWords[changedIndex]
                    modifiedWords[changedIndex] = modifiedWords[changedIndex + 1]
                    modifiedWords[changedIndex + 1] = temp
                }
            }

            changedIndex = -1
            changedAction = ""
            saveChanges()
        }

        GlStateManager.popMatrix()
    }

    override fun handleMouseInput() {
        super.handleMouseInput()

        if (Mouse.getEventButtonState()) {
            mouseClickEvent()
        }
        if (!Mouse.getEventButtonState()) {
            if (Mouse.getEventDWheel() != 0) {
                lastMouseScroll = Mouse.getEventDWheel()
                noMouseScrollFrames = 0
            }
        }
    }

    @Throws(IOException::class)
    fun mouseClickEvent() {
        if (!currentlyEditing) {
            if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft, guiTop, sizeX, sizeY - 25)) {
                lastClickedWidth = mouseX
                lastClickedHeight = mouseY
            }
        }
        val x = guiLeft + 180
        var y = guiTop + 140
        if (currentlyEditing) {
            if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x - 30, y - 10, 60, 20)) {
                SoundUtils.playClickSound()
                currentlyEditing = false
                modifiedWords.removeAt(currentIndex)
                currentIndex = -1
                saveChanges()
                currentTextBox = ""
            }
            if (currentIndex < modifiedWords.size && currentIndex != -1) {
                y = guiTop + 20
                if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x - 30, y - 10, 60, 20)) {
                    SoundUtils.playClickSound()
                    modifiedWords[currentIndex].enabled = !modifiedWords[currentIndex].enabled
                    saveChanges()
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft, guiTop + 35, sizeX, 30)) {
                    SoundUtils.playClickSound()
                    currentTextBox = "phrase"
                    currentText = modifiedWords[currentIndex].phrase
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft, guiTop + 90, sizeX, 30)) {
                    SoundUtils.playClickSound()
                    currentTextBox = "replacement"
                    currentText = modifiedWords[currentIndex].replacement
                } else {
                    if (currentTextBox != "") {
                        SoundUtils.playClickSound()
                        currentTextBox = ""
                    }
                }
            }
        }
        y = guiTop + 170
        if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x - 30, y - 10, 60, 20)) {
            SoundUtils.playClickSound()
            if (currentlyEditing) {
                currentIndex = -1
                currentTextBox = ""
            } else {
                modifiedWords.add(VisualWord("", "", true))
                currentTextBox = "phrase"
                currentIndex = modifiedWords.size - 1
                saveChanges()
            }
            currentlyEditing = !currentlyEditing
        }
        if (shouldDrawImport){
            val importX = guiLeft + sizeX - 45
            val importY = guiTop + sizeY - 10
            if (GuiRenderUtils.isPointInRect(mouseX, mouseY, importX - 45, importY - 10, 90, 20)) {
                SoundUtils.playClickSound()
                tryImport()
            }
        }
    }

    @Throws(IOException::class)
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
        if (!currentlyEditing) return
        if (currentTextBox == "") return
        if (currentIndex >= modifiedWords.size || currentIndex == -1) return

        if (keyCode == Keyboard.KEY_ESCAPE) {
            saveTextChanges()
            currentTextBox = ""
            return
        }

        if (keyCode == Keyboard.KEY_BACK) {
            if (currentText.isNotEmpty()) {
                currentText = if (KeyboardManager.isControlKeyDown()) {
                    ""
                } else if (KeyboardManager.isShiftKeyDown()) {
                    val lastSpaceIndex = currentText.lastIndexOf(' ')
                    if (lastSpaceIndex >= 0) {
                        currentText.substring(0, lastSpaceIndex)
                    } else {
                        ""
                    }
                } else {
                    currentText.substring(0, currentText.length - 1)
                }
                saveTextChanges()
            }

            return
        }

        if (currentText.length < maxTextLength && !Character.isISOControl(typedChar)) {
            currentText += typedChar
            saveTextChanges()
            return
        }

        if (KeyboardManager.isPastingKeysDown()) {
            SkyHanniMod.coroutineScope.launch {
                val clipboard = OSUtils.readFromClipboard() ?: ""
                for (char in clipboard) {
                    if (currentText.length < maxTextLength && !Character.isISOControl(char)) {
                        currentText += char
                    }
                }
                saveTextChanges()
            }
        }
    }

    private fun saveTextChanges() {
        if (currentTextBox == "phrase") {
            modifiedWords[currentIndex].phrase = currentText
        } else if (currentTextBox == "replacement") {
            modifiedWords[currentIndex].replacement = currentText
        }
        saveChanges()
    }

    private fun scrollScreen() {
        scrollVelocity += lastMouseScroll / 48.0
        scrollVelocity *= 0.95
        pageScroll += scrollVelocity.toInt() + lastMouseScroll / 24

        noMouseScrollFrames++

        if (noMouseScrollFrames >= maxNoInputFrames) {
            scrollVelocity *= 0.75
        }

        if (pageScroll > 0) {
            pageScroll = 0
        }

        pageScroll = MathHelper.clamp_int(pageScroll, -(SkyHanniMod.feature.storage.modifiedWords.size * 30 - 100), 0)
        lastMouseScroll = 0
    }

    private fun saveChanges() {
        ModifyVisualWords.modifiedWords = modifiedWords
        ModifyVisualWords.textCache.invalidateAll()
        SkyHanniMod.feature.storage.modifiedWords = modifiedWords
    }

    private fun tryImport() {
        if (sbeConfigPath.exists()) {
            val json = ConfigManager.gson.fromJson(
                FileReader(sbeConfigPath),
                JsonObject::class.java
            )
            var importedWords = 0
            var skippedWords = 0
            val lists = json["custom"].asJsonObject["visualWords"].asJsonArray
            loop@ for (line in lists) {
                "(?<from>.*)@-(?<to>.*)@:-(?<state>false|true)".toPattern().matchMatcher(line.asString) {
                    val from = group("from")
                    val to = group("to")
                    val state = group("state").toBoolean()

                    if (modifiedWords.any { it.phrase == from }) {
                        skippedWords++
                        continue@loop
                    }

                    modifiedWords.add(VisualWord(from.replace("&", "&&"), to.replace("&", "&&"), state))
                    importedWords++
                }
            }
            if (importedWords > 0 || skippedWords > 0) {
                chat("§e[SkyHanni] §aSuccessfully imported §e$importedWords §aand skipped §e$skippedWords §aVisualWords from SkyBlockExtras !")
                SkyHanniMod.feature.storage.visualWordsImported = true
            }
        }
    }
}