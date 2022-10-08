package at.hannibal2.skyhanni.config;

import at.hannibal2.skyhanni.config.core.GuiScreenElementWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.Set;

public class ConfigGuiForgeInterop implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraft) {}

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return WrappedSkyHanniConfig.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement runtimeOptionCategoryElement) {
        return null;
    }

    public static class WrappedSkyHanniConfig extends GuiScreenElementWrapper {

        private final GuiScreen parent;

        public WrappedSkyHanniConfig(GuiScreen parent) {
            super(ConfigEditor.editor);
            this.parent = parent;
        }

        @Override
        public void handleKeyboardInput() throws IOException {
            if (Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
                Minecraft.getMinecraft().displayGuiScreen(parent);
                return;
            }
            super.handleKeyboardInput();
        }
    }
}
