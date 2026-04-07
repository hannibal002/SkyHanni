package at.hannibal2.skyhanni.features.misc.visualwords

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ChatUtils.chat
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment as HA
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment as VA
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.convertToFormatted
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemRenderableConfig
import com.google.gson.JsonObject
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.Minecraft
import java.awt.Color
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Constructs the full Renderable display for [VisualWordScreen].
 *
 * Stateless — all mutable state lives in [VisualWordScreen].
 */
@SkyHanniModule
object VisualWordGui {

    private val COLOR_BG = ChromaColour.fromStaticRGB(18, 18, 28, 235)
    private val COLOR_OUTLINE_TOP = ChromaColour.fromStaticRGB(100, 100, 160, 255)
    private val COLOR_OUTLINE_BOT = ChromaColour.fromStaticRGB(55, 55, 100, 255)
    private val COLOR_ROW_NORMAL = ChromaColour.fromStaticRGB(30, 30, 50, 180)
    private val COLOR_ROW_HOVER = ChromaColour.fromStaticRGB(55, 55, 85, 220)
    private val COLOR_BTN_ADD = ChromaColour.fromStaticRGB(35, 90, 35, 215)
    private val COLOR_BTN_DELETE = ChromaColour.fromStaticRGB(110, 30, 30, 215)
    private val COLOR_BTN_BACK = ChromaColour.fromStaticRGB(75, 65, 25, 215)
    private val COLOR_BTN_NEUTRAL = ChromaColour.fromStaticRGB(45, 45, 68, 215)
    private val COLOR_BTN_ENABLED = ChromaColour.fromStaticRGB(35, 95, 35, 215)
    private val COLOR_BTN_DISABLED = ChromaColour.fromStaticRGB(110, 35, 35, 215)

    val sbeConfigPath: File = File("." + File.separator + "config" + File.separator + "SkyblockExtras.cfg")

    // TODO regex tests (idk hanni asked for the todo)
    private val replacementLinePattern = "(?<from>.*)@-(?<to>.*)@:-(?<state>false|true)".toPattern()

    private val upSkull by lazy {
        ItemUtils.createSkull(
            displayName = "§aMove Up",
            uuid = "7f68dd73-1ff6-4193-b246-820975d6fab1",
            value = SkullTextureHolder.getTexture("UP_ARROW"),
        )
    }
    private val downSkull by lazy {
        ItemUtils.createSkull(
            displayName = "§aMove Down",
            uuid = "e4ace6de-0629-4719-aea3-3e113314dd3f",
            value = SkullTextureHolder.getTexture("DOWN_ARROW"),
        )
    }
    private val defaultConfig = ItemRenderableConfig { scale = 1.0 }
    private val dimmedConfig = ItemRenderableConfig {
        scale = 1.0
        alpha = 0.4f
    }
    private val upItem by lazy { Renderable.item(upSkull, defaultConfig) }
    private val upItemDimmed by lazy { Renderable.item(upSkull, dimmedConfig) }
    private val downItem by lazy { Renderable.item(downSkull, defaultConfig) }
    private val downItemDimmed by lazy { Renderable.item(downSkull, dimmedConfig) }

    fun isInGui(): Boolean = Minecraft.getInstance().screen is VisualWordScreen

