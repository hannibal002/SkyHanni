package at.hannibal2.skyhanni.test.config

import at.hannibal2.skyhanni.config.MoulConfigEditorComponent
import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.gui.CloseEventListener
import io.github.notenoughupdates.moulconfig.gui.CloseEventListener.CloseAction
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MoulConfigEditorComponentTest {

    @Test
    fun `after close is forwarded to the editor and option listeners`() {
        val optionEditor = CloseAwareOptionEditor()
        val editor = createEditor(optionEditor)

        GuiContext(MoulConfigEditorComponent(editor)).onAfterClose()

        verify(exactly = 1) { editor.onAfterClose() }
        assertEquals(1, optionEditor.afterCloseCalls)
    }

    @Test
    fun `option listeners can deny close`() {
        val optionEditor = CloseAwareOptionEditor(beforeCloseAction = CloseAction.DENY_CLOSE)
        val editor = createEditor(optionEditor)

        val result = GuiContext(MoulConfigEditorComponent(editor)).onBeforeClose()

        assertEquals(CloseAction.DENY_CLOSE, result)
        assertEquals(1, optionEditor.beforeCloseCalls)
    }

    private fun createEditor(optionEditor: GuiOptionEditor): MoulConfigEditor<TestConfig> {
        val option = mockk<ProcessedOption>()
        every { option.editor } returns optionEditor

        return mockk {
            every { allOptions } returns listOf(option)
            every { onBeforeClose() } returns CloseAction.NO_OBJECTIONS_TO_CLOSE
            every { onAfterClose() } just Runs
        }
    }

    private class CloseAwareOptionEditor(
        private val beforeCloseAction: CloseAction = CloseAction.NO_OBJECTIONS_TO_CLOSE,
    ) : GuiOptionEditor(createOption()), CloseEventListener {

        var beforeCloseCalls = 0
            private set
        var afterCloseCalls = 0
            private set

        override fun onBeforeClose(): CloseAction {
            beforeCloseCalls++
            return beforeCloseAction
        }

        override fun onAfterClose() {
            afterCloseCalls++
        }
    }

    private class TestConfig : Config()

    companion object {
        private fun createOption(): ProcessedOption = mockk {
            every { searchTags } returns emptyArray()
        }
    }
}
