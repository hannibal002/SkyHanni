package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.utils.RenderDisplayHelper.Companion.NO_INVENTORY

/**
 * Bundles the render visibility parameters for a [RenderDisplayHelper].
 *
 * Use this instead of passing six separate arguments to [RenderDisplayHelper] directly.
 * Tracker subclasses override [at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker.renderConfig] with a single property
 * rather than overriding six separate open vals.
 *
 * @property inventoryDetector an [InventoryDetector] the display should render inside.
 *   Defaults to [NO_INVENTORY], meaning no specific inventory triggers rendering.
 * @property outsideInventory whether the display renders when no inventory GUI is open.
 * @property inOwnInventory whether the display renders when the player's own inventory is open.
 * @property condition extra render gate; the display only renders when this returns true.
 *   Typically, this should wrap the feature's isEnabled() check (or its parallel),
 *   but it may also include feature specific condition checking, etc.
 * @property onlyOnIsland restrict rendering to a specific Skyblock island, or null for any island.
 * @property onlyOnIslandTag restrict rendering to islands with a specific tag, or null for any.
 * @property onRender block to run when the config is used to render.
 */
class RenderDisplayConfig(
    val inventoryDetector: InventoryDetector = NO_INVENTORY,
    val outsideInventory: Boolean = false,
    val inOwnInventory: Boolean = false,
    val condition: () -> Boolean = { true },
    val onlyOnIsland: IslandType? = null,
    val onlyOnIslandTag: IslandTypeTag? = null,
    val onRender: () -> Unit = { },
)
