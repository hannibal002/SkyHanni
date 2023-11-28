package at.hannibal2.skyhanni.config.features.about;

import at.hannibal2.skyhanni.utils.OSUtils;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class Licenses {
    @ConfigOption(name = "MoulConfig", desc = "MoulConfig is available under the LGPL 3.0 License or later version")
    @ConfigEditorButton(buttonText = "Source")
    public Runnable moulConfig = () -> OSUtils.openBrowser("https://github.com/NotEnoughUpdates/MoulConfig");

    @ConfigOption(name = "NotEnoughUpdates", desc = "NotEnoughUpdates is available under the LGPL 3.0 License or later version")
    @ConfigEditorButton(buttonText = "Source")
    public Runnable notEnoughUpdates = () -> OSUtils.openBrowser("https://github.com/NotEnoughUpdates/NotEnoughUpdates");

    @ConfigOption(name = "Forge", desc = "Forge is available under the LGPL 3.0 license")
    @ConfigEditorButton(buttonText = "Source")
    public Runnable forge = () -> OSUtils.openBrowser("https://github.com/MinecraftForge/MinecraftForge");

    @ConfigOption(name = "LibAutoUpdate", desc = "LibAutoUpdate is available under the BSD 2 Clause License")
    @ConfigEditorButton(buttonText = "Source")
    public Runnable libAutoUpdate = () -> OSUtils.openBrowser("https://git.nea.moe/nea/libautoupdate/");

    @ConfigOption(name = "Mixin", desc = "Mixin is available under the MIT License")
    @ConfigEditorButton(buttonText = "Source")
    public Runnable mixin = () -> OSUtils.openBrowser("https://github.com/SpongePowered/Mixin/");

    @ConfigOption(name = "DiscordIPC", desc = "DiscordIPC is available under the Apache License 2.0")
    @ConfigEditorButton(buttonText = "GitHub")
    public Runnable discordRPC = () -> OSUtils.openBrowser("https://github.com/jagrosh/DiscordIPC");
}
