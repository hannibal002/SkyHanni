package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.RegexUtils.anyMatches
import at.hannibal2.hanni.utils.SkyBlockItemModifierUtils.getRecipientName
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object RestorePieceOfWizardPortalLore {

    private val config get() = HanniMod.feature.misc

    private val item = "WIZARD_PORTAL_MEMENTO".toInternalName()

    private val earnedPattern by RepoPattern.pattern(
        "misc.restore.wizard.portal.earned",
        "§7Earned by:.*"
    )

    @HandleEvent
    fun onToolTip(event: ToolTipEvent) {
        if (!config.restorePieceOfWizardPortalLore) return
        val stack = event.itemStack
        if (stack.getInternalName() != item) return
        if (earnedPattern.anyMatches(stack.getLore())) return
        val recipient = stack.getRecipientName() ?: return
        event.toolTip.add(5, "§7Earned by: $recipient")
    }
}
