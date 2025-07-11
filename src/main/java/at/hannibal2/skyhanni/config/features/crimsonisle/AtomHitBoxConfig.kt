package at.hannibal2.skyhanni.config.features.crimsonisle

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.nether.AtomHitBox
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class AtomHitBoxConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show Exes, Wais and Zees hitbox.")
    @ConfigEditorBoolean
    @SearchTag("atom x y z xyz")
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Select Atoms", desc = "Select which atoms you want to show the hitbox for.")
    @ConfigEditorDraggableList
    val atomsEntries: MutableList<AtomHitBox.AtomType> = mutableListOf(
        AtomHitBox.AtomType.EXE,
        AtomHitBox.AtomType.WAI,
        AtomHitBox.AtomType.ZEE,
    )
}
