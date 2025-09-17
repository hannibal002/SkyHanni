package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigGuiManager
import at.hannibal2.skyhanni.test.command.ErrorManager
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import net.minecraft.client.Minecraft
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.javaField
//#if FORGE
import io.github.notenoughupdates.moulconfig.gui.GuiScreenElementWrapper
//#else
//$$ import io.github.notenoughupdates.moulconfig.gui.GuiContext
//$$ import io.github.notenoughupdates.moulconfig.gui.GuiElementComponent
//$$ import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
//$$ import net.minecraft.text.Text
//#endif

object ConfigUtils {

    /**
     * Migrates a Boolean to an Enum Constant.
     *
     * @param element The JsonElement to migrate
     * @param trueValue The enum value it should map to if the value is true
     * @param falseValue The enum value it should map to if the value is false
     * @return The migrated JsonElement
     */
    fun <T : Enum<T>> migrateBooleanToEnum(element: JsonElement, trueValue: T, falseValue: T): JsonElement {
        require(element is JsonPrimitive) { "Expected a JsonPrimitive but got ${element.javaClass.simpleName}" }
        return JsonPrimitive(if (element.asBoolean) trueValue.name else falseValue.name)
    }

    private fun KProperty0<*>.tryFindEditor(editor: MoulConfigEditor<*>): ProcessedOption? {
        return editor.getOptionFromField(this.javaField ?: return null)
    }

    fun KProperty0<*>.jumpToEditor() {
        if (tryJumpToEditor(ConfigGuiManager.getEditorInstance())) return

        ErrorManager.crashInDevEnv("Can not open config $name")
        ErrorManager.logErrorStateWithData(
            "Can not open the config",
            "error while trying to jump to an editor element",
            "this.name" to this.name,
            "this.toString()" to this.toString(),
            "this" to this,
        )
    }

    private fun KProperty0<*>.tryJumpToEditor(editor: MoulConfigEditor<*>): Boolean {
        val option = tryFindEditor(editor) ?: return false
        editor.search("")
        if (!editor.goToOption(option)) return false
        openEditor(editor)
        return true
    }

    fun openEditor(editor: MoulConfigEditor<*>) {
        //#if FORGE
        SkyHanniMod.screenToOpen = GuiScreenElementWrapper(editor)
        //#else
        //$$ SkyHanniMod.screenToOpen = MoulConfigScreenComponent(Text.empty(), GuiContext(GuiElementComponent(editor)), null)
        //#endif
    }

    val configScreenCurrentlyOpen: Boolean
        get() =
            //#if FORGE
            Minecraft.getMinecraft().currentScreen is GuiScreenElementWrapper
    //#else
    //$$ MinecraftClient.getInstance().currentScreen is MoulConfigScreenComponent
    //#endif

    fun String.asStructuredText() = StructuredText.of(this)
}
