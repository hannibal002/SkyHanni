package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.EmojiJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.minecraft.TexturesReloadEvent
//#if MC < 1.21
import at.hannibal2.skyhanni.mixins.transformers.AccessorFontRenderer
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiScreen
//#endif
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.compat.createResourceLocation
import at.hannibal2.skyhanni.utils.json.BaseGsonBuilder.gson
import com.google.gson.annotations.Expose
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.texture.SimpleTexture
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

//#if MC > 1.21
//$$ import kotlin.jvm.optionals.getOrNull
//#endif

@SkyHanniModule
object EmojiReplacer {
    private val config get() = SkyHanniMod.feature.gui
    private val gson = gson().create()

    private var emojiNameMap: Map<String, String>? = null
    private var reverseEmojiNameMap: MutableMap<String, String>? = null

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val emojiJson = event.getConstant<EmojiJson>("Emojis")
        emojiNameMap = emojiJson.emojiNames

        val reverseMap = mutableMapOf<String, String>()

        reverseEmojiNameMap = reverseMap

        for ((emojiName, emojiString) in emojiJson.emojiNames) {
            if ((reverseMap[emojiName]?.length ?: 0) < emojiString.length) {
                reverseMap[emojiString] = emojiName
            }
        }
    }

    private val emojiRootResource = createResourceLocation("skyhanni:emoji/emoji_meta.json")

    private var emojiSpritesheetResource: ResourceLocation? = null
    private var emojiSpritesheetWidth = 0
    private var emojiSpritesheetHeight = 0
    private var emojiSprites: List<String>? = null

    // Texture pack support
    //TODO: Figure out why this event isnt done correctly
    @HandleEvent(TexturesReloadEvent::class)
    fun onTexturesLoad() {
        val mc = Minecraft.getMinecraft()

        val emojiRootStream =
            //#if MC < 1.21
            mc.resourceManager.getResource(emojiRootResource)?.inputStream ?: return
            //#else
            //$$ mc.resourceManager.getResource(emojiRootResource).getOrNull()?.inputStream ?: return
            //#endif

        val emojiRoot = gson.fromJson(emojiRootStream.reader(), EmojiRootDefinition::class.java)

        emojiSpritesheetWidth = emojiRoot.width
        emojiSpritesheetHeight = emojiRoot.height
        emojiSpritesheetResource = createResourceLocation(emojiRoot.resource)
        emojiSprites = emojiRoot.emojis

        mc.textureManager?.loadTexture(emojiSpritesheetResource, SimpleTexture(emojiSpritesheetResource))
    }


    private var stringIndex: Int = -1
    private var emojiEnd = -1
    private var renderedString: String = ""
    private var isShadow = false

    fun setCharIndex(index: Int, s: String, shadow: Boolean) {
        if (!isEnabled()) return
        if (index == 0) emojiEnd = -1
        renderedString = s
        stringIndex = index
        isShadow = shadow
    }

    private const val EMOJI_DISPLAY_WIDTH = 8.0f

    fun renderEmojiChar(fontRenderer: FontRenderer, char: Char, posX: Float, posY: Float, textureManager: TextureManager?, render: Boolean): Float {
        if (!isEnabled()) return -1.0f

        val emojiResourceLocation = emojiSpritesheetResource ?: return -1.0f

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
            val emojiChar = nameMap[emojiName]
            if (emojiChar == null) {
                emojiEnd = oldEmojiEnd
                return -1.0f
            } else if (!render) {
                return EMOJI_DISPLAY_WIDTH
            }

            val emojiIndex = emojiSprites?.indexOf(emojiChar) ?: -1

            if (emojiIndex < 0) {
                emojiEnd = oldEmojiEnd
                return -1.0f
            }

            val spriteSheetX = (emojiIndex % emojiSpritesheetWidth).toFloat()
            val spriteSheetY = (emojiIndex / emojiSpritesheetWidth).toFloat()
            val brightness = if (isShadow) 0.25f else 1.0f

            val red = (fontRenderer as AccessorFontRenderer).red
            val green = (fontRenderer as AccessorFontRenderer).green
            val blue = (fontRenderer as AccessorFontRenderer).blue
            val alpha = (fontRenderer as AccessorFontRenderer).alpha

//             GuiRenderUtils.drawTexturedRect(
//                 posX.toInt(), posY.toInt(),
//                 EMOJI_DISPLAY_WIDTH.toInt(), EMOJI_DISPLAY_WIDTH.toInt(),
//                 spriteSheetX / emojiSpritesheetWidth,
//                 (spriteSheetX + 1.0f) / emojiSpritesheetWidth,
//                 spriteSheetY / emojiSpritesheetHeight,
//                 (spriteSheetY + 1.0f) / emojiSpritesheetHeight,
//                 emojiResourceLocation,
//                 alpha
//             )

            textureManager.bindTexture(emojiResourceLocation)

            GL11.glColor4f(
                brightness,
                brightness,
                brightness,
                alpha
            )

            GL11.glBegin(GL11.GL_TRIANGLE_STRIP)

            GL11.glTexCoord2f(
                spriteSheetX / emojiSpritesheetWidth,
                spriteSheetY / emojiSpritesheetHeight,
            )
            GL11.glVertex3f(posX, posY, 0.0f)

            GL11.glTexCoord2f(
                spriteSheetX / emojiSpritesheetWidth,
                (spriteSheetY + 1.0f) / emojiSpritesheetHeight,
            )
            GL11.glVertex3f(posX, posY + EMOJI_DISPLAY_WIDTH, 0.0f)

            GL11.glTexCoord2f(
                (spriteSheetX + 1.0f) / emojiSpritesheetWidth,
                spriteSheetY / emojiSpritesheetHeight,
            )
            GL11.glVertex3f(posX + EMOJI_DISPLAY_WIDTH, posY, 0.0f)

            GL11.glTexCoord2f(
                (spriteSheetX + 1.0f) / emojiSpritesheetWidth,
                (spriteSheetY + 1.0f) / emojiSpritesheetHeight,
            )
            GL11.glVertex3f(posX + EMOJI_DISPLAY_WIDTH, posY + EMOJI_DISPLAY_WIDTH, 0.0f)

            GL11.glEnd()

            GL11.glColor4f(
                red,
                green,
                blue,
                alpha
            )

            return EMOJI_DISPLAY_WIDTH + 1.0f
        }
        return -1.0f
    }


    private fun anyEmojiStartWith(string: String): Boolean {
        return reverseEmojiNameMap?.any {
            it.key.startsWith(string)
        } ?: false
    }

    private fun emojisStartingWith(emojiList: List<String>, string: String): List<String> {
        return emojiList.filter {
            it.startsWith(string)
        }
    }

    private fun getEmojiString(string: String): String {
        val unicodeToName = reverseEmojiNameMap ?: return ""
        return ":${unicodeToName[string]}:"
    }

    private fun isEmojiString(string: String): Boolean {
        val unicodeToName = reverseEmojiNameMap ?: return false
        return unicodeToName.containsKey(string)
    }

    fun replaceEmojis(string: String): String {
        if (!isEnabled()) return string
        val unicodeMap = reverseEmojiNameMap ?: return string
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
                } else {
                    builder.append(char)
                }
                i = oldI
            } else {
                builder.append(char)
            }
            i++
        }
        return builder.toString()
    }

    fun handleKeyboardInput(screen: GuiScreen) {
        if (!isEnabled()) return

        // From this point onwards, vanilla 1.8.9 discards any key input anyway, so it is ok to read all inputs
        if (Keyboard.getEventKeyState() || Keyboard.getEventKey() != 0) return

        val char = Keyboard.getEventCharacter()

        val accessorScreen = (screen as AccessorGuiScreen)

        val unicodeMap = reverseEmojiNameMap ?: return
        var string = char.toString()
        var startingWith = emojisStartingWith(unicodeMap.keys.toList(), string)
        var lastValidEmoji: String? = null
        while (startingWith.isNotEmpty()) {
            if (!Keyboard.next() || Keyboard.getEventKeyState()) {
                if (isEmojiString(string)) {
                    accessorScreen.keyTyped_skyhanni(char, -1)
                    for (char in getEmojiString(string)) {
                        accessorScreen.keyTyped_skyhanni(char, -1)
                    }
                }
                return
            }
            string += Keyboard.getEventCharacter()
            startingWith = emojisStartingWith(startingWith, string)
            if (isEmojiString(string)) {
                lastValidEmoji = string
            }
        }
        if (lastValidEmoji != null) {
            for (char in getEmojiString(lastValidEmoji)) {
                accessorScreen.keyTyped_skyhanni(char, -1)
            }
        }
    }

    fun isEnabled() = config.emojiReplace
}

data class EmojiRootDefinition(
    @Expose val width: Int = 1,
    @Expose val height: Int = width,
    @Expose val resource: String,
    @Expose val emojis: List<String>
)
