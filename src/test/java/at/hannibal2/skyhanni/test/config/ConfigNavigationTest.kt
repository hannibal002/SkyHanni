package at.hannibal2.skyhanni.test.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigGuiManager
import at.hannibal2.skyhanni.config.SkyHanniConfig
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import net.minecraft.client.Minecraft
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ConfigNavigationTest {
    @Test
    fun `jumps do not accumulate previous screens or replace the current editor screen`() {
        val editor = mockk<MoulConfigEditor<SkyHanniConfig>>(relaxed = true)
        val option = mockk<ProcessedOption>()
        every { editor.getOptionFromField(any()) } returns option
        every { editor.goToOption(option) } returns true
        val linkedEditor = mockk<MoulConfigEditor<SkyHanniConfig>>()
        val config = TestConfig()
        val pendingScreen = SkyHanniMod.screenToOpen

        mockkObject(ConfigGuiManager)
        mockkStatic(MinecraftCompat::class, Minecraft::class)
        try {
            every { Minecraft.getInstance() } returns mockk(relaxed = true)
            var current = ConfigUtils.createConfigScreen(editor)
            every { ConfigGuiManager.getEditorInstance() } returns editor
            every { MinecraftCompat.screen } answers { current }
            SkyHanniMod.screenToOpen = null

            repeat(3) {
                val previous = current
                ConfigUtils.openEditor(linkedEditor, previousScreen = current)
                current = SkyHanniMod.screenToOpen as MoulConfigScreenComponent
                assertSame(previous, current.previousScreen)
                config::enabled.jumpToEditor()
                current = SkyHanniMod.screenToOpen as MoulConfigScreenComponent
                SkyHanniMod.screenToOpen = null
                assertNull(current.previousScreen)
            }

            val previous = current
            current = ConfigUtils.createConfigScreen(editor, previous)
            config::enabled.jumpToEditor()
            assertNull(SkyHanniMod.screenToOpen)
            assertSame(previous, current.previousScreen)
        } finally {
            SkyHanniMod.screenToOpen = pendingScreen
            unmockkStatic(MinecraftCompat::class, Minecraft::class)
            unmockkObject(ConfigGuiManager)
        }
    }

    private class TestConfig {
        val enabled = true
    }
}
