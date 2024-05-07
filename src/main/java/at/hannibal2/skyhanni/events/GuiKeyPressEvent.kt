package at.hannibal2.skyhanni.events

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class GuiKeyPressEvent(val guiContainer: GuiContainer) : LorenzEvent()
