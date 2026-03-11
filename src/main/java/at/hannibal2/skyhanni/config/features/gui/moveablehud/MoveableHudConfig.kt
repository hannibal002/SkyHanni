package at.hannibal2.skyhanni.config.features.gui.moveablehud

import at.hannibal2.skyhanni.config.core.config.Position

interface MoveableHudConfig {
    var enabled: Boolean
    val position: Position
    var showOutsideSkyblock: Boolean
}
