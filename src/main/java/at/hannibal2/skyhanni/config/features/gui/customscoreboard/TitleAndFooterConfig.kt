package at.hannibal2.skyhanni.config.features.gui.customscoreboard

//#if TODO
import at.hannibal2.skyhanni.utils.RenderUtils
//#endif
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// todo 1.21 impl needed
class TitleAndFooterConfig {
    //#if TODO
    @Expose
    @ConfigOption(name = "Title Alignment", desc = "Align the title in the scoreboard.")
    @ConfigEditorDropdown
    var alignTitle: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.CENTER
    //#endif

    @Expose
    @ConfigOption(
        name = "Custom Title",
        desc = "What should be displayed as the title of the scoreboard.\n" +
            "Use && for colors.\n" +
            "Use \"\\n\" for new line."
    )
    @ConfigEditorText
    var customTitle: String = "&&6&&lSKYBLOCK"

    @Expose
    @ConfigOption(name = "Use Custom Title", desc = "Use a custom title instead of the default Hypixel title.")
    @ConfigEditorBoolean
    var useCustomTitle: Boolean = true

    @Expose
    @ConfigOption(name = "Use Custom Title Outside SkyBlock", desc = "Use a custom title outside of SkyBlock.")
    @ConfigEditorBoolean
    var useCustomTitleOutsideSkyBlock: Boolean = false

    //#if TODO
    @Expose
    @ConfigOption(name = "Footer Alignment", desc = "Align the footer in the scoreboard.")
    @ConfigEditorDropdown
    var alignFooter: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT
    //#endif

    @Expose
    @ConfigOption(
        name = "Custom Footer",
        desc = "What should be displayed as the footer of the scoreboard.\n" +
            "Use && for colors.\n" +
            "Use \"\\n\" for new line."
    )
    @ConfigEditorText
    var customFooter: String = "&&ewww.hypixel.net"

    @Expose
    @ConfigOption(
        name = "Custom Alpha Footer",
        desc = "What should be displayed as the footer of the scoreboard when on the Alpha Server.\n" +
            "Use && for colors.\n" +
            "Use \"\\n\" for new line."
    )
    @ConfigEditorText
    var customAlphaFooter: String = "&&ealpha.hypixel.net"
}
