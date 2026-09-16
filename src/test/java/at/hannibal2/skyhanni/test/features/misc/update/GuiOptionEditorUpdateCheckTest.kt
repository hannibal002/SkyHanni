package at.hannibal2.skyhanni.test.features.misc.update

import at.hannibal2.skyhanni.config.core.elements.GuiElementButton
import at.hannibal2.skyhanni.features.misc.update.ChangelogViewer
import at.hannibal2.skyhanni.features.misc.update.GuiOptionEditorUpdateCheck
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import at.hannibal2.skyhanni.utils.ReflectionUtils.getPrivateFieldValue
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GuiOptionEditorUpdateCheckTest {

    private lateinit var editor: GuiOptionEditorUpdateCheck

    @BeforeEach
    fun setUp() {
        val option = mockk<ProcessedOption> {
            every { searchTags } returns emptyArray()
        }
        editor = GuiOptionEditorUpdateCheck(option)
        editor.setButtonWidth("download", BUTTON_WIDTH)
        editor.setButtonWidth("changelog", BUTTON_WIDTH)

        mockkObject(UpdateManager)
        every { UpdateManager.updateState } returns UpdateManager.UpdateState.AVAILABLE
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(UpdateManager)
    }

    @Test
    fun `download click triggers once on release`() {
        every { UpdateManager.getDownloadPage() } returns null

        assertFalse(mouseInput(DOWNLOAD_X, DOWNLOAD_Y, mouseDown = true))
        assertTrue(mouseInput(DOWNLOAD_X, DOWNLOAD_Y, mouseDown = false))

        verify(exactly = 1) { UpdateManager.getDownloadPage() }
    }

    @Test
    fun `changelog click triggers once on release`() {
        mockkObject(ChangelogViewer)
        try {
            every { UpdateManager.getNextVersion() } returns "999.0"
            every { ChangelogViewer.showChangelog(any(), any()) } just Runs

            assertFalse(mouseInput(CHANGELOG_X, CHANGELOG_Y, mouseDown = true))
            assertTrue(mouseInput(CHANGELOG_X, CHANGELOG_Y, mouseDown = false))

            verify(exactly = 1) { ChangelogViewer.showChangelog(any(), any()) }
        } finally {
            unmockkObject(ChangelogViewer)
        }
    }

    @Test
    fun `release after close does not trigger stale click`() {
        every { UpdateManager.getDownloadPage() } returns null

        assertFalse(mouseInput(DOWNLOAD_X, DOWNLOAD_Y, mouseDown = true))
        editor.onAfterClose()
        assertFalse(mouseInput(DOWNLOAD_X, DOWNLOAD_Y, mouseDown = false))

        verify(exactly = 0) { UpdateManager.getDownloadPage() }
    }

    private fun mouseInput(mouseX: Int, mouseY: Int, mouseDown: Boolean): Boolean = editor.mouseInput(
        x = 0,
        y = 0,
        width = EDITOR_WIDTH,
        mouseX = mouseX,
        mouseY = mouseY,
        mouseEvent = MouseEvent.Click(mouseButton = 0, mouseState = mouseDown),
    )

    private fun GuiOptionEditorUpdateCheck.setButtonWidth(fieldName: String, width: Int) {
        (getPrivateFieldValue(fieldName) as GuiElementButton).width = width
    }

    companion object {
        private const val EDITOR_WIDTH = 200
        private const val BUTTON_WIDTH = 40
        private const val BUTTON_X = EDITOR_WIDTH - 20 - BUTTON_WIDTH + 1
        private const val DOWNLOAD_X = BUTTON_X
        private const val DOWNLOAD_Y = 11
        private const val CHANGELOG_X = BUTTON_X
        private const val CHANGELOG_Y = 40
    }
}
