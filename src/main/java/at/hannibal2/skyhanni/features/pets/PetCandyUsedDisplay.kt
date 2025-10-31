package at.hannibal2.hanni.features.pets

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.GuiRenderItemEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RenderUtils.drawSlotText
import at.hannibal2.hanni.utils.SkyBlockItemModifierUtils.getMaxPetLevel
import at.hannibal2.hanni.utils.SkyBlockItemModifierUtils.getPetCandyUsed
import at.hannibal2.hanni.utils.SkyBlockItemModifierUtils.getPetLevel

@HanniModule
object PetCandyUsedDisplay {

    private val config get() = HanniMod.feature.misc.petCandy

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        if (!config.showCandy) return

        val stack = event.stack?.takeIf { it.stackSize == 1 } ?: return
        if (config.hideOnMaxed) {
            if (stack.getPetLevel() == stack.getMaxPetLevel()) return
        }

        val petCandyUsed = stack.getPetCandyUsed() ?: return
        if (petCandyUsed == 0) return

        val stackTip = "§c$petCandyUsed"
        val x = event.x + 13
        val y = event.y + 1

        event.drawSlotText(x, y, stackTip, .9f)
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(22, "misc.petCandyUsed", "misc.petCandy.showCandy")
    }
}
