package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object OdgerTotalCaught : TrophyTotalCaught() {

    private val config get() = SkyHanniMod.feature.fishing.trophyFishing

    private val patternGroup = RepoPattern.group("fishing.trophy.odger")

    /**
     * REGEX-TEST: Discovered
     */
    override val discoveredPattern by patternGroup.pattern(
        "discovered.new",
        "Discovered",
    )

    /**
     * REGEX-TEST: Bronze ✖
     * REGEX-TEST: Bronze ✔ (4)
     */
    override val bronzePattern by patternGroup.pattern(
        "bronze.new",
        "^Bronze.*",
    )

    override fun isInInventory() = TrophyFishManager.odgerInventory.isInside()
    override fun isEnabled() = config.totalFishCaught
    override fun countsFor(cleanName: String) =
        TrophyFishManager.fish?.get(TrophyFishApi.getInternalName(cleanName))

    // Not island-gated because Odger has an Abiphone contact
    @HandleEvent(onlyOnSkyblock = true)
    private fun onToolTipEvent(event: ToolTipTextEvent) {
        handleToolTip(event)
    }
}
