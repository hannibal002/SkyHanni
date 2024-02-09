package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class MiningEventConfig {

//     @Expose
//     @ConfigOption(name = "Enabled", desc = "Show information about upcoming Dwarven Mines and Crystal Hollows mining events, also enables you sending data.")
//     @ConfigEditorBoolean
//     @FeatureToggle
//     public boolean enabled = true;
//
//     @Expose
//     @ConfigOption(name = "Show Outside Mining Islands", desc = "Shows the event tracker when you are not inside of the Dwarven Mines or Crystal Hollows.")
//     @ConfigEditorBoolean
//     public boolean outsideMining = true;
//
//     @Expose
//     @ConfigOption(name = "What to Show", desc = "Choose which island's events are shown in the gui.")
//     @ConfigEditorDropdown
//     public ShowType showType = ShowType.BOTH;
//
//     @Expose
//     @ConfigOption(name = "Show Warnings For Events", desc = "Shows the warnings when select mining events are about to start.")
//     @ConfigEditorBoolean
//     @FeatureToggle
//     public boolean showWarnings = false;

    //todo remove when released
    @Expose
    @ConfigOption(name = "Send Test data", desc = "Sends test data to make sure the api works.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean sendData = true;

//     @Expose
//     @ConfigOption(name = "Events to Warn for", desc = "Choose which mining events you get warned about.")
//     @ConfigEditorDraggableList
//     public List<MiningEvent> eventsToWarn = new ArrayList<>(Collections.singletonList(MiningEvent.DOUBLE_POWDER));

    public enum ShowType {
        BOTH("Both Mining Islands"),
        CRYSTAL("Crystal Hollows Only"),
        DWARVEN("Dwarven Mines Only")
        ;

        private final String str;

        ShowType(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
