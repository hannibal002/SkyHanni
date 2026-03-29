package at.hannibal2.skyhanni.utils

import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.client.KeyMapping

/**
 * Registers a SkyHanni config keybind with Minecraft's vanilla keybind system,
 * allowing it to appear in the controls screen and stay in sync with MoulConfig.
 *
 * Create instances in `init` blocks of `@SkyHanniModule` objects. For abstract or open
 * classes that define keybinds, create instances in a companion object instead to avoid
 * registering duplicate binds per subclass instance.
 *
 * @param displayName The human-readable name shown in the controls screen.
 * @param keyProvider Lambda returning the config `Property<Int>` backing this keybind.
 * @param category The controls screen grouping label (e.g. "Inventory", "Garden").
 * @param condition Returns true when this keybind should appear in the controls screen.
 */
class SkyHanniKeyBind(
    val displayName: String,
    val keyProvider: () -> Property<Int>,
    val category: String,
    val condition: () -> Boolean,
) {
    init {
        SkyHanniKeyBindManager.register(this)
    }

    internal var keyMapping: KeyMapping? = null
}
