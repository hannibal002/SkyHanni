package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.gui.GuiScreen

@Suppress("NoEmptyClassBody")
abstract class SkyhanniBaseScreen : GuiScreen(
    //#if MC > 1.12
    //$$ net.minecraft.network.chat.TextComponent.EMPTY
    //#endif
)
