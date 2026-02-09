package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatchesComponent
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getRecipientName
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object RestorePieceOfWizardPortalLore {

    private val config get() = SkyHanniMod.feature.misc

    private val item = "WIZARD_PORTAL_MEMENTO".toInternalName()

    /**
     * REGEX-TEST: Earned by: [MVP+] Throwpo
     */
    private val earnedPattern by RepoPattern.pattern(
        "misc.restore.wizard.portal.earned",
        "Earned by:.*"
    )

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        if (!config.restorePieceOfWizardPortalLore) return
        val stack = event.itemStack
        if (stack.getInternalName() != item) return
        if (earnedPattern.anyMatchesComponent(stack.getLoreComponent())) return
        val recipient = stack.getRecipientName() ?: return
        event.toolTip.add(5, "§7Earned by: $recipient")
    }
}
