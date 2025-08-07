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
//$$ import net.minecraft.client.font.BitmapFont
//$$ import net.minecraft.client.font.Font
//$$ import net.minecraft.client.font.FontFilterType
//$$ import net.minecraft.client.font.FontStorage
//$$ import net.minecraft.text.CharacterVisitor
//$$ import net.minecraft.text.Style
//$$ import net.minecraft.util.Formatting
//$$ import kotlin.jvm.optionals.getOrNull
//#endif

@SkyHanniModule
object EmojiReplacer {
    private val config get() = SkyHanniMod.feature.gui
    private val gson = gson().create()

    //#if MC > 1.21
    //$$ val EMOJI_IDENTIFIER = Identifier.of("skyhanni", "emoji_font")
    //#endif

    private var emojiNameMap: Map<String, String>? = null
    private var reverseEmojiNameMap: MutableMap<String, String>? = null

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val emojiJson = event.getConstant<EmojiJson>("Emojis")
        emojiNameMap = emojiJson.emojiNames

        val reverseMap = mutableMapOf<String, String>()

        reverseEmojiNameMap = reverseMap

        for ((emojiName, emojiString) in emojiJson.emojiNames) {
            val oldLength = reverseMap[emojiString]?.length
            if (oldLength == null || oldLength > emojiName.length) {
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
    // TODO: Figure out why this event isnt done correctly
    @HandleEvent(TexturesReloadEvent::class)
    fun onTexturesLoad() {
        val mc = Minecraft.getMinecraft()

        val emojiRootStream =
            //#if MC < 1.21
            mc.resourceManager.getResource(emojiRootResource)?.inputStream ?: return
        //#else
        //$$     mc.resourceManager.getResource(emojiRootResource).getOrNull()?.inputStream ?: return
        //#endif

        val emojiRoot = gson.fromJson(emojiRootStream.reader(), EmojiRootDefinition::class.java)

        emojiSpritesheetWidth = emojiRoot.width
        emojiSpritesheetHeight = emojiRoot.height
        var resource = createResourceLocation(emojiRoot.resource)
        //#if MC < 1.21
        resource = createResourceLocation(resource.resourceDomain, "textures/" + resource.resourcePath)
        //#endif
        emojiSpritesheetResource = resource
        emojiSprites = emojiRoot.emojis

        mc.textureManager?.loadTexture(emojiSpritesheetResource, SimpleTexture(emojiSpritesheetResource))

        //#if MC > 1.21
        //$$ loadFont(resource, emojiRoot.width, emojiRoot.height, emojiRoot.emojis)
        //#endif
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

        val sortedUnicodeKeySet = unicodeMap.keys.sortedByDescending { it.length }

        var tempString = string

        for (unicodeKey in sortedUnicodeKeySet) {
            val unicodeValue = unicodeMap[unicodeKey] ?: continue
            tempString = tempString.replace(unicodeKey, ":$unicodeValue:")
        }

        return tempString

//         while (i < string.length) {
//             val char = string[i].toString()
//
//             if (anyEmojiStartWith(char)) {
//                 var emoji = char
//                 var list = emojisStartingWith(unicodeMap.keys.toList(), emoji)
//                 var oldI = i
//                 var lastValidEmoji: String? = null
//                 while (i + 1 < string.length && list.isNotEmpty()) {
//                     emoji += string[++i]
//                     list = emojisStartingWith(list, emoji)
//                     if (isEmojiString(emoji)) {
//                         lastValidEmoji = emoji
//                         oldI = i
//                     }
//                 }
//                 if (lastValidEmoji != null) {
//                     builder.append(getEmojiString(lastValidEmoji))
//                 } else {
//                     builder.append(char)
//                 }
//                 i = oldI
//             } else {
//                 builder.append(char)
//             }
//             i++
//         }
//         return builder.toString()
    }

    //#if MC < 1.21
    private const val EMOJI_DISPLAY_WIDTH = 8.0f

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

    @Suppress("ReturnCount")
    fun renderEmojiChar(
        fontRenderer: FontRenderer,
        char: Char,
        posX: Float,
        posY: Float,
        textureManager: TextureManager?,
        render: Boolean,
    ): Float {
        if (!isEnabled()) return -1.0f

        if (stringIndex <= emojiEnd) return 0.0f

        val emojiResourceLocation = emojiSpritesheetResource
        val nameMap = emojiNameMap

        if (textureManager == null || emojiResourceLocation == null || nameMap == null) return -1.0f

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
                    for (emojiChar in getEmojiString(string)) {
                        accessorScreen.keyTyped_skyhanni(emojiChar, -1)
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
            for (emojiChar in getEmojiString(lastValidEmoji)) {
                accessorScreen.keyTyped_skyhanni(emojiChar, -1)
            }
        }
    }

    //#else
    //$$ private val renderQueue: MutableList<CharRendering> = mutableListOf()
    //$$ private var bypassProcessing = false
    //$$ private var inEmoji = false
    //$$ private var emojiString = StringBuilder()
    //$$ private var EMOJI_FONT: FontStorage? = null
    //$$ private var EMOJI_FONT_MAP: MutableMap<String, Int> = mutableMapOf()
    //$$
    //$$ fun loadFont(file: Identifier, width: Int, height: Int, emojis: List<String>) {
    //$$     val mc = MinecraftClient.getInstance()
    //$$     val positions = Array(height) { y ->
    //$$         IntArray(width) { x ->
    //$$             val index = y * width + x
    //$$             if (index < emojis.size) EMOJI_FONT_MAP[emojis[index]] = index + 100
    //$$             index + 100
    //$$         }
    //$$     }
    //$$     val loadable = BitmapFont.Loader(file, 8, 7, positions).build().left().getOrNull()
    //$$     val font = loadable?.load(MinecraftClient.getInstance().resourceManager)
    //$$     EMOJI_FONT = FontStorage(mc.textureManager, EMOJI_IDENTIFIER)
    //$$     EMOJI_FONT?.setFonts(listOf(
    //$$         Font.FontFilterPair(
    //$$             font,
    //$$             FontFilterType.FilterMap.NO_FILTER
    //$$         )
    //$$     ), emptySet<FontFilterType>())
    //$$ }
    //$$ fun getFontStorage(): FontStorage? {
    //$$     return EMOJI_FONT
    //$$ }
    //$$ fun handleEmojiRender(char: Int, style: Style, offset: Int, drawer: TextRenderer.Drawer): Boolean {
    //$$     if (bypassProcessing) return false
    //$$
    //$$     if (!inEmoji && char != ':'.code) return false
    //$$
    //$$     if (char == ':'.code) {
    //$$         if (inEmoji) {
    //$$             val emojiStringFinal = emojiString.toString()
    //$$             bypassProcessing = true
    //$$
    //$$             val map = EmojiReplacer.emojiNameMap
    //$$
    //$$             if (map?.contains(emojiStringFinal) ?: false) {
    //$$                 val fontIndex = EMOJI_FONT_MAP[EmojiReplacer.emojiNameMap?.get(emojiStringFinal)]
    //$$
    //$$                 if (fontIndex != null) {
    //$$
    //$$                     drawer.accept(
    //$$                         offset,
    //$$                         style.withFont(EMOJI_IDENTIFIER),
    //$$                         fontIndex
    //$$                     )
    //$$
    //$$                 } else {
    //$$                     renderQueue.add(CharRendering(
    //$$                         char,
    //$$                         style,
    //$$                         offset
    //$$                     ))
    //$$
    //$$                     for (memoryChar in renderQueue) {
    //$$                         drawer.accept(memoryChar.offset, Style.EMPTY.withColor(Formatting.RED), memoryChar.char)
    //$$                     }
    //$$                 }
    //$$
    //$$                 inEmoji = false
    //$$                 renderQueue.clear()
    //$$
    //$$             } else {
    //$$                 for (memoryChar in renderQueue) {
    //$$                     drawer.accept(memoryChar.offset, memoryChar.style, memoryChar.char)
    //$$                 }
    //$$                 renderQueue.clear()
    //$$                 renderQueue.add(CharRendering(
    //$$                     char,
    //$$                     style,
    //$$                     offset
    //$$                 ))
    //$$             }
    //$$             emojiString.clear()
    //$$             bypassProcessing = false
    //$$         } else {
    //$$             renderQueue.add(CharRendering(
    //$$                 char,
    //$$                 style,
    //$$                 offset
    //$$             ))
    //$$             inEmoji = true
    //$$         }
    //$$     } else {
    //$$         renderQueue.add(CharRendering(
    //$$             char,
    //$$             style,
    //$$             offset
    //$$         ))
    //$$         emojiString.appendCodePoint(char)
    //$$     }
    //$$
    //$$     return true
    //$$ }
    //$$
    //$$ fun handleEnd(visitor: CharacterVisitor) {
    //$$     val drawer = visitor as? TextRenderer.Drawer ?: return
    //$$     bypassProcessing = true
    //$$     for (memoryChar in renderQueue) {
    //$$         drawer.accept(memoryChar.offset, memoryChar.style, memoryChar.char)
    //$$     }
    //$$     bypassProcessing = false
    //$$     emojiString.clear()
    //$$     renderQueue.clear()
    //$$     inEmoji = false
    //$$ }
    //#endif

    fun isEnabled() = config.emojiReplace
}

//#if MC > 1.21
//$$ data class CharRendering(
//$$     val char: Int,
//$$     val style: Style,
//$$     val offset: Int
//$$ )
//#endif


data class EmojiRootDefinition(
    @Expose val width: Int = 1,
    @Expose val height: Int = width,
    @Expose val resource: String,
    @Expose val emojis: List<String>
)