    fun onCommand() {
        if (!SkyBlockUtils.onHypixel && !OutsideSBFeature.MODIFY_VISUAL_WORDS.isSelected()) {
            ChatUtils.userError("You need to join Hypixel to use this feature!")
            return
        }
        SkyHanniMod.screenToOpen = VisualWordScreen()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shwords") {
            description = "Opens the config list for modifying visual words"
            callback { onCommand() }
        }
    }

    /**
     * Constructs the full display [Renderable] for the current state of [screen].
     * Stateless — all mutable state lives in [VisualWordScreen].
     */
    fun buildDisplay(screen: VisualWordScreen): Renderable {
        val body = if (screen.currentlyEditing) buildEditView(screen) else buildListView(screen)
        return Renderable.drawInsideFloatingRectWithBorder(
            body,
            backgroundColor = COLOR_BG,
            lightColor = COLOR_OUTLINE_TOP,
            darkColor = COLOR_OUTLINE_BOT,
            padding = 14,
            radius = 12,
            smoothness = 2,
            borderThickness = 2,
        )
    }

    private fun buildListView(screen: VisualWordScreen): Renderable {
        val header = Renderable.text("§bVisual Word Replacements", scale = 1.2, horizontalAlign = HA.CENTER)
        val subtitle = Renderable.text(
            "§7Each entry replaces §bPhrase§7 with §aReplacement",
            scale = 0.85,
            horizontalAlign = HA.CENTER,
        )

        val listArea = if (screen.modifiedWords.isEmpty()) {
            Renderable.text("§7No entries yet — click §aAdd New§7 to start.", horizontalAlign = HA.CENTER)
        } else {
            Renderable.scrollList(
                screen.modifiedWords.mapIndexed { index, word -> buildWordRow(screen, index, word) },
                height = 150,
                scrollValue = screen.listScrollValue,
                bypassChecks = true,
                showScrollableTipsInList = false,
                showScrollbar = true,
            )
        }

        return Renderable.vertical(
            listOf(header, subtitle, listArea, buildListBottomRow(screen)),
            spacing = 6,
            horizontalAlign = HA.CENTER,
        )
    }

    private fun buildWordRow(screen: VisualWordScreen, index: Int, word: VisualWord): Renderable {
        val indexLabel = Renderable.fixedSizeLine(Renderable.text("§7${index + 1}."), width = 20)

        val textFixed = Renderable.fixedSizeLine(
            Renderable.vertical(
                listOf(
                    Renderable.text(word.phrase.convertToFormatted()),
                    Renderable.text(word.replacement.convertToFormatted()),
                ),
                spacing = 1,
            ),
            width = 230,
        )

        val upBtn = if (index > 0) Renderable.clickable(
            upItem.withTip(),
            onLeftClick = { screen.moveWord(index, up = true) },
            bypassChecks = true,
        ) else upItemDimmed.withTip()

        val downBtn = if (index < screen.modifiedWords.lastIndex) Renderable.clickable(
            downItem.withTip(),
            onLeftClick = { screen.moveWord(index, up = false) },
            bypassChecks = true,
        ) else downItemDimmed.withTip()

        val statusItem = if (word.enabled) ColoredBlockCompat.GREEN.createStainedClay()
        else ColoredBlockCompat.RED.createStainedClay()

        val statusBtn = Renderable.clickable(
            Renderable.item(statusItem) { scale = 0.9 },
            onLeftClick = { screen.toggleEnabled(index) },
            bypassChecks = true,
        )

        val clickableText = Renderable.clickable(
            textFixed,
            onLeftClick = { screen.enterEditMode(index) },
            bypassChecks = true,
        )
        val rowContent = Renderable.horizontal(
            listOf(indexLabel, clickableText, upBtn, downBtn, statusBtn),
            spacing = 4,
            verticalAlign = VA.CENTER,
        )
        return Renderable.hoverable(
            Renderable.drawInsideRoundedRect(rowContent, COLOR_ROW_HOVER.toColor(), padding = 3, radius = 5),
            Renderable.drawInsideRoundedRect(rowContent, COLOR_ROW_NORMAL.toColor(), padding = 3, radius = 5),
            bypassChecks = true,
        )
    }

    private fun buildListBottomRow(screen: VisualWordScreen): Renderable {
        val addBtn = buildButton("§a+ Add New", COLOR_BTN_ADD.toColor()) { screen.addNewWord() }
        if (!sbeConfigPath.exists() || SkyHanniMod.feature.storage.visualWordsImported) return addBtn
        val importBtn = buildButton("§eImport from SBE", COLOR_BTN_NEUTRAL.toColor()) { tryImportFromSbe(screen) }
        return Renderable.horizontal(listOf(addBtn, importBtn), spacing = 8, verticalAlign = VA.CENTER)
    }

    private fun buildEditView(screen: VisualWordScreen): Renderable {
        val word = screen.modifiedWords.getOrNull(screen.currentIndex) ?: return buildListView(screen)

        val header = Renderable.text("§bEdit Replacement", scale = 1.2, horizontalAlign = HA.CENTER)
        val hint = Renderable.text(
            "§8Tip: use \"&&\" to produce the §r§8Minecraft formatting character §r§8(e.g. &&aGreen)",
            scale = 0.8,
        )

        val toggleRow = Renderable.horizontal(
            listOf(
                buildButton(
                    if (word.enabled) "§a● Enabled" else "§c● Disabled",
                    if (word.enabled) COLOR_BTN_ENABLED.toColor() else COLOR_BTN_DISABLED.toColor(),
                ) { screen.toggleEnabled(screen.currentIndex) },
                buildButton(
                    if (word.isCaseSensitive()) "§bCase Sensitive" else "§7Case Insensitive",
                    COLOR_BTN_NEUTRAL.toColor(),
                ) { screen.toggleCaseSensitive(screen.currentIndex) },
            ),
            spacing = 8,
            verticalAlign = VA.CENTER,
        )

        val actionRow = Renderable.horizontal(
            listOf(
                buildButton("§c✗ Delete", COLOR_BTN_DELETE.toColor()) { screen.deleteWord(screen.currentIndex) },
                buildButton("§e← Back", COLOR_BTN_BACK.toColor()) { screen.exitEditMode() },
            ),
            spacing = 8,
            verticalAlign = VA.CENTER,
        )

        return Renderable.vertical(
            listOf(
                header,
                buildFieldSection(screen, "§bPhrase §7(the text to be replaced):", screen.phraseInput),
                buildFieldSection(screen, "§aReplacement §7(what to replace it with):", screen.replacementInput),
                hint,
                toggleRow,
                actionRow,
            ),
            spacing = 8,
            horizontalAlign = HA.CENTER,
        )
    }

    private fun buildFieldSection(screen: VisualWordScreen, label: String, textInput: TextInput) =
        Renderable.vertical(
            listOf(Renderable.text(label), buildTextField(screen, textInput, fieldWidth = 310)),
            spacing = 3,
        )

    /**
     * An editable text-field Renderable. Reads live from [textInput] every frame.
     * Becomes active when [VisualWordScreen.activeInput] points to it; click to focus.
     */
    @Suppress("SameParameterValue")
    private fun buildTextField(screen: VisualWordScreen, textInput: TextInput, fieldWidth: Int) = Renderable.clickable(
        buildLiveField(screen, textInput, fieldWidth),
        onLeftClick = { screen.activeInput = textInput },
        bypassChecks = true,
    )

    private fun buildLiveField(screen: VisualWordScreen, textInput: TextInput, fieldWidth: Int): Renderable =
        object : Renderable {
            override val width = fieldWidth
            override val height = 16
            override val horizontalAlign = HA.LEFT
            override val verticalAlign = VA.TOP

            override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
                val isActive = screen.activeInput === textInput
                if (isActive) {
                    GuiRenderUtils.drawFloatingRectLight(0, 0, width, height, false)
                    textInput.makeActive()
                    textInput.handle()
                } else {
                    GuiRenderUtils.drawFloatingRectDark(0, 0, width, height, false)
                }
                val displayText = if (isActive) textInput.editText() else textInput.textBox
                DrawContextUtils.pushPop {
                    DrawContextUtils.translate(3f, ((height - 8) / 2).toFloat())
                    RenderableUtils.renderString(displayText, scale = 1.0, color = Color.WHITE)
                }
            }
        }

    private fun buildButton(label: String, color: Color, onClick: () -> Unit): Renderable {
        val text = Renderable.text(label)
        return Renderable.clickable(
            Renderable.hoverable(
                Renderable.drawInsideRoundedRect(text, color.brighter(), padding = 5, radius = 6),
                Renderable.drawInsideRoundedRect(text, color, padding = 5, radius = 6),
                bypassChecks = true,
            ),
            onLeftClick = onClick,
            bypassChecks = true,
        )
    }

    private fun tryImportFromSbe(screen: VisualWordScreen) {
        InputStreamReader(FileInputStream(sbeConfigPath), StandardCharsets.UTF_8).use { reader ->
            try {
                val json = ConfigManager.gson.fromJson(reader, JsonObject::class.java)
                importFromSbeJson(json, screen)
            } catch (e: Throwable) {
                ErrorManager.logErrorWithData(e, "Failed to load visual words from SBE")
            }
        }
    }

    private fun importFromSbeJson(
        json: JsonObject,
        screen: VisualWordScreen,
    ) {
        var importedWords = 0
        var skippedWords = 0

        for (line in json["custom"].asJsonObject["visualWords"].asJsonArray) {
            replacementLinePattern.matchMatcher(line.asString) {
                val from = group("from").replace("&", "&&")
                val to = group("to").replace("&", "&&")
                val state = group("state").toBoolean()
                if (screen.modifiedWords.any { it.phrase == from }) {
                    skippedWords++
                    return@matchMatcher
                }
                screen.modifiedWords.add(VisualWord(from, to, state, caseSensitive = false))
                importedWords++
            }
        }

        if (importedWords > 0 || skippedWords > 0) {
            chat("§aSuccessfully imported §e$importedWords §awords and skipped §e$skippedWords §afrom SkyBlockExtras!")
            SkyHanniMod.feature.storage.visualWordsImported = true
            screen.saveChanges()
            screen.rebuildDisplay()
        }
    }
}
