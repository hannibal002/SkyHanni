package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.EmojiJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorMixinGuiChat
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import org.lwjgl.util.Color

@SkyHanniModule
object EmojiReplacer {
    private val config get() = SkyHanniMod.feature.gui
    private var emojiNameMap: Map<String, Int>? = null
    private var reverseUnicodeToName: Map<String, String>? = null
    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val emojiJson = event.getConstant<EmojiJson>("Emojis")
        emojiNameMap = emojiJson.emojiNames
        reverseUnicodeToName = emojiJson.unicodeNames
    }
    private var stringIndex: Int = -1
    private var emojiEnd = -1
    private var renderedString: String = ""
    private var isShadow = false
    private val emojiResource = ResourceLocation("skyhanni:emoji/emoji_table.png")
    private const val EMOJI_WIDTH = 72.0f
    private const val EMOJI_DISPLAY_WIDTH = 8.0f
    private const val EMOJI_SPRITESHEET_COUNT = 44

    fun renderEmojiChar(char: Char, posX: Float, posY: Float, textureManager: TextureManager?, render: Boolean): Float {
        if (!isEnabled()) return -1.0f
        val nameMap = emojiNameMap ?: return -1.0f
        if (stringIndex <= emojiEnd) return 0.0f
        if (textureManager == null) return -1.0f
        if (char == ':') {
            val oldEmojiEnd = emojiEnd
            for (i in stringIndex + 1 until renderedString.length) {
                if (renderedString[i] == ':') {
                    emojiEnd = i
                    break
                }
            }
            if (stringIndex > emojiEnd) return -1.0f
            val emojiName = renderedString.slice(stringIndex + 1..<emojiEnd)
            val emojiIndex = nameMap[emojiName]
            if (emojiIndex == null) {
                emojiEnd = oldEmojiEnd
                return -1.0f
            } else if (!render) {
                return EMOJI_DISPLAY_WIDTH
            }
            textureManager.bindTexture(emojiResource)
            val spriteSheetX = (emojiIndex % EMOJI_SPRITESHEET_COUNT) * EMOJI_WIDTH
            val spriteSheetY = (emojiIndex / EMOJI_SPRITESHEET_COUNT) * EMOJI_WIDTH
            val brightness = if (isShadow) 0.25f else 1.0f
            GL11.glColor4f(
                brightness,
                brightness,
                brightness,
                lastColor.alpha / 255.0f
            )
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP)
            GL11.glTexCoord2f(
                spriteSheetX / (EMOJI_WIDTH * EMOJI_SPRITESHEET_COUNT),
                (spriteSheetY) / (EMOJI_WIDTH * EMOJI_SPRITESHEET_COUNT),
            )
            GL11.glVertex3f(posX, posY, 0.0f)
            GL11.glTexCoord2f(
                spriteSheetX / (EMOJI_WIDTH * EMOJI_SPRITESHEET_COUNT),
                (spriteSheetY + EMOJI_WIDTH) / (EMOJI_WIDTH * EMOJI_SPRITESHEET_COUNT),
            )
            GL11.glVertex3f(posX, posY + EMOJI_DISPLAY_WIDTH, 0.0f)
            GL11.glTexCoord2f(
                (spriteSheetX + EMOJI_WIDTH) / (EMOJI_WIDTH * EMOJI_SPRITESHEET_COUNT),
                spriteSheetY / (EMOJI_WIDTH * EMOJI_SPRITESHEET_COUNT),
            )
            GL11.glVertex3f(posX + EMOJI_DISPLAY_WIDTH, posY, 0.0f)
            GL11.glTexCoord2f(
                (spriteSheetX + EMOJI_WIDTH) / (EMOJI_WIDTH * EMOJI_SPRITESHEET_COUNT),
                (spriteSheetY + EMOJI_WIDTH) / (EMOJI_WIDTH * EMOJI_SPRITESHEET_COUNT),
            )
            GL11.glVertex3f(posX + EMOJI_DISPLAY_WIDTH, posY + EMOJI_DISPLAY_WIDTH, 0.0f)
            GL11.glEnd()
            GL11.glColor4f(
                lastColor.red / 255.0f,
                lastColor.green / 255.0f,
                lastColor.blue / 255.0f,
                lastColor.alpha / 255.0f
            )
            return EMOJI_DISPLAY_WIDTH + 1.0f
        }
        return -1.0f
    }

    private var lastColor: Color = Color()

    fun setLastColor(r: Float, g: Float, b: Float, a: Float) {
        lastColor = Color((r * 255.0).toInt(), (g * 255.0).toInt(), (b * 255.0).toInt(), (a * 255.0).toInt())
    }

    fun setCharIndex(index: Int, s: String, shadow: Boolean) {
        if (!isEnabled()) return
        if (index == 0) emojiEnd = -1
        renderedString = s
        stringIndex = index
        isShadow = shadow
    }

    fun initializeRenderer(textureManager: TextureManager) {
        textureManager.bindTexture(emojiResource)
    }

    private fun anyEmojiStartWith(string: String): Boolean {
        return reverseUnicodeToName?.any {
            it.key.startsWith(string)
        } ?: false
    }

    private fun emojisStartingWith(emojiList: List<String>, string: String): List<String> {
        return emojiList.filter {
            it.startsWith(string)
        }
    }

    private fun getEmojiString(string: String): String {
        val unicodeToName = reverseUnicodeToName ?: return ""
        return ":${unicodeToName[string]}:"
    }

    private fun isEmojiString(string: String): Boolean {
        val unicodeToName = reverseUnicodeToName ?: return false
        return unicodeToName.containsKey(string)
    }

    fun replaceEmojis(string: String): String {
        if (!isEnabled()) return string
        val unicodeMap = reverseUnicodeToName ?: return string
        val builder = StringBuilder()
        var i = 0

        while (i < string.length) {
            val char = string[i].toString()

            if (anyEmojiStartWith(char)) {
                var emoji = char
                var list = emojisStartingWith(unicodeMap.keys.toList(), emoji)
                var oldI = i
                var lastValidEmoji: String? = null
                while (i + 1 < string.length && list.isNotEmpty()) {
                    emoji += string[++i]
                    list = emojisStartingWith(list, emoji)
                    if (isEmojiString(emoji)) {
                        lastValidEmoji = emoji
                        oldI = i
                    }
                }
                if (lastValidEmoji != null) {
                    builder.append(getEmojiString(lastValidEmoji))
                }
                i = oldI
            } else {
                builder.append(char)
            }
            i++
        }
        return builder.toString()
    }

    fun handleKeyboardInput() {
        if (!isEnabled()) return
        if (Keyboard.getEventKeyState()) return
        val char = Keyboard.getEventCharacter()
        val currentScreen = Minecraft.getMinecraft().currentScreen
        if (currentScreen !is GuiChat) return
        val chat = (currentScreen as AccessorMixinGuiChat)
        if (!anyEmojiStartWith(char.toString())) return
        val unicodeMap = reverseUnicodeToName ?: return
        var string = char.toString()
        var startingWith = emojisStartingWith(unicodeMap.keys.toList(), string)
        var lastValidEmoji: String? = null
        while (startingWith.isNotEmpty()) {
            if (!Keyboard.next()) {
                if (isEmojiString(string)) {
                    chat.inputField_skyhanni.writeText(getEmojiString(string))
                }
                return
            }
            string += Keyboard.getEventCharacter()
            startingWith = emojisStartingWith(startingWith, string)
            if (isEmojiString(string)) {
                lastValidEmoji = string
            }
        }
        if (lastValidEmoji != null) chat.inputField_skyhanni.writeText(getEmojiString(lastValidEmoji))
    }

    fun isEnabled() = config.emojiReplace
}
