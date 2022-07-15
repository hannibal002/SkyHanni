package at.hannibal2.skyhanni.config.gui.core.config.gui;

import at.hannibal2.skyhanni.config.gui.core.config.struct.ConfigProcessor;
import at.hannibal2.skyhanni.config.gui.textures.Textures;
import java.util.stream.Collectors;
import org.lwjgl.input.Mouse;

public class GuiOptionEditorStyle extends GuiOptionEditorDropdown {

    public GuiOptionEditorStyle(ConfigProcessor.ProcessedOption option, int selected) {
        super(option, Textures.styles.stream().map(t -> t.displayName).collect(Collectors.toList()).toArray(new String[] {}), selected, true);
    }

    @Override
    public boolean mouseInputOverlay(int x, int y, int width, int mouseX, int mouseY) {
        int height = getHeight();

        int left = x + width / 6 - 40;
        int top = y + height - 7 - 14;

        if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0) {
            if (!(mouseX >= left && mouseX <= left + 80 && mouseY >= top && mouseY <= top + 14) && open) {
                this.open = false;
                if (mouseX >= left && mouseX <= left + 80) {
                    int dropdownY = 13;
                    for (int ordinal = 0; ordinal < values.length; ordinal++) {
                        if (mouseY >= top + 3 + dropdownY && mouseY <= top + 3 + dropdownY + 12) {
                            selected = ordinal;
                            option.set(selected);
                            Textures.setTexture(selected);
                            return true;
                        }
                        dropdownY += 12;
                    }
                }
                return true;
            }
        }

        return false;
    }
}
