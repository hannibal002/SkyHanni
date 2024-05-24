package at.hannibal2.skyhanni.config.features.fishing;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;
import net.minecraft.client.Minecraft;

public class LavaReplacementConfig {

    @Expose
    @ConfigOption(name = "Render Type", desc = "")
    @ConfigEditorDropdown
    public Property<RenderType> renderType = Property.of(RenderType.COLOR);

    public enum RenderType {
        NONE("Disabled"),
        TEXTURE("Texture"),
        COLOR("Color")
        ;

        private final String str;
        RenderType(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }

    @Expose
    @ConfigOption(name = "Lava Color", desc = "Color")
    @ConfigEditorColour
    public String color = "0:255:255:13:0";


    @Expose
    @ConfigOption(name = "Reload", desc = "Reload renderers\n§cMay freeze your game for a few seconds!")
    @ConfigEditorButton(buttonText = "RELOAD")
    public Runnable reload = () -> Minecraft.getMinecraft().renderGlobal.loadRenderers();
}
