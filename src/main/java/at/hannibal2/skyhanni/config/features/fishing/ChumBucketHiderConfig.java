package at.hannibal2.skyhanni.config.features.fishing;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class ChumBucketHiderConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Hide the Chum/Chumcap Bucket name tags for other players.")
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> enabled = Property.of(true);

    @Expose
    @ConfigOption(name = "Hide Bucket", desc = "Hide the Chum/Chumcap Bucket.")
    @ConfigEditorBoolean
    public Property<Boolean> hideBucket = Property.of(false);

    @Expose
    @ConfigOption(name = "Hide Own", desc = "Hides your own Chum/Chumcap Bucket.")
    @ConfigEditorBoolean
    public Property<Boolean> hideOwn = Property.of(false);
}
