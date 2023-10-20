package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getRecipientName
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RestorePieceOfWizardPortalLore {

    private val config get() = SkyHanniMod.feature.misc

    private val item by lazy { "WIZARD_PORTAL_MEMENTO".asInternalName() }

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!config.restorePieceOfWizardPortalLore) return
        val stack = event.itemStack
        if (stack.getInternalName() != item) return
        val recipient = stack.getRecipientName() ?: return
        if (!event.toolTip[5].contains(recipient)) {
            event.toolTip.add(5, "§7Earned by: $recipient")
        }
    }

}