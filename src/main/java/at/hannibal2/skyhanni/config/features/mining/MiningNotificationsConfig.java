package at.hannibal2.skyhanni.config.features.mining;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.features.mining.MiningNotifications.NotificationList;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MiningNotificationsConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Toggles the Mining Notifications")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(
            name = "Notification List",
            desc = "Drag text to change which events send a title."
    )
    @ConfigEditorDraggableList
    public List<NotificationList> notifications = new ArrayList<>(Arrays.asList(
            NotificationList.MINESHAFT_SPAWN,
            NotificationList.SCRAP,
            NotificationList.GOLDEN_GOBLIN,
            NotificationList.DIAMOND_GOBLIN
    ));

    @Expose
    @ConfigOption(name = "Play Sound", desc = "Plays a ding when a notification is triggered")
    @ConfigEditorBoolean
    public boolean playSound = true;

}
