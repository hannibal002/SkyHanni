package at.hannibal2.skyhanni.config.features.slayer

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SlayerTimeMessagesTemplatesConfig {
    @Expose
    @ConfigOption(name = "Time to Kill", desc = "Template for the time to kill message.")
    @ConfigEditorText
    var timeToKill: String = "It took &b{time}&e to kill {boss}"

    @Expose
    @ConfigOption(name = "Compact Time to Kill", desc = "Template for the compact time to kill message.")
    @ConfigEditorText
    var compactTimeToKill: String = "{boss} &etook &b{time}"

    @Expose
    @ConfigOption(name = "New Personal Best", desc = "Template for a new personal best.")
    @ConfigEditorText
    var newPersonalBest: String =
        "&e&lNEW PERSONAL BEST! &a{time} &7(Previous {previous}) &efor {boss}"

    @Expose
    @ConfigOption(name = "Compact New Personal Best", desc = "Template for a compact new personal best.")
    @ConfigEditorText
    var compactNewPersonalBest: String =
        "&e&lNEW PB! {boss} &ein &c{previous} &e-> &a{time}"

    @Expose
    @ConfigOption(name = "First Personal Best", desc = "Template when setting the first personal best.")
    @ConfigEditorText
    var firstPersonalBest: String =
        "&e&lNEW PERSONAL BEST! &a{time} &efor {boss}"

    @Expose
    @ConfigOption(name = "Compact First Personal Best", desc = "Template for the first compact personal best.")
    @ConfigEditorText
    var compactFirstPersonalBest: String =
        "&e&lNEW PB! {boss} &ein &a{time}"

    @Expose
    @ConfigOption(name = "Personal Best", desc = "Template for displaying the current personal best.")
    @ConfigEditorText
    var personalBest: String =
        "{boss} &ePersonal best: &6{previous}"

    @Expose
    @ConfigOption(name = "Compact Personal Best", desc = "Template for displaying the compact personal best.")
    @ConfigEditorText
    var compactPersonalBest: String =
        "{boss} &ePB: &6{previous}"

    @Expose
    @ConfigOption(name = "Quest Complete", desc = "Template for quest completion.")
    @ConfigEditorText
    var questComplete: String =
        "Slayer quest took &b{time}&e to complete."

    @Expose
    @ConfigOption(name = "Compact Quest Complete", desc = "Template for compact quest completion.")
    @ConfigEditorText
    var compactQuestComplete: String =
        "Quest took &b{time}&e in total."

    @Expose
    @ConfigOption(name = "Title", desc = "Title shown when displaying a title.")
    @ConfigEditorText
    var title: String = "&e&lNEW PB!"

    @Expose
    @ConfigOption(name = "Subtitle", desc = "Subtitle shown when displaying a title.")
    @ConfigEditorText
    var subtitle: String = "{boss}\n&b{time}"
}
