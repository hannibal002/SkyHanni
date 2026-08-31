package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import io.github.notenoughupdates.moulconfig.gui.CloseEventListener
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.platform.MoulConfigScreenComponent
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

// MoulConfig stores previousScreen but never navigates back to it on close, so that is handled here.
internal class SkyHanniConfigScreen(
    title: Component,
    guiContext: GuiContext,
    previousScreen: Screen?,
) : MoulConfigScreenComponent(title, guiContext, previousScreen) {

    override fun onClose() {
        val previous = previousScreen ?: return super.onClose()
        if (guiContext.onBeforeClose() == CloseEventListener.CloseAction.NO_OBJECTIONS_TO_CLOSE) {
            MinecraftCompat.screen = previous
        }
    }
}
