package at.hannibal2.skyhanni.features

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.FeatureToggle
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean
import io.github.moulberry.moulconfig.annotations.ConfigOption
import io.github.moulberry.moulconfig.internal.GlScissorStack
import io.github.moulberry.moulconfig.internal.RenderUtils
import io.github.moulberry.moulconfig.internal.TextRenderUtils
import io.github.moulberry.moulconfig.processor.ConfigProcessorDriver
import io.github.moulberry.moulconfig.processor.ConfigStructureReader
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.event.ClickEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Mouse
import java.lang.reflect.Field

object DefaultConfigFeatures {
    @SubscribeEvent
    fun onTick(event: TickEvent) {
        val p = Minecraft.getMinecraft().thePlayer ?: return
//        MinecraftForge.EVENT_BUS.unregister(this)
        if (SkyHanniMod.feature.storage.hasPlayedBefore) return
        SkyHanniMod.feature.storage.hasPlayedBefore = true
        p.addChatMessage(
            ChatComponentText(
                "§e[SkyHanni] Looks like this is the first time you are using SkyHanni." +
                        " Click here to configure default options, or run /shdefaultoptions."
            ).setChatStyle(
                ChatStyle().setChatClickEvent(
                    ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        "/shdefaultoptions"
                    )
                )
            )
        )
    }

    class FeatureToggleProcessor : ConfigStructureReader {

        var latestCategory: Category? = null

        val allOptions = mutableListOf<FeatureToggleableOption>()
        val orderedOptions by lazy {
            allOptions.groupBy { it.category }
        }

        data class FeatureToggleableOption(
            val name: String, val description: String, val previouslyEnabled: Boolean,
            val isTrueEnabled: Boolean, val category: Category,
            val setter: (Boolean) -> Unit
        )

        data class Category(val name: String, val description: String)

        override fun beginCategory(baseObject: Any?, field: Field?, name: String, description: String) {
            latestCategory = Category(name, description)
        }

        override fun endCategory() {
        }

        override fun beginAccordion(baseObject: Any?, field: Field?, option: ConfigOption?, id: Int) {
        }

        override fun endAccordion() {
        }

        override fun emitOption(baseObject: Any, field: Field, option: ConfigOption) {
            val featureToggle = field.getAnnotation(FeatureToggle::class.java) ?: return
            field.getAnnotation(ConfigEditorBoolean::class.java)
                ?: error("Feature toggle found without ConfigEditorBoolean: $field")
            allOptions.add(
                FeatureToggleableOption(
                    option.name,
                    option.desc,
                    field.getBoolean(baseObject),
                    featureToggle.trueIsEnabled,
                    latestCategory!!,
                    { field.setBoolean(baseObject, it) }
                )
            )
        }

        override fun emitGuiOverlay(baseObject: Any?, field: Field?, option: ConfigOption?) {
        }
    }

    fun onCommand() {
        val processor = FeatureToggleProcessor()
        ConfigProcessorDriver.processConfig(SkyHanniMod.feature.javaClass, SkyHanniMod.feature, processor)
        SkyHanniMod.screenToOpen = DefaultConfigOptionGui(processor.orderedOptions)
    }

    class DefaultConfigOptionGui(val orderedOptions: Map<FeatureToggleProcessor.Category, List<FeatureToggleProcessor.FeatureToggleableOption>>) :
        GuiScreen() {
        val w = 400
        val h = 300
        val bars = 40
        var scroll = 0
        val padding = 10
        var wasMouseDown = false
        val cardHeight = 30

        enum class ResetSuggestionState(val label: String) {
            TURN_ALL_OFF("§c§lTurn all off"),
            TURN_ALL_ON("§a§lTurn all on"),
            LEAVE_DEFAULTS("§b§lLeave unchanged"), ;

            val next get() = entries[(ordinal + 1) % entries.size]
        }

        val resetSuggestionState =
            orderedOptions.keys.associateWith { ResetSuggestionState.LEAVE_DEFAULTS }.toMutableMap()

        override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
            super.drawScreen(mouseX, mouseY, partialTicks)
            drawDefaultBackground()
            RenderUtils.drawFloatingRectDark((width - w) / 2, (height - h) / 2, w, h)
            val sr = ScaledResolution(mc)
            var hoveringTextToDraw: List<String>? = null
            var mx = mouseX - ((width - w) / 2) - padding
            val isMouseDown = Mouse.isButtonDown(0)
            val shouldClick = isMouseDown && !wasMouseDown
            wasMouseDown = isMouseDown
            val isMouseInScrollArea = mx in 0..w && mouseY in ((height - h) / 2) + bars..((height + h) / 2 - bars)
            var my = mouseY - ((height - h) / 2 + bars) + scroll

            GlStateManager.pushMatrix()
            GlStateManager.translate(width / 2F, (height - h) / 2F, 0F)
            GlStateManager.scale(2f, 2f, 1f)
            TextRenderUtils.drawStringCentered(
                "§5SkyHanni Default Options",
                mc.fontRendererObj,
                0F,
                mc.fontRendererObj.FONT_HEIGHT.toFloat(),
                false,
                -1
            )
            GlStateManager.popMatrix()

            GlStateManager.pushMatrix()
            GlStateManager.translate(
                (width - w) / 2F + padding,
                (height + h) / 2F - mc.fontRendererObj.FONT_HEIGHT * 2,
                0F
            )
            var i = 0
            fun bt(title: String, tooltip: List<String>, func: () -> Unit) {
                val lw = mc.fontRendererObj.getStringWidth(title)
                var s = false
                if (mouseX - ((width - w) / 2 + padding) in i..(i + lw)
                    && mouseY - (height + h) / 2 in -bars..0
                ) {
                    s = true
                    hoveringTextToDraw = tooltip
                    if (shouldClick) {
                        func()
                    }
                }
                RenderUtils.drawFloatingRectDark(i - 1, -3, lw + 4, 14)
                mc.fontRendererObj.drawString(title, 2 + i.toFloat(), 0F, if (s) 0xFF00FF00.toInt() else -1, s)
                i += lw + 12
            }
            bt("Apply choices", listOf(), {
                applyCategorySelections(resetSuggestionState, orderedOptions)
                mc.displayGuiScreen(null)
            })
            bt(
                "Turn all on",
                listOf(),
                { resetSuggestionState.entries.forEach { it.setValue(ResetSuggestionState.TURN_ALL_ON) } })
            bt(
                "Turn all off",
                listOf(),
                { resetSuggestionState.entries.forEach { it.setValue(ResetSuggestionState.TURN_ALL_OFF) } })
            bt(
                "Leave all untouched",
                listOf(),
                { resetSuggestionState.entries.forEach { it.setValue(ResetSuggestionState.LEAVE_DEFAULTS) } })
            bt(
                "Cancel",
                listOf(),
                { mc.displayGuiScreen(null) })
            GlStateManager.popMatrix()

            GlStateManager.pushMatrix()
            GlScissorStack.push((width - w) / 2, (height - h) / 2 + bars, (width + w) / 2, (height + h) / 2 - bars, sr)
            GlStateManager.translate((width - w) / 2F + padding, (height - h) / 2F + bars - scroll, 0F)

            for ((cat) in orderedOptions.entries) {
                val suggestionState = resetSuggestionState[cat]!!
                drawRect(0, 0, w - padding * 2, 1, 0xFF808080.toInt())
                drawRect(0, 30, w - padding * 2, 31, 0xFF808080.toInt())
                drawRect(0, 0, 1, 30, 0xFF808080.toInt())
                drawRect(w - padding * 2 - 1, 0, w - padding * 2, 30, 0xFF808080.toInt())
                mc.fontRendererObj.drawString("§e${cat.name} ${suggestionState.label}", 4, 4, -1)
                mc.fontRendererObj.drawSplitString("§7${cat.description}", 4, 14, w - padding * 2-8, -1)
                if (isMouseInScrollArea && my in 0..cardHeight) {
                    hoveringTextToDraw =
                        listOf("§e${cat.name}", "§7${cat.description}", "§7Current plan: ${suggestionState.label}", "§aClick to toggle!")
                    if (shouldClick) {
                        resetSuggestionState[cat] = suggestionState.next
                    }
                }
                my -= cardHeight
                GlStateManager.translate(0F, cardHeight.toFloat(), 0F)
            }


            GlStateManager.popMatrix()
            GlScissorStack.pop(sr)
            if (hoveringTextToDraw != null) {
                Utils.drawHoveringText(hoveringTextToDraw, mouseX, mouseY, width, height, 100, mc.fontRendererObj)
            }

        }

        fun scroll(s: Int) {
            scroll = Math.max(0, Math.min(s, (orderedOptions.size + 1) * cardHeight - h + bars + padding * 2))
        }

        override fun handleMouseInput() {
            super.handleMouseInput()
            if (Mouse.getEventDWheel() != 0)
                scroll(scroll - Mouse.getEventDWheel())
        }
    }

    private fun applyCategorySelections(
        resetSuggestionState: MutableMap<FeatureToggleProcessor.Category, DefaultConfigOptionGui.ResetSuggestionState>,
        orderedOptions: Map<FeatureToggleProcessor.Category, List<FeatureToggleProcessor.FeatureToggleableOption>>
    ) {
        orderedOptions.forEach { cat, opt ->
            val resetState = resetSuggestionState[cat]!!
            if (resetState == DefaultConfigOptionGui.ResetSuggestionState.LEAVE_DEFAULTS) return@forEach
            for (o in opt) {
                val onState = o.isTrueEnabled
                val setTO = if (resetState == DefaultConfigOptionGui.ResetSuggestionState.TURN_ALL_ON) {
                    onState
                } else {
                    !onState
                }
                o.setter(setTO)
            }
        }
    }


}
