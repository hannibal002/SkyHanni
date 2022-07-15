package at.hannibal2.skyhanni.config;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.config.config.ConfigEditor;
import at.hannibal2.skyhanni.config.core.GuiElement;
import at.hannibal2.skyhanni.config.core.GuiScreenElementWrapper;
import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.core.config.annotations.*;
import at.hannibal2.skyhanni.config.core.config.gui.GuiPositionEditor;
import at.hannibal2.skyhanni.config.features.*;
import com.google.gson.annotations.Expose;
import net.minecraft.client.Minecraft;

public class Features {

    private void editOverlay(String activeConfig, int width, int height, Position position) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiPositionEditor(position, width, height, () -> {}, () -> {}, () -> SkyHanniMod.screenToOpen = new GuiScreenElementWrapper(new ConfigEditor(SkyHanniMod.feature, activeConfig))));
    }

    public void executeRunnable(String runnableId) {
        String activeConfigCategory = null;
        if (Minecraft.getMinecraft().currentScreen instanceof GuiScreenElementWrapper) {
            GuiScreenElementWrapper wrapper = (GuiScreenElementWrapper) Minecraft.getMinecraft().currentScreen;
            GuiElement element = wrapper.element;
            if (element instanceof ConfigEditor) {
                activeConfigCategory = ((ConfigEditor) element).getSelectedCategoryName();
            }
        }

        if (runnableId.equals("petDisplay")) {
            editOverlay(activeConfigCategory, 200, 16, misc.petDisplayPos);
            return;
        }

        if (runnableId.equals("testPos")) {
            editOverlay(activeConfigCategory, 200, 16, debug.testPos);
            return;
        }

        if (runnableId.equals("dungeonMilestoneDisplay")) {
            editOverlay(activeConfigCategory, 200, 16, dungeon.milestoneDisplayPos);
            return;
        }

        if (runnableId.equals("dungeonDeathCounter")) {
            editOverlay(activeConfigCategory, 200, 16, dungeon.deathCounterPos);
            return;
        }

        if (runnableId.equals("bestSellMethod")) {
            editOverlay(activeConfigCategory, 200, 16, bazaar.bestSellMethodPos);
            return;
        }
    }

    @Expose
    @Category(name = "Chat", desc = "Chat related features.")
    public Chat chat = new Chat();

    @Expose
    @Category(name = "Dungeon", desc = "Features that change the catacombs dungeon experience.")
    public Dungeon dungeon = new Dungeon();

    @Expose
    @Category(name = "Items", desc = "Changing the behavior around items and the inventory.")
    public Items items = new Items();

    @Expose
    @Category(name = "Bazaar", desc = "Bazaar settings.")
    public Bazaar bazaar = new Bazaar();

    @Expose
    @Category(name = "Fishing", desc = "Fishing stuff.")
    public Fishing fishing = new Fishing();

    @Expose
    @Category(name = "Misc", desc = "Settings without a category.")
    public Misc misc = new Misc();

    @Expose
    @Category(name = "Apis", desc = "Api Data")
    public ApiData apiData = new ApiData();

    @Expose
    @Category(name = "Debug", desc = "Debug and test stuff.")
    public Debug debug = new Debug();






}
