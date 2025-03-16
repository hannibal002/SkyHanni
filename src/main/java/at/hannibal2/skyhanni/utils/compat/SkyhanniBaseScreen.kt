package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.gui.GuiScreen

abstract class SkyhanniBaseScreen : GuiScreen(
    //#if MC > 1.20
    //$$ net.minecraft.text.Text.empty()
    //#elseif MC > 1.12
    //$$ net.minecraft.network.chat.TextComponent.EMPTY
    //#endif
)
