package de.hype.bingonet.shared.constants

/**
 * Enumeration representing various ChChest items in the game.
 * These constants define specific ChChest items that players can obtain.
 * Use these constants to refer to specific ChChest items. For non-listed use the Custom Value. Example usage below.
 *
 *
 * The available ChChest items are:
 *
 *  * `PrehistoricEgg`: Represents a prehistoric egg item.
 *  * `Pickonimbus2000`: Represents the Pickonimbus 2000 item.
 *  * `ControlSwitch`: Represents a control switch item.
 *  * `ElectronTransmitter`: Represents an electron transmitter item.
 *  * `FTX3070`: Represents the FTX 3070 item.
 *  * `RobotronReflector`: Represents the Robotron Reflector item.
 *  * `SuperliteMotor`: Represents a Superlite Motor item.
 *  * `SyntheticHeart`: Represents a synthetic heart item.
 *  * `FlawlessGemstone`: Represents a flawless gemstone item.
 *  * `JungleHeart`: Represents a Jungle Heart item.
 *
 * How to create a Custom Enum:
 * <pre>
 * `new ChChestItem("(Your Item name)")`
 * Make sure too use the EXACT display name!
</pre> *
 */
enum class ValueableChChestItem(val displayName: String, val iconPath: String) {
    PrehistoricEgg("Prehistoric Egg", "prehistoric_egg"),

    Pickonimbus2000("Pickonimbus 2000", "pickonimbus"),

    ControlSwitch("Control Switch", "control_switch"),

    ElectronTransmitter("Electron Transmitter", "electron_transmitter"),

    FTX3070("FTX 3070", "ftx_3070"),

    RobotronReflector("Robotron Reflector", "robotron_reflector"),

    SuperliteMotor("Superlite Motor", "superlite_motor"),

    SyntheticHeart("Synthetic Heart", "synthetic_heart"),

    FlawlessGemstone("Flawless Gemstone", "flawless_gemstone"),
    GEMSTONE_POWDER("Gemstone Powder", "legendary_gemstone_powder"),
    MITHRIL_POWDER("Mithril Powder", "legendary_mithril_powder");

    companion object {
        fun get(displayName: String, count: IntRange): ValueableChChestItem? {
            if (displayName.startsWith("Flawless") && displayName.endsWith("Gemstone")) {
                return FlawlessGemstone
            }
            if (displayName.contains("Gemstone Powder")) {
                if (count.first > 3_000) return GEMSTONE_POWDER
                else return null
            }
            if (displayName.contains("Mithril Powder")) {
                if (count.first > 3_000) return MITHRIL_POWDER
                else return null
            }
            return entries.firstOrNull { it.displayName == displayName }
        }
    }

    val isFlawlessGemstone: Boolean
        get() = this == ValueableChChestItem.FlawlessGemstone

    val isRoboPart: Boolean
        get() {
            this in listOf(
                ValueableChChestItem.ControlSwitch,
                ValueableChChestItem.ElectronTransmitter,
                ValueableChChestItem.FTX3070,
                ValueableChChestItem.RobotronReflector,
                ValueableChChestItem.SuperliteMotor,
                ValueableChChestItem.SyntheticHeart
            )
            return false
        }

    val isPowder: Boolean
        get() = this == ValueableChChestItem.GEMSTONE_POWDER ||
                this == ValueableChChestItem.MITHRIL_POWDER

}
