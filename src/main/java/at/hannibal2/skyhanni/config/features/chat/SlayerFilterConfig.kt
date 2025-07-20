package at.hannibal2.skyhanni.config.features.chat

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class SlayerFilterConfig {

    @Expose
    @ConfigOption(name = "Slayer Complete", desc = "Hide 'SLAYER QUEST COMPLETE!' messages.")
    @ConfigEditorBoolean
    var slayerComplete: Boolean = false

    @Expose
    @ConfigOption(name = "Slayer Killed", desc = "Hide 'SLAYER BOSS SLAIN!' and miniboss kill credit messages.")
    @ConfigEditorBoolean
    var slayerKilled: Boolean = false

    @Expose
    @ConfigOption(name = "Slayer Level", desc = "Hide 'Slayer LVL' messages.")
    @ConfigEditorBoolean
    var slayerLevel: Boolean = false

    @Expose
    @ConfigOption(name = "Slayer Maddox", desc = "Hide 'Talk to Maddox to claim your Slayer XP!' messages.")
    @ConfigEditorBoolean
    var slayerMaddox: Boolean = false

    // TODO move this somewhere else as it is all RING... not just slayer
    @Expose
    @ConfigOption(name = "Slayer Ring", desc = "Hide 'RING...' messages for Slayer quests.")
    @SearchTag("abiphone")
    @ConfigEditorBoolean
    var slayerRing: Boolean = false

    @Expose
    @ConfigOption(name = "Slayer Start", desc = "Hide 'SLAYER QUEST STARTED!' messages.")
    @ConfigEditorBoolean
    var slayerStart: Boolean = false

    @Expose
    @ConfigOption(name = "Slayer Drops", desc = "Filter specific Slayer drop messages.")
    @Accordion
    val slayerDrops: SlayerFilterDropsConfig = SlayerFilterDropsConfig()
}
