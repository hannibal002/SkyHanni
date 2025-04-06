package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.SignType
import net.minecraft.client.gui.inventory.GuiEditSign

class SignOpenEvent(val gui: GuiEditSign, val signType: SignType) : SkyHanniEvent()
