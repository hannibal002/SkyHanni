package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.CancellableHanniEvent
import net.minecraft.client.gui.inventory.GuiContainer

class GuiKeyPressEvent(val guiContainer: GuiContainer) : CancellableHanniEvent()
